<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { NAlert, NButton, NCard, NSpace, NSwitch, NTag } from 'naive-ui'
import * as THREE from 'three'

const STORAGE_KEY = 'sports-venue-3d-payload'
const SEMANTICS = ['COURT', 'STAND', 'GATE', 'EVAC', 'SERVICE']

const hostRef = ref(null)
const three = reactive({
  scene: null,
  camera: null,
  renderer: null,
  controls: null,
  frameId: 0,
  resizeHandler: null,
  loadedCount: 0,
  status: '',
  modelName: '',
  semanticGroups: new Map()
})

const interaction = reactive({
  dragging: false,
  panning: false,
  lastX: 0,
  lastY: 0,
  yaw: 0.82,
  pitch: 0.52,
  distance: 38,
  targetX: 0,
  targetY: 0,
  targetZ: 0
})

const viewConfig = reactive({
  showGrid: true,
  showAxes: false,
  semanticVisible: {
    COURT: true,
    STAND: true,
    GATE: true,
    EVAC: true,
    SERVICE: true
  }
})

const semanticLabel = {
  COURT: '比赛场地',
  STAND: '看台',
  GATE: '出入口',
  EVAC: '疏散通道/跑道',
  SERVICE: '服务区'
}

const semanticColor = {
  COURT: '#3b82f6',
  STAND: '#8b5cf6',
  GATE: '#ef4444',
  EVAC: '#f59e0b',
  SERVICE: '#10b981'
}

const semanticStats = reactive({ COURT: 0, STAND: 0, GATE: 0, EVAC: 0, SERVICE: 0 })
const totalStats = computed(() => Object.values(semanticStats).reduce((a, b) => a + b, 0))

function getColor(type) {
  return new THREE.Color(semanticColor[type] || '#64748b')
}

function toWorldX(x) {
  return (x - 600) / 20
}

function toWorldZ(y) {
  return (y - 380) / 20
}

function pxToMeter(px) {
  return px / 20
}

function baseHeight(type) {
  if (type === 'STAND') return 3.2
  if (type === 'COURT') return 0.3
  if (type === 'GATE') return 1.8
  if (type === 'SERVICE') return 1.4
  return 0.35
}

function clearSceneContent() {
  if (!three.scene) return
  const preserve = new Set(['__floor__', '__ambient__', '__dirLight__', '__grid__', '__axes__'])
  const removeList = three.scene.children.filter((child) => !preserve.has(child.name))
  removeList.forEach((obj) => {
    three.scene.remove(obj)
    obj.traverse?.((node) => {
      if (node.geometry) node.geometry.dispose()
      if (node.material) {
        if (Array.isArray(node.material)) node.material.forEach((m) => m.dispose())
        else node.material.dispose()
      }
    })
  })
  three.semanticGroups.clear()
  SEMANTICS.forEach((k) => {
    semanticStats[k] = 0
  })
}

function ensureSemanticGroup(type) {
  let g = three.semanticGroups.get(type)
  if (g) return g
  g = new THREE.Group()
  g.name = `__semantic_${type}__`
  g.visible = Boolean(viewConfig.semanticVisible[type])
  three.semanticGroups.set(type, g)
  three.scene.add(g)
  return g
}

function makeGrassMaterial() {
  const c = document.createElement('canvas')
  c.width = 256
  c.height = 256
  const ctx = c.getContext('2d')
  ctx.fillStyle = '#2f8d46'
  ctx.fillRect(0, 0, 256, 256)
  for (let i = 0; i < 16; i += 1) {
    ctx.fillStyle = i % 2 ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.05)'
    ctx.fillRect(i * 16, 0, 16, 256)
  }
  const tex = new THREE.CanvasTexture(c)
  tex.wrapS = THREE.RepeatWrapping
  tex.wrapT = THREE.RepeatWrapping
  tex.repeat.set(4, 4)
  return new THREE.MeshStandardMaterial({ map: tex, roughness: 0.9, metalness: 0.02 })
}

