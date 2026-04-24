<template>
  <view class="page">
    <view class="bg-orb orb-one"></view>
    <view class="bg-orb orb-two"></view>
    <view class="bg-orb orb-three"></view>

    <view class="section-block">
      <view class="overview-card">
        <view class="logo-wrap">
          <image class="logo" src="/static/logo.svg" mode="aspectFit"></image>
        </view>
        <view class="hero-content">
          <text class="section-tag">体育馆服务平台</text>
          <text class="hero-title">体育馆预约与仓库管理系统</text>
          <text class="hero-subtitle">一体化预约、借用与公告服务，面向校园用户的便捷入口。</text>
        </view>
        <view class="stats-row">
          <view class="stat-card">
            <text class="stat-label">可预约场馆</text>
            <text class="stat-value">120+</text>
          </view>
          <view class="stat-card">
            <text class="stat-label">活跃用户</text>
            <text class="stat-value">12,000+</text>
          </view>
          <view class="stat-card">
            <text class="stat-label">预约成功率</text>
            <text class="stat-value">98.7%</text>
          </view>
        </view>
      </view>
    </view>

    <view class="hero-card">
      <view class="entry-card">
        <text class="entry-title">统一认证入口</text>
        <text class="entry-desc">支持用户端账号认证与权限隔离</text>
        <text class="entry-link" @click="openAuth('login')">进入登录</text>
      </view>
    </view>

    <view class="section-block">
      <text class="section-tag">三步完成预约</text>
      <text class="section-title">轻松上手，分分钟开打</text>
      <view class="quick-grid">
        <view class="quick-item">
          <view class="title-row">
            <uni-icons type="home-filled" size="16" color="#4d74f8"></uni-icons>
            <text class="quick-title">选场馆</text>
          </view>
          <text class="quick-desc">智能推荐，筛到你喜欢</text>
        </view>
        <view class="quick-item">
          <view class="title-row">
            <uni-icons type="calendar-filled" size="16" color="#4d74f8"></uni-icons>
            <text class="quick-title">一键预约</text>
          </view>
          <text class="quick-desc">实时余位，秒级锁定</text>
        </view>
        <view class="quick-item">
          <view class="title-row">
            <uni-icons type="checkbox-filled" size="16" color="#4d74f8"></uni-icons>
            <text class="quick-title">到场核销</text>
          </view>
          <text class="quick-desc">凭预约码快速入场</text>
        </view>
      </view>
    </view>

    <view class="section-block">
      <text class="section-tag">核心功能</text>
      <text class="section-title">公告、场地与器材，一站管理</text>
      <view class="feature-grid">
        <view class="feature-item">
          <view class="title-row">
            <uni-icons type="notification-filled" size="16" color="#4d74f8"></uni-icons>
            <text class="feature-title">公告中心</text>
          </view>
          <text class="feature-desc">浏览平台通知，重要信息集中展示</text>
        </view>
        <view class="feature-item">
          <view class="title-row">
            <uni-icons type="map-filled" size="16" color="#4d74f8"></uni-icons>
            <text class="feature-title">场地预约</text>
          </view>
          <text class="feature-desc">按条件筛选场地，在线选择预约时段</text>
        </view>
        <view class="feature-item">
          <view class="title-row">
            <uni-icons type="gift-filled" size="16" color="#4d74f8"></uni-icons>
            <text class="feature-title">器材借用</text>
          </view>
          <text class="feature-desc">查看可借库存，提交申请并跟踪进度</text>
        </view>
        <view class="feature-item">
          <view class="title-row">
            <uni-icons type="person-filled" size="16" color="#4d74f8"></uni-icons>
            <text class="feature-title">我的中心</text>
          </view>
          <text class="feature-desc">管理账号资料，查看预约与借用状态</text>
        </view>
      </view>
    </view>

    <view class="welfare-card">
      <view class="welfare-text">
        <text class="welfare-tag">新手福利</text>
        <text class="welfare-title">免费注册，解锁预约与器材借用</text>
        <text class="welfare-desc">创建账号即可使用场地预约、器材申请与公告浏览，随时查看进度。</text>
      </view>
      <button class="btn btn-primary welfare-btn" @click="openAuth('register')">立即注册</button>
    </view>

    <view v-if="showAuthModal" class="modal-mask" @click="closeAuth"></view>
    <view v-if="showAuthModal" class="modal-panel">
      <view class="modal-header">
        <text class="modal-title">{{ activeTab === "login" ? "欢迎登录" : "创建账号" }}</text>
        <text class="modal-subtitle">
          {{ activeTab === "login" ? "登录后可查看预约、借用与公告进度" : "完成注册后可立即体验场地预约与器材借用" }}
        </text>
      </view>

      <view class="tabs">
        <view
          class="tab-item"
          :class="{ active: activeTab === 'login' }"
          @click="switchTab('login')"
        >
          登录
        </view>
        <view
          class="tab-item"
          :class="{ active: activeTab === 'register' }"
          @click="switchTab('register')"
        >
          注册
        </view>
      </view>

      <view v-if="activeTab === 'login'" class="form-wrap">
        <input
          v-model.trim="loginForm.username"
          class="input form-item"
          type="text"
          placeholder="请输入用户名"
          placeholder-class="input-placeholder"
        />
        <view class="password-wrap form-item">
          <input
            :key="`login-password-${loginPwdVisible ? 'visible' : 'hidden'}`"
            v-model="loginForm.password"
            class="input password-input"
            type="text"
            :password="!loginPwdVisible"
            placeholder="请输入密码"
            placeholder-class="input-placeholder"
          />
          <text class="eye-btn" @click="loginPwdVisible = !loginPwdVisible">{{ loginPwdVisible ? "隐藏" : "显示" }}</text>
        </view>
        <button class="btn btn-primary full form-item" :disabled="submitting" @click="handleLogin">登录</button>
        <text class="switch-text form-item">
          还没有账号？
          <text class="switch-link" @click="switchTab('register')">去注册</text>
        </text>
      </view>

      <view v-else class="form-wrap">
        <input
          v-model.trim="registerForm.username"
          class="input form-item"
          type="text"
          placeholder="用户名（必填）"
          placeholder-class="input-placeholder"
        />
        <view class="password-wrap form-item">
          <input
            :key="`register-password-${registerPwdVisible ? 'visible' : 'hidden'}`"
            v-model="registerForm.password"
            class="input password-input"
            type="text"
            :password="!registerPwdVisible"
            placeholder="密码（必填）"
            placeholder-class="input-placeholder"
          />
          <text class="eye-btn" @click="registerPwdVisible = !registerPwdVisible">{{ registerPwdVisible ? "隐藏" : "显示" }}</text>
        </view>
        <view class="password-wrap form-item">
          <input
            :key="`register-confirm-password-${registerConfirmPwdVisible ? 'visible' : 'hidden'}`"
            v-model="registerForm.confirmPassword"
            class="input password-input"
            type="text"
            :password="!registerConfirmPwdVisible"
            placeholder="确认密码（必填）"
            placeholder-class="input-placeholder"
          />
          <text class="eye-btn" @click="registerConfirmPwdVisible = !registerConfirmPwdVisible">
            {{ registerConfirmPwdVisible ? "隐藏" : "显示" }}
          </text>
        </view>
        <input
          v-model.trim="registerForm.realName"
          class="input form-item"
          type="text"
          placeholder="真实姓名（必填）"
          placeholder-class="input-placeholder"
        />
        <input
          v-model.trim="registerForm.email"
          class="input form-item"
          type="text"
          placeholder="邮箱（可选）"
          placeholder-class="input-placeholder"
        />
        <input
          v-model.trim="registerForm.phone"
          class="input form-item"
          type="text"
          placeholder="手机号（可选）"
          placeholder-class="input-placeholder"
        />
        <button class="btn btn-primary full form-item" :disabled="submitting" @click="handleRegister">提交注册</button>
        <text class="switch-text form-item">
          已有账号？
          <text class="switch-link" @click="switchTab('login')">去登录</text>
        </text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from "vue";
