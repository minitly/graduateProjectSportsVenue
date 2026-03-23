<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { NAlert, NButton, NCard, NInput, NInputNumber, NSelect, NSpace, NSwitch, NTag } from 'naive-ui'

const STORAGE_KEY = 'sports-venue-2d-model-v33'
const CANVAS_WIDTH = 1200
const CANVAS_HEIGHT = 760

const modelConfig = reactive({
  modelName: '综合体育馆主场模型',
  gridSize: 20,
  showGrid: true,
  snapToGrid: true,
  tool: 'select',
  activeLayerId: 'layer-zones',
  semanticType: 'COURT',
  seatCount: 300,
  laneWidth: 16,
  arcSpan: 140,
  seatPerSqm: 0.45,
  gateFlowPerMeter: 75,
  pathFlowPerMeter: 55,
  walkSpeedMpm: 72,
  sectorCount: 6,
  ringOffset: 42,
  ringWidth: 20,
  radialCorridorCount: 8,
  complexGateCount: 8,
  standRingCount: 2,
  show3DPreview: true,
  extrusionDepth: 26,
  obliqueAngle: 38
})

const state = reactive({
  layers: [
    { id: 'layer-base', name: '底图层', visible: true, locked: true },
    { id: 'layer-zones', name: '功能区层', visible: true, locked: false },
    { id: 'layer-paths', name: '通道层', visible: true, locked: false },
    { id: 'layer-gates', name: '出入口层', visible: true, locked: false },
    { id: 'layer-links', name: '连通关系层', visible: true, locked: false }
  ],
  entities: [],
  links: [],
  drawingPoints: [],
  selectedEntityIds: [],
  draggingEntities: null,
  message: '',
  jsonText: ''
})

const history = reactive({ undoStack: [], redoStack: [], restoring: false })
const canvasRef = ref(null)
const router = useRouter()

const toolOptions = [
  { label: '选择', value: 'select' },
  { label: '多边形功能区', value: 'draw-polygon' },
  { label: '折线通道', value: 'draw-polyline' },
  { label: '弧形看台/跑道', value: 'draw-arc' }
]

const semanticOptions = [
  { label: '比赛场地', value: 'COURT' },
  { label: '看台', value: 'STAND' },
  { label: '出入口', value: 'GATE' },
  { label: '疏散区', value: 'EVAC' },
  { label: '服务区', value: 'SERVICE' }
]

const layerOptions = computed(() => state.layers.map((l) => ({ label: l.name, value: l.id })))
const canUndo = computed(() => history.undoStack.length > 0)
const canRedo = computed(() => history.redoStack.length > 0)

const visibleEntities = computed(() =>
  state.entities.filter((entity) => {
    const layer = state.layers.find((l) => l.id === entity.layerId)
    return layer?.visible
  })
)

const selectedEntities = computed(() => state.entities.filter((e) => state.selectedEntityIds.includes(e.id)))
const selectedPrimary = computed(() => selectedEntities.value[0] || null)

const metrics = computed(() => {
  const zones = state.entities.filter((e) => e.kind === 'polygon')
  const paths = state.entities.filter((e) => e.kind === 'polyline')
  const arcs = state.entities.filter((e) => e.kind === 'arc')
  const stands = state.entities.filter((z) => z.semanticType === 'STAND')
  const gates = state.entities.filter((z) => z.semanticType === 'GATE')
  const capacity = stands.reduce((sum, s) => sum + Number(s.seatCount || 0), 0)
  const zoneArea = zones.reduce((sum, z) => sum + polygonArea(z.points), 0)
  const pathLen = paths.reduce((sum, p) => sum + polylineLength(p.points), 0)
  const arcLen = arcs.reduce((sum, a) => sum + arcLength(a), 0)
  return {
    zones: zones.length,
    paths: paths.length,
    arcs: arcs.length,
    gates: gates.length,
    totalCapacity: capacity,
    totalArea: zoneArea,
    totalPathLength: pathLen + arcLen
  }
})

const evacuationEstimate = computed(() => {
  const gates = state.entities.filter((e) => e.semanticType === 'GATE')
  const gateWidthSum = gates.reduce((sum, g) => sum + gateWidthMeters(g), 0)
  const paths = state.entities.filter((e) => e.kind === 'polyline' && e.semanticType === 'EVAC')
  const pathWidthSum = paths.reduce((sum, p) => sum + pxToMeter(p.laneWidth || 8), 0)

  const gateFlow = gateWidthSum * modelConfig.gateFlowPerMeter
  const pathFlow = pathWidthSum * modelConfig.pathFlowPerMeter
  const bottleneck = Math.max(1, Math.min(gateFlow, pathFlow))

  const stands = state.entities.filter((e) => e.semanticType === 'STAND')
  const times = stands.map((s) => estimateStandEvacMinutes(s, gates))
  const maxWalk = times.length ? Math.max(...times) : 0
  const throughputTime = metrics.value.totalCapacity ? metrics.value.totalCapacity / bottleneck : 0

  return {
    gateWidthSum,
    pathWidthSum,
    gateFlow,
    pathFlow,
    bottleneck,
    throughputTime,
    maxWalk,
    totalMinutes: Math.max(maxWalk, throughputTime)
  }
})

const validationIssues = computed(() => {
  const issues = []
  if (!state.entities.some((e) => e.semanticType === 'COURT')) issues.push('缺少比赛场地（COURT）')
  if (!state.entities.some((e) => e.semanticType === 'GATE')) issues.push('缺少出入口（GATE）')
  if (!state.entities.some((e) => e.semanticType === 'EVAC')) issues.push('缺少疏散通道（EVAC）')
  if (!state.links.length) issues.push('未建立分区-通道-出口连通关系（可点击“自动生成连通关系”）')
  const narrowPaths = state.entities.filter((e) => e.kind === 'polyline' && (e.laneWidth || 0) < 12)
  if (narrowPaths.length) issues.push(`存在 ${narrowPaths.length} 条通道宽度过小（< 12px）`)
  if (metrics.value.totalCapacity > 0 && evacuationEstimate.value.totalMinutes > 8) {
    issues.push(`预计疏散时间 ${evacuationEstimate.value.totalMinutes.toFixed(1)} 分钟，建议优化（目标 < 8 分钟）`)
  }
  return issues
})

const selectedSummary = computed(() => {
  if (!selectedPrimary.value) return '未选择实体'
  if (state.selectedEntityIds.length > 1) return `已选择 ${state.selectedEntityIds.length} 个实体，可批量移动`
  if (selectedPrimary.value.kind === 'polygon') {
    return `${selectedPrimary.value.name}｜${selectedPrimary.value.semanticType}｜面积 ${polygonArea(selectedPrimary.value.points).toFixed(1)}㎡`
  }
  if (selectedPrimary.value.kind === 'polyline') {
    return `${selectedPrimary.value.name}｜通道长度 ${polylineLength(selectedPrimary.value.points).toFixed(1)}m`
  }
  return `${selectedPrimary.value.name}｜弧长 ${arcLength(selectedPrimary.value).toFixed(1)}m`
})

const visibleLinks = computed(() => {
  const linkLayer = state.layers.find((x) => x.id === 'layer-links')
  if (!linkLayer?.visible) return []
  return state.links
    .map((l) => {
      const from = state.entities.find((e) => e.id === l.fromId)
      const to = state.entities.find((e) => e.id === l.toId)
      if (!from || !to) return null
      return { id: l.id, from: entityCenter(from), to: entityCenter(to) }
    })
    .filter(Boolean)
})

