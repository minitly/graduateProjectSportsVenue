<template>
  <view class="page">
    <view class="module-card">
      <view class="module-head">
        <text class="module-title">器材借用</text>
        <text class="module-subtitle">快速查看库存并提交借用申请</text>
      </view>

      <view class="search-panel">
        <view class="row">
          <input v-model.trim="query.keyword" class="search-input" placeholder="器材名称 / 型号" />
          <input v-model.trim="query.type" class="search-input" placeholder="器材类型（如 球类）" />
        </view>
        <view class="row">
          <picker class="only-picker" :range="onlyAvailableOptions" range-key="label" @change="onOnlyAvailableChange">
            <view class="picker-box">{{ onlyAvailableLabel }}</view>
          </picker>
          <button class="btn btn-search action-btn" @click="handleSearch">查询</button>
          <button class="btn btn-reset action-btn" @click="resetSearch">重置</button>
        </view>
      </view>

      <view class="list-shell">
        <scroll-view scroll-y class="list-scroll">
          <view v-if="loading" class="state-text">器材加载中...</view>
          <view v-else-if="!list.length" class="state-text">暂无器材数据</view>
          <view v-else class="item-grid">
            <view v-for="item in list" :key="item.id" class="item-card">
              <view class="item-head">
                <text class="item-title">{{ item.name || "-" }}</text>
                <view class="tag-group">
                  <text class="tag tag-info">押金 ¥{{ item.depositAmount || 0 }}</text>
                  <text class="tag tag-success">租金 ¥{{ item.borrowAmount || 0 }}</text>
                </view>
              </view>
              <text class="item-meta">{{ item.type || "未分类" }} · {{ item.model || "无型号" }}</text>
              <view class="item-stat">
                <text>库存 {{ item.totalQuantity || 0 }}</text>
                <text>可借 {{ item.availableQuantity || 0 }}</text>
                <text>损坏 {{ item.damagedQuantity || 0 }}</text>
              </view>
              <text class="item-desc">{{ item.description || "暂无描述" }}</text>
              <view class="item-action">
                <button class="btn btn-search apply-btn" :disabled="Number(item.availableQuantity || 0) <= 0" @click="openBorrowModal(item)">申请借用</button>
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

    <view v-if="borrowModal.show" class="modal-mask" @click="closeBorrowModal"></view>
    <view v-if="borrowModal.show" class="modal-panel">
      <view class="modal-header">
        <text class="modal-title">提交借用申请</text>
        <text class="close-btn" @click="closeBorrowModal">×</text>
      </view>
      <text class="borrow-name">{{ borrowModal.item?.name || "-" }}</text>
      <text class="borrow-meta">可借数量：{{ borrowModal.item?.availableQuantity || 0 }}</text>
      <text class="borrow-tag">{{ borrowModal.item?.type || "未分类" }}</text>

      <view class="field">
        <text class="field-label">借用数量</text>
        <view class="qty-row">
          <button class="qty-btn" @click="decreaseQty">-</button>
          <input v-model.number="borrowModal.quantity" type="number" class="qty-input" />
          <button class="qty-btn" @click="increaseQty">+</button>
        </view>
      </view>

      <view class="field">
        <text class="field-label">用途说明</text>
        <textarea v-model.trim="borrowModal.remark" class="remark-input" placeholder="例如：篮球训练" />
      </view>

      <view class="modal-actions">
        <button class="btn btn-reset" @click="closeBorrowModal">取消</button>
        <button class="btn btn-search" :disabled="borrowModal.submitting" @click="submitBorrow">提交申请</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from "vue";
import { applyBorrowApi, fetchItemListApi } from "../../api/equipment";
import PaginationBar from "../../components/PaginationBar.vue";