function makeTrackMaterial() {
  const c = document.createElement('canvas')
  c.width = 256
  c.height = 64
  const ctx = c.getContext('2d')
  ctx.fillStyle = '#c85a24'
  ctx.fillRect(0, 0, 256, 64)
  ctx.strokeStyle = 'rgba(255,255,255,0.45)'
  for (let i = 0; i < 8; i += 1) {
    ctx.beginPath()
    ctx.moveTo(0, i * 8)
    ctx.lineTo(256, i * 8)
    ctx.stroke()
  }
  const tex = new THREE.CanvasTexture(c)
  tex.wrapS = THREE.RepeatWrapping
  tex.wrapT = THREE.RepeatWrapping
  tex.repeat.set(2, 4)
  return new THREE.MeshStandardMaterial({ map: tex, roughness: 0.92, metalness: 0.02 })
}

function createPolygonShape(points) {
  if (!Array.isArray(points) || points.length < 3) return null
  const shape = new THREE.Shape()
  shape.moveTo(toWorldX(points[0].x), toWorldZ(points[0].y))
  for (let i = 1; i < points.length; i += 1) {
    shape.lineTo(toWorldX(points[i].x), toWorldZ(points[i].y))
  }
  shape.closePath()
  return shape
}

function createExtrudedMesh(points, semanticType, options = {}) {
  const shape = createPolygonShape(points)
  if (!shape) return null
  const geometry = new THREE.ExtrudeGeometry(shape, {
    depth: options.depth ?? baseHeight(semanticType),
    bevelEnabled: false,
    steps: 1
  })
  geometry.rotateX(-Math.PI / 2)

  let material
  if (options.material) {
    material = options.material
  } else {
    material = new THREE.MeshStandardMaterial({
      color: options.color || getColor(semanticType),
      transparent: true,
      opacity: options.opacity ?? 0.9,
      roughness: 0.78,
      metalness: 0.05
    })
  }

  const mesh = new THREE.Mesh(geometry, material)
  mesh.castShadow = true
  mesh.receiveShadow = true
  return mesh
}

function createArcMesh(entity, customDepth = null) {
  const cx = toWorldX(entity.cx)
  const cz = toWorldZ(entity.cy)
  const lane = Math.max(2, (entity.laneWidth || 12) / 20)
  const radius = Math.max(0.2, entity.radius / 20)
  const innerR = Math.max(0.15, radius - lane / 2)
  const outerR = innerR + lane
  const start = THREE.MathUtils.degToRad(entity.startAngle || 0)
  const end = THREE.MathUtils.degToRad(entity.endAngle || 0)

  const shape = new THREE.Shape()
  shape.absarc(cx, cz, outerR, start, end, false)
  const hole = new THREE.Path()
  hole.absarc(cx, cz, innerR, end, start, true)
  shape.holes.push(hole)

  const geometry = new THREE.ExtrudeGeometry(shape, {
    depth: customDepth ?? baseHeight(entity.semanticType),
    bevelEnabled: false,
    steps: 1
  })
  geometry.rotateX(-Math.PI / 2)

  const material = new THREE.MeshStandardMaterial({
    color: getColor(entity.semanticType),
    transparent: true,
    opacity: 0.92,
    roughness: 0.82,
    metalness: 0.05
  })

  const mesh = new THREE.Mesh(geometry, material)
  mesh.castShadow = true
  mesh.receiveShadow = true
  return mesh
}

function createPolylineGroup(entity) {
  if (!Array.isArray(entity.points) || entity.points.length < 2) return null
  const g = new THREE.Group()
  const width = Math.max(0.15, (entity.laneWidth || 8) / 20)
  const h = baseHeight(entity.semanticType)

  const isTrack = (entity.name || '').includes('跑道')
  const mat = isTrack
    ? makeTrackMaterial()
    : new THREE.MeshStandardMaterial({
        color: getColor(entity.semanticType),
        transparent: true,
        opacity: 0.95,
        roughness: 0.85,
        metalness: 0.04
      })

  for (let i = 1; i < entity.points.length; i += 1) {
    const a = entity.points[i - 1]
    const b = entity.points[i]
    const ax = toWorldX(a.x)
    const az = toWorldZ(a.y)
    const bx = toWorldX(b.x)
    const bz = toWorldZ(b.y)
    const dx = bx - ax
    const dz = bz - az
    const len = Math.sqrt(dx * dx + dz * dz)
    if (len < 0.001) continue

    const geom = new THREE.BoxGeometry(width, h, len)
    const mesh = new THREE.Mesh(geom, mat)
    mesh.position.set((ax + bx) / 2, h / 2, (az + bz) / 2)
    mesh.rotation.y = Math.atan2(dx, dz)
    mesh.castShadow = true
    mesh.receiveShadow = true
    g.add(mesh)
  }

  return g
}

