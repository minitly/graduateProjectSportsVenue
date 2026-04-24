<template>
  <view class="page">
    <view class="module-card">
      <view class="module-head">
        <text class="module-title">公告中心</text>
        <text class="module-subtitle">查看平台已发布公告与通知</text>
      </view>

      <view class="search-panel">
        <input v-model.trim="query.keyword" class="search-input" placeholder="按标题搜索公告" />
        <button class="btn btn-search" @click="handleSearch">查询</button>
        <button class="btn btn-reset" @click="resetSearch">重置</button>
      </view>

      <view class="list-shell">
        <scroll-view scroll-y class="list-scroll">
          <view v-if="loading" class="state-text">公告加载中...</view>
          <view v-else-if="!list.length" class="state-text">暂无公告数据</view>
          <view v-else class="notice-grid">
            <view v-for="item in list" :key="item.id" class="notice-card">
              <view class="notice-top">
                <text class="notice-title">{{ item.title || "-" }}</text>
                <text class="notice-tag">已发布</text>
              </view>
              <text class="notice-desc">{{ summaryText(item.content) }}</text>
              <text class="notice-time">发布时间：{{ formatDateTime(item.publishTime || item.updateTime) }}</text>
              <view class="action-row">
                <button class="btn btn-detail" @click="openDetail(item.id)">查看详情</button>
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

    <view v-if="showDetail" class="modal-mask" @click="closeDetail"></view>
    <view v-if="showDetail" class="modal-panel">
      <view class="detail-head">
        <text class="detail-title">{{ detail?.title || "公告详情" }}</text>
      </view>
      <scroll-view scroll-y class="detail-scroll">
        <text class="detail-time">发布时间：{{ formatDateTime(detail?.publishTime || detail?.updateTime) }}</text>
        <rich-text class="detail-content" :nodes="detail?.content || ''"></rich-text>
      </scroll-view>
      <button class="btn btn-search detail-close" @click="closeDetail">关闭</button>
    </view>
  </view>
</template>

<script setup>
import dayjs from "dayjs";
import { reactive, ref } from "vue";
import { fetchNoticeDetailApi, fetchNoticeListApi } from "../../api/notice";
import PaginationBar from "../../components/PaginationBar.vue";

const loading = ref(false);
const showDetail = ref(false);
const detail = ref(null);
const list = ref([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = ref(5);
const pageSizeOptions = [5, 10, 20];
const query = reactive({ keyword: "" });

function formatDateTime(value) {
  return value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-";
}

function summaryText(html) {
  const text = String(html || "").replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim();
  if (!text) return "暂无正文";
  return text.length > 46 ? `${text.slice(0, 46)}...` : text;
}

function handleSearch() {
  pageNo.value = 1;
  loadList();
}

function resetSearch() {
  query.keyword = "";
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
    const params = { pageNo: pageNo.value, pageSize: pageSize.value };
    if (query.keyword) params.keyword = query.keyword;
    const res = await fetchNoticeListApi(params);
    list.value = res.code === 200 ? res.data?.records || [] : [];
    total.value = res.code === 200 ? Number(res.data?.total || 0) : 0;
  } catch (error) {
    uni.showToast({ title: error.message || "公告加载失败", icon: "none" });
  } finally {
    loading.value = false;
  }
}

async function openDetail(id) {
  try {
    const res = await fetchNoticeDetailApi(id);
    if (res.code !== 200 || !res.data) {
      uni.showToast({ title: res.message || "详情加载失败", icon: "none" });
      return;
    }
    detail.value = res.data;
    showDetail.value = true;
  } catch (error) {
    uni.showToast({ title: error.message || "详情加载失败", icon: "none" });
  }
}

function closeDetail() {
  showDetail.value = false;
}

loadList();
</script>

<style scoped>
.page { height: 100vh; background: #f3f6ff; padding: 24rpx; box-sizing: border-box; display: flex; overflow: hidden; }
.module-card { background: #fff; border-radius: 24rpx; padding: 24rpx; box-shadow: 0 8rpx 22rpx rgba(40,67,129,.06); flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }
.module-head { margin-bottom: 14rpx; }
.module-title { color: #1e2c46; font-size: 36rpx; font-weight: 700; }
.module-subtitle { margin-top: 8rpx; display: block; color: #6e7d99; font-size: 24rpx; }
.search-panel { display: flex; align-items: center; gap: 12rpx; }
.search-input { flex: 1; min-width: 0; height: 76rpx; border-radius: 16rpx; border: 1rpx solid #dce5fb; background: #f8faff; padding: 0 20rpx; color: #2d3c5a; font-size: 26rpx; }
.btn { margin: 0; border: none; border-radius: 999rpx; font-size: 24rpx; line-height: 62rpx; height: 62rpx; padding: 0 24rpx; }
.btn-search { color: #fff; background: linear-gradient(135deg, #4d74ff 0%, #78a3ff 100%); }
.btn-reset, .btn-detail { color: #4a5b7d; background: #eef2fb; }
.list-shell { margin-top: 14rpx; flex: 1; min-height: 0; border: 1rpx solid #e2e9fb; border-radius: 16rpx; background: #f8faff; display: flex; flex-direction: column; overflow: hidden; }
.list-scroll { flex: 1; min-height: 0; padding: 14rpx; box-sizing: border-box; }
.pager-wrap { border-top: 1rpx solid #e3e9f8; background: #fff; border-bottom-left-radius: 16rpx; border-bottom-right-radius: 16rpx; padding: 6rpx 10rpx 12rpx; }
.state-text { color: #7b88a3; font-size: 25rpx; text-align: center; padding: 24rpx 0; }
.notice-grid { display: flex; flex-direction: column; gap: 14rpx; }
.notice-card { background: #fff; border-radius: 18rpx; border: 1rpx solid #e2e9fb; padding: 18rpx; }
.notice-top { display: flex; align-items: center; justify-content: space-between; gap: 10rpx; }
.notice-title { color: #1f2d47; font-size: 30rpx; font-weight: 700; flex: 1; min-width: 0; }
.notice-tag { padding: 4rpx 14rpx; border-radius: 999rpx; background: rgba(77,116,248,.12); color: #4d74f8; font-size: 22rpx; }
.notice-desc { margin-top: 10rpx; display: block; color: #667792; font-size: 24rpx; line-height: 1.45; }
.notice-time { margin-top: 8rpx; display: block; color: #8491ab; font-size: 22rpx; }
.action-row { margin-top: 10rpx; display: flex; justify-content: flex-end; }
.modal-mask { position: fixed; inset: 0; background: rgba(8,17,36,.45); z-index: 20; }
.modal-panel { position: fixed; left: 32rpx; right: 32rpx; top: 50%; transform: translateY(-50%); max-height: 80vh; background: #fff; border-radius: 24rpx; z-index: 21; padding: 22rpx; box-sizing: border-box; display: flex; flex-direction: column; }
.detail-head { margin-bottom: 8rpx; }
.detail-title { color: #1e2c46; font-size: 34rpx; font-weight: 700; }
.detail-scroll { flex: 1; min-height: 0; margin-top: 8rpx; }
.detail-time { color: #7a87a2; font-size: 22rpx; display: block; margin-bottom: 8rpx; }
.detail-content { color: #2b3a58; font-size: 26rpx; line-height: 1.6; }
.detail-close { margin-top: 14rpx; }
</style>

