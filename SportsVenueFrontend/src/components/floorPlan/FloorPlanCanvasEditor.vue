<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { NButton, NInput, NInputNumber } from 'naive-ui'
import { createDefaultRect, normalizeFloorPlanModel } from '../../utils/floorPlan'

const props = defineProps({
  modelValue: {
    type: Object,
    required: true
  },
  readonly: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])

const dragState = reactive({
  itemId: '',
  mode: '',
  startX: 0,
  startY: 0,
  originX: 0,
  originY: 0,
  originW: 0,
  originH: 0,
  active: false
})

const selectedId = ref('')

const model = computed(() => normalizeFloorPlanModel(props.modelValue))
const canvasStyle = computed(() => ({
  width: `${model.value.canvas.width}px`,
  height: `${model.value.canvas.height}px`,
  background: model.value.canvas.backgroundColor
}))

const selectedRect = computed(() => model.value.items.find((item) => item.id === selectedId.value) || null)
const normalizedBackgroundColor = computed(() => {
  const raw = (model.value.canvas.backgroundColor || '').trim()
  const isHexColor = /^#([0-9a-fA-F]{6})$/.test(raw)
  return isHexColor ? raw : '#f8fafc'
})

watch(
  () => model.value.items,
  (items) => {
    if (!items.length) {
      selectedId.value = ''
      return
    }
    if (!selectedId.value || !items.some((item) => item.id === selectedId.value)) {
      selectedId.value = items[0].id
    }
  },
  { immediate: true, deep: true }
)

function emitModel(nextModel) {
  emit('update:modelValue', normalizeFloorPlanModel(nextModel))
}

function updateCanvasField(key, value) {
  const next = normalizeFloorPlanModel(model.value)
  next.canvas[key] = value
  emitModel(next)
}

function handleBackgroundColorPick(value) {
  updateCanvasField('backgroundColor', value || '#f8fafc')
}

function updateRect(rectId, patch) {
  const next = normalizeFloorPlanModel(model.value)
  const idx = next.items.findIndex((item) => item.id === rectId)
  if (idx < 0) return
  next.items[idx] = { ...next.items[idx], ...patch }
  if (next.items[idx].w < 40) next.items[idx].w = 40
  if (next.items[idx].h < 40) next.items[idx].h = 40
  if (next.items[idx].rotation > 360) next.items[idx].rotation = 360
  if (next.items[idx].rotation < -360) next.items[idx].rotation = -360
  emitModel(next)
}

function addRect() {
  if (props.readonly) return
  const next = normalizeFloorPlanModel(model.value)
  const rect = createDefaultRect(next.items.length)
  next.items.push(rect)
  selectedId.value = rect.id
  emitModel(next)
}

function removeSelected() {
  if (props.readonly || !selectedRect.value) return
  const next = normalizeFloorPlanModel(model.value)
  next.items = next.items.filter((item) => item.id !== selectedRect.value.id)
  emitModel(next)
}

function selectRect(id) {
  selectedId.value = id
}

function beginDrag(event, rect, mode = 'move') {
  if (props.readonly) return
  event.preventDefault()
  event.stopPropagation()
  selectedId.value = rect.id
  dragState.itemId = rect.id
  dragState.mode = mode
  dragState.startX = event.clientX
  dragState.startY = event.clientY
  dragState.originX = rect.x
  dragState.originY = rect.y
  dragState.originW = rect.w
  dragState.originH = rect.h
  dragState.active = true
  window.addEventListener('mousemove', onDragMove)
  window.addEventListener('mouseup', endDrag)
}

function onDragMove(event) {
  if (!dragState.active || !dragState.itemId) return
  const deltaX = event.clientX - dragState.startX
  const deltaY = event.clientY - dragState.startY
  if (dragState.mode === 'resize') {
    updateRect(dragState.itemId, {
      w: Math.max(40, Math.round(dragState.originW + deltaX)),
      h: Math.max(40, Math.round(dragState.originH + deltaY))
    })
    return
  }
  updateRect(dragState.itemId, {
    x: Math.round(dragState.originX + deltaX),
    y: Math.round(dragState.originY + deltaY)
  })
}

function endDrag() {
  dragState.active = false
  dragState.itemId = ''
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', endDrag)
}
</script>