const preview3DShapes = computed(() => {
  const angle = (modelConfig.obliqueAngle * Math.PI) / 180
  const kx = Math.cos(angle)
  const ky = Math.sin(angle)

  const semanticHeight = {
    COURT: 8,
    STAND: 52,
    GATE: 16,
    EVAC: 6,
    SERVICE: 14
  }

  const toFootprint = (entity) => {
    if (entity.kind === 'polygon') return entity.points || []
    if (entity.kind === 'arc') {
      const steps = 24
      const start = entity.startAngle || 0
      const end = entity.endAngle || 0
      const outer = entity.radius + (entity.laneWidth || 12) / 2
      const inner = Math.max(6, entity.radius - (entity.laneWidth || 12) / 2)
      const outerPts = []
      const innerPts = []
      for (let i = 0; i <= steps; i += 1) {
        const a = start + ((end - start) * i) / steps
        outerPts.push(polarToCartesian(entity.cx, entity.cy, outer, a))
      }
      for (let i = steps; i >= 0; i -= 1) {
        const a = start + ((end - start) * i) / steps
        innerPts.push(polarToCartesian(entity.cx, entity.cy, inner, a))
      }
      return [...outerPts, ...innerPts]
    }
    return []
  }

  const project = (p, z) => ({ x: p.x + z * kx, y: p.y - z * ky })

  const tops = []
  const sides = []
  const grounds = []

  visibleEntities.value.forEach((entity) => {
    const footprint = toFootprint(entity)
    if (!footprint.length) return
    const h = (semanticHeight[entity.semanticType] || 10) * (modelConfig.extrusionDepth / 26)

    const g = footprint.map((p) => `${p.x},${p.y}`).join(' ')
    const t = footprint.map((p) => project(p, h)).map((p) => `${p.x},${p.y}`).join(' ')
    grounds.push({ id: `${entity.id}-g`, points: g, color: `${semanticColor(entity.semanticType)}22` })
    tops.push({ id: `${entity.id}-t`, points: t, color: `${semanticColor(entity.semanticType)}88` })

    for (let i = 0; i < footprint.length; i += 1) {
      const a = footprint[i]
      const b = footprint[(i + 1) % footprint.length]
      const ap = project(a, h)
      const bp = project(b, h)
      const side = `${a.x},${a.y} ${b.x},${b.y} ${bp.x},${bp.y} ${ap.x},${ap.y}`
      sides.push({ id: `${entity.id}-s-${i}`, points: side, color: `${semanticColor(entity.semanticType)}44` })
    }
  })

  return { grounds, tops, sides }
})

watch(
  () => ({
    modelName: modelConfig.modelName,
    gridSize: modelConfig.gridSize,
    showGrid: modelConfig.showGrid,
    snapToGrid: modelConfig.snapToGrid,
    layers: state.layers,
    entities: state.entities,
    links: state.links
  }),
  () => {
    if (history.restoring) return
    recordSnapshot()
  },
  { deep: true }
)