import UniIcons from "@dcloudio/uni-ui/lib/uni-icons/uni-icons.vue";
import { loginApi, registerApi } from "../../api/auth";
import { useAuthStore } from "../../stores/auth";

const authStore = useAuthStore();
const showAuthModal = ref(false);
const activeTab = ref("login");
const submitting = ref(false);
const loginPwdVisible = ref(false);
const registerPwdVisible = ref(false);
const registerConfirmPwdVisible = ref(false);

const loginForm = reactive({
  username: "",
  password: ""
});

const registerForm = reactive({
  username: "",
  password: "",
  confirmPassword: "",
  realName: "",
  email: "",
  phone: ""
});

function openAuth(tab) {
  activeTab.value = tab;
  resetPasswordVisible();
  showAuthModal.value = true;
}

function closeAuth() {
  showAuthModal.value = false;
}

function switchTab(tab) {
  activeTab.value = tab;
  resetPasswordVisible();
}

function resetPasswordVisible() {
  loginPwdVisible.value = false;
  registerPwdVisible.value = false;
  registerConfirmPwdVisible.value = false;
}

function showError(message) {
  uni.showModal({
    title: "提示",
    content: message || "操作失败，请稍后重试",
    showCancel: false
  });
}

function validateLoginForm() {
  if (!loginForm.username) return "请输入用户名";
  if (!loginForm.password) return "请输入密码";
  return "";
}

