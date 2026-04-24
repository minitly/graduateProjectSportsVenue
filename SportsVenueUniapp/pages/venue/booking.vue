<template>
  <view class="page">
    <view class="module-card">
      <view class="module-head">
        <text class="module-title">预约时段</text>
        <text class="module-subtitle">{{ venue?.name || "-" }}</text>
      </view>

      <view v-if="loading" class="state-text">数据加载中...</view>
      <view v-else-if="!venue" class="state-text">未获取到场地信息</view>
      <view v-else class="content-shell">
        <view class="form-panel">
          <text class="meta-text">{{ venue.openTime || "--:--" }} - {{ venue.closeTime || "--:--" }} · {{ venue.type || "-" }}</text>
          <view class="field">
            <text class="field-label">预约日期</text>
            <picker mode="date" :value="bookingDate" :start="dateStart" :end="dateEnd" @change="onDateChange">
              <view class="picker-box">{{ bookingDateLabel }}</view>
            </picker>
          </view>
          <view class="row">
            <view class="field half">
              <text class="field-label">开始时间</text>
              <picker :range="startTimeOptions" range-key="label" :value="startTimeIndex" @change="onStartTimeChange">
                <view class="picker-box">{{ startTime || "请选择" }}</view>
              </picker>
            </view>
            <view class="field half">
              <text class="field-label">结束时间</text>
              <picker :range="endTimeOptions" range-key="label" :value="endTimeIndex" @change="onEndTimeChange">
                <view class="picker-box">{{ endTime || "请选择" }}</view>
              </picker>
            </view>
          </view>
          <text class="hint">点击两个可预约时段即可快速圈选连续区间</text>
        </view>

        <view class="slot-shell">
          <view class="slot-head">
            <text class="slot-title">可选时段表格</text>
            <view class="legend-row">
              <text class="legend available">可预约</text>
              <text class="legend selected">已选择</text>
              <text class="legend occupied">占用</text>
              <text class="legend disabled">不可用</text>
            </view>
          </view>
          <scroll-view scroll-y class="slot-scroll">
            <view v-for="slot in slotStatusList" :key="slot.value" class="slot-row" :class="slot.status" @click="handleSlotSelect(slot)">
              <text>{{ slot.range }}</text>
              <text class="slot-status">{{ getSlotStatusText(slot.status) }}</text>
            </view>
            <view v-if="!slotStatusList.length" class="state-text">无可选时段</view>
          </scroll-view>
        </view>

        <view class="occupied-panel">
          <text class="occupied-title">已占用区间</text>
          <text v-if="!occupiedRanges.length" class="occupied-empty">暂无占用</text>
          <view v-for="item in occupiedRanges" :key="item.start" class="occupied-item">{{ item.start }} ~ {{ item.end }}</view>
        </view>
      </view>

      <view class="action-bar">
        <button class="btn btn-clear" @click="clearSlotSelection">清空时段</button>
        <button class="btn btn-cancel" @click="goBack">取消</button>
        <button class="btn btn-submit" :disabled="submitting" @click="submitBooking">提交预约</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import dayjs from "dayjs";
