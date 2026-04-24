<template>
  <view class="page">
    <view class="module-card">
      <view class="tab-row">
        <view
          v-for="tab in statusTabs"
          :key="tab.value"
          class="tab-item"
          :class="{ active: activeStatus === tab.value }"
          @click="switchStatus(tab.value)"
        >
          {{ tab.label }}
        </view>
      </view>

      <view class="list-shell">
        <scroll-view scroll-y class="list-scroll">
          <view v-if="loading" class="state-text">加载中...</view>
          <view v-else-if="!list.length" class="state-text">暂无数据</view>
          <view v-else class="list-grid">
            <view v-for="item in list" :key="item.id" class="record-card">
              <view class="head">
                <text class="title">预约编号 #{{ item.id }}</text>
                <text class="tag" :class="statusClass(item.status)">{{ statusText(item.status) }}</text>
              </view>
              <text class="line">{{ item.venueName || "-" }}</text>
              <view class="meta-grid">
                <text>开始：{{ formatDateTime(item.startTime) }}</text>
                <text>结束：{{ formatDateTime(item.endTime) }}</text>
                <text>创建：{{ formatDateTime(item.createTime) }}</text>
              </view>
              <view class="action-row" v-if="item.status === 'APPLIED'">
                <button class="btn btn-warn" @click="cancelBooking(item)">取消预约</button>
              </view>
            </view>
          </view>
        </scroll-view>
        <view class="pager-wrap">
          <PaginationBar :page-no="pageNo" :page-size="pageSize" :total="total" :size-options="pageSizeOptions" @change="onPageChange" />
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import dayjs from "dayjs";
import { ref } from "vue";
import { onLoad, onShow } from "@dcloudio/uni-app";
import PaginationBar from "../../components/PaginationBar.vue";
import { fetchMyViolationStatusApi } from "../../api/booking";
import { cancelBookingApi, fetchMyBookingListApi } from "../../api/mine";