<template>
  <div class="floor-editor">
    <div class="floor-editor__toolbar">
      <NButton type="primary" :disabled="readonly" @click="addRect">新增矩形</NButton>
      <NButton tertiary :disabled="readonly || !selectedRect" @click="removeSelected">删除选中</NButton>
      <span class="floor-editor__hint">拖拽矩形移动；右下角拖拽缩放；属性区可调旋转与文案</span>
    </div>

    <div class="floor-editor__body">
      <div class="canvas-wrap">
        <div class="canvas-stage" :style="canvasStyle">
          <button
            v-for="item in model.items"
            :key="item.id"
            type="button"
            class="rect-item"
            :class="{ active: item.id === selectedId }"
            :style="{
              width: `${item.w}px`,
              height: `${item.h}px`,
              transform: `translate(${item.x}px, ${item.y}px) rotate(${item.rotation}deg)`
            }"
            @mousedown="beginDrag($event, item, 'move')"
            @click.stop="selectRect(item.id)"
          >
            <span>{{ item.label || '未命名区域' }}</span>
            <i v-if="!readonly" class="resize-handle" @mousedown.stop.prevent="beginDrag($event, item, 'resize')"></i>
          </button>
        </div>
      </div>

      <div class="prop-panel">
        <h4>画布设置</h4>
        <div class="prop-grid">
          <label>宽度</label>
          <NInputNumber
            :disabled="readonly"
            :value="model.canvas.width"
            :min="480"
            :max="2200"
            @update:value="(v) => updateCanvasField('width', Number(v) || 960)"
          />
          <label>高度</label>
          <NInputNumber
            :disabled="readonly"
            :value="model.canvas.height"
            :min="320"
            :max="1600"
            @update:value="(v) => updateCanvasField('height', Number(v) || 600)"
          />
          <label>背景色</label>
          <div class="color-picker-wrap">
            <input
              class="color-picker"
              type="color"
              :disabled="readonly"
              :value="normalizedBackgroundColor"
              @input="(e) => handleBackgroundColorPick(e.target.value)"
            />
            <span class="color-value">{{ model.canvas.backgroundColor }}</span>
          </div>
        </div>

        <h4>区域属性</h4>
        <div v-if="selectedRect" class="prop-grid">
          <label>X</label>
          <NInputNumber :disabled="readonly" :value="selectedRect.x" @update:value="(v) => updateRect(selectedRect.id, { x: Number(v) || 0 })" />
          <label>Y</label>
          <NInputNumber :disabled="readonly" :value="selectedRect.y" @update:value="(v) => updateRect(selectedRect.id, { y: Number(v) || 0 })" />
          <label>宽度</label>
          <NInputNumber :disabled="readonly" :min="40" :value="selectedRect.w" @update:value="(v) => updateRect(selectedRect.id, { w: Number(v) || 40 })" />
          <label>高度</label>
          <NInputNumber :disabled="readonly" :min="40" :value="selectedRect.h" @update:value="(v) => updateRect(selectedRect.id, { h: Number(v) || 40 })" />
          <label>旋转</label>
          <NInputNumber :disabled="readonly" :min="-360" :max="360" :value="selectedRect.rotation" @update:value="(v) => updateRect(selectedRect.id, { rotation: Number(v) || 0 })" />
          <label>区域名称</label>
          <NInput :disabled="readonly" :value="selectedRect.label" @update:value="(v) => updateRect(selectedRect.id, { label: v || '' })" />
        </div>
        <p v-else class="empty-tip">请选择一个矩形区域进行编辑</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.floor-editor {
  display: grid;
  gap: 16px;
}

.floor-editor__toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.floor-editor__hint {
  font-size: 12px;
  color: #64748b;
}

.floor-editor__body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
  align-items: start;
}

.canvas-wrap {
  border: 1px solid #dbe4f0;
  border-radius: 12px;
  background: #fff;
  padding: 12px;
  overflow: auto;
}

.canvas-stage {
  position: relative;
  margin: 0 auto;
  border: 1px dashed #c5d2e6;
  border-radius: 10px;
  overflow: hidden;
}

.rect-item {
  position: absolute;
  display: grid;
  place-items: center;
  border: 2px solid #4f7bc3;
  border-radius: 10px;
  background: rgba(79, 123, 195, 0.18);
  color: #1f3e67;
  font-size: 13px;
  font-weight: 600;
  cursor: move;
  user-select: none;
}

.rect-item.active {
  border-color: #1d4ed8;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.25);
}

.rect-item span {
  padding: 8px;
  text-align: center;
  word-break: break-word;
}

.resize-handle {
  position: absolute;
  width: 12px;
  height: 12px;
  right: -6px;
  bottom: -6px;
  border-radius: 50%;
  background: #1d4ed8;
  cursor: nwse-resize;
}

.prop-panel {
  border: 1px solid #dbe4f0;
  border-radius: 12px;
  background: #fff;
  padding: 14px;
  display: grid;
  gap: 12px;
}

.prop-panel h4 {
  margin: 0;
  font-size: 14px;
}

.prop-grid {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}

.prop-grid label {
  font-size: 12px;
  color: #64748b;
}

.empty-tip {
  margin: 0;
  font-size: 12px;
  color: #94a3b8;
}

.color-picker-wrap {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.color-picker {
  width: 38px;
  height: 30px;
  border: 1px solid #dbe4f0;
  border-radius: 8px;
  background: #fff;
  padding: 2px;
  cursor: pointer;
}

.color-picker:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.color-value {
  font-size: 12px;
  color: #475569;
}

@media (max-width: 1200px) {
  .floor-editor__body {
    grid-template-columns: 1fr;
  }
}
</style>
