<template>
  <view class="page">
    <view class="module-card">
      <view class="module-head">
        <text class="module-title">场地图</text>
        <text class="module-subtitle">仅展示已发布场地图（PUBLISHED）</text>
      </view>
      <view class="search-row">
        <input v-model.trim="query.keyword" class="search-input" placeholder="按标题模糊查询场地图" />
        <button class="btn btn-search" @click="handleSearch">查询</button>
        <button class="btn btn-reset" @click="resetSearch">重置</button>
      </view>
      <view class="list-shell">
        <scroll-view scroll-y class="list-scroll">
          <view v-if="loading" class="state-text">场地图加载中...</view>
          <view v-else-if="!list.length" class="state-text">暂无场地图数据</view>
          <view v-else class="list-grid">
            <view v-for="item in list" :key="item.id" class="sku-card">
              <view class="sku-top">
                <text class="sku-title">{{ item.title }}</text>
                <text class="status-tag">{{ item.status }}</text>
              </view>
              <text class="sku-desc">{{ item.description || "-" }}</text>
              <text class="sku-update">更新时间：{{ formatDateTime(item.updateTime) }}</text>
              <button class="btn btn-detail" @click="goDetail(item.id)">查看详情</button>
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
import dayjs from "dayjs";
import { reactive, ref } from "vue";
import { fetchFloorPlanListApi } from "../../api/venue";
import PaginationBar from "../../components/PaginationBar.vue";

const loading = ref(false);
const list = ref([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = ref(5);
const pageSizeOptions = [5, 10, 20];
const query = reactive({ keyword: "" });

function formatDateTime(value) {
  return value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-";
}

function resetSearch() {
  query.keyword = "";
  pageNo.value = 1;
  loadList();
}

function handleSearch() {
  pageNo.value = 1;
  loadList();
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/venue/floorplan-detail?id=${id}` });
}

async function loadList() {
  loading.value = true;
  try {
    const params = { status: "PUBLISHED", pageNo: pageNo.value, pageSize: pageSize.value };
    if (query.keyword) params.keyword = query.keyword;
    const res = await fetchFloorPlanListApi(params);
    list.value = res.code === 200 ? res.data?.records || [] : [];
    total.value = res.code === 200 ? Number(res.data?.total || 0) : 0;
  } catch (error) {
    uni.showToast({ title: error.message || "场地图加载失败", icon: "none" });
  } finally {
    loading.value = false;
  }
}

function onPageChange(payload) {
  pageNo.value = payload.pageNo;
  pageSize.value = payload.pageSize;
  loadList();
}

loadList();
</script>

<style scoped>
.page { height: 100vh; background: #f3f6ff; padding: 24rpx; box-sizing: border-box; display: flex; overflow: hidden; }
.module-card { background: #fff; border-radius: 24rpx; padding: 24rpx; box-shadow: 0 8rpx 22rpx rgba(40,67,129,.06); flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }
.module-head { margin-bottom: 14rpx; }
.module-title { color: #1e2c46; font-size: 36rpx; font-weight: 700; }
.module-subtitle { margin-top: 8rpx; display: block; color: #6e7d99; font-size: 24rpx; }
.search-row { display: flex; align-items: center; gap: 12rpx; }
.search-input { flex: 1; height: 76rpx; border-radius: 16rpx; border: 1rpx solid #dce5fb; background: #f8faff; padding: 0 20rpx; color: #2d3c5a; font-size: 26rpx; }
.btn { margin: 0; border: none; border-radius: 999rpx; font-size: 24rpx; line-height: 62rpx; height: 62rpx; padding: 0 24rpx; }
.btn-search { color: #fff; background: linear-gradient(135deg, #4d74ff 0%, #78a3ff 100%); }
.btn-reset, .btn-detail { color: #4a5b7d; background: #eef2fb; }
.list-shell { margin-top: 14rpx; flex: 1; min-height: 0; border: 1rpx solid #e2e9fb; border-radius: 16rpx; background: #f8faff; display: flex; flex-direction: column; overflow: hidden; }
.list-scroll { flex: 1; min-height: 0; padding: 14rpx; box-sizing: border-box; }
.pager-wrap { border-top: 1rpx solid #e3e9f8; background: #fff; border-bottom-left-radius: 16rpx; border-bottom-right-radius: 16rpx; padding: 6rpx 10rpx 12rpx; }
.state-text { color: #7b88a3; font-size: 25rpx; text-align: center; padding: 24rpx 0; }
.list-grid { display: flex; flex-direction: column; gap: 22rpx; }
.sku-card { background: #f8faff; border: 1rpx solid #e2e9fb; border-radius: 18rpx; padding: 18rpx; }
.sku-top { display: flex; justify-content: space-between; align-items: center; }
.sku-title { color: #1f2d47; font-size: 31rpx; font-weight: 700; }
.status-tag { padding: 4rpx 14rpx; border-radius: 999rpx; background: rgba(77,116,248,.12); color: #4d74f8; font-size: 22rpx; }
.sku-desc, .sku-update { margin-top: 8rpx; display: block; color: #667792; font-size: 24rpx; }
</style>