function validateRegisterForm() {
  if (!registerForm.username) return "请输入用户名";
  if (!registerForm.password) return "请输入密码";
  if (!registerForm.confirmPassword) return "请输入确认密码";
  if (registerForm.password !== registerForm.confirmPassword) return "两次输入的密码不一致";
  if (!registerForm.realName) return "请输入真实姓名";
  if (registerForm.email && !/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(registerForm.email)) {
    return "邮箱格式不正确";
  }
  if (registerForm.phone && !/^1[3-9]\d{9}$/.test(registerForm.phone)) {
    return "手机号格式不正确";
  }
  return "";
}

async function handleLogin() {
  const error = validateLoginForm();
  if (error) {
    showError(error);
    return;
  }

  submitting.value = true;
  try {
    const res = await loginApi({
      username: loginForm.username,
      password: loginForm.password
    });

    if (res.code !== 200 || !res.data?.token) {
      showError(res.message || "登录失败");
      return;
    }

    authStore.setAuth({
      token: res.data.token,
      userInfo: res.data
    });

    uni.showToast({
      title: "登录成功",
      icon: "success"
    });

    showAuthModal.value = false;
    uni.switchTab({
      url: "/pages/venue/index"
    });
  } catch (err) {
    showError(err.message);
  } finally {
    submitting.value = false;
  }
}