function nextId(prefix) {
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1000)}`
}

function clamp(v, min, max) {
  return Math.max(min, Math.min(max, v))
}

function snapToGrid(value) {
  if (!modelConfig.snapToGrid) return Math.round(value)
  return Math.round(value / modelConfig.gridSize) * modelConfig.gridSize
}

function getCanvasPoint(event) {
  const host = canvasRef.value
  if (!host) return { x: 0, y: 0 }
  const rect = host.getBoundingClientRect()
  return {
    x: clamp(snapToGrid(event.clientX - rect.left), 0, CANVAS_WIDTH),
    y: clamp(snapToGrid(event.clientY - rect.top), 0, CANVAS_HEIGHT)
  }
}

function isLayerEditable(layerId) {
  const layer = state.layers.find((l) => l.id === layerId)
  return Boolean(layer && layer.visible && !layer.locked)
}

function canDraw() {
  return isLayerEditable(modelConfig.activeLayerId)
}

function snapshot() {
  return JSON.parse(
    JSON.stringify({
      modelName: modelConfig.modelName,
      gridSize: modelConfig.gridSize,
      showGrid: modelConfig.showGrid,
      snapToGrid: modelConfig.snapToGrid,
      layers: state.layers,
      entities: state.entities,
      links: state.links
    })
  )
}

function recordSnapshot() {
  const snap = snapshot()
  const last = history.undoStack[history.undoStack.length - 1]
  if (last && JSON.stringify(last) === JSON.stringify(snap)) return
  history.undoStack.push(snap)
  if (history.undoStack.length > 80) history.undoStack.shift()
  history.redoStack = []
}

function restoreFrom(snap) {
  history.restoring = true
  modelConfig.modelName = snap.modelName
  modelConfig.gridSize = snap.gridSize
  modelConfig.showGrid = snap.showGrid
  modelConfig.snapToGrid = snap.snapToGrid
  state.layers = snap.layers
  state.entities = snap.entities
  state.links = snap.links || []
  state.selectedEntityIds = []
  state.drawingPoints = []
  history.restoring = false
}

function undo() {
  if (!canUndo.value) return
  const current = snapshot()
  const prev = history.undoStack.pop()
  if (!prev) return
  history.redoStack.push(current)
  restoreFrom(prev)
  state.message = '已撤销'
}

function redo() {
  if (!canRedo.value) return
  const current = snapshot()
  const next = history.redoStack.pop()
  if (!next) return
  history.undoStack.push(current)
  restoreFrom(next)
  state.message = '已重做'
}

function semanticColor(type) {
  const map = { COURT: '#2563eb', STAND: '#7c3aed', GATE: '#dc2626', EVAC: '#d97706', SERVICE: '#059669' }
  return map[type] || '#334155'
}

function pointsToPath(points, closed) {
  if (!points?.length) return ''
  const first = points[0]
  let d = `M ${first.x} ${first.y}`
  for (let i = 1; i < points.length; i += 1) d += ` L ${points[i].x} ${points[i].y}`
  if (closed) d += ' Z'
  return d
}

function arcToPath(arc) {
  const start = polarToCartesian(arc.cx, arc.cy, arc.radius, arc.startAngle)
  const end = polarToCartesian(arc.cx, arc.cy, arc.radius, arc.endAngle)
  const largeArcFlag = Math.abs(arc.endAngle - arc.startAngle) > 180 ? 1 : 0
  const sweepFlag = arc.endAngle > arc.startAngle ? 1 : 0
  return `M ${start.x} ${start.y} A ${arc.radius} ${arc.radius} 0 ${largeArcFlag} ${sweepFlag} ${end.x} ${end.y}`
}

function polarToCartesian(cx, cy, r, degree) {
  const rad = (degree * Math.PI) / 180
  return { x: cx + r * Math.cos(rad), y: cy + r * Math.sin(rad) }
}

function pxToMeter(px) {
  return px / 20
}

function polylineLength(points) {
  if (!Array.isArray(points) || points.length < 2) return 0
  let sum = 0
  for (let i = 1; i < points.length; i += 1) {
    const dx = points[i].x - points[i - 1].x
    const dy = points[i].y - points[i - 1].y
    sum += Math.sqrt(dx * dx + dy * dy)
  }
  return pxToMeter(sum)
}

function polygonArea(points) {
  if (!Array.isArray(points) || points.length < 3) return 0
  let sum = 0
  for (let i = 0; i < points.length; i += 1) {
    const p1 = points[i]
    const p2 = points[(i + 1) % points.length]
    sum += p1.x * p2.y - p2.x * p1.y
  }
  return Math.abs(sum) / 2 / 100
}

function arcLength(arc) {
  const angle = Math.abs((arc.endAngle || 0) - (arc.startAngle || 0))
  return pxToMeter((2 * Math.PI * arc.radius) * (angle / 360))
}

function gateWidthMeters(entity) {
  if (entity.kind !== 'polygon' || !entity.points?.length) return 0
  const xs = entity.points.map((p) => p.x)
  const ys = entity.points.map((p) => p.y)
  const w = Math.max(...xs) - Math.min(...xs)
  const h = Math.max(...ys) - Math.min(...ys)
  return pxToMeter(Math.min(w, h))
}

function entityCenter(entity) {
  if (entity.kind === 'arc') return { x: entity.cx, y: entity.cy }
  if (!entity.points?.length) return { x: 0, y: 0 }
  const xs = entity.points.map((p) => p.x)
  const ys = entity.points.map((p) => p.y)
  return { x: (Math.min(...xs) + Math.max(...xs)) / 2, y: (Math.min(...ys) + Math.max(...ys)) / 2 }
}

function distance(a, b) {
  const dx = a.x - b.x
  const dy = a.y - b.y
  return Math.sqrt(dx * dx + dy * dy)
}

function estimateStandEvacMinutes(stand, gates) {
  if (!gates.length) return 0
  const c = entityCenter(stand)
  let minDist = Number.POSITIVE_INFINITY
  gates.forEach((g) => {
    const d = distance(c, entityCenter(g))
    if (d < minDist) minDist = d
  })
  return pxToMeter(minDist) / Math.max(1, modelConfig.walkSpeedMpm)
}

function onCanvasClick(event) {
  const point = getCanvasPoint(event)
  if (!canDraw()) {
    state.message = '当前图层不可编辑'
    return
  }
  if (modelConfig.tool === 'draw-polygon' || modelConfig.tool === 'draw-polyline') {
    state.drawingPoints.push(point)
    return
  }
  if (modelConfig.tool === 'draw-arc') {
    state.drawingPoints.push(point)
    if (state.drawingPoints.length === 2) {
      const c = state.drawingPoints[0]
      const p = state.drawingPoints[1]
      const radius = Math.max(20, Math.sqrt((p.x - c.x) ** 2 + (p.y - c.y) ** 2))
      state.entities.push({
        id: nextId('a'),
        kind: 'arc',
        layerId: modelConfig.activeLayerId,
        name: `弧形-${state.entities.length + 1}`,
        semanticType: modelConfig.semanticType,
        cx: c.x,
        cy: c.y,
        radius,
        startAngle: -modelConfig.arcSpan / 2,
        endAngle: modelConfig.arcSpan / 2,
        laneWidth: modelConfig.laneWidth,
        seatCount: modelConfig.semanticType === 'STAND' ? modelConfig.seatCount : 0
      })
      state.drawingPoints = []
      state.message = '弧形构件创建成功'
    }
    return
  }
  if (modelConfig.tool === 'select') {
    state.selectedEntityIds = []
  }
}

function finishDrawing() {
  if (!canDraw()) return
  if (modelConfig.tool === 'draw-polygon') {
    if (state.drawingPoints.length < 3) {
      state.message = '多边形至少需要3个点'
      return
    }
    state.entities.push({
      id: nextId('p'),
      kind: 'polygon',
      layerId: modelConfig.activeLayerId,
      name: `${modelConfig.semanticType}-${state.entities.length + 1}`,
      semanticType: modelConfig.semanticType,
      seatCount: modelConfig.semanticType === 'STAND' ? modelConfig.seatCount : 0,
      points: state.drawingPoints.map((p) => ({ ...p }))
    })
    state.drawingPoints = []
    state.message = '功能区创建完成'
  }
  if (modelConfig.tool === 'draw-polyline') {
    if (state.drawingPoints.length < 2) {
      state.message = '折线至少需要2个点'
      return
    }
    state.entities.push({
      id: nextId('l'),
      kind: 'polyline',
      layerId: modelConfig.activeLayerId,
      name: `通道-${state.entities.length + 1}`,
      semanticType: 'EVAC',
      laneWidth: modelConfig.laneWidth,
      points: state.drawingPoints.map((p) => ({ ...p }))
    })
    state.drawingPoints = []
    state.message = '通道创建完成'
  }
}

function cancelDrawing() {
  state.drawingPoints = []
}

function selectEntity(id, event) {
  if (event?.shiftKey) {
    if (state.selectedEntityIds.includes(id)) {
      state.selectedEntityIds = state.selectedEntityIds.filter((x) => x !== id)
    } else {
      state.selectedEntityIds = [...state.selectedEntityIds, id]
    }
  } else {
    state.selectedEntityIds = [id]
  }
}

function onEntityMouseDown(id, event) {
  if (modelConfig.tool !== 'select') return
  const entity = state.entities.find((e) => e.id === id)
  if (!entity || !isLayerEditable(entity.layerId)) return
  if (!state.selectedEntityIds.includes(id)) state.selectedEntityIds = [id]
  const start = getCanvasPoint(event)
  const items = state.selectedEntityIds
    .map((eid) => state.entities.find((e) => e.id === eid))
    .filter((e) => e && isLayerEditable(e.layerId))
    .map((e) => ({ id: e.id, copy: JSON.parse(JSON.stringify(e)) }))
  state.draggingEntities = { start, items }
}

function onCanvasMouseMove(event) {
  if (!state.draggingEntities) return
  const point = getCanvasPoint(event)
  const dx = point.x - state.draggingEntities.start.x
  const dy = point.y - state.draggingEntities.start.y
  state.draggingEntities.items.forEach((item) => {
    const entity = state.entities.find((e) => e.id === item.id)
    if (!entity) return
    if (entity.kind === 'arc') {
      entity.cx = clamp(snapToGrid(item.copy.cx + dx), 0, CANVAS_WIDTH)
      entity.cy = clamp(snapToGrid(item.copy.cy + dy), 0, CANVAS_HEIGHT)
      return
    }
    entity.points = item.copy.points.map((p) => ({
      x: clamp(snapToGrid(p.x + dx), 0, CANVAS_WIDTH),
      y: clamp(snapToGrid(p.y + dy), 0, CANVAS_HEIGHT)
    }))
  })
}

function onCanvasMouseUp() {
  state.draggingEntities = null
}

function deleteSelected() {
  if (!state.selectedEntityIds.length) return
  const removeSet = new Set(state.selectedEntityIds)
  state.entities = state.entities.filter((e) => !removeSet.has(e.id))
  state.links = state.links.filter((l) => !removeSet.has(l.fromId) && !removeSet.has(l.toId))
  state.selectedEntityIds = []
  state.message = '已删除选中实体'
}

function autoSeatSelectedStand() {
  if (!selectedPrimary.value || selectedPrimary.value.semanticType !== 'STAND') {
    state.message = '请先选择一个看台实体（STAND）'
    return
  }
  const e = selectedPrimary.value
  const area = e.kind === 'polygon' ? polygonArea(e.points) : arcLength(e) * pxToMeter(e.laneWidth || 16)
  e.seatCount = Math.max(0, Math.round(area * modelConfig.seatPerSqm))
  state.message = `已自动排座：${e.seatCount} 座`
}

function applySemanticToSelected() {
  if (!selectedPrimary.value) {
    state.message = '请先选择一个实体，再应用语义'
    return
  }
  selectedPrimary.value.semanticType = modelConfig.semanticType
  if (modelConfig.semanticType === 'STAND' && !selectedPrimary.value.seatCount) {
    selectedPrimary.value.seatCount = modelConfig.seatCount
  }
  if (selectedPrimary.value.kind === 'polyline' || selectedPrimary.value.kind === 'arc') {
    selectedPrimary.value.laneWidth = modelConfig.laneWidth
  }
  state.message = `已应用语义：${modelConfig.semanticType}`
}

function generateStandSectors() {
  const base = selectedPrimary.value
  if (!base || base.kind !== 'arc' || base.semanticType !== 'STAND') {
    state.message = '请先选择一个弧形看台（arc + STAND）'
    return
  }
  const totalSpan = base.endAngle - base.startAngle
  const part = totalSpan / Math.max(1, modelConfig.sectorCount)
  const sectors = []
  for (let i = 0; i < modelConfig.sectorCount; i += 1) {
    sectors.push({
      id: nextId('stand-sector'),
      kind: 'arc',
      layerId: base.layerId,
      name: `${base.name}-分区${i + 1}`,
      semanticType: 'STAND',
      cx: base.cx,
      cy: base.cy,
      radius: base.radius,
      startAngle: base.startAngle + i * part,
      endAngle: base.startAngle + (i + 1) * part,
      laneWidth: base.laneWidth,
      seatCount: Math.round((base.seatCount || modelConfig.seatCount) / modelConfig.sectorCount)
    })
  }
  state.entities = state.entities.filter((e) => e.id !== base.id).concat(sectors)
  state.selectedEntityIds = sectors.map((x) => x.id)
  state.message = `已生成 ${sectors.length} 个看台扇区`
}

function generateRingCorridor() {
  const court = state.entities.find((e) => e.semanticType === 'COURT' && e.kind === 'polygon')
  if (!court) {
    state.message = '未找到比赛场地（COURT polygon）作为环道基准'
    return
  }
  const xs = court.points.map((p) => p.x)
  const ys = court.points.map((p) => p.y)
  const minX = Math.min(...xs) - modelConfig.ringOffset
  const maxX = Math.max(...xs) + modelConfig.ringOffset
  const minY = Math.min(...ys) - modelConfig.ringOffset
  const maxY = Math.max(...ys) + modelConfig.ringOffset

  state.entities.push({
    id: nextId('ring'),
    kind: 'polyline',
    layerId: 'layer-paths',
    name: '环形疏散通道',
    semanticType: 'EVAC',
    laneWidth: modelConfig.ringWidth,
    points: [
      { x: clamp(minX, 0, CANVAS_WIDTH), y: clamp(minY, 0, CANVAS_HEIGHT) },
      { x: clamp(maxX, 0, CANVAS_WIDTH), y: clamp(minY, 0, CANVAS_HEIGHT) },
      { x: clamp(maxX, 0, CANVAS_WIDTH), y: clamp(maxY, 0, CANVAS_HEIGHT) },
      { x: clamp(minX, 0, CANVAS_WIDTH), y: clamp(maxY, 0, CANVAS_HEIGHT) },
      { x: clamp(minX, 0, CANVAS_WIDTH), y: clamp(minY, 0, CANVAS_HEIGHT) }
    ]
  })
  state.message = '已生成环形通道'
}

function autoBuildLinks() {
  const stands = state.entities.filter((e) => e.semanticType === 'STAND')
  const paths = state.entities.filter((e) => e.kind === 'polyline' && e.semanticType === 'EVAC')
  const gates = state.entities.filter((e) => e.semanticType === 'GATE')
  if (!stands.length || !paths.length || !gates.length) {
    state.message = '生成连通关系需要至少存在看台、通道和出入口'
    return
  }

  const links = []
  stands.forEach((s) => {
    const nearestPath = nearestEntity(s, paths)
    if (nearestPath) links.push({ id: nextId('link'), fromId: s.id, toId: nearestPath.id })
  })
  paths.forEach((p) => {
    const nearestGate = nearestEntity(p, gates)
    if (nearestGate) links.push({ id: nextId('link'), fromId: p.id, toId: nearestGate.id })
  })
  state.links = links
  state.message = `已生成 ${links.length} 条连通关系`
}

function generateComplexArena() {
  const cx = 600
  const cy = 380
  const baseRadius = 185
  const ringGap = 42
  const standLane = 20

  const entities = []

  entities.push({
    id: nextId('court-main'),
    kind: 'polygon',
    layerId: 'layer-zones',
    name: '足球主赛场',
    semanticType: 'COURT',
    seatCount: 0,
    points: [
      { x: 430, y: 270 },
      { x: 770, y: 270 },
      { x: 770, y: 490 },
      { x: 430, y: 490 }
    ]
  })

  entities.push({
    id: nextId('service-core'),
    kind: 'polygon',
    layerId: 'layer-zones',
    name: '内场服务区',
    semanticType: 'SERVICE',
    seatCount: 0,
    points: [
      { x: 520, y: 320 },
      { x: 680, y: 320 },
      { x: 680, y: 440 },
      { x: 520, y: 440 }
    ]
  })

  entities.push({
    id: nextId('basketball-court'),
    kind: 'polygon',
    layerId: 'layer-zones',
    name: '副馆篮球场',
    semanticType: 'COURT',
    seatCount: 0,
    points: [
      { x: 840, y: 300 },
      { x: 1040, y: 300 },
      { x: 1040, y: 430 },
      { x: 840, y: 430 }
    ]
  })

  entities.push({
    id: nextId('basketball-stand'),
    kind: 'arc',
    layerId: 'layer-zones',
    name: '副馆看台',
    semanticType: 'STAND',
    cx: 940,
    cy: 365,
    radius: 108,
    startAngle: 210,
    endAngle: 330,
    laneWidth: 16,
    seatCount: 900
  })

  for (let ring = 0; ring < modelConfig.standRingCount; ring += 1) {
    const radius = baseRadius + ring * ringGap
    const spanPerSector = 360 / modelConfig.sectorCount
    for (let i = 0; i < modelConfig.sectorCount; i += 1) {
      const start = i * spanPerSector + 5
      const end = (i + 1) * spanPerSector - 5
      entities.push({
        id: nextId('stand'),
        kind: 'arc',
        layerId: 'layer-zones',
        name: `R${ring + 1}-看台分区${i + 1}`,
        semanticType: 'STAND',
        cx,
        cy,
        radius,
        startAngle: start,
        endAngle: end,
        laneWidth: standLane,
        seatCount: Math.round((modelConfig.seatCount * (ring + 1)) / 1.5)
      })
    }
  }

  const runningRadiusInner = baseRadius - 34
  const runningRadiusOuter = runningRadiusInner + 26
  entities.push({
    id: nextId('track-inner-a'),
    kind: 'arc',
    layerId: 'layer-paths',
    name: '跑道内弧-北',
    semanticType: 'EVAC',
    cx,
    cy,
    radius: runningRadiusInner,
    startAngle: 200,
    endAngle: 340,
    laneWidth: 8,
    seatCount: 0
  })
  entities.push({
    id: nextId('track-inner-b'),
    kind: 'arc',
    layerId: 'layer-paths',
    name: '跑道内弧-南',
    semanticType: 'EVAC',
    cx,
    cy,
    radius: runningRadiusInner,
    startAngle: 20,
    endAngle: 160,
    laneWidth: 8,
    seatCount: 0
  })
  entities.push({
    id: nextId('track-outer-a'),
    kind: 'arc',
    layerId: 'layer-paths',
    name: '跑道外弧-北',
    semanticType: 'EVAC',
    cx,
    cy,
    radius: runningRadiusOuter,
    startAngle: 200,
    endAngle: 340,
    laneWidth: 8,
    seatCount: 0
  })
  entities.push({
    id: nextId('track-outer-b'),
    kind: 'arc',
    layerId: 'layer-paths',
    name: '跑道外弧-南',
    semanticType: 'EVAC',
    cx,
    cy,
    radius: runningRadiusOuter,
    startAngle: 20,
    endAngle: 160,
    laneWidth: 8,
    seatCount: 0
  })

  entities.push({
    id: nextId('track-top'),
    kind: 'polyline',
    layerId: 'layer-paths',
    name: '跑道上直道',
    semanticType: 'EVAC',
    laneWidth: 8,
    points: [
      { x: cx - runningRadiusInner, y: cy - 140 },
      { x: cx + runningRadiusInner, y: cy - 140 }
    ]
  })
  entities.push({
    id: nextId('track-bottom'),
    kind: 'polyline',
    layerId: 'layer-paths',
    name: '跑道下直道',
    semanticType: 'EVAC',
    laneWidth: 8,
    points: [
      { x: cx - runningRadiusInner, y: cy + 140 },
      { x: cx + runningRadiusInner, y: cy + 140 }
    ]
  })

  const radialCount = modelConfig.radialCorridorCount
  for (let i = 0; i < radialCount; i += 1) {
    const angle = (i / radialCount) * 360
    const inPoint = polarToCartesian(cx, cy, baseRadius - 40, angle)
    const midPoint = polarToCartesian(cx, cy, baseRadius + modelConfig.standRingCount * ringGap * 0.45, angle)
    const outPoint = polarToCartesian(cx, cy, baseRadius + modelConfig.standRingCount * ringGap + 68, angle)
    entities.push({
      id: nextId('path-radial'),
      kind: 'polyline',
      layerId: 'layer-paths',
      name: `放射通道-${i + 1}`,
      semanticType: 'EVAC',
      laneWidth: modelConfig.laneWidth,
      points: [inPoint, midPoint, outPoint]
    })
  }

  const ringRadiusOuter = baseRadius + modelConfig.standRingCount * ringGap + modelConfig.ringOffset
  const ringRadiusInner = ringRadiusOuter - 34
  const ringOuter = []
  const ringInner = []
  for (let i = 0; i <= 32; i += 1) {
    const a = (i / 32) * 360
    ringOuter.push(polarToCartesian(cx, cy, ringRadiusOuter, a))
    ringInner.push(polarToCartesian(cx, cy, ringRadiusInner, a))
  }
  entities.push({
    id: nextId('path-ring-outer'),
    kind: 'polyline',
    layerId: 'layer-paths',
    name: '外围环道-外圈',
    semanticType: 'EVAC',
    laneWidth: modelConfig.ringWidth,
    points: ringOuter
  })
  entities.push({
    id: nextId('path-ring-inner'),
    kind: 'polyline',
    layerId: 'layer-paths',
    name: '外围环道-内圈',
    semanticType: 'EVAC',
    laneWidth: modelConfig.ringWidth - 4,
    points: ringInner
  })

  for (let i = 0; i < modelConfig.complexGateCount; i += 1) {
    const a = (i / modelConfig.complexGateCount) * 360
    const gCenter = polarToCartesian(cx, cy, ringRadiusOuter + 48, a)
    entities.push({
      id: nextId('gate'),
      kind: 'polygon',
      layerId: 'layer-gates',
      name: `外围出口-${i + 1}`,
      semanticType: 'GATE',
      seatCount: 0,
      points: [
        { x: gCenter.x - 14, y: gCenter.y - 10 },
        { x: gCenter.x + 14, y: gCenter.y - 10 },
        { x: gCenter.x + 14, y: gCenter.y + 10 },
        { x: gCenter.x - 14, y: gCenter.y + 10 }
      ]
    })
  }

  state.entities = entities
  state.links = []
  state.selectedEntityIds = []
  modelConfig.modelName = '完整体育场总模型'
  autoBuildLinks()
  state.message = '已生成完整体育场（主场+跑道+多环看台+双环通道+多出口）'
}

function nearestEntity(source, targets) {
  const c = entityCenter(source)
  let best = null
  let d = Number.POSITIVE_INFINITY
  targets.forEach((t) => {
    const td = distance(c, entityCenter(t))
    if (td < d) {
      d = td
      best = t
    }
  })
  return best
}

function buildTemplate(template) {
  if (template === 'basketball') {
    modelConfig.modelName = '篮球馆标准模板'
    state.entities = [
      { id: 't-court', kind: 'polygon', layerId: 'layer-zones', name: '比赛主场地', semanticType: 'COURT', seatCount: 0, points: [{ x: 430, y: 250 }, { x: 770, y: 250 }, { x: 770, y: 480 }, { x: 430, y: 480 }] },
      { id: 't-stand-n', kind: 'arc', layerId: 'layer-zones', name: '北侧弧形看台', semanticType: 'STAND', cx: 600, cy: 200, radius: 230, startAngle: 200, endAngle: 340, laneWidth: 24, seatCount: 1200 },
      { id: 't-stand-s', kind: 'arc', layerId: 'layer-zones', name: '南侧弧形看台', semanticType: 'STAND', cx: 600, cy: 530, radius: 230, startAngle: 20, endAngle: 160, laneWidth: 24, seatCount: 1200 },
      { id: 't-gate-w', kind: 'polygon', layerId: 'layer-gates', name: '西侧主入口', semanticType: 'GATE', seatCount: 0, points: [{ x: 260, y: 340 }, { x: 330, y: 340 }, { x: 330, y: 390 }, { x: 260, y: 390 }] },
      { id: 't-gate-e', kind: 'polygon', layerId: 'layer-gates', name: '东侧主入口', semanticType: 'GATE', seatCount: 0, points: [{ x: 870, y: 340 }, { x: 940, y: 340 }, { x: 940, y: 390 }, { x: 870, y: 390 }] },
      { id: 't-path-main', kind: 'polyline', layerId: 'layer-paths', name: '主疏散通道', semanticType: 'EVAC', laneWidth: 18, points: [{ x: 300, y: 365 }, { x: 430, y: 365 }, { x: 770, y: 365 }, { x: 900, y: 365 }] }
    ]
    state.links = []
    state.selectedEntityIds = []
    state.message = '已加载篮球馆标准模板'
  }

  if (template === 'track') {
    modelConfig.modelName = '田径场标准模板'
    state.entities = [
      { id: 'r-left', kind: 'arc', layerId: 'layer-zones', name: '跑道左弧', semanticType: 'COURT', cx: 380, cy: 380, radius: 140, startAngle: 90, endAngle: 270, laneWidth: 30, seatCount: 0 },
      { id: 'r-right', kind: 'arc', layerId: 'layer-zones', name: '跑道右弧', semanticType: 'COURT', cx: 820, cy: 380, radius: 140, startAngle: -90, endAngle: 90, laneWidth: 30, seatCount: 0 },
      { id: 'r-mid-top', kind: 'polyline', layerId: 'layer-paths', name: '跑道上直道', semanticType: 'EVAC', laneWidth: 30, points: [{ x: 380, y: 240 }, { x: 820, y: 240 }] },
      { id: 'r-mid-bottom', kind: 'polyline', layerId: 'layer-paths', name: '跑道下直道', semanticType: 'EVAC', laneWidth: 30, points: [{ x: 380, y: 520 }, { x: 820, y: 520 }] }
    ]
    state.links = []
    state.selectedEntityIds = []
    state.message = '已加载田径场标准模板'
  }
}

function toggleLayerVisible(layerId) {
  const layer = state.layers.find((l) => l.id === layerId)
  if (layer) layer.visible = !layer.visible
}

function toggleLayerLocked(layerId) {
  const layer = state.layers.find((l) => l.id === layerId)
  if (!layer || layer.id === 'layer-base') return
  layer.locked = !layer.locked
}

function exportJson() {
  const payload = {
    version: 3.3,
    type: 'sports-venue-semantic-model',
    modelName: modelConfig.modelName,
    config: {
      gridSize: modelConfig.gridSize,
      showGrid: modelConfig.showGrid,
      snapToGrid: modelConfig.snapToGrid,
      seatPerSqm: modelConfig.seatPerSqm,
      gateFlowPerMeter: modelConfig.gateFlowPerMeter,
      pathFlowPerMeter: modelConfig.pathFlowPerMeter,
      walkSpeedMpm: modelConfig.walkSpeedMpm
    },
    layers: state.layers,
    entities: state.entities,
    links: state.links
  }
  const text = JSON.stringify(payload, null, 2)
  state.jsonText = text
  const blob = new Blob([text], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${modelConfig.modelName || 'venue-model-v33'}.json`
  a.click()
  URL.revokeObjectURL(url)
  state.message = '已导出v3.3模型'
}

