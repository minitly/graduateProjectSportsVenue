<template>
  <view class="page">
    <view class="module-card">
      <view class="wallet-top">
        <view>
          <text class="label">当前余额（元）</text>
          <view class="balance-row">
            <text class="balance">¥{{ formatMoney(balance) }}</text>
            <view class="refresh-btn" @click="refreshBalance">⟳</view>
          </view>
        </view>
        <button class="btn btn-primary btn-recharge" @click="openRecharge">充值</button>
      </view>

      <view class="filter-row">
        <picker class="type-picker" :range="typeOptions" range-key="label" @change="onTypeChange">
          <view class="picker-box">{{ selectedTypeLabel }}</view>
        </picker>
        <picker mode="date" :value="filters.startDate" @change="onStartDateChange">
          <view class="picker-box">{{ filters.startDate || "开始日期" }}</view>
        </picker>
        <picker mode="date" :value="filters.endDate" @change="onEndDateChange">
          <view class="picker-box">{{ filters.endDate || "结束日期" }}</view>
        </picker>
        <button class="btn btn-primary small" @click="handleSearch">查询</button>
        <button class="btn btn-ghost small" @click="resetFilters">重置</button>
      </view>

      <view class="list-shell">
        <scroll-view scroll-y class="list-scroll">
          <view v-if="loading" class="state-text">流水加载中...</view>
          <view v-else-if="!list.length" class="state-text">暂无流水数据</view>
          <view v-else class="tx-grid">
            <view v-for="item in list" :key="item.id" class="tx-card">
              <view class="tx-head">
                <text class="tx-type">{{ txTypeText(item.type) }}</text>
                <text class="tx-amount" :class="amountClass(item.amount)">{{ signedAmount(item.amount) }}</text>
              </view>
              <text class="tx-line">时间：{{ formatDateTime(item.createTime) }}</text>
              <text class="tx-line">变动前：{{ formatMoney(item.beforeBalance) }}  变动后：{{ formatMoney(item.afterBalance) }}</text>
              <text class="tx-line">业务：{{ item.bizType || "-" }} #{{ item.bizId ?? "-" }}</text>
              <text class="tx-line">备注：{{ item.remark || "-" }}</text>
            </view>
          </view>
        </scroll-view>
        <view class="pager-wrap">
          <PaginationBar :page-no="pageNo" :page-size="pageSize" :total="total" :size-options="pageSizeOptions" @change="onPageChange" />
        </view>
      </view>
    </view>

    <view v-if="showRecharge" class="modal-mask" @click="closeRecharge"></view>
    <view v-if="showRecharge" class="modal-panel">
      <view class="modal-head">
        <text class="modal-title">余额充值</text>
        <text class="close-btn" @click="closeRecharge">×</text>
      </view>
      <view class="field">
        <text class="field-label">充值金额（元）</text>
        <view class="amount-row">
          <button class="step-btn" @click="decreaseAmount">-</button>
          <input v-model.number="recharge.amount" type="digit" class="amount-input" placeholder="请输入" />
          <button class="step-btn" @click="increaseAmount">+</button>
        </view>
      </view>
      <view class="field">
        <text class="field-label">备注（可选）</text>
        <textarea v-model.trim="recharge.remark" class="remark-input" placeholder="例如：账户充值" />
      </view>
      <view class="action-row">
        <button class="btn btn-ghost" @click="closeRecharge">取消</button>
        <button class="btn btn-primary" :disabled="rechargeSubmitting" @click="submitRecharge">立即充值</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import dayjs from "dayjs";