function createSteppedStand(entity) {
  const steps = 6
  const group = new THREE.Group()
  const baseLane = Math.max(8, entity.laneWidth || 18)
  for (let i = 0; i < steps; i += 1) {
    const part = {
      ...entity,
      laneWidth: Math.max(3, baseLane / steps - 0.4),
      radius: entity.radius + ((i - (steps - 1) / 2) * baseLane) / steps
    }
    const stepMesh = createArcMesh(part, 0.42 + i * 0.22)
    if (stepMesh) group.add(stepMesh)
  }
  return group
}

function createGateStyle(entity) {
  if (entity.kind !== 'polygon' || !entity.points?.length) return createExtrudedMesh(entity.points, entity.semanticType)

  const xs = entity.points.map((p) => toWorldX(p.x))
  const zs = entity.points.map((p) => toWorldZ(p.y))
  const minX = Math.min(...xs)
  const maxX = Math.max(...xs)
  const minZ = Math.min(...zs)
  const maxZ = Math.max(...zs)
  const w = Math.max(0.6, maxX - minX)
  const d = Math.max(0.6, maxZ - minZ)

  const group = new THREE.Group()
  const pillarMat = new THREE.MeshStandardMaterial({ color: '#b91c1c', roughness: 0.6, metalness: 0.08 })
  const beamMat = new THREE.MeshStandardMaterial({ color: '#ef4444', roughness: 0.58, metalness: 0.1 })

  const pillarW = Math.max(0.15, w * 0.18)
  const pillarH = 2.6
  const beamH = 0.36

  const left = new THREE.Mesh(new THREE.BoxGeometry(pillarW, pillarH, d), pillarMat)
  left.position.set(minX + pillarW / 2, pillarH / 2, (minZ + maxZ) / 2)

  const right = new THREE.Mesh(new THREE.BoxGeometry(pillarW, pillarH, d), pillarMat)
  right.position.set(maxX - pillarW / 2, pillarH / 2, (minZ + maxZ) / 2)

  const beam = new THREE.Mesh(new THREE.BoxGeometry(w, beamH, d), beamMat)
  beam.position.set((minX + maxX) / 2, pillarH - beamH / 2, (minZ + maxZ) / 2)

  group.add(left)
  group.add(right)
  group.add(beam)

  group.traverse((node) => {
    if (node.isMesh) {
      node.castShadow = true
      node.receiveShadow = true
    }
  })

  return group
}

function addEntityMesh(entity) {
  const type = entity.semanticType || 'SERVICE'
  const group = ensureSemanticGroup(type)
  let obj = null

  if (entity.kind === 'polygon') {
    if (type === 'COURT') {
      obj = createExtrudedMesh(entity.points, type, {
        depth: 0.24,
        material: makeGrassMaterial(),
        opacity: 1
      })
    } else if (type === 'GATE') {
      obj = createGateStyle(entity)
    } else {
      obj = createExtrudedMesh(entity.points, type)
    }
  } else if (entity.kind === 'arc') {
    if (type === 'STAND') obj = createSteppedStand(entity)
    else obj = createArcMesh(entity)
  } else if (entity.kind === 'polyline') {
    obj = createPolylineGroup(entity)
  }

  if (!obj) return false

  obj.userData = { name: entity.name, semanticType: type }
  group.add(obj)
  semanticStats[type] += 1
  return true
}

function loadModelToScene() {
  if (!three.scene) return
  clearSceneContent()

  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    three.loadedCount = 0
    three.modelName = ''
    three.status = '未检测到2D模型，请先在2D页面点击“发送到3D预览”。'
    return
  }

  try {
    const model = JSON.parse(raw)
    const entities = Array.isArray(model.entities) ? model.entities : []
    let count = 0

    entities.forEach((entity) => {
      if (addEntityMesh(entity)) count += 1
    })

    applySemanticVisibility()
    three.loadedCount = count
    three.modelName = model.modelName || '未命名'
    three.status = `已加载模型：${three.modelName}，3D对象 ${count} 个`
  } catch {
    three.loadedCount = 0
    three.modelName = ''
    three.status = '3D模型读取失败，请重新从2D页面发送数据。'
  }
}

