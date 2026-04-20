<script setup>
import { computed } from 'vue'
import { normalizeFloorPlanModel } from '../../utils/floorPlan'

const props = defineProps({
  model: {
    type: Object,
    required: true
  },
  maxHeight: {
    type: Number,
    default: 520
  }
})

const normalized = computed(() => normalizeFloorPlanModel(props.model))
const stageStyle = computed(() => ({
  width: `${normalized.value.canvas.width}px`,
  height: `${normalized.value.canvas.height}px`,
  background: normalized.value.canvas.backgroundColor
}))
</script>

<template>
  <div class="preview-wrap" :style="{ maxHeight: `${maxHeight}px` }">
    <div class="preview-stage" :style="stageStyle">
      <div
        v-for="item in normalized.items"
        :key="item.id"
        class="preview-rect"
        :style="{
          width: `${item.w}px`,
          height: `${item.h}px`,
          transform: `translate(${item.x}px, ${item.y}px) rotate(${item.rotation}deg)`
        }"
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
</style>
