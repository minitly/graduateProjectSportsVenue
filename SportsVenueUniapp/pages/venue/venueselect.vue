<template>
  <view class="page">
    <view class="module-card">
      <view class="module-head">
        <text class="module-title">场地选择</text>
        <text class="module-subtitle">所有场地均可查看详情，仅可预约状态显示预约按钮</text>
      </view>
      <view class="search-panel">
        <view class="search-row-first">
          <input v-model.trim="query.keyword" class="search-input keyword-input" placeholder="关键字（场地名称/编号）" />
          <picker class="status-picker" :range="statusOptions" range-key="label" @change="onStatusChange">
            <view class="picker-box">状态：{{ selectedStatusLabel }}</view>
          </picker>
        </view>
        <view class="search-row-second">
          <input v-model.trim="query.type" class="search-input type-input" placeholder="场地类型（如 篮球场）" />
          <button class="btn btn-search action-btn" @click="handleSearch">查询</button>
          <button class="btn btn-reset action-btn" @click="resetSearch">重置</button>
        </view>
      </view>
      <view class="list-shell">
        <scroll-view scroll-y class="list-scroll">
          <view v-if="loading" class="state-text">场地加载中...</view>
          <view v-else-if="!list.length" class="state-text">暂无场地数据</view>
          <view v-else class="venue-grid">
            <view v-for="item in list" :key="item.id" class="venue-card">
              <image v-if="item.coverPreview" :src="item.coverPreview" class="venue-image" mode="aspectFill" lazy-load />
              <view v-else class="venue-image placeholder">暂无图片</view>
              <view class="venue-body">
                <view class="venue-top">
                  <text class="venue-name">{{ item.name }}</text>
                  <text class="venue-status">{{ statusText(item.status) }}</text>
                </view>
                <text class="venue-meta">编号 {{ item.code }} · {{ item.type }} · 容量 {{ item.capacity || "-" }}</text>
                <text class="venue-desc">{{ item.description || "-" }}</text>
                <view class="venue-info">
                  <text>开放时间 {{ item.openTime || "--:--" }} - {{ item.closeTime || "--:--" }}</text>
                  <text class="venue-price">¥{{ item.price || 0 }}/小时</text>
                </view>
                <view class="action-row">
                  <button class="btn btn-detail" @click="goDetail(item.id)">查看详情</button>
                  <button v-if="item.status === 'AVAILABLE'" class="btn btn-book" @click="goBooking(item)">预约时段</button>
                </view>
              </view>
            </view>
          </view>
        </scroll-view>
        <view class="pager-wrap">
          <PaginationBar
            :page-no="pageNo"
            :page-size="pageSize"
            :total="total"
            :size-options="pageSizeOptions"
            @change="onPageChange"
          />
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from "vue";
import { fetchVenueListApi } from "../../api/venue";
import { fetchMyViolationStatusApi } from "../../api/booking";
import { resolveImagePaths } from "../../utils/image";
import PaginationBar from "../../components/PaginationBar.vue";