const loading = ref(false);
const list = ref([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = ref(5);
const pageSizeOptions = [5, 10, 20];
const activeStatus = ref("ALL");
const violationCountMonth = ref(0);
const statusTabs = [
  { label: "全部", value: "ALL" },
  { label: "正在预约", value: "APPLIED" },
  { label: "已取消", value: "CANCELED" },
  { label: "已完成", value: "VERIFIED" },
  { label: "违规", value: "VIOLATION" }
];

function statusText(status) {
  const map = { APPLIED: "正在预约", CANCELED: "已取消", VERIFIED: "已完成", VIOLATION: "违规" };
  return map[status] || status || "-";
}

function statusClass(status) {
  const map = {
    APPLIED: "tag-blue",
    CANCELED: "tag-gray",
    VERIFIED: "tag-green",
    VIOLATION: "tag-red"
  };
  return map[status] || "tag-gray";
}

function formatDateTime(value) {
  return value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-";
}

function switchStatus(status) {
  activeStatus.value = status;
  pageNo.value = 1;
  loadList();
}

function onPageChange(payload) {
  pageNo.value = payload.pageNo;
  pageSize.value = payload.pageSize;
  loadList();
}

async function loadViolationCounter() {
  try {
    const res = await fetchMyViolationStatusApi();
    if (res.code === 200 && res.data) {
      violationCountMonth.value = Number(res.data.violationCountMonth || 0);
    }
  } catch (_) {}
}

async function loadList() {
  loading.value = true;
  try {
    const params = {
      pageNo: pageNo.value,
      pageSize: pageSize.value
    };
    if (activeStatus.value !== "ALL") params.status = activeStatus.value;
    const res = await fetchMyBookingListApi(params);
    list.value = res.code === 200 ? res.data?.records || [] : [];
    total.value = res.code === 200 ? Number(res.data?.total || 0) : 0;
  } catch (error) {
    uni.showToast({ title: error.message || "加载失败", icon: "none" });
  } finally {
    loading.value = false;
  }
}

function hoursUntilStart(item) {
  const startAt = new Date(item?.startTime).getTime();
  if (!Number.isFinite(startAt)) return Infinity;
  return (startAt - Date.now()) / (1000 * 60 * 60);
}

function doCancel(item) {
  uni.showModal({
    title: "确认取消预约",
    content: `确认取消预约 #${item.id} 吗？`,
    success: async (modalRes) => {
      if (!modalRes.confirm) return;
      try {
        const res = await cancelBookingApi(item.id);
        if (res.code !== 200) {
          uni.showModal({ title: "取消失败", content: res.message || "请稍后重试", showCancel: false });
          return;
        }
        uni.showToast({ title: "预约已取消", icon: "success" });
        loadList();
      } catch (error) {
        uni.showModal({ title: "取消失败", content: error.message || "无法连接后端服务", showCancel: false });
      }
    }
  });
}

function cancelBooking(item) {
  const h = hoursUntilStart(item);
  if (h < 2) {
    const nextCount = Number(violationCountMonth.value || 0) + 1;
    uni.showModal({
      title: "临近开始时间取消确认",
      content: `当前预约距离开始不足2小时，取消将计为违规。\n当前为第 ${nextCount} 次违规；累计3次违规将无法再预约。`,
      confirmText: "仍要取消",
      cancelText: "返回",
      success: (res) => {
        if (res.confirm) doCancel(item);
      }
    });
    return;
  }
  doCancel(item);
}

onLoad((query) => {
  if (query?.status && statusTabs.some((tab) => tab.value === query.status)) {
    activeStatus.value = query.status;
  }
});

onShow(() => {
  loadViolationCounter();
  loadList();
});
</script>

<style scoped>
.page { height: 100vh; background: #f3f6ff; padding: 24rpx; box-sizing: border-box; overflow: hidden; }
.module-card { height: 100%; background: #fff; border-radius: 20rpx; padding: 16rpx; box-sizing: border-box; display: flex; flex-direction: column; overflow: hidden; }
.tab-row { display: flex; gap: 8rpx; overflow-x: auto; }
.tab-item { flex-shrink: 0; padding: 12rpx 20rpx; border-radius: 999rpx; background: #eef2fb; color: #4a5b7d; font-size: 24rpx; }
.tab-item.active { background: linear-gradient(135deg, #4d74ff 0%, #78a3ff 100%); color: #fff; }
.list-shell { margin-top: 12rpx; flex: 1; min-height: 0; border: 1rpx solid #e2e9fb; border-radius: 16rpx; background: #f8faff; display: flex; flex-direction: column; overflow: hidden; }
.list-scroll { flex: 1; min-height: 0; padding: 12rpx; box-sizing: border-box; }
.pager-wrap { border-top: 1rpx solid #e3e9f8; background: #fff; padding: 6rpx 10rpx 12rpx; }
.state-text { color: #7b88a3; text-align: center; padding: 24rpx 0; font-size: 24rpx; }
.list-grid { display: flex; flex-direction: column; gap: 10rpx; }
.record-card { background: #fff; border: 1rpx solid #dfe8fb; border-radius: 14rpx; padding: 14rpx; }
.head { display: flex; justify-content: space-between; align-items: center; gap: 10rpx; }
.title { font-size: 28rpx; font-weight: 700; color: #1f2d47; }
.tag { padding: 4rpx 12rpx; border-radius: 999rpx; font-size: 20rpx; }
.tag-blue { background: rgba(77,116,248,.12); color: #4d74f8; }
.tag-gray { background: rgba(100, 116, 139, .14); color: #475569; }
.tag-green { background: rgba(34, 197, 94, .14); color: #15803d; }
.tag-red { background: rgba(239, 68, 68, .14); color: #b91c1c; }
.line { display: block; margin-top: 6rpx; color: #61708d; font-size: 24rpx; }
.meta-grid { margin-top: 8rpx; display: flex; flex-direction: column; gap: 4rpx; color: #6f7f9b; font-size: 22rpx; }
.action-row { margin-top: 10rpx; display: flex; justify-content: flex-end; }
.btn { margin: 0; border: none; border-radius: 999rpx; height: 56rpx; line-height: 56rpx; font-size: 22rpx; padding: 0 20rpx; }
.btn-warn { background: #eef2fb; color: #7a5f29; }
</style>