const loading = ref(false);
const list = ref([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = ref(5);
const pageSizeOptions = [5, 10, 20];
const query = reactive({
  keyword: "",
  type: "",
  onlyAvailable: false
});

const onlyAvailableOptions = [
  { label: "全部", value: false },
  { label: "仅可借", value: true }
];

const borrowModal = reactive({
  show: false,
  item: null,
  quantity: 1,
  remark: "",
  submitting: false
});

const onlyAvailableLabel = computed(() => (query.onlyAvailable ? "仅可借" : "全部"));

function onOnlyAvailableChange(e) {
  const selected = onlyAvailableOptions[e.detail.value];
  query.onlyAvailable = Boolean(selected?.value);
}

function handleSearch() {
  pageNo.value = 1;
  loadList();
}

function resetSearch() {
  query.keyword = "";
  query.type = "";
  query.onlyAvailable = false;
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
      pageSize: pageSize.value,
      onlyAvailable: query.onlyAvailable
    };
    if (query.keyword) params.keyword = query.keyword;
    if (query.type) params.type = query.type;
    const res = await fetchItemListApi(params);
    list.value = res.code === 200 ? res.data?.records || [] : [];
    total.value = res.code === 200 ? Number(res.data?.total || 0) : 0;
  } catch (error) {
    uni.showToast({ title: error.message || "器材加载失败", icon: "none" });
  } finally {
    loading.value = false;
  }
}

function openBorrowModal(item) {
  borrowModal.show = true;
  borrowModal.item = item;
  borrowModal.quantity = 1;
  borrowModal.remark = "";
  borrowModal.submitting = false;
}

function closeBorrowModal() {
  borrowModal.show = false;
}

function decreaseQty() {
  const value = Number(borrowModal.quantity || 1);
  borrowModal.quantity = Math.max(1, value - 1);
}

function increaseQty() {
  const max = Number(borrowModal.item?.availableQuantity || 1);
  const value = Number(borrowModal.quantity || 1);
  borrowModal.quantity = Math.min(max, value + 1);
}

function validateBorrowForm() {
  const available = Number(borrowModal.item?.availableQuantity || 0);
  const quantity = Number(borrowModal.quantity);
  if (!Number.isInteger(quantity) || quantity <= 0) {
    return "请输入正确的借用数量";
  }
  if (quantity > available) {
    return "借用数量不可超过可借数量";
  }
  return "";
}