const loading = ref(false);
const list = ref([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = ref(5);
const pageSizeOptions = [5, 10, 20];
const statusOptions = [
  { label: "全部", value: "" },
  { label: "可预约", value: "AVAILABLE" },
  { label: "已停用", value: "DISABLED" },
  { label: "维护中", value: "MAINTAIN" },
  { label: "暂停预约", value: "SUSPEND" }
];
const query = reactive({ keyword: "", type: "", status: "" });

const selectedStatusLabel = computed(() => statusOptions.find((o) => o.value === query.status)?.label || "全部");

function onStatusChange(e) {
  query.status = statusOptions[e.detail.value].value;
}

function resetSearch() {
  query.keyword = "";
  query.type = "";
  query.status = "";
  pageNo.value = 1;
  loadList();
}

function handleSearch() {
  pageNo.value = 1;
  loadList();
}

function statusText(status) {
  const map = { AVAILABLE: "可预约", DISABLED: "已停用", MAINTAIN: "维护中", SUSPEND: "暂停预约" };
  return map[status] || status || "-";
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/venue/detail?id=${id}` });
}

function goBooking(item) {
  if (!item?.id) return;
  checkAndGoBooking(item.id);
}

async function loadList() {
  loading.value = true;
  try {
    const params = { pageNo: pageNo.value, pageSize: pageSize.value };
    if (query.keyword) params.keyword = query.keyword;
    if (query.type) params.type = query.type;
    if (query.status) params.status = query.status;
    const res = await fetchVenueListApi(params);
    const records = res.code === 200 ? res.data?.records || [] : [];
    total.value = res.code === 200 ? Number(res.data?.total || 0) : 0;
    const map = await resolveImagePaths(records.map((r) => r.coverImageUrl), {
      preferTempFile: true,
      concurrency: 2
    });
    list.value = records.map((item) => ({ ...item, coverPreview: item.coverImageUrl ? map[item.coverImageUrl] || "" : "" }));
  } catch (error) {
    uni.showToast({ title: error.message || "场地加载失败", icon: "none" });
  } finally {
    loading.value = false;
  }
}

function onPageChange(payload) {
  pageNo.value = payload.pageNo;
  pageSize.value = payload.pageSize;
  loadList();
}

function formatViolationMessage(data) {
  const violationMonth = data?.violationMonth || "-";
  const violationCountMonth = data?.violationCountMonth ?? 0;
  const bannedUntil = data?.bookingBannedUntil ? String(data.bookingBannedUntil).replace("T", " ") : "-";
  return `你已违规，无法预约场地。\n违规月份：${violationMonth}\n当月违规次数：${violationCountMonth}\n限制截止：${bannedUntil}`;
}

async function checkAndGoBooking(id) {
  try {
    const res = await fetchMyViolationStatusApi();
    if (res.code !== 200) {
      uni.showModal({
        title: "提示",
        content: res.message || "校验预约资格失败",
        showCancel: false
      });
      return;
    }
    if (res.data?.isViolationUser) {
      uni.showModal({
        title: "当前账号已违规，暂不可预约",
        content: formatViolationMessage(res.data),
        showCancel: false
      });
      return;
    }
    uni.navigateTo({
      url: `/pages/venue/booking?venueId=${id}`
    });
  } catch (error) {
    uni.showModal({
      title: "提示",
      content: error?.message || "校验预约资格失败",
      showCancel: false
    });
  }
}

loadList();
</script>

<style scoped>
.page { height: 100vh; background: #f3f6ff; padding: 24rpx; box-sizing: border-box; display: flex; overflow: hidden; }
.module-card { background: #fff; border-radius: 24rpx; padding: 24rpx; box-shadow: 0 8rpx 22rpx rgba(40,67,129,.06); flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }
.module-head { margin-bottom: 14rpx; }
.module-title { color: #1e2c46; font-size: 36rpx; font-weight: 700; }
.module-subtitle { margin-top: 8rpx; display: block; color: #6e7d99; font-size: 24rpx; }
.search-panel { display: flex; flex-direction: column; gap: 12rpx; }
.search-row-first, .search-row-second { display: flex; align-items: center; gap: 12rpx; }
.keyword-input, .type-input { flex: 2; min-width: 0; }
.status-picker, .action-btn { flex: 1; min-width: 0; }
.search-input, .picker-box { height: 76rpx; border-radius: 16rpx; border: 1rpx solid #dce5fb; background: #f8faff; padding: 0 20rpx; display: flex; align-items: center; color: #2d3c5a; font-size: 26rpx; }
.action-row { display: flex; align-items: center; gap: 12rpx; margin-top: 10rpx; }
.btn { margin: 0; border: none; border-radius: 999rpx; font-size: 24rpx; line-height: 62rpx; height: 62rpx; padding: 0 24rpx; }
.btn-search, .btn-book { color: #fff; background: linear-gradient(135deg, #4d74ff 0%, #78a3ff 100%); }
.btn-reset, .btn-detail { color: #4a5b7d; background: #eef2fb; }
.list-shell { margin-top: 14rpx; flex: 1; min-height: 0; border: 1rpx solid #e2e9fb; border-radius: 16rpx; background: #f8faff; display: flex; flex-direction: column; overflow: hidden; }
.list-scroll { flex: 1; min-height: 0; padding: 14rpx; box-sizing: border-box; }
.pager-wrap { border-top: 1rpx solid #e3e9f8; background: #fff; border-bottom-left-radius: 16rpx; border-bottom-right-radius: 16rpx; padding: 6rpx 10rpx 12rpx; }
.state-text { color: #7b88a3; font-size: 25rpx; text-align: center; padding: 24rpx 0; }
.venue-grid { display: flex; flex-direction: column; gap: 14rpx; }
.venue-card { background: #fff; border-radius: 20rpx; border: 1rpx solid #e2e9fb; overflow: hidden; }
.venue-image { width: 100%; height: 280rpx; background: #edf2ff; }
.placeholder { display: flex; align-items: center; justify-content: center; color: #8b97af; font-size: 24rpx; }
.venue-body { padding: 18rpx; }
.venue-top { display: flex; justify-content: space-between; align-items: center; }
.venue-name { color: #1f2d47; font-size: 31rpx; font-weight: 700; }
.venue-status { padding: 4rpx 14rpx; border-radius: 999rpx; background: rgba(77,116,248,.12); color: #4d74f8; font-size: 22rpx; }
.venue-meta, .venue-desc, .venue-info { margin-top: 8rpx; display: block; color: #667792; font-size: 24rpx; }
.venue-info { display: flex; justify-content: space-between; align-items: center; }
.venue-price { color: #1f2d47; font-size: 30rpx; font-weight: 700; }
</style>

