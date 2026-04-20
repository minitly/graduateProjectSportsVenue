const MIN_RECT_SIZE = 40

function clampNumber(value, min, max) {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return min
  return Math.min(max, Math.max(min, parsed))
}

function normalizeRect(item, index = 0) {
  return {
    id: item?.id || `rect_${Date.now()}_${index}`,
    type: 'rect',
    x: clampNumber(item?.x ?? 40 + index * 20, -2000, 2000),
    y: clampNumber(item?.y ?? 40 + index * 20, -2000, 2000),
    w: clampNumber(item?.w ?? 200, MIN_RECT_SIZE, 2000),
    h: clampNumber(item?.h ?? 120, MIN_RECT_SIZE, 2000),
    rotation: clampNumber(item?.rotation ?? 0, -360, 360),
    label: typeof item?.label === 'string' ? item.label : ''
  }
}

export function createDefaultFloorPlanModel() {
  return {
    canvas: {
      width: 960,
      height: 600,
      backgroundColor: '#f8fafc'
    },
    items: []
  }
}

export function normalizeFloorPlanModel(model) {
  const fallback = createDefaultFloorPlanModel()
  const source = model && typeof model === 'object' ? model : fallback
  const canvas = source.canvas && typeof source.canvas === 'object' ? source.canvas : {}
  const items = Array.isArray(source.items) ? source.items : []

  return {
    canvas: {
      width: clampNumber(canvas.width ?? fallback.canvas.width, 480, 2200),
      height: clampNumber(canvas.height ?? fallback.canvas.height, 320, 1600),
      backgroundColor: typeof canvas.backgroundColor === 'string' && canvas.backgroundColor.trim()
        ? canvas.backgroundColor.trim()
        : fallback.canvas.backgroundColor
    },
    items: items.map((item, index) => normalizeRect(item, index))
  }
}

export function parseFloorPlanContent(contentJson) {
  if (!contentJson || typeof contentJson !== 'string') {
    return normalizeFloorPlanModel(createDefaultFloorPlanModel())
  }
  const parsed = JSON.parse(contentJson)
  return normalizeFloorPlanModel(parsed)
}

export function stringifyFloorPlanContent(model) {
  return JSON.stringify(normalizeFloorPlanModel(model))
}

export function createDefaultRect(index = 0) {
  return normalizeRect(
    {
      id: `rect_${Date.now()}_${Math.floor(Math.random() * 10000)}`,
      x: 48 + index * 24,
      y: 48 + index * 24,
      w: 220,
      h: 120,
      rotation: 0,
      label: `区域 ${index + 1}`
    },
    index
  )
}