function importJson() {
  if (!state.jsonText.trim()) return
  try {
    const parsed = JSON.parse(state.jsonText)
    if (!Array.isArray(parsed.layers) || !Array.isArray(parsed.entities)) {
      state.message = 'JSON缺少layers或entities'
      return
    }
    modelConfig.modelName = parsed.modelName || modelConfig.modelName
    modelConfig.gridSize = Number(parsed.config?.gridSize) || modelConfig.gridSize
    modelConfig.showGrid = parsed.config?.showGrid ?? modelConfig.showGrid
    modelConfig.snapToGrid = parsed.config?.snapToGrid ?? modelConfig.snapToGrid
    modelConfig.seatPerSqm = Number(parsed.config?.seatPerSqm) || modelConfig.seatPerSqm
    modelConfig.gateFlowPerMeter = Number(parsed.config?.gateFlowPerMeter) || modelConfig.gateFlowPerMeter
    modelConfig.pathFlowPerMeter = Number(parsed.config?.pathFlowPerMeter) || modelConfig.pathFlowPerMeter
    modelConfig.walkSpeedMpm = Number(parsed.config?.walkSpeedMpm) || modelConfig.walkSpeedMpm
    state.layers = parsed.layers
    state.entities = parsed.entities
    state.links = Array.isArray(parsed.links) ? parsed.links : []
    state.selectedEntityIds = []
    state.drawingPoints = []
    state.message = '导入成功'
  } catch {
    state.message = 'JSON解析失败'
  }
}

