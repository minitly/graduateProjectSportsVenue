<template>
  <view class="pager" v-if="total > 0">
    <button class="pager-btn" :disabled="pageNo <= 1" @click="emitChange(pageNo - 1, pageSize)">上一页</button>
    <text class="pager-text">第 {{ pageNo }} / {{ totalPages }} 页</text>
    <button class="pager-btn" :disabled="pageNo >= totalPages" @click="emitChange(pageNo + 1, pageSize)">下一页</button>
    <text class="size-label">每页</text>
    <picker :range="sizeOptions" @change="onSizeChange">
      <view class="size-picker">{{ pageSize }}</view>
    </picker>
    <text class="size-label">条</text>
  </view>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  pageNo: { type: Number, default: 1 },
  pageSize: { type: Number, default: 5 },
  total: { type: Number, default: 0 },
  sizeOptions: { type: Array, default: () => [5, 10, 20] }
});

const emit = defineEmits(["change"]);

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)));

function emitChange(nextPageNo, nextPageSize) {
  const page = Math.min(Math.max(1, nextPageNo), totalPages.value);
  emit("change", { pageNo: page, pageSize: nextPageSize });
}

function onSizeChange(e) {
  const nextSize = Number(props.sizeOptions[e.detail.value]);
  emit("change", { pageNo: 1, pageSize: nextSize });
}
</script>

<style scoped>
.pager {
  margin-top: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  flex-wrap: nowrap;
}

.pager-btn {
  margin: 0;
  min-width: 118rpx;
  height: 56rpx;
  line-height: 56rpx;
  border-radius: 999rpx;
  border: none;
  color: #4a5b7d;
  background: #eef2fb;
  font-size: 22rpx;
  padding: 0 14rpx;
}

.pager-text {
  text-align: center;
  color: #5f6f8c;
  font-size: 24rpx;
  white-space: nowrap;
}

.size-label {
  color: #687999;
  font-size: 22rpx;
  white-space: nowrap;
}

.size-picker {
  min-width: 70rpx;
  text-align: center;
  height: 48rpx;
  line-height: 48rpx;
  border-radius: 999rpx;
  border: 1rpx solid #dce5fb;
  background: #f8faff;
  color: #2d3c5a;
  font-size: 22rpx;
  padding: 0 12rpx;
  white-space: nowrap;
}
</style>

