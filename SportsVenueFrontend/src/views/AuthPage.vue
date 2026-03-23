<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LoginForm from '../components/auth/LoginForm.vue'
import RegisterForm from '../components/auth/RegisterForm.vue'
import { useAuthStore } from '../stores/auth'
import { useToast } from '../composables/useToast'

const apiBase = 'http://localhost:9999/sportsVenue'
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { pushToast } = useToast()

const activeTab = computed(() => (route.query.tab === 'register' ? 'register' : 'login'))
const loading = ref(false)
const rememberMe = ref(true)
const agreePolicy = ref(true)

function showToast(type, message) {
  pushToast(message, type)
}

function handleLoginSuccess(data) {
  authStore.setSession(data.token || '', {
    userId: data.userId,
    username: data.username || '',
    realName: data.realName || '',
    role: data.role || data.selectedRole || 'USER',
    permissions: data.permissions || []
  })
  showToast('success', '登录成功')
  router.replace('/app/venues')
}

function handleRegisterSuccess() {
  showToast('success', '注册成功,请使用新账号登录')
  router.replace({
    path: '/auth',
    query: { tab: 'login' }
  })
}

function handleSwitchTab(tab) {
  router.replace({
    path: '/auth',
    query: { tab }
  })
}
</script>

<template>
  <div class="auth-page">
    <header class="auth-hero">
      <div class="brand">
        <div class="brand-mark">SV</div>
        <div>
          <h1>体育馆预约与仓库管理系统</h1>
          <p>统一认证入口 · 支持角色分离 · JWT 无状态登录</p>
        </div>
      </div>
    </header>

    <section class="auth-card">
      <div class="card">
        <div class="auth-header">
          <div>
            <h3>账号认证</h3>
            <p class="text-muted">欢迎回来，请选择登录或注册。</p>
          </div>
          <div class="security-tip">
            <span class="shield">🔒</span>
            <span>数据安全由服务端统一校验</span>
          </div>
        </div>

        <div class="tabs">
          <button
            class="tab"
            :class="{ active: activeTab === 'login' }"
            type="button"
            @click="handleSwitchTab('login')"
          >
            登录
          </button>
          <button
            class="tab"
            :class="{ active: activeTab === 'register' }"
            type="button"
            @click="handleSwitchTab('register')"
          >
            注册
          </button>
        </div>

        <div class="flip-stage" :class="{ flipped: activeTab === 'register' }">
          <div class="flip-card">
            <div class="flip-face front">
              <LoginForm
                :api-base="apiBase"
                :loading="loading"
                :remember-me="rememberMe"
                @update:remember-me="rememberMe = $event"
                @update:loading="loading = $event"
                @toast="showToast"
                @success="handleLoginSuccess"
              />
            </div>
            <div class="flip-face back">
              <RegisterForm
                :api-base="apiBase"
                :loading="loading"
                :agree-policy="agreePolicy"
                @update:agree-policy="agreePolicy = $event"
                @update:loading="loading = $event"
                @toast="showToast"
                @success="handleRegisterSuccess"
              />
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
