<template>
  <view class="page">
    <view v-if="loading" class="state-text">场地图详情加载中...</view>
    <view v-else-if="!detail" class="state-text">未获取到场地图详情</view>
    <view v-else class="card">
      <view class="head">
        <text class="title">{{ detail.title }}</text>
        <text class="status">PUBLISHED</text>
      </view>
      <text class="desc">{{ detail.description || "-" }}</text>
      <text class="update">发布时间：{{ formatDateTime(detail.updateTime) }}</text>
      <view class="canvas-wrap">
        <view class="canvas-inner">
          <view v-for="item in items" :key="item.id" class="shape" :style="shapeStyle(item)">
            <text class="shape-label">{{ item.label || item.id }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import dayjs from "dayjs";
import { computed, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { fetchFloorPlanDetailApi } from "../../api/venue";

const loading = ref(false);
const detail = ref(null);
const contentObj = ref(null);
const items = computed(() => contentObj.value?.items || []);

function formatDateTime(value) {
  return value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-";
}

function shapeStyle(item) {
  const canvasWidth = contentObj.value?.canvas?.width || 1200;
  const canvasHeight = contentObj.value?.canvas?.height || 800;
  const rotation = Number(item.rotation || 0);
  return {
    left: `${((item.x || 0) / canvasWidth) * 100}%`,
    top: `${((item.y || 0) / canvasHeight) * 100}%`,
    width: `${((item.w || 200) / canvasWidth) * 100}%`,
    height: `${((item.h || 120) / canvasHeight) * 100}%`,
    transform: `rotate(${rotation}deg)`,
    transformOrigin: "center center"
  };
}

async function loadDetail(id) {
  loading.value = true;
  try {
    const res = await fetchFloorPlanDetailApi(id);
    if (res.code !== 200 || !res.data) return;
    detail.value = res.data;
    try {
      contentObj.value = JSON.parse(res.data.contentJson || "{}");
    } catch (error) {
      contentObj.value = { canvas: { width: 1200, height: 800 }, items: [] };
    }
  } catch (error) {
    uni.showToast({ title: error.message || "加载失败", icon: "none" });
  } finally {
    loading.value = false;
  }
}

onLoad((query) => {
  if (query?.id) loadDetail(query.id);
});
</script>

<style scoped>
.page { min-height: 100vh; background: #f3f6ff; padding: 24rpx; }
.card { background: #fff; border-radius: 20rpx; padding: 20rpx; }
.head { display: flex; justify-content: space-between; align-items: center; }
.title { color: #1f2d47; font-size: 34rpx; font-weight: 700; }
.status { color: #4ea86e; border: 1rpx solid #8dd3a6; background: #ecfbf2; border-radius: 8rpx; padding: 2rpx 10rpx; font-size: 22rpx; }
.desc, .update { display: block; margin-top: 10rpx; color: #667792; font-size: 24rpx; }
.canvas-wrap { margin-top: 16rpx; border: 1rpx solid #e2e9fb; border-radius: 14rpx; background: #f6f8fd; padding: 12rpx; }
.canvas-inner { width: 100%; height: 520rpx; border-radius: 12rpx; background: #b9c0cb; position: relative; overflow: hidden; }
.shape { position: absolute; border: 2rpx solid #5c8fe6; border-radius: 12rpx; background: rgba(242, 247, 255, 0.45); display: flex; align-items: center; justify-content: center; }
.shape-label { color: #2a3a57; font-size: 22rpx; font-weight: 600; }
.state-text { text-align: center; color: #7b88a3; padding: 40rpx 0; }
</style>