import { computed, reactive, ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import PaginationBar from "../../components/PaginationBar.vue";
import { useAuthStore } from "../../stores/auth";
import { fetchMyWalletTransactionsApi, fetchUserByIdApi, rechargeApi } from "../../api/mine";

const authStore = useAuthStore();
const loading = ref(false);
const rechargeSubmitting = ref(false);
const balance = ref(0);
const list = ref([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = ref(5);
const pageSizeOptions = [5, 10, 20];
const showRecharge = ref(false);
const recharge = reactive({
  amount: "",
  remark: ""
});
const filters = reactive({
  type: "",
  startDate: "",
  endDate: ""
});
const typeOptions = [
  { label: "全部类型", value: "" },
  { label: "充值", value: "RECHARGE" },
  { label: "退款", value: "REFUND" },
  { label: "预约扣费", value: "BOOKING_DEBIT" },
  { label: "借用租金扣费", value: "BORROW_RENT_DEBIT" },
  { label: "借用押金扣费", value: "BORROW_DEPOSIT_DEBIT" },
  { label: "借用押金退款", value: "BORROW_DEPOSIT_REFUND" }
];
const selectedTypeLabel = computed(() => typeOptions.find((x) => x.value === filters.type)?.label || "全部类型");

function formatDateTime(v) {
  return v ? dayjs(v).format("YYYY-MM-DD HH:mm") : "-";
}
function formatMoney(v) {
  const n = Number(v || 0);
  return Number.isFinite(n) ? n.toFixed(2) : "0.00";
}
function signedAmount(v) {
  const n = Number(v || 0);
  if (!Number.isFinite(n)) return "0.00";
  return `${n > 0 ? "+" : ""}${n.toFixed(2)}`;
}
function amountClass(v) {
  const n = Number(v || 0);
  if (n > 0) return "plus";
  if (n < 0) return "minus";
  return "";
}
function txTypeText(type) {
  const map = {
    RECHARGE: "充值",
    REFUND: "退款",
    BOOKING_DEBIT: "预约扣费",
    BORROW_RENT_DEBIT: "借用租金扣费",
    BORROW_DEPOSIT_DEBIT: "借用押金扣费",
    BORROW_DEPOSIT_REFUND: "借用押金退款"
  };
  return map[type] || type || "-";
}

function resolveCurrentUserId() {
  return authStore.userInfo?.id || authStore.userInfo?.userId || "";
}

function onTypeChange(e) {
  const selected = typeOptions[e.detail.value];
  filters.type = selected?.value || "";
}
function onStartDateChange(e) {
  filters.startDate = e.detail.value;
}
function onEndDateChange(e) {
  filters.endDate = e.detail.value;
}

function handleSearch() {
  pageNo.value = 1;
  loadTransactions();
}
function resetFilters() {
  filters.type = "";
  filters.startDate = "";
  filters.endDate = "";
  pageNo.value = 1;
  loadTransactions();
}
function onPageChange(payload) {
  pageNo.value = payload.pageNo;
  pageSize.value = payload.pageSize;
  loadTransactions();
}

async function refreshBalance() {
  const uid = resolveCurrentUserId();
  if (!uid) return;
  try {
    const res = await fetchUserByIdApi(uid);
    if (res.code === 200 && res.data) {
      balance.value = Number(res.data.balance || 0);
    }
  } catch (_) {}
}

async function loadTransactions() {
  loading.value = true;
  try {
    const params = {
      pageNo: pageNo.value,
      pageSize: pageSize.value
    };
    if (filters.type) params.type = filters.type;
    if (filters.startDate) params.startDate = filters.startDate;
    if (filters.endDate) params.endDate = filters.endDate;
    const res = await fetchMyWalletTransactionsApi(params);
    list.value = res.code === 200 ? res.data?.records || [] : [];
    total.value = res.code === 200 ? Number(res.data?.total || 0) : 0;
  } catch (error) {
    uni.showToast({ title: error.message || "流水加载失败", icon: "none" });
  } finally {
    loading.value = false;
  }
}

function openRecharge() {
  recharge.amount = "";
  recharge.remark = "";
  showRecharge.value = true;
}
function closeRecharge() {
  showRecharge.value = false;
}
function decreaseAmount() {
  const n = Number(recharge.amount || 0);
  recharge.amount = Math.max(0, Number((n - 10).toFixed(2)));
}
function increaseAmount() {
  const n = Number(recharge.amount || 0);
  recharge.amount = Number((n + 10).toFixed(2));
}

async function submitRecharge() {
  const amount = Number(recharge.amount);
  if (!Number.isFinite(amount) || amount <= 0) {
    uni.showModal({ title: "提示", content: "充值金额必须大于0", showCancel: false });
    return;
  }
  rechargeSubmitting.value = true;
  try {
    const res = await rechargeApi({
      amount: Number(amount.toFixed(2)),
      remark: recharge.remark || undefined
    });
    if (res.code !== 200) {
      uni.showModal({ title: "充值失败", content: res.message || "请稍后重试", showCancel: false });
      return;
    }
    uni.showToast({ title: "充值成功", icon: "success" });
    closeRecharge();
    await refreshBalance();
    await loadTransactions();
  } catch (error) {
    uni.showModal({ title: "充值失败", content: error.message || "请稍后重试", showCancel: false });
  } finally {
    rechargeSubmitting.value = false;
  }
}

onShow(() => {
  const localBalance = Number(authStore.userInfo?.balance);
  if (Number.isFinite(localBalance)) {
    balance.value = localBalance;
  }
  refreshBalance();
  loadTransactions();
});
</script>

<style scoped>
.page { height: 100vh; background: #f3f6ff; padding: 24rpx; box-sizing: border-box; overflow: hidden; }
.module-card { height: 100%; background: #fff; border-radius: 20rpx; padding: 16rpx; box-sizing: border-box; display: flex; flex-direction: column; overflow: hidden; }
.wallet-top { display: flex; align-items: center; justify-content: space-between; gap: 12rpx; }
.label { color: #64748b; font-size: 22rpx; display: block; }
.balance-row { margin-top: 4rpx; display: flex; align-items: center; gap: 10rpx; }
.balance { color: #0f172a; font-size: 48rpx; font-weight: 700; }
.refresh-btn { width: 44rpx; height: 44rpx; line-height: 44rpx; text-align: center; border-radius: 50%; background: #eef2fb; color: #4a5b7d; }
.filter-row { margin-top: 12rpx; display: grid; grid-template-columns: 1.2fr 1fr 1fr auto auto; gap: 8rpx; align-items: center; }
.picker-box { height: 64rpx; border-radius: 12rpx; border: 1rpx solid #dce5fb; background: #f8faff; padding: 0 14rpx; display: flex; align-items: center; color: #2d3c5a; font-size: 22rpx; }
.list-shell { margin-top: 12rpx; flex: 1; min-height: 0; border: 1rpx solid #e2e9fb; border-radius: 16rpx; background: #f8faff; display: flex; flex-direction: column; overflow: hidden; }
.list-scroll { flex: 1; min-height: 0; padding: 12rpx; box-sizing: border-box; }
.pager-wrap { border-top: 1rpx solid #e3e9f8; background: #fff; padding: 6rpx 10rpx 12rpx; }
.state-text { color: #7b88a3; text-align: center; padding: 24rpx 0; font-size: 24rpx; }
.tx-grid { display: flex; flex-direction: column; gap: 10rpx; }
.tx-card { background: #fff; border: 1rpx solid #dfe8fb; border-radius: 14rpx; padding: 12rpx; }
.tx-head { display: flex; justify-content: space-between; align-items: center; }
.tx-type { color: #1f2d47; font-size: 26rpx; font-weight: 700; }
.tx-amount { font-size: 28rpx; font-weight: 700; color: #334155; }
.tx-amount.plus { color: #15803d; }
.tx-amount.minus { color: #b91c1c; }
.tx-line { margin-top: 6rpx; display: block; color: #64748b; font-size: 22rpx; }
.btn { margin: 0; border: none; border-radius: 999rpx; height: 58rpx; line-height: 58rpx; font-size: 22rpx; padding: 0 20rpx; }
.btn-recharge { min-width: 148rpx; height: 68rpx; line-height: 68rpx; font-size: 26rpx; padding: 0 28rpx; }
.btn.small { height: 56rpx; line-height: 56rpx; padding: 0 16rpx; }
.btn-primary { color: #fff; background: linear-gradient(135deg, #4d74ff 0%, #78a3ff 100%); }
.btn-ghost { color: #4a5b7d; background: #eef2fb; }
.modal-mask { position: fixed; inset: 0; background: rgba(8,17,36,.45); z-index: 20; }
.modal-panel { position: fixed; left: 32rpx; right: 32rpx; top: 50%; transform: translateY(-50%); background: #fff; border-radius: 24rpx; z-index: 21; padding: 20rpx; box-sizing: border-box; }
.modal-head { display: flex; justify-content: space-between; align-items: center; }
.modal-title { color: #1e2c46; font-size: 34rpx; font-weight: 700; }
.close-btn { color: #7d8aa3; font-size: 40rpx; line-height: 1; padding: 0 8rpx; }
.field { margin-top: 12rpx; }
.field-label { color: #556783; font-size: 24rpx; display: block; margin-bottom: 8rpx; }
.amount-row { display: flex; gap: 8rpx; align-items: center; }
.step-btn { margin: 0; width: 72rpx; min-width: 72rpx; height: 64rpx; line-height: 64rpx; border-radius: 14rpx; border: none; color: #4a5b7d; background: #eef2fb; font-size: 34rpx; }
.amount-input { flex: 1; height: 64rpx; border-radius: 14rpx; border: 1rpx solid #dce5fb; background: #f8faff; text-align: center; color: #2d3c5a; font-size: 26rpx; }
.remark-input { width: 100%; min-height: 120rpx; border-radius: 16rpx; border: 1rpx solid #dce5fb; background: #f8faff; padding: 14rpx 16rpx; box-sizing: border-box; color: #2d3c5a; font-size: 24rpx; }
.action-row { margin-top: 14rpx; display: flex; justify-content: flex-end; gap: 10rpx; }
</style>