async function submitBorrow() {
  if (!borrowModal.item?.id) return;
  const error = validateBorrowForm();
  if (error) {
    uni.showModal({ title: "提示", content: error, showCancel: false });
    return;
  }
  borrowModal.submitting = true;
  try {
    const res = await applyBorrowApi({
      itemId: borrowModal.item.id,
      quantity: Number(borrowModal.quantity),
      remark: borrowModal.remark
    });
    if (res.code !== 200) {
      uni.showModal({
        title: "借用申请失败",
        content: res.message || "请稍后重试",
        showCancel: false
      });
      return;
    }
    uni.showModal({
      title: "提交成功",
      content: "借用申请已提交",
      showCancel: false
    });
    closeBorrowModal();
    loadList();
  } catch (err) {
    uni.showModal({
      title: "借用申请失败",
      content: err.message || "无法连接后端服务",
      showCancel: false
    });
  } finally {
    borrowModal.submitting = false;
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
.row { display: flex; align-items: center; gap: 12rpx; }
.search-input, .picker-box { flex: 1; min-width: 0; height: 76rpx; border-radius: 16rpx; border: 1rpx solid #dce5fb; background: #f8faff; padding: 0 20rpx; display: flex; align-items: center; color: #2d3c5a; font-size: 26rpx; }
.only-picker { flex: 1; min-width: 0; }
.action-btn { flex: 1; min-width: 0; }
.btn { margin: 0; border: none; border-radius: 999rpx; font-size: 24rpx; line-height: 62rpx; height: 62rpx; padding: 0 24rpx; }
.btn-search { color: #fff; background: linear-gradient(135deg, #4d74ff 0%, #78a3ff 100%); }
.btn-reset, .btn-detail { color: #4a5b7d; background: #eef2fb; }
.list-shell { margin-top: 14rpx; flex: 1; min-height: 0; border: 1rpx solid #e2e9fb; border-radius: 16rpx; background: #f8faff; display: flex; flex-direction: column; overflow: hidden; }
.list-scroll { flex: 1; min-height: 0; padding: 14rpx; box-sizing: border-box; }
.pager-wrap { border-top: 1rpx solid #e3e9f8; background: #fff; border-bottom-left-radius: 16rpx; border-bottom-right-radius: 16rpx; padding: 6rpx 10rpx 12rpx; }
.state-text { color: #7b88a3; font-size: 25rpx; text-align: center; padding: 24rpx 0; }
.item-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12rpx; }
.item-card { background: #fff; border-radius: 18rpx; border: 1rpx solid #e2e9fb; padding: 14rpx; display: flex; flex-direction: column; }
.item-head { display: flex; flex-direction: column; gap: 6rpx; }
.item-title { color: #1f2d47; font-size: 28rpx; font-weight: 700; }
.tag-group { display: flex; gap: 6rpx; flex-wrap: wrap; }
.tag { padding: 2rpx 10rpx; border-radius: 999rpx; font-size: 20rpx; }
.tag-info { color: #2f75d6; background: #eaf2ff; }
.tag-success { color: #2c8b5f; background: #e8f9ef; }
.item-meta { margin-top: 8rpx; color: #6a7895; font-size: 22rpx; }
.item-stat { margin-top: 8rpx; display: flex; justify-content: space-between; color: #4a5a7a; font-size: 22rpx; }
.item-desc { margin-top: 8rpx; color: #667792; font-size: 22rpx; line-height: 1.4; min-height: 62rpx; }
.item-action { margin-top: auto; padding-top: 12rpx; display: flex; }
.apply-btn { width: 100%; }
.modal-mask { position: fixed; inset: 0; background: rgba(8,17,36,.45); z-index: 20; }
.modal-panel { position: fixed; left: 28rpx; right: 28rpx; top: 50%; transform: translateY(-50%); background: #fff; border-radius: 24rpx; z-index: 21; padding: 20rpx; box-sizing: border-box; }
.modal-header { display: flex; justify-content: space-between; align-items: center; }
.modal-title { color: #1e2c46; font-size: 34rpx; font-weight: 700; }
.close-btn { color: #7d8aa3; font-size: 40rpx; line-height: 1; padding: 0 8rpx; }
.borrow-name { margin-top: 18rpx; display: block; color: #1f2d47; font-size: 30rpx; font-weight: 700; }
.borrow-meta { margin-top: 8rpx; display: block; color: #6a7895; font-size: 24rpx; }
.borrow-tag { margin-top: 8rpx; display: inline-block; padding: 2rpx 10rpx; border-radius: 8rpx; border: 1rpx solid #cfe0ff; color: #2f75d6; background: #eef4ff; font-size: 22rpx; }
.field { margin-top: 14rpx; }
.field-label { color: #556783; font-size: 24rpx; display: block; margin-bottom: 8rpx; }
.qty-row { display: flex; align-items: center; gap: 8rpx; }
.qty-btn { margin: 0; width: 72rpx; min-width: 72rpx; height: 64rpx; line-height: 64rpx; border-radius: 14rpx; border: none; color: #4a5b7d; background: #eef2fb; font-size: 34rpx; }
.qty-input { flex: 1; height: 64rpx; border-radius: 14rpx; border: 1rpx solid #dce5fb; background: #f8faff; text-align: center; color: #2d3c5a; font-size: 26rpx; }
.remark-input { width: 100%; min-height: 130rpx; border-radius: 16rpx; border: 1rpx solid #dce5fb; background: #f8faff; padding: 14rpx 16rpx; box-sizing: border-box; color: #2d3c5a; font-size: 24rpx; }
.modal-actions { margin-top: 14rpx; display: flex; gap: 10rpx; justify-content: flex-end; }
</style>