function saveLocal() {
  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      modelName: modelConfig.modelName,
      config: {
        gridSize: modelConfig.gridSize,
        showGrid: modelConfig.showGrid,
        snapToGrid: modelConfig.snapToGrid,
        seatPerSqm: modelConfig.seatPerSqm,
        gateFlowPerMeter: modelConfig.gateFlowPerMeter,
        pathFlowPerMeter: modelConfig.pathFlowPerMeter,
        walkSpeedMpm: modelConfig.walkSpeedMpm
      },
      layers: state.layers,
      entities: state.entities,
      links: state.links
    })
  )
  state.message = '已保存到本地'
}

function sendTo3DPreview() {
  const payload = {
    version: 3.3,
    type: 'sports-venue-semantic-model',
    modelName: modelConfig.modelName,
    config: {
      gridSize: modelConfig.gridSize,
      showGrid: modelConfig.showGrid,
      snapToGrid: modelConfig.snapToGrid,
      seatPerSqm: modelConfig.seatPerSqm,
      gateFlowPerMeter: modelConfig.gateFlowPerMeter,
      pathFlowPerMeter: modelConfig.pathFlowPerMeter,
      walkSpeedMpm: modelConfig.walkSpeedMpm
    },
    layers: state.layers,
    entities: state.entities,
    links: state.links
  }
  localStorage.setItem('sports-venue-3d-payload', JSON.stringify(payload))
  router.push('/app/admin-model-3d')
}

