<template>
  <view class="page">
    <view v-if="loading" class="state-text">场地详情加载中...</view>
    <view v-else-if="!detail" class="state-text">未获取到场地详情</view>
    <view v-else class="card">
      <view class="grid">
        <view class="item"><text class="label">场地ID</text><text class="value">{{ detail.id }}</text></view>
        <view class="item"><text class="label">场地名称</text><text class="value">{{ detail.name }}</text></view>
        <view class="item"><text class="label">场地编号</text><text class="value">{{ detail.code }}</text></view>
        <view class="item"><text class="label">场地类型</text><text class="value">{{ detail.type }}</text></view>
        <view class="item"><text class="label">状态</text><text class="value">{{ statusText(detail.status) }}</text></view>
        <view class="item"><text class="label">容量</text><text class="value">{{ detail.capacity || "-" }}</text></view>
        <view class="item"><text class="label">价格</text><text class="value">¥{{ detail.price || 0 }}/小时</text></view>
        <view class="item"><text class="label">开放时间</text><text class="value">{{ detail.openTime || "-" }}</text></view>
        <view class="item"><text class="label">关闭时间</text><text class="value">{{ detail.closeTime || "-" }}</text></view>
        <view class="item"><text class="label">开放说明</text><text class="value">{{ detail.openTimeDesc || "-" }}</text></view>
      </view>

      <view class="block">
        <text class="label">描述</text>
        <text class="value">{{ detail.description || "-" }}</text>
      </view>
      <view class="block">
        <text class="label">备注</text>
        <text class="value">{{ detail.remark || "-" }}</text>
      </view>
      <view class="block">
        <text class="label">封面</text>
        <image v-if="coverPreview" :src="coverPreview" class="cover" mode="aspectFill" />
        <view v-else class="empty-cover">暂无图片</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { fetchVenueDetailApi } from "../../api/venue";
import { resolveImagePath } from "../../utils/image";

const loading = ref(false);
const detail = ref(null);
const coverPreview = ref("");

function statusText(status) {
  const map = {
    AVAILABLE: "可预约",
    DISABLED: "已停用",
    MAINTAIN: "维护中",
    SUSPEND: "暂停预约"
  };
  return map[status] || status || "-";
}

async function loadDetail(id) {
  loading.value = true;
  try {
    const res = await fetchVenueDetailApi(id);
    if (res.code !== 200 || !res.data) return;
    detail.value = res.data;
    if (res.data.coverImageUrl) {
      coverPreview.value = await resolveImagePath(res.data.coverImageUrl, { preferTempFile: false });
    }
  } catch (error) {
    uni.showToast({ title: error.message || "加载失败", icon: "none" });
  } finally {
    loading.value = false;
  }
}

onLoad((query) => {
  if (query?.id) {
    loadDetail(query.id);
  }
});
</script>

<style scoped>
.page { min-height: 100vh; background: #f3f6ff; padding: 24rpx; }
.card { background: #fff; border-radius: 20rpx; padding: 18rpx; }
.grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12rpx; }
.item, .block { border: 1rpx solid #e2e9fb; background: #f8faff; border-radius: 14rpx; padding: 14rpx; }
.block { margin-top: 12rpx; }
.label { color: #6d7c97; font-size: 23rpx; display: block; }
.value { color: #1f2d47; font-size: 30rpx; font-weight: 600; margin-top: 6rpx; display: block; }
.cover, .empty-cover { width: 100%; height: 280rpx; border-radius: 12rpx; margin-top: 10rpx; background: #edf2ff; }
.empty-cover { display: flex; align-items: center; justify-content: center; color: #8a97b0; font-size: 24rpx; }
.state-text { text-align: center; color: #7b88a3; padding: 40rpx 0; }
</style>

