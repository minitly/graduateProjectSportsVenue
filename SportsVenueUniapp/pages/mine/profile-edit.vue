<template>
  <view class="page">
    <view class="module-card">
      <text class="title">编辑资料</text>

      <view class="field">
        <text class="label">真实姓名</text>
        <input v-model.trim="form.realName" class="input" placeholder="请输入真实姓名" />
      </view>

      <view class="field">
        <text class="label">手机号</text>
        <input v-model.trim="form.phone" class="input" placeholder="请输入手机号" />
      </view>

      <view class="field">
        <text class="label">邮箱</text>
        <input v-model.trim="form.email" class="input" placeholder="请输入邮箱" />
      </view>

      <view class="field">
        <text class="label">新密码（可选）</text>
        <input v-model="form.password" class="input" placeholder="留空表示不修改" password />
      </view>

      <view class="action-row">
        <button class="btn btn-ghost" @click="resetForm">重置</button>
        <button class="btn btn-primary" :disabled="saving" @click="saveProfile">保存修改</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { useAuthStore } from "../../stores/auth";
import { fetchUserByIdApi, updateUserApi } from "../../api/mine";

const authStore = useAuthStore();
const saving = ref(false);
const profile = ref(null);
const form = reactive({
  realName: "",
  phone: "",
  email: "",
  password: ""
});

function resolveCurrentUserId() {
  return authStore.userInfo?.id || authStore.userInfo?.userId || "";
}

function patchForm(data) {
  form.realName = data?.realName ?? "";
  form.phone = data?.phone ?? "";
  form.email = data?.email ?? "";
  form.password = "";
}

async function loadProfile() {
  const uid = resolveCurrentUserId();
  if (!uid) return;
  try {
    const res = await fetchUserByIdApi(uid);
    if (res.code !== 200 || !res.data) return;
    profile.value = res.data;
    patchForm(res.data);
  } catch (_) {}
}

function resetForm() {
  patchForm(profile.value || {});
}

async function saveProfile() {
  if (!profile.value?.id) return;
  saving.value = true;
  try {
    const payload = {
      id: profile.value.id,
      username: profile.value.username,
      role: profile.value.role,
      status: profile.value.status,
      realName: form.realName ?? "",
      phone: form.phone ?? "",
      email: form.email ?? "",
      password: form.password || undefined
    };
    const res = await updateUserApi(profile.value.id, payload);
    if (res.code !== 200 || !res.data) {
      uni.showModal({ title: "保存失败", content: res.message || "请稍后重试", showCancel: false });
      return;
    }
    profile.value = res.data;
    patchForm(res.data);
    authStore.setAuth({
      token: authStore.token,
      userInfo: {
        ...(authStore.userInfo || {}),
        realName: res.data.realName,
        phone: res.data.phone,
        email: res.data.email
      }
    });
    uni.showToast({ title: "资料更新成功", icon: "success" });
  } catch (error) {
    uni.showModal({ title: "保存失败", content: error.message || "请稍后重试", showCancel: false });
  } finally {
    saving.value = false;
  }
}

onShow(() => {
  loadProfile();
});
</script>

<style scoped>
.page { min-height: 100vh; padding: 24rpx; background: #f3f6ff; }
.module-card { background: #fff; border-radius: 24rpx; padding: 24rpx; box-shadow: 0 8rpx 22rpx rgba(40,67,129,.06); }
.title { color: #1e2c46; font-size: 36rpx; font-weight: 700; display: block; margin-bottom: 10rpx; }
.field { margin-top: 14rpx; }
.label { color: #556783; font-size: 24rpx; display: block; margin-bottom: 8rpx; }
.input { height: 76rpx; border-radius: 16rpx; border: 1rpx solid #dce5fb; background: #f8faff; padding: 0 20rpx; color: #2d3c5a; font-size: 26rpx; box-sizing: border-box; }
.action-row { margin-top: 18rpx; display: flex; justify-content: flex-end; gap: 10rpx; }
.btn { margin: 0; border: none; border-radius: 999rpx; height: 58rpx; line-height: 58rpx; font-size: 22rpx; padding: 0 20rpx; }
.btn-primary { color: #fff; background: linear-gradient(135deg, #4d74ff 0%, #78a3ff 100%); }
.btn-ghost { color: #4a5b7d; background: #eef2fb; }
</style>