function loadLocal() {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    state.message = '本地没有数据'
    return
  }
  try {
    const parsed = JSON.parse(raw)
    modelConfig.modelName = parsed.modelName || modelConfig.modelName
    modelConfig.gridSize = Number(parsed.config?.gridSize) || modelConfig.gridSize
    modelConfig.showGrid = parsed.config?.showGrid ?? modelConfig.showGrid
    modelConfig.snapToGrid = parsed.config?.snapToGrid ?? modelConfig.snapToGrid
    modelConfig.seatPerSqm = Number(parsed.config?.seatPerSqm) || modelConfig.seatPerSqm
    modelConfig.gateFlowPerMeter = Number(parsed.config?.gateFlowPerMeter) || modelConfig.gateFlowPerMeter
    modelConfig.pathFlowPerMeter = Number(parsed.config?.pathFlowPerMeter) || modelConfig.pathFlowPerMeter
    modelConfig.walkSpeedMpm = Number(parsed.config?.walkSpeedMpm) || modelConfig.walkSpeedMpm
    state.layers = parsed.layers || state.layers
    state.entities = parsed.entities || state.entities
    state.links = parsed.links || []
    state.selectedEntityIds = []
    state.message = '已恢复本地模型'
  } catch {
    state.message = '本地数据损坏'
  }
}

function onKeyDown(event) {
  const tag = event.target?.tagName?.toLowerCase()
  if (tag === 'input' || tag === 'textarea') return

  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'z') {
    undo()
    event.preventDefault()
    return
  }
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'y') {
    redo()
    event.preventDefault()
    return
  }
  if (event.key === 'Enter') {
    finishDrawing()
    event.preventDefault()
  }
  if (event.key === 'Escape') cancelDrawing()
  if ((event.key === 'Delete' || event.key === 'Backspace') && state.selectedEntityIds.length) {
    deleteSelected()
    event.preventDefault()
  }
}

onMounted(() => {
  window.addEventListener('keydown', onKeyDown)
  buildTemplate('basketball')
  recordSnapshot()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeyDown)
})
</script>