import { computed, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { fetchVenueDetailApi } from "../../api/venue";
import { createBookingApi, fetchMyViolationStatusApi, fetchOccupiedSlotsApi } from "../../api/booking";

const loading = ref(false);
const submitting = ref(false);
const venueId = ref("");
const venue = ref(null);
const bookingDate = ref("");
const startTime = ref("");
const endTime = ref("");
const occupied = ref([]);

const dateStart = computed(() => dayjs().format("YYYY-MM-DD"));
const dateEnd = computed(() => dayjs().add(6, "day").format("YYYY-MM-DD"));
const bookingDateLabel = computed(() => {
  if (!bookingDate.value) return "请选择日期";
  return `${bookingDate.value} ${formatWeekday(bookingDate.value)}`;
});

const timeSlots = computed(() => {
  if (!venue.value?.openTime || !venue.value?.closeTime) return [];
  const [startHour] = String(venue.value.openTime).split(":").map(Number);
  const [endHour] = String(venue.value.closeTime).split(":").map(Number);
  const result = [];
  for (let hour = startHour; hour < endHour; hour += 1) {
    const value = `${String(hour).padStart(2, "0")}:00`;
    result.push({ label: value, value });
  }
  return result;
});

const unavailableSlots = computed(() => {
  if (!bookingDate.value) return [];
  return occupied.value.map((item) => {
    const d = new Date(item.slotStartTime);
    if (Number.isNaN(d.getTime())) return "";
    return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
  }).filter(Boolean);
});

const startTimeOptions = computed(() => timeSlots.value.map((slot) => ({
  ...slot,
  disabled: isPastStartSlot(slot.value) || slotHourOverlapsOccupied(slot.value)
})));

const startTimeIndex = computed(() => {
  const idx = startTimeOptions.value.findIndex((s) => s.value === startTime.value);
  return idx >= 0 ? idx : 0;
});

const endTimeOptions = computed(() => {
  if (!startTime.value) return [];
  const startIdx = timeSlots.value.findIndex((slot) => slot.value === startTime.value);
  if (startIdx < 0) return [];
  const list = timeSlots.value
    .slice(startIdx + 1)
    .filter((slot) => !isPastStartSlot(slot.value))
    .map((slot) => ({ label: slot.value, value: slot.value }));
  if (venue.value?.closeTime && !list.some((item) => item.value === venue.value.closeTime)) {
    list.push({ label: venue.value.closeTime, value: venue.value.closeTime });
  }
  return list;
});

const endTimeIndex = computed(() => {
  const idx = endTimeOptions.value.findIndex((s) => s.value === endTime.value);
  return idx >= 0 ? idx : 0;
});

const occupiedRanges = computed(() => occupied.value.map((slot) => ({
  start: slot.slotStartTime,
  end: slot.slotEndTime
})));

const slotStatusList = computed(() => {
  const occupiedSet = new Set(unavailableSlots.value);
  return timeSlots.value.map((slot, index) => {
    let status = "available";
    if (isPastStartSlot(slot.value)) {
      status = "disabled";
    } else if (occupiedSet.has(slot.value) || slotHourOverlapsOccupied(slot.value)) {
      status = "occupied";
    } else if (startTime.value && !endTime.value) {
      const startIdx = timeSlots.value.findIndex((item) => item.value === startTime.value);
      if (index === startIdx) status = "selected";
    } else if (startTime.value && endTime.value) {
      const startIdx = timeSlots.value.findIndex((item) => item.value === startTime.value);
      let endIdx = timeSlots.value.findIndex((item) => item.value === endTime.value);
      if (endIdx === -1 && endTime.value === venue.value?.closeTime) {
        endIdx = timeSlots.value.length;
      }
      if (index >= startIdx && index < endIdx) status = "selected";
    }
    return {
      ...slot,
      status,
      range: `${slot.value} - ${timeSlots.value[index + 1]?.value || venue.value?.closeTime || "--:--"}`
    };
  });
});

function formatWeekday(dateStr) {
  const weekdays = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"];
  const d = new Date(`${dateStr}T00:00:00`);
  if (Number.isNaN(d.getTime())) return "";
  return weekdays[d.getDay()];
}

function getTodayDateString() {
  return dayjs().format("YYYY-MM-DD");
}

function getEarliestStartHourForToday() {
  return dayjs().add(1, "hour").hour();
}

function isPastStartSlot(slotValue) {
  if (!bookingDate.value || bookingDate.value !== getTodayDateString()) return false;
  const [hour] = String(slotValue).split(":").map(Number);
  return hour < getEarliestStartHourForToday();
}

function slotHourOverlapsOccupied(slotValue) {
  if (!bookingDate.value || !slotValue || !occupied.value.length) return false;
  const slotStart = new Date(`${bookingDate.value}T${slotValue}:00`).getTime();
  const slotEnd = slotStart + 60 * 60 * 1000;
  if (!Number.isFinite(slotStart)) return false;
  return occupied.value.some((item) => {
    const os = new Date(item.slotStartTime).getTime();
    const oe = new Date(item.slotEndTime).getTime();
    return Number.isFinite(os) && Number.isFinite(oe) && slotStart < oe && slotEnd > os;
  });
}

function bookingEndAfterStart() {
  if (!bookingDate.value || !startTime.value || !endTime.value) return false;
  const a = new Date(`${bookingDate.value}T${startTime.value}:00`).getTime();
  const b = new Date(`${bookingDate.value}T${endTime.value}:00`).getTime();
  return Number.isFinite(a) && Number.isFinite(b) && b > a;
}

function bookingRangeOverlapsOccupied() {
  if (!bookingDate.value || !startTime.value || !endTime.value) return false;
  const rangeStart = new Date(`${bookingDate.value}T${startTime.value}:00`).getTime();
  const rangeEnd = new Date(`${bookingDate.value}T${endTime.value}:00`).getTime();
  if (!Number.isFinite(rangeStart) || !Number.isFinite(rangeEnd)) return true;
  return occupied.value.some((item) => {
    const os = new Date(item.slotStartTime).getTime();
    const oe = new Date(item.slotEndTime).getTime();
    return Number.isFinite(os) && Number.isFinite(oe) && rangeStart < oe && rangeEnd > os;
  });
}

function clearSlotSelection() {
  startTime.value = "";
  endTime.value = "";
}

function getSlotEndBoundary(slotIndex) {
  return timeSlots.value[slotIndex + 1]?.value || venue.value?.closeTime || timeSlots.value[slotIndex]?.value || "";
}

function handleSlotSelect(slot) {
  if (!slot || slot.status === "occupied" || slot.status === "disabled") return;
  const clickedIndex = timeSlots.value.findIndex((item) => item.value === slot.value);
  if (clickedIndex < 0) return;
  if (!startTime.value || endTime.value) {
    startTime.value = slot.value;
    endTime.value = "";
    return;
  }
  const startIdx = timeSlots.value.findIndex((item) => item.value === startTime.value);
  if (startIdx < 0 || clickedIndex < startIdx) {
    startTime.value = slot.value;
    endTime.value = "";
    return;
  }
  const hasOccupiedBetween = timeSlots.value.slice(startIdx, clickedIndex + 1).some((item) => unavailableSlots.value.includes(item.value));
  if (hasOccupiedBetween) {
    uni.showToast({ title: "选中区间含已占用时段", icon: "none" });
    startTime.value = slot.value;
    endTime.value = "";
    return;
  }
  endTime.value = getSlotEndBoundary(clickedIndex);
}

function getSlotStatusText(status) {
  if (status === "selected") return "已选择";
  if (status === "occupied") return "已占用";
  if (status === "disabled") return "不可用";
  return "可预约";
}

function onDateChange(e) {
  bookingDate.value = e.detail.value;
  clearSlotSelection();
  loadOccupiedSlots();
}

function onStartTimeChange(e) {
  const item = startTimeOptions.value[e.detail.value];
  if (!item || item.disabled) return;
  startTime.value = item.value;
  if (endTime.value && !bookingEndAfterStart()) {
    endTime.value = "";
  }
}

function onEndTimeChange(e) {
  const item = endTimeOptions.value[e.detail.value];
  if (!item) return;
  endTime.value = item.value;
}

async function checkViolationAndBlock() {
  const res = await fetchMyViolationStatusApi();
  if (res.code !== 200) return false;
  if (!res.data?.isViolationUser) return false;
  await new Promise((resolve) => {
    uni.showModal({
      title: "当前账号已违规",
      content: "你本月已失去预约资格，暂不可预约场地。",
      showCancel: false,
      complete: resolve
    });
  });
  goBack();
  return true;
}

async function loadVenueDetail() {
  const res = await fetchVenueDetailApi(venueId.value);
  if (res.code !== 200 || !res.data) {
    throw new Error(res.message || "获取场地信息失败");
  }
  venue.value = res.data;
  if (!bookingDate.value) {
    bookingDate.value = dateStart.value;
  }
}

async function loadOccupiedSlots() {
  if (!venue.value?.id || !bookingDate.value) return;
  const res = await fetchOccupiedSlotsApi({
    venueId: venue.value.id,
    startDate: bookingDate.value,
    endDate: bookingDate.value
  });
  if (res.code !== 200) {
    throw new Error(res.message || "加载已占用时段失败");
  }
  occupied.value = res.data || [];
}

async function reloadCurrentPageState() {
  clearSlotSelection();
  await loadVenueDetail();
  await loadOccupiedSlots();
}

async function submitBooking() {
  if (!venue.value?.id || !bookingDate.value || !startTime.value || !endTime.value) {
    uni.showToast({ title: "请选择预约日期与时间", icon: "none" });
    return;
  }
  if (!bookingEndAfterStart()) {
    uni.showModal({
      title: "时间无效",
      content: "结束时间必须晚于开始时间。",
      showCancel: false
    });
    return;
  }
  if (bookingRangeOverlapsOccupied()) {
    uni.showModal({
      title: "时段冲突",
      content: "所选时间段与已有预约重叠，请重新选择。",
      showCancel: false
    });
    await reloadCurrentPageState();
    return;
  }

  submitting.value = true;
  try {
    const res = await createBookingApi({
      venueId: venue.value.id,
      startTime: `${bookingDate.value}T${startTime.value}:00`,
      endTime: `${bookingDate.value}T${endTime.value}:00`
    });
    if (res.code !== 200) {
      await reloadCurrentPageState();
      uni.showModal({
        title: "预约失败",
        content: res.message || "预约失败，请重试",
        showCancel: false
      });
      return;
    }
    await new Promise((resolve) => {
      uni.showModal({
        title: "预约成功",
        content: "预约申请已提交",
        showCancel: false,
        complete: resolve
      });
    });
    goBack();
  } catch (error) {
    await reloadCurrentPageState();
    uni.showModal({
      title: "预约失败",
      content: error.message || "网络异常，请稍后再试",
      showCancel: false
    });
  } finally {
    submitting.value = false;
  }
}

function goBack() {
  uni.navigateBack();
}

onLoad(async (query) => {
  if (!query?.venueId) {
    uni.showToast({ title: "缺少场地ID", icon: "none" });
    return;
  }
  venueId.value = query.venueId;
  loading.value = true;
  try {
    const blocked = await checkViolationAndBlock();
    if (blocked) return;
    await loadVenueDetail();
    await loadOccupiedSlots();
  } catch (error) {
    uni.showToast({ title: error.message || "加载失败", icon: "none" });
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.page { height: 100vh; background: #f3f6ff; padding: 24rpx; box-sizing: border-box; display: flex; overflow: hidden; }
.module-card { flex: 1; min-height: 0; display: flex; flex-direction: column; background: #fff; border-radius: 24rpx; padding: 24rpx; box-shadow: 0 8rpx 22rpx rgba(40,67,129,.06); overflow: hidden; }
.module-head { margin-bottom: 10rpx; }
.module-title { color: #1e2c46; font-size: 36rpx; font-weight: 700; }
.module-subtitle { margin-top: 8rpx; display: block; color: #6e7d99; font-size: 24rpx; }
.content-shell { flex: 1; min-height: 0; display: flex; flex-direction: column; gap: 12rpx; overflow: hidden; }
.form-panel { background: #f8faff; border: 1rpx solid #e2e9fb; border-radius: 16rpx; padding: 14rpx; }
.meta-text { color: #667792; font-size: 23rpx; display: block; margin-bottom: 8rpx; }
.row { display: flex; gap: 12rpx; }
.field { margin-top: 8rpx; }
.half { flex: 1; min-width: 0; }
.field-label { color: #61708d; font-size: 24rpx; display: block; margin-bottom: 6rpx; }
.picker-box { height: 68rpx; border-radius: 14rpx; border: 1rpx solid #dce5fb; background: #fff; display: flex; align-items: center; padding: 0 18rpx; color: #2d3c5a; font-size: 25rpx; }
.hint { margin-top: 8rpx; color: #8a97b0; font-size: 22rpx; display: block; }
.slot-shell { flex: 1; min-height: 0; border: 1rpx solid #e2e9fb; border-radius: 16rpx; background: #f8faff; overflow: hidden; display: flex; flex-direction: column; }
.slot-head { padding: 10rpx 14rpx; border-bottom: 1rpx solid #e3e9f8; background: #fff; }
.slot-title { color: #1f2d47; font-size: 26rpx; font-weight: 600; }
.legend-row { margin-top: 6rpx; display: flex; gap: 8rpx; flex-wrap: wrap; }
.legend { font-size: 20rpx; padding: 4rpx 10rpx; border-radius: 999rpx; }
.legend.available { background: #eaf7ff; color: #177cb0; }
.legend.selected { background: #efe8ff; color: #6d4fd5; }
.legend.occupied { background: #ffe9eb; color: #c64251; }
.legend.disabled { background: #f0f2f7; color: #6d7584; }
.slot-scroll { flex: 1; min-height: 0; }
.slot-row { padding: 16rpx 14rpx; display: flex; justify-content: space-between; align-items: center; border-bottom: 1rpx solid #e9eefc; color: #2a3a57; font-size: 24rpx; }
.slot-status { font-size: 22rpx; }
.slot-row.available { background: #fff; }
.slot-row.selected { background: #f2edff; }
.slot-row.occupied { background: #fff1f3; color: #a73745; }
.slot-row.disabled { background: #f1f3f8; color: #7d8595; }
.occupied-panel { background: #f8faff; border: 1rpx solid #e2e9fb; border-radius: 16rpx; padding: 12rpx 14rpx; max-height: 170rpx; overflow-y: auto; }
.occupied-title { color: #1f2d47; font-size: 24rpx; font-weight: 600; display: block; }
.occupied-empty { color: #8a97b0; font-size: 22rpx; margin-top: 8rpx; display: block; }
.occupied-item { margin-top: 8rpx; color: #5f6f8c; font-size: 22rpx; }
.action-bar { margin-top: 12rpx; display: flex; gap: 10rpx; }
.btn { margin: 0; border: none; border-radius: 999rpx; font-size: 24rpx; line-height: 62rpx; height: 62rpx; padding: 0 20rpx; }
.btn-clear, .btn-cancel { color: #4a5b7d; background: #eef2fb; }
.btn-submit { color: #fff; background: linear-gradient(135deg, #4d74ff 0%, #78a3ff 100%); flex: 1; }
.state-text { color: #7b88a3; font-size: 25rpx; text-align: center; padding: 24rpx 0; }
</style>

