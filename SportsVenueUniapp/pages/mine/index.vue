<template>
  <view class="page">
    <view class="module-card">
      <view class="top-row">
        <text class="title">用户中心</text>
        <button class="logout-btn" @click="logout">退出登录</button>
      </view>

      <view class="user-card">
        <view class="avatar">{{ avatarLetter }}</view>
        <view class="user-main">
          <text class="real-name">{{ user.realName || "-" }}</text>
          <text class="user-name">@{{ user.username || "-" }}</text>
        </view>
      </view>

      <view class="info-grid">
        <view class="info-item">
          <text class="info-label">本月违规次数</text>
          <text class="info-value">{{ violation.violationCountMonth }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">违规月份</text>
          <text class="info-value">{{ violation.violationMonth || "-" }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">预约禁用截至</text>
          <text class="info-value">{{ formatDate(violation.bookingBannedUntil) }}</text>
        </view>
      </view>

      <view class="order-section">
        <view class="section-head">
          <text class="section-title">我的预约</text>
          <text class="all-link" @click="goBookings('ALL')">全部</text>
        </view>
        <view class="icon-row icon-row-4">
          <view class="icon-item" @click="goBookings('APPLIED')">
            <uni-icons type="calendar" size="22" color="#4d74f8"></uni-icons>
            <text>正在预约</text>
          </view>
          <view class="icon-item" @click="goBookings('CANCELED')">
            <uni-icons type="closeempty" size="22" color="#4d74f8"></uni-icons>
            <text>已取消</text>
          </view>
          <view class="icon-item" @click="goBookings('VERIFIED')">
            <uni-icons type="checkbox-filled" size="22" color="#4d74f8"></uni-icons>
            <text>已完成</text>
          </view>
          <view class="icon-item" @click="goBookings('VIOLATION')">
            <uni-icons type="info" size="22" color="#4d74f8"></uni-icons>
            <text>违规</text>
          </view>
        </view>
      </view>

      <view class="order-section">
        <view class="section-head">
          <text class="section-title">我的借用</text>
          <text class="all-link" @click="goBorrows('ALL')">全部</text>
        </view>
        <view class="icon-row icon-row-3">
          <view class="icon-item" @click="goBorrows('REQUESTED')">
            <uni-icons type="paperplane" size="22" color="#4d74f8"></uni-icons>
            <text>提出申请</text>
          </view>
          <view class="icon-item" @click="goBorrows('USING')">
            <uni-icons type="loop" size="22" color="#4d74f8"></uni-icons>
            <text>使用中</text>
          </view>
          <view class="icon-item" @click="goBorrows('RETURNED')">
            <uni-icons type="checkmarkempty" size="22" color="#4d74f8"></uni-icons>
            <text>已归还</text>
          </view>
        </view>
      </view>

      <view class="entry-list">
        <view class="entry-item" @click="goWallet">
          <view class="entry-left">
            <uni-icons type="wallet-filled" size="20" color="#4d74f8"></uni-icons>
            <text>我的钱包</text>
          </view>
          <uni-icons type="right" size="18" color="#9aa8c3"></uni-icons>
        </view>
        <view class="entry-item" @click="goProfileEdit">
          <view class="entry-left">
            <uni-icons type="compose" size="20" color="#4d74f8"></uni-icons>
            <text>修改资料</text>
          </view>
          <uni-icons type="right" size="18" color="#9aa8c3"></uni-icons>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import dayjs from "dayjs";
import { computed, reactive } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { useAuthStore } from "../../stores/auth";
import { fetchMyViolationStatusApi } from "../../api/booking";
import { fetchUserByIdApi } from "../../api/mine";

const authStore = useAuthStore();
const user = reactive({
  id: "",
  username: "",
  realName: ""
});
const violation = reactive({
  violationCountMonth: 0,
  violationMonth: "",
  bookingBannedUntil: ""
});

const avatarLetter = computed(() => {
  const source = String(user.realName || user.username || "U");
  return source.slice(0, 1).toUpperCase();
});

function formatDateTime(value) {
  return value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-";
}

function formatDate(value) {
  return value ? dayjs(value).format("YYYY-MM-DD") : "-";
}

async function loadUserInfo() {
  const localUser = authStore.userInfo || {};
  user.id = localUser.id || "";
  user.username = localUser.username || "";
  user.realName = localUser.realName || "";
  if (!user.id) return;
  try {
    const res = await fetchUserByIdApi(user.id);
    if (res.code === 200 && res.data) {
      user.username = res.data.username || user.username;
      user.realName = res.data.realName || user.realName;
    }
  } catch (_) {}
}

async function loadViolationStatus() {
  try {
    const res = await fetchMyViolationStatusApi();
    if (res.code !== 200 || !res.data) return;
    violation.violationCountMonth = res.data.violationCountMonth ?? 0;
    violation.violationMonth = res.data.violationMonth || "";
    violation.bookingBannedUntil = res.data.bookingBannedUntil || "";
  } catch (_) {}
}

function goBookings(status) {
  uni.navigateTo({ url: `/pages/mine/bookings?status=${status}` });
}

function goBorrows(status) {
  uni.navigateTo({ url: `/pages/mine/borrows?status=${status}` });
}

function goWallet() {
  uni.navigateTo({ url: "/pages/mine/wallet" });
}

function goProfileEdit() {
  uni.navigateTo({ url: "/pages/mine/profile-edit" });
}

function logout() {
  authStore.clearAuth();
  uni.reLaunch({ url: "/pages/index/index" });
}

onShow(() => {
  loadUserInfo();
  loadViolationStatus();
});
</script>

<style scoped>
.page { min-height: 100vh; padding: 24rpx; background: #f3f6ff; }
.module-card { background: #fff; border-radius: 24rpx; padding: 24rpx; box-shadow: 0 8rpx 22rpx rgba(40,67,129,.06); }
.top-row { display: flex; justify-content: space-between; align-items: center; }
.title { color: #1e2c46; font-size: 36rpx; font-weight: 700; }
.logout-btn { margin: 0; border: none; border-radius: 999rpx; background: #eef2fb; color: #4a5b7d; font-size: 24rpx; height: 58rpx; line-height: 58rpx; padding: 0 20rpx; }
.user-card { margin-top: 16rpx; display: flex; align-items: center; gap: 16rpx; background: #f8faff; border: 1rpx solid #e2e9fb; border-radius: 16rpx; padding: 16rpx; }
.avatar { width: 88rpx; height: 88rpx; border-radius: 50%; background: linear-gradient(135deg, #4d74ff 0%, #78a3ff 100%); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 40rpx; font-weight: 700; }
.real-name { color: #1f2d47; font-size: 32rpx; font-weight: 700; display: block; }
.user-name { color: #7584a0; font-size: 24rpx; display: block; margin-top: 4rpx; }
.info-grid { margin-top: 14rpx; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10rpx; }
.info-item { background: #f8faff; border: 1rpx solid #e2e9fb; border-radius: 14rpx; padding: 14rpx; }
.info-label { color: #72819b; font-size: 22rpx; display: block; }
.info-value { margin-top: 6rpx; color: #1f2d47; font-size: 26rpx; font-weight: 700; display: block; }
.order-section { margin-top: 16rpx; background: #f8faff; border: 1rpx solid #e2e9fb; border-radius: 16rpx; padding: 14rpx; }
.section-head { display: flex; justify-content: space-between; align-items: center; }
.section-title { color: #1f2d47; font-size: 30rpx; font-weight: 700; }
.all-link { color: #4d74f8; font-size: 24rpx; }
.icon-row { margin-top: 12rpx; display: grid; gap: 8rpx; }
.icon-row-4 { grid-template-columns: repeat(4, minmax(0, 1fr)); }
.icon-row-3 { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.icon-item { background: #fff; border: 1rpx solid #dfe8fb; border-radius: 12rpx; min-height: 108rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8rpx; }
.icon-item text { color: #4a5b7d; font-size: 22rpx; }
.entry-list { margin-top: 16rpx; background: #f8faff; border: 1rpx solid #e2e9fb; border-radius: 16rpx; overflow: hidden; }
.entry-item { min-height: 86rpx; display: flex; align-items: center; justify-content: space-between; padding: 0 16rpx; background: #fff; }
.entry-item + .entry-item { border-top: 1rpx solid #edf1fb; }
.entry-left { display: flex; align-items: center; gap: 12rpx; color: #1f2d47; font-size: 26rpx; }
</style>