<template>
  <div class="admin-model-page">
    <section class="card model-hero">
      <div>
        <p class="section-kicker">2D建模管理 · 真实场馆增强包</p>
        <h2>真实体育场馆语义建模</h2>
        <p class="text-muted">新增看台扇区生成、环形通道生成、分区-通道-出口连通关系与路径化疏散估算。</p>
      </div>
      <NTag type="success">v3.3 semantic model</NTag>
    </section>

    <section class="model-grid">
      <NCard title="建模控制台" class="model-card">
        <NSpace vertical :size="12">
          <div class="field"><label>模型名称</label><NInput v-model:value="modelConfig.modelName" /></div>
          <div class="field"><label>工具</label><NSelect v-model:value="modelConfig.tool" :options="toolOptions" /></div>
          <div class="field"><label>语义类型</label><NSelect v-model:value="modelConfig.semanticType" :options="semanticOptions" /></div>
          <NSpace><NButton tertiary @click="applySemanticToSelected">应用到当前选中实体</NButton></NSpace>
          <div class="field"><label>激活图层</label><NSelect v-model:value="modelConfig.activeLayerId" :options="layerOptions" /></div>
          <div class="field"><label>看台容量（绘制看台时）</label><NInputNumber v-model:value="modelConfig.seatCount" :min="0" :step="50" style="width: 100%" /></div>
          <div class="field"><label>通道/弧形宽度（px）</label><NInputNumber v-model:value="modelConfig.laneWidth" :min="4" :step="2" style="width: 100%" /></div>
          <div class="field"><label>弧线跨度（°）</label><NInputNumber v-model:value="modelConfig.arcSpan" :min="20" :max="340" :step="10" style="width: 100%" /></div>
          <div class="field"><label>网格大小</label><NInputNumber v-model:value="modelConfig.gridSize" :min="8" :max="80" :step="2" style="width: 100%" /></div>
          <div class="switch-line"><span>显示网格</span><NSwitch v-model:value="modelConfig.showGrid" /></div>
          <div class="switch-line"><span>吸附网格</span><NSwitch v-model:value="modelConfig.snapToGrid" /></div>
          <div class="switch-line"><span>显示伪3D体块</span><NSwitch v-model:value="modelConfig.show3DPreview" /></div>
          <div class="field"><label>伪3D挤出深度</label><NInputNumber v-model:value="modelConfig.extrusionDepth" :min="8" :max="80" :step="2" style="width: 100%" /></div>
          <div class="field"><label>伪3D投影角度(°)</label><NInputNumber v-model:value="modelConfig.obliqueAngle" :min="10" :max="70" :step="2" style="width: 100%" /></div>

          <NSpace wrap>
            <NButton :disabled="!canUndo" @click="undo">撤销</NButton>
            <NButton :disabled="!canRedo" @click="redo">重做</NButton>
            <NButton type="primary" @click="finishDrawing">完成绘制(Enter)</NButton>
            <NButton tertiary @click="cancelDrawing">取消(Esc)</NButton>
            <NButton tertiary @click="buildTemplate('basketball')">篮球馆模板</NButton>
            <NButton tertiary @click="buildTemplate('track')">田径场模板</NButton>
          </NSpace>

          <NSpace wrap>
            <NButton tertiary @click="saveLocal">保存本地</NButton>
            <NButton tertiary @click="loadLocal">恢复本地</NButton>
            <NButton tertiary @click="exportJson">导出JSON</NButton>
            <NButton tertiary @click="importJson">导入JSON</NButton>
            <NButton type="info" secondary @click="sendTo3DPreview">发送到3D预览</NButton>
            <NButton type="error" secondary @click="deleteSelected">删除选中</NButton>
          </NSpace>

          <NAlert type="info" :show-icon="false">{{ selectedSummary }}</NAlert>
          <NAlert v-if="state.message" type="success" :show-icon="false">{{ state.message }}</NAlert>

          <div class="field"><label>JSON粘贴区（导入）</label><NInput v-model:value="state.jsonText" type="textarea" :autosize="{ minRows: 4, maxRows: 8 }" /></div>
        </NSpace>
      </NCard>

      <NCard title="场馆建模画布（SVG）" class="model-card">
        <div
          ref="canvasRef"
          class="canvas"
          :style="{
            width: `${CANVAS_WIDTH}px`,
            height: `${CANVAS_HEIGHT}px`,
            backgroundSize: `${modelConfig.gridSize}px ${modelConfig.gridSize}px`,
            backgroundImage: modelConfig.showGrid
              ? 'linear-gradient(to right, rgba(120,120,120,.18) 1px, transparent 1px), linear-gradient(to bottom, rgba(120,120,120,.18) 1px, transparent 1px)'
              : 'none'
          }"
          @click="onCanvasClick"
          @mousemove="onCanvasMouseMove"
          @mouseup="onCanvasMouseUp"
          @mouseleave="onCanvasMouseUp"
        >
          <svg :width="CANVAS_WIDTH" :height="CANVAS_HEIGHT">
            <g v-for="entity in visibleEntities" :key="entity.id">
              <path v-if="entity.kind === 'polygon'" :d="pointsToPath(entity.points, true)" :fill="`${semanticColor(entity.semanticType)}33`" :stroke="semanticColor(entity.semanticType)" :stroke-width="state.selectedEntityIds.includes(entity.id) ? 3 : 2" @click.stop="selectEntity(entity.id, $event)" @mousedown.stop="onEntityMouseDown(entity.id, $event)" />
              <path v-if="entity.kind === 'polyline'" :d="pointsToPath(entity.points, false)" fill="none" :stroke="semanticColor(entity.semanticType)" :stroke-width="entity.laneWidth || 8" stroke-linecap="round" stroke-linejoin="round" @click.stop="selectEntity(entity.id, $event)" @mousedown.stop="onEntityMouseDown(entity.id, $event)" />
              <path v-if="entity.kind === 'arc'" :d="arcToPath(entity)" fill="none" :stroke="semanticColor(entity.semanticType)" :stroke-width="entity.laneWidth || 10" stroke-linecap="round" @click.stop="selectEntity(entity.id, $event)" @mousedown.stop="onEntityMouseDown(entity.id, $event)" />
            </g>

            <line v-for="l in visibleLinks" :key="l.id" :x1="l.from.x" :y1="l.from.y" :x2="l.to.x" :y2="l.to.y" stroke="#0ea5e9" stroke-width="2" stroke-dasharray="5 4" opacity="0.9" />

            <g v-if="modelConfig.show3DPreview">
              <polygon v-for="shape in preview3DShapes.grounds" :key="shape.id" :points="shape.points" :fill="shape.color" stroke="none" />
              <polygon v-for="shape in preview3DShapes.sides" :key="shape.id" :points="shape.points" :fill="shape.color" stroke="none" />
              <polygon v-for="shape in preview3DShapes.tops" :key="shape.id" :points="shape.points" :fill="shape.color" stroke="#0f172a" stroke-width="0.6" />
            </g>

            <g v-if="state.drawingPoints.length">
              <path v-if="modelConfig.tool !== 'draw-arc'" :d="pointsToPath(state.drawingPoints, false)" fill="none" stroke="#16a34a" stroke-width="2" stroke-dasharray="4 4" />
              <circle v-for="(p, idx) in state.drawingPoints" :key="`draw-${idx}`" :cx="p.x" :cy="p.y" r="4" fill="#16a34a" />
            </g>
          </svg>
        </div>

        <div class="legend-row">
          <span>功能区 {{ metrics.zones }}</span><span>弧形构件 {{ metrics.arcs }}</span><span>通道 {{ metrics.paths }}</span><span>出入口 {{ metrics.gates }}</span>
          <span>总容量 {{ metrics.totalCapacity }}</span><span>总面积 {{ metrics.totalArea.toFixed(1) }}㎡</span><span>总通道长度 {{ metrics.totalPathLength.toFixed(1) }}m</span>
        </div>
      </NCard>
    </section>

    <section class="model-grid secondary-grid">
      <NCard title="真实场馆生成器" class="model-card">
        <div class="field"><label>看台扇区数量</label><NInputNumber v-model:value="modelConfig.sectorCount" :min="2" :max="24" style="width: 100%" /></div>
        <div class="field"><label>看台环数（复杂模板）</label><NInputNumber v-model:value="modelConfig.standRingCount" :min="1" :max="4" style="width: 100%" /></div>
        <div class="field"><label>放射通道数量（复杂模板）</label><NInputNumber v-model:value="modelConfig.radialCorridorCount" :min="4" :max="20" style="width: 100%" /></div>
        <div class="field"><label>出口数量（复杂模板）</label><NInputNumber v-model:value="modelConfig.complexGateCount" :min="4" :max="20" style="width: 100%" /></div>
        <NSpace><NButton type="primary" @click="generateComplexArena">一键生成完整体育场总模型</NButton></NSpace>
        <NSpace><NButton type="primary" secondary @click="generateStandSectors">按选中弧形看台生成扇区</NButton></NSpace>
        <div class="field"><label>环道偏移(px)</label><NInputNumber v-model:value="modelConfig.ringOffset" :min="10" :max="200" style="width: 100%" /></div>
        <div class="field"><label>环道宽度(px)</label><NInputNumber v-model:value="modelConfig.ringWidth" :min="6" :max="80" style="width: 100%" /></div>
        <NSpace><NButton tertiary @click="generateRingCorridor">围绕主场生成环形通道</NButton></NSpace>
        <NSpace><NButton tertiary @click="autoBuildLinks">自动生成连通关系</NButton></NSpace>
      </NCard>

      <NCard title="实体清单与属性" class="model-card">
        <div class="list-wrap">
          <button v-for="entity in state.entities" :key="entity.id" class="row btn-row" :class="{ active: state.selectedEntityIds.includes(entity.id) }" @click="selectEntity(entity.id, $event)">
            <div><strong>{{ entity.name }}</strong><p class="text-muted">{{ entity.kind }} · {{ entity.semanticType }} · {{ entity.layerId }}</p></div>
            <span v-if="entity.semanticType === 'STAND'">座位 {{ entity.seatCount || 0 }}</span>
          </button>
        </div>

        <div v-if="selectedPrimary" class="prop-panel">
          <h4>选中实体参数</h4>
          <div class="field"><label>名称</label><NInput v-model:value="selectedPrimary.name" /></div>
          <div class="field"><label>语义</label><NSelect v-model:value="selectedPrimary.semanticType" :options="semanticOptions" /></div>
          <div class="field" v-if="selectedPrimary.semanticType === 'STAND'"><label>看台容量</label><NInputNumber v-model:value="selectedPrimary.seatCount" :min="0" style="width: 100%" /></div>
          <template v-if="selectedPrimary.kind === 'arc'">
            <div class="field"><label>半径(px)</label><NInputNumber v-model:value="selectedPrimary.radius" :min="20" style="width: 100%" /></div>
            <div class="field"><label>起始角(°)</label><NInputNumber v-model:value="selectedPrimary.startAngle" :min="-360" :max="360" style="width: 100%" /></div>
            <div class="field"><label>结束角(°)</label><NInputNumber v-model:value="selectedPrimary.endAngle" :min="-360" :max="360" style="width: 100%" /></div>
            <div class="field"><label>弧宽(px)</label><NInputNumber v-model:value="selectedPrimary.laneWidth" :min="2" style="width: 100%" /></div>
          </template>
          <template v-if="selectedPrimary.kind === 'polyline'">
            <div class="field"><label>通道宽度(px)</label><NInputNumber v-model:value="selectedPrimary.laneWidth" :min="2" style="width: 100%" /></div>
          </template>
          <NSpace>
            <NButton tertiary @click="autoSeatSelectedStand">自动排座</NButton>
            <NButton type="error" secondary @click="deleteSelected">删除</NButton>
          </NSpace>
        </div>
      </NCard>
    </section>

    <section class="model-grid secondary-grid">
      <NCard title="疏散能力估算（路径化）" class="model-card">
        <div class="field"><label>步行速度（m/分钟）</label><NInputNumber v-model:value="modelConfig.walkSpeedMpm" :min="20" :max="120" style="width: 100%" /></div>
        <div class="field"><label>出口单位流量（人/分钟/米）</label><NInputNumber v-model:value="modelConfig.gateFlowPerMeter" :min="10" :step="5" style="width: 100%" /></div>
        <div class="field"><label>通道单位流量（人/分钟/米）</label><NInputNumber v-model:value="modelConfig.pathFlowPerMeter" :min="10" :step="5" style="width: 100%" /></div>
        <p>总出口宽度：{{ evacuationEstimate.gateWidthSum.toFixed(2) }} m</p>
        <p>总通道宽度：{{ evacuationEstimate.pathWidthSum.toFixed(2) }} m</p>
        <p>出口通行能力：{{ evacuationEstimate.gateFlow.toFixed(0) }} 人/分钟</p>
        <p>通道通行能力：{{ evacuationEstimate.pathFlow.toFixed(0) }} 人/分钟</p>
        <p>吞吐时间：{{ evacuationEstimate.throughputTime.toFixed(1) }} 分钟</p>
        <p>最远分区步行时间：{{ evacuationEstimate.maxWalk.toFixed(1) }} 分钟</p>
        <NAlert type="info" :show-icon="false">预计总疏散时间：{{ evacuationEstimate.totalMinutes.toFixed(1) }} 分钟</NAlert>
      </NCard>

      <NCard title="场馆规则校验" class="model-card">
        <div v-if="validationIssues.length" class="list-wrap">
          <NAlert v-for="(issue, index) in validationIssues" :key="index" type="warning" :show-icon="false">{{ issue }}</NAlert>
        </div>
        <NAlert v-else type="success" :show-icon="false">已通过基础规则校验</NAlert>
      </NCard>
    </section>
  </div>
