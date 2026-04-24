<template>
  <view class="page">
    <view class="hero-card">
      <text class="hero-tag">场地服务</text>
      <text class="hero-title">场地与场地图</text>
      <text class="hero-desc">请选择要进入的模块，继续查询和操作。</text>
    </view>

    <view class="entry-grid">
      <view class="entry-card" @click="goModule('floorPlan')">
        <view class="icon-wrap">
          <uni-icons type="map-filled" size="38" color="#4d74f8"></uni-icons>
        </view>
        <text class="entry-title">场地图</text>
        <text class="entry-desc">查看已发布场地图，进入详情页浏览布局。</text>
      </view>

      <view class="entry-card" @click="goModule('venueSelect')">
        <view class="icon-wrap">
          <uni-icons type="calendar-filled" size="38" color="#4d74f8"></uni-icons>
        </view>
        <text class="entry-title">场地选择</text>
        <text class="entry-desc">按条件筛选场地并查看详情，可预约场地展示预约按钮。</text>
      </view>
    </view>
  </view>
</template>

<script setup>
function goModule(module) {
  const pathMap = {
    floorPlan: "/pages/venue/floorplan",
    venueSelect: "/pages/venue/venueselect"
  };
  uni.navigateTo({
    url: pathMap[module]
  });
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f3f6ff;
  padding: 24rpx;
}

.hero-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 24rpx;
  box-shadow: 0 8rpx 22rpx rgba(40, 67, 129, 0.06);
}

.hero-tag {
  color: #4d74f8;
  font-size: 24rpx;
  font-weight: 600;
}

.hero-title {
  margin-top: 8rpx;
  display: block;
  color: #1f2d47;
  font-size: 52rpx;
  line-height: 1.22;
  font-weight: 700;
}

.hero-desc {
  margin-top: 12rpx;
  display: block;
  color: #647491;
  font-size: 26rpx;
}

.entry-grid {
  margin-top: 20rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.entry-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  box-shadow: 0 8rpx 22rpx rgba(40, 67, 129, 0.06);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.icon-wrap {
  width: 108rpx;
  height: 108rpx;
  border-radius: 999rpx;
  background: rgba(77, 116, 248, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
}

.entry-title {
  margin-top: 18rpx;
  color: #1f2d47;
  font-size: 34rpx;
  font-weight: 700;
}

.entry-desc {
  margin-top: 10rpx;
  color: #677995;
  font-size: 24rpx;
  line-height: 1.5;
}
</style>

