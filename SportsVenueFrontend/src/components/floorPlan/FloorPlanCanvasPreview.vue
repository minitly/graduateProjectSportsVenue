<script setup>
import { computed } from 'vue'
import { normalizeFloorPlanModel } from '../../utils/floorPlan'

const props = defineProps({
  model: {
    type: Object,
    required: true
  },
  interactive: {
    type: Boolean,
    default: false
  },
  highlightItemUid: {
    type: String,
    default: ''
  },
  maxHeight: {
    type: Number,
    default: 520
  },
  /** 场地 id → 状态；已知非 AVAILABLE 的区域不可点击并弱化显示 */
  venueStatusById: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['item-click'])

const normalized = computed(() => normalizeFloorPlanModel(props.model))
const stageStyle = computed(() => ({
  width: `${normalized.value.canvas.width}px`,
  height: `${normalized.value.canvas.height}px`,
  background: normalized.value.canvas.backgroundColor
}))

function isKnownUnavailableVenue(item) {
  if (!item?.venueId) return false
  const vid = Number(item.venueId)
  if (!Number.isFinite(vid) || vid <= 0) return false
  const st = props.venueStatusById?.[vid]
  if (st === undefined || st === null || st === '') return false
  return String(st).toUpperCase() !== 'AVAILABLE'
}

function rectStyle(item) {
  const color = /^#([0-9a-fA-F]{6})$/.test(item?.color || '') ? item.color : '#4f7bc3'
  const isHighlighted = !props.highlightItemUid || props.highlightItemUid === item?.id
  const unavailable = isKnownUnavailableVenue(item)
  const clickable = Boolean(props.interactive && item?.venueId && !unavailable)

  const layout = {
    width: `${item.w}px`,
    height: `${item.h}px`,
    transform: `translate(${item.x}px, ${item.y}px) rotate(${item.rotation}deg)`
  }

  if (unavailable) {
    return layout
  }

  return {
    ...layout,
    borderColor: color,
    background: `${color}33`,
    color: '#1f3e67',
    opacity: isHighlighted ? 1 : 0.35,
    boxShadow: isHighlighted ? `0 0 0 2px ${color}66` : 'none',
    cursor: clickable ? 'pointer' : 'default'
  }
}

function handleRectClick(item) {
  if (!props.interactive) return
  if (!item?.venueId) return
  emit('item-click', item)
}
</script>

<template>
  <div class="preview-wrap" :style="{ maxHeight: `${maxHeight}px` }">
    <div class="preview-stage" :style="stageStyle">
      <div
        v-for="item in normalized.items"
        :key="item.id"
        class="preview-rect"
        :class="{ unavailable: isKnownUnavailableVenue(item) }"
        :style="rectStyle(item)"
        @click="handleRectClick(item)"
      >
        <span>{{ item.label || '未命名区域' }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.preview-wrap {
  border: 1px solid #dbe4f0;
  border-radius: 12px;
  background: #fff;
  padding: 12px;
  overflow: auto;
}

.preview-stage {
  position: relative;
  margin: 0 auto;
  border: 1px dashed #c5d2e6;
  border-radius: 10px;
  overflow: hidden;
}

.preview-rect {
  position: absolute;
  display: grid;
  place-items: center;
  border: 2px solid #4f7bc3;
  border-radius: 10px;
  background: rgba(79, 123, 195, 0.18);
  color: #1f3e67;
  font-size: 13px;
  font-weight: 600;
}

.preview-rect span {
  text-align: center;
  padding: 8px;
  word-break: break-word;
}

.preview-rect.unavailable {
  opacity: 0.4;
  filter: grayscale(0.72);
  cursor: not-allowed;
  border-color: rgba(138, 152, 172, 0.42);
  background: rgba(148, 158, 176, 0.12);
  box-shadow: none;
}

.preview-rect.unavailable:hover {
  cursor: not-allowed;
}

.preview-rect.unavailable span {
  color: rgba(31, 62, 103, 0.38);
}
</style>