</template>

<style scoped>
.admin-model-page { display: grid; gap: 16px; }
.model-hero { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.model-grid { display: grid; grid-template-columns: 360px 1fr; gap: 16px; }
.secondary-grid { grid-template-columns: 1fr 1fr; }
.model-card { border-radius: 16px; }
.field { display: grid; gap: 8px; margin-bottom: 8px; }
.switch-line { display: flex; justify-content: space-between; align-items: center; }
.canvas { border: 1px dashed rgba(15, 23, 42, 0.2); border-radius: 8px; overflow: hidden; }
.legend-row { margin-top: 10px; display: flex; gap: 12px; flex-wrap: wrap; color: rgba(15, 23, 42, 0.72); font-size: 13px; }
.list-wrap { display: grid; gap: 8px; }
.row { border: 1px solid rgba(148, 163, 184, 0.45); border-radius: 10px; padding: 10px; background: #fff; display: flex; justify-content: space-between; align-items: center; }
.btn-row { cursor: pointer; text-align: left; }
.row.active { border-color: #2563eb; }
.prop-panel { margin-top: 12px; display: grid; gap: 8px; }
@media (max-width: 1260px) {
  .model-grid, .secondary-grid { grid-template-columns: 1fr; }
  .canvas { width: 100% !important; overflow: auto; }
}
</style>
