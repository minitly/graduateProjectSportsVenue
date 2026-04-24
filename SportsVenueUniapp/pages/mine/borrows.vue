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
                <text class="title">借用单 #{{ item.id }}</text>
                <text class="tag" :class="statusClass(item.status)">{{ statusText(item.status) }}</text>
              </view>
              <view class="meta-grid">
                <text>器材：{{ item.itemName || "-" }}</text>
                <text>数量：{{ item.quantity ?? "-" }}</text>
                <text>申请时间：{{ formatDateTime(item.requestedTime || item.createTime) }}</text>
                <text>借出确认：{{ formatDateTime(item.approvedTime) }}</text>
                <text>归还确认：{{ formatDateTime(item.returnedTime) }}</text>
                <text>借出状态：{{ conditionText(item.conditionOnBorrow) }}</text>
                <text>归还状态：{{ conditionText(item.conditionOnReturn) }}</text>
                <text>损坏数量：{{ item.damagedLostCount ?? "-" }}</text>
                <text>备注：{{ item.remark || "-" }}</text>
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
import { fetchMyBorrowListApi } from "../../api/mine";

const loading = ref(false);
const list = ref([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = ref(5);
const pageSizeOptions = [5, 10, 20];
const activeStatus = ref("ALL");
const statusTabs = [
  { label: "全部", value: "ALL" },
  { label: "提出申请", value: "REQUESTED" },
  { label: "使用中", value: "USING" },
  { label: "已归还", value: "RETURNED" }
];

function statusText(status) {
  const map = { REQUESTED: "提出申请", USING: "使用中", RETURNED: "已归还" };
  return map[status] || status || "-";
}

function statusClass(status) {
  const map = {
    REQUESTED: "tag-blue",
    USING: "tag-yellow",
    RETURNED: "tag-green"
  };
  return map[status] || "tag-gray";
}

function conditionText(value) {
  const map = {
    GOOD: "完好",
    DAMAGED: "损坏",
    LOST: "丢失"
  };
  return map[value] || (value || "-");
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

async function loadList() {
  loading.value = true;
  try {
    const params = {
      pageNo: pageNo.value,
      pageSize: pageSize.value
    };
    if (activeStatus.value !== "ALL") params.status = activeStatus.value;
    const res = await fetchMyBorrowListApi(params);
    list.value = res.code === 200 ? res.data?.records || [] : [];
    total.value = res.code === 200 ? Number(res.data?.total || 0) : 0;
  } catch (error) {
    uni.showToast({ title: error.message || "加载失败", icon: "none" });
  } finally {
    loading.value = false;
  }
}

onLoad((query) => {
  if (query?.status && statusTabs.some((tab) => tab.value === query.status)) {
    activeStatus.value = query.status;
  }
});

onShow(() => {
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
.tag-yellow { background: rgba(234, 163, 40, .14); color: #b7791f; }
.tag-green { background: rgba(34, 197, 94, .14); color: #15803d; }
.tag-gray { background: rgba(100, 116, 139, .14); color: #475569; }
.meta-grid { margin-top: 8rpx; display: flex; flex-direction: column; gap: 4rpx; color: #6f7f9b; font-size: 22rpx; }
</style>

