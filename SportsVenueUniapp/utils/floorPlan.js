function clampNumber(value, min, max) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) return min;
  return Math.min(max, Math.max(min, parsed));
}

function normalizeRect(item, index = 0) {
  return {
    uid: item?.uid || item?.id || `rect_${Date.now()}_${index}`,
    id: item?.id || item?.uid || `rect_${Date.now()}_${index}`,
    x: clampNumber(item?.x ?? 40 + index * 20, -2000, 2000),
    y: clampNumber(item?.y ?? 40 + index * 20, -2000, 2000),
    w: clampNumber(item?.w ?? 200, 20, 2000),
    h: clampNumber(item?.h ?? 120, 20, 2000),
    rotation: clampNumber(item?.rotation ?? 0, -360, 360),
    label: typeof item?.label === "string" ? item.label : "",
    color: typeof item?.color === "string" ? item.color : "",
    venueId: item?.venueId ?? item?.venue_id ?? null
  };
}

export function normalizeFloorPlanModel(model) {
  const fallback = { canvas: { width: 960, height: 600, backgroundColor: "#f8fafc" }, items: [] };
  const source = model && typeof model === "object" ? model : fallback;
  const canvas = source.canvas && typeof source.canvas === "object" ? source.canvas : {};
  const items = Array.isArray(source.items) ? source.items : [];

  return {
    canvas: {
      width: clampNumber(canvas.width ?? fallback.canvas.width, 240, 2200),
      height: clampNumber(canvas.height ?? fallback.canvas.height, 200, 1600),
      backgroundColor: typeof canvas.backgroundColor === "string" && canvas.backgroundColor.trim()
        ? canvas.backgroundColor.trim()
        : fallback.canvas.backgroundColor
    },
    items: items.map((it, idx) => normalizeRect(it, idx))
  };
}

export function parseFloorPlanContent(contentJson) {
  if (!contentJson || typeof contentJson !== "string") {
    return normalizeFloorPlanModel(null);
  }
  try {
    const parsed = JSON.parse(contentJson);
    return normalizeFloorPlanModel(parsed);
  } catch (_) {
    return normalizeFloorPlanModel(null);
  }
}