async function handleRegister() {
  const error = validateRegisterForm();
  if (error) {
    showError(error);
    return;
  }

  submitting.value = true;
  try {
    const res = await registerApi({
      username: registerForm.username,
      password: registerForm.password,
      realName: registerForm.realName,
      email: registerForm.email,
      phone: registerForm.phone
    });

    if (res.code !== 200) {
      showError(res.message || "注册失败");
      return;
    }

    uni.showModal({
      title: "注册成功",
      content: "账号已创建，请使用用户名和密码登录",
      showCancel: false
    });

    activeTab.value = "login";
  } catch (err) {
    showError(err.message);
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 48rpx 32rpx 60rpx;
  background: linear-gradient(180deg, #edf3ff 0%, #f7faff 100%);
  position: relative;
  overflow: hidden;
}

.bg-orb {
  position: absolute;
  border-radius: 999rpx;
  filter: blur(6rpx);
}

.orb-one {
  width: 280rpx;
  height: 280rpx;
  right: -90rpx;
  top: 40rpx;
  background: rgba(85, 110, 255, 0.15);
}

.orb-two {
  width: 240rpx;
  height: 240rpx;
  left: -70rpx;
  top: 460rpx;
  background: rgba(113, 204, 255, 0.18);
}

.orb-three {
  width: 220rpx;
  height: 220rpx;
  right: -60rpx;
  top: 980rpx;
  background: rgba(151, 134, 255, 0.14);
}

.hero-card {
  position: relative;
  z-index: 1;
  margin-top: 20rpx;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 28rpx;
  padding: 28rpx 24rpx;
  box-shadow: 0 10rpx 30rpx rgba(49, 83, 167, 0.08);
}

.overview-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 28rpx;
  padding: 32rpx 28rpx;
  box-shadow: 0 10rpx 30rpx rgba(49, 83, 167, 0.08);
}

.logo-wrap {
  width: 90rpx;
  height: 90rpx;
  border-radius: 22rpx;
  background: linear-gradient(135deg, #4b7cff 0%, #5aa7ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo {
  width: 58rpx;
  height: 58rpx;
}

.entry-card {
  width: 100%;
  padding: 18rpx 22rpx;
  border-radius: 20rpx;
  background: #f4f8ff;
}

.entry-title {
  display: block;
  color: #2a3858;
  font-size: 30rpx;
  font-weight: 700;
}

.entry-desc {
  margin-top: 8rpx;
  display: block;
  color: #60708f;
  font-size: 24rpx;
}

.entry-link {
  margin-top: 10rpx;
  display: inline-block;
  color: #3f67eb;
  font-size: 26rpx;
  font-weight: 600;
}

.hero-content {
  margin-top: 20rpx;
  display: flex;
  flex-direction: column;
}

.section-tag {
  color: #4c6dea;
  font-size: 24rpx;
  font-weight: 600;
}

.hero-title {
  margin-top: 8rpx;
  font-size: 42rpx;
  line-height: 1.4;
  font-weight: 700;
  color: #172339;
}

.hero-subtitle {
  margin-top: 14rpx;
  color: #53627d;
  font-size: 28rpx;
  line-height: 1.5;
}

.stats-row {
  margin-top: 24rpx;
  display: flex;
}

.stat-card + .stat-card {
  margin-left: 14rpx;
}

.stat-card {
  flex: 1;
  background: #fff;
  border-radius: 18rpx;
  padding: 18rpx 14rpx;
  box-shadow: 0 6rpx 16rpx rgba(30, 63, 138, 0.07);
}

.stat-label {
  color: #6a7895;
  font-size: 22rpx;
}

.stat-value {
  margin-top: 8rpx;
  display: block;
  color: #1e2c46;
  font-size: 32rpx;
  font-weight: 700;
}

.btn {
  margin: 0;
  border-radius: 999rpx;
  border: none;
  height: 84rpx;
  line-height: 84rpx;
  font-size: 30rpx;
}

.btn-primary {
  color: #fff;
  background: linear-gradient(135deg, #4b72ff 0%, #5f9dff 100%);
}

.section-block {
  margin-top: 26rpx;
  position: relative;
  z-index: 1;
}

.section-title {
  margin-top: 6rpx;
  display: block;
  color: #1b2842;
  font-size: 46rpx;
  font-weight: 700;
  line-height: 1.3;
}

.quick-grid {
  margin-top: 16rpx;
  display: flex;
  flex-direction: column;
}

.quick-item {
  background: #fff;
  border-radius: 18rpx;
  padding: 20rpx 22rpx;
  box-shadow: 0 8rpx 22rpx rgba(37, 64, 133, 0.06);
}

.quick-item + .quick-item {
  margin-top: 14rpx;
}

.quick-title {
  font-size: 30rpx;
  color: #202f4a;
  font-weight: 700;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.quick-desc {
  margin-top: 8rpx;
  display: block;
  font-size: 24rpx;
  color: #697895;
}

.feature-grid {
  margin-top: 16rpx;
  display: flex;
  flex-direction: column;
}

.feature-item {
  background: #fff;
  border-radius: 24rpx;
  padding: 26rpx;
  box-shadow: 0 8rpx 22rpx rgba(37, 64, 133, 0.06);
}

.feature-item + .feature-item {
  margin-top: 18rpx;
}

.feature-title {
  color: #1c2a45;
  font-size: 32rpx;
  font-weight: 700;
}

.feature-desc {
  margin-top: 10rpx;
  display: block;
  color: #61708d;
  font-size: 26rpx;
}

.welfare-card {
  margin-top: 26rpx;
  position: relative;
  z-index: 1;
  background: linear-gradient(120deg, #eef4ff 0%, #e7f2ff 100%);
  border: 1rpx solid #dbe8ff;
  border-radius: 24rpx;
  padding: 24rpx;
}

.welfare-tag {
  display: block;
  color: #4f70e8;
  font-size: 24rpx;
  font-weight: 700;
}

.welfare-title {
  display: block;
  margin-top: 8rpx;
  color: #1f2d47;
  font-size: 36rpx;
  font-weight: 700;
}

.welfare-desc {
  display: block;
  margin-top: 10rpx;
  color: #5f6f8c;
  font-size: 24rpx;
  line-height: 1.6;
}

.welfare-btn {
  margin-top: 20rpx;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(8, 17, 36, 0.45);
  z-index: 20;
}

.modal-panel {
  position: fixed;
  left: 32rpx;
  right: 32rpx;
  top: 50%;
  transform: translateY(-50%);
  max-height: 84vh;
  overflow-y: auto;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border-radius: 30rpx;
  z-index: 21;
  padding: 24rpx 24rpx 32rpx;
  box-shadow: 0 16rpx 40rpx rgba(41, 64, 120, 0.18);
  box-sizing: border-box;
}

.modal-header {
  margin-bottom: 16rpx;
}

.modal-title {
  display: block;
  color: #1e2c46;
  font-size: 38rpx;
  font-weight: 700;
  line-height: 1.25;
}

.modal-subtitle {
  margin-top: 8rpx;
  display: block;
  color: #6f7f9b;
  font-size: 24rpx;
  line-height: 1.45;
}

.tabs {
  display: flex;
  background: #eef3ff;
  border-radius: 999rpx;
  padding: 8rpx;
  border: 1rpx solid #dde7ff;
}

.tab-item {
  flex: 1;
  text-align: center;
  line-height: 66rpx;
  border-radius: 999rpx;
  color: #7482a0;
  font-size: 26rpx;
  font-weight: 500;
  transition: all 0.2s ease;
}

.tab-item.active {
  background: linear-gradient(135deg, #5a7dff 0%, #79a2ff 100%);
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 6rpx 16rpx rgba(79, 119, 245, 0.35);
}

.form-wrap {
  margin-top: 20rpx;
  display: flex;
  flex-direction: column;
}

.form-item + .form-item {
  margin-top: 14rpx;
}

.password-wrap {
  display: flex;
  align-items: center;
}

.password-input {
  flex: 1;
  min-width: 0;
}

.eye-btn {
  margin-left: 14rpx;
  min-width: 86rpx;
  text-align: center;
  line-height: 68rpx;
  height: 68rpx;
  border-radius: 999rpx;
  background: #edf3ff;
  border: 1rpx solid #dce6ff;
  color: #6d7fa0;
  font-size: 24rpx;
}

.input {
  height: 84rpx;
  border-radius: 20rpx;
  border: 1rpx solid #dfe8fb;
  padding: 0 26rpx;
  font-size: 28rpx;
  color: #263654;
  background: #f7faff;
  box-sizing: border-box;
}

.input-placeholder {
  color: #9aa8c3;
  font-size: 27rpx;
}

.full {
  width: 100%;
  margin-top: 14rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 30rpx;
  font-weight: 600;
  letter-spacing: 1rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #4d74ff 0%, #76a1ff 100%);
  box-shadow: 0 10rpx 22rpx rgba(74, 111, 241, 0.32);
}

.switch-text {
  margin-top: 10rpx;
  text-align: center;
  color: #8a97b0;
  font-size: 24rpx;
  line-height: 1.4;
}

.switch-link {
  color: #4d74f8;
  font-weight: 500;
}
</style>