function applySemanticVisibility() {
  SEMANTICS.forEach((s) => {
    const g = three.semanticGroups.get(s)
    if (g) g.visible = Boolean(viewConfig.semanticVisible[s])
  })
}

function updateHelpersVisible() {
  if (!three.scene) return
  const grid = three.scene.getObjectByName('__grid__')
  const axes = three.scene.getObjectByName('__axes__')
  if (grid) grid.visible = viewConfig.showGrid
  if (axes) axes.visible = viewConfig.showAxes
}

function updateCameraFromInteraction() {
  if (!three.camera) return
  const cp = Math.cos(interaction.pitch)
  const x = interaction.targetX + interaction.distance * cp * Math.sin(interaction.yaw)
  const y = interaction.targetY + interaction.distance * Math.sin(interaction.pitch)
  const z = interaction.targetZ + interaction.distance * cp * Math.cos(interaction.yaw)
  three.camera.position.set(x, y, z)
  three.camera.lookAt(interaction.targetX, interaction.targetY, interaction.targetZ)
}

function bindMouseControls(host) {
  const onMouseDown = (e) => {
    if (e.button === 0) interaction.dragging = true
    if (e.button === 2) interaction.panning = true
    interaction.lastX = e.clientX
    interaction.lastY = e.clientY
  }

  const onMouseMove = (e) => {
    const dx = e.clientX - interaction.lastX
    const dy = e.clientY - interaction.lastY
    interaction.lastX = e.clientX
    interaction.lastY = e.clientY

    if (interaction.dragging) {
      interaction.yaw -= dx * 0.008
      interaction.pitch -= dy * 0.006
      interaction.pitch = Math.max(-1.35, Math.min(1.35, interaction.pitch))
      updateCameraFromInteraction()
    }

    if (interaction.panning) {
      const panScale = interaction.distance * 0.0022
      interaction.targetX -= dx * panScale
      interaction.targetZ += dy * panScale
      updateCameraFromInteraction()
    }
  }

  const onMouseUp = () => {
    interaction.dragging = false
    interaction.panning = false
  }

  const onWheel = (e) => {
    e.preventDefault()
    const delta = e.deltaY > 0 ? 1.08 : 0.92
    interaction.distance = Math.max(8, Math.min(160, interaction.distance * delta))
    updateCameraFromInteraction()
  }

  const onContextMenu = (e) => e.preventDefault()

  host.addEventListener('mousedown', onMouseDown)
  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('mouseup', onMouseUp)
  host.addEventListener('wheel', onWheel, { passive: false })
  host.addEventListener('contextmenu', onContextMenu)

  three.controls = {
    dispose() {
      host.removeEventListener('mousedown', onMouseDown)
      window.removeEventListener('mousemove', onMouseMove)
      window.removeEventListener('mouseup', onMouseUp)
      host.removeEventListener('wheel', onWheel)
      host.removeEventListener('contextmenu', onContextMenu)
    }
  }
}

function initThree() {
  const host = hostRef.value
  if (!host) return

  const width = host.clientWidth || 960
  const height = host.clientHeight || 600

  const scene = new THREE.Scene()
  scene.background = new THREE.Color('#eaf2ff')

  const camera = new THREE.PerspectiveCamera(50, width / height, 0.1, 1200)

  const renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(width, height)
  renderer.shadowMap.enabled = true
  host.innerHTML = ''
  host.appendChild(renderer.domElement)

  bindMouseControls(host)
  updateCameraFromInteraction()

  const ambient = new THREE.AmbientLight('#ffffff', 0.52)
  ambient.name = '__ambient__'
  scene.add(ambient)

  const dir = new THREE.DirectionalLight('#ffffff', 0.95)
  dir.position.set(28, 36, 24)
  dir.castShadow = true
  dir.name = '__dirLight__'
  scene.add(dir)

  const floorGeom = new THREE.PlaneGeometry(260, 260)
  const floorMat = new THREE.MeshStandardMaterial({ color: '#dbeafe', roughness: 0.96, metalness: 0 })
  const floor = new THREE.Mesh(floorGeom, floorMat)
  floor.rotation.x = -Math.PI / 2
  floor.receiveShadow = true
  floor.name = '__floor__'
  scene.add(floor)

  const grid = new THREE.GridHelper(260, 52, '#93c5fd', '#cbd5e1')
  grid.position.y = 0.01
  grid.name = '__grid__'
  scene.add(grid)

  const axes = new THREE.AxesHelper(8)
  axes.name = '__axes__'
  axes.visible = false
  scene.add(axes)

  three.scene = scene
  three.camera = camera
  three.renderer = renderer

  const animate = () => {
    three.frameId = requestAnimationFrame(animate)
    renderer.render(scene, camera)
  }
  animate()

  const onResize = () => {
    const w = host.clientWidth || 960
    const h = host.clientHeight || 600
    camera.aspect = w / h
    camera.updateProjectionMatrix()
    renderer.setSize(w, h)
  }

  three.resizeHandler = onResize
  window.addEventListener('resize', onResize)

  loadModelToScene()
}

function refreshModel() {
  loadModelToScene()
}

function toggleSemantic(semantic) {
  viewConfig.semanticVisible[semantic] = !viewConfig.semanticVisible[semantic]
  applySemanticVisibility()
}

onMounted(() => {
  initThree()
})

onBeforeUnmount(() => {
  if (three.frameId) cancelAnimationFrame(three.frameId)
  if (three.resizeHandler) window.removeEventListener('resize', three.resizeHandler)
  if (three.controls) three.controls.dispose()
  if (three.renderer) three.renderer.dispose()
})
</script>

<template>
  <div class="admin-model-3d-page">
    <section class="card model-hero">
      <div>
        <p class="section-kicker">3D场馆预览 · 真实感增强</p>
        <h2>真实场馆3D可视化</h2>
        <p class="text-muted">看台台阶化、草坪/跑道材质、出入口门洞样式与语义图层控制。</p>
      </div>
      <NTag type="success">Three.js Enhanced</NTag>
    </section>

    <section class="panel-grid">
      <NCard title="场景控制" class="panel-card">
        <NSpace vertical :size="10">
          <NAlert type="info" :show-icon="false">{{ three.status || '正在初始化3D场景...' }}</NAlert>
          <p class="text-muted">模型：{{ three.modelName || '未加载' }} ｜ 对象：{{ totalStats }}</p>

          <div class="switch-row">
            <span>显示地面网格</span>
            <NSwitch v-model:value="viewConfig.showGrid" @update:value="updateHelpersVisible" />
          </div>
          <div class="switch-row">
            <span>显示坐标轴</span>
            <NSwitch v-model:value="viewConfig.showAxes" @update:value="updateHelpersVisible" />
          </div>

          <h4>语义图层显隐</h4>
          <div v-for="s in SEMANTICS" :key="s" class="switch-row">
            <span>{{ semanticLabel[s] }}（{{ semanticStats[s] }}）</span>
            <NSwitch :value="viewConfig.semanticVisible[s]" @update:value="toggleSemantic(s)" />
          </div>

          <NButton type="primary" @click="refreshModel">重新加载模型</NButton>
        </NSpace>
      </NCard>

      <NCard title="3D场景窗口" class="scene-card">
        <div ref="hostRef" class="three-host" />
        <p class="text-muted hint">鼠标左键旋转、滚轮缩放、右键平移。先在2D页面点击“发送到3D预览”可加载最新模型。</p>
      </NCard>
    </section>
  </div>
</template>

<style scoped>
.admin-model-3d-page {
  display: grid;
  gap: 16px;
}

.model-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.panel-grid {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
}

.panel-card,
.scene-card {
  border-radius: 16px;
}

.switch-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.three-host {
  width: 100%;
  height: 700px;
  border-radius: 12px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  overflow: hidden;
  background: #eef4ff;
}

.hint {
  margin-top: 10px;
}

@media (max-width: 1180px) {
  .panel-grid {
    grid-template-columns: 1fr;
  }

  .three-host {
    height: 560px;
  }
}
</style>
