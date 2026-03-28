<script setup>
import { computed, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LoginForm from '../components/auth/LoginForm.vue'
import RegisterForm from '../components/auth/RegisterForm.vue'
import { useAuthStore } from '../stores/auth'
import { useToast } from '../composables/useToast'
import logo from '../assets/logo.svg'

const apiBase = 'http://localhost:9999/sportsVenue'
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { pushToast } = useToast()

const activeTab = computed(() => (route.query.tab === 'register' ? 'register' : 'login'))
const loading = ref(false)
const rememberMe = ref(true)
const agreePolicy = ref(true)
const showAgreementPanel = ref(false)
const originalBodyOverflow =
  typeof document !== 'undefined' ? document.body.style.overflow : ''

function showToast(type, message) {
  pushToast(message, type)
}


function handleLoginSuccess(data) {
  authStore.setSession(data.token || '', {
    userId: data.userId,
    username: data.username || '',
    realName: data.realName || data.username || '',
    role: data.role || data.selectedRole || 'USER',
    permissions: data.permissions || []
  }, rememberMe.value)
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

function openAgreementPanel() {
  showAgreementPanel.value = true
}

function closeAgreementPanel() {
  showAgreementPanel.value = false
}

function handleGlobalKeydown(event) {
  if (event.key === 'Escape' && showAgreementPanel.value) {
    closeAgreementPanel()
  }
}

watch(showAgreementPanel, (visible) => {
  if (typeof document === 'undefined') return
  document.body.style.overflow = visible ? 'hidden' : originalBodyOverflow || ''
})

if (typeof window !== 'undefined') {
  window.addEventListener('keydown', handleGlobalKeydown)
}

onUnmounted(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('keydown', handleGlobalKeydown)
  }
  if (typeof document !== 'undefined') {
    document.body.style.overflow = originalBodyOverflow || ''
  }
})


</script>

<template>
  <div class="auth-page">
    <header class="auth-hero">
      <div class="brand" role="button" tabindex="0" @click="router.push('/')" @keyup.enter="router.push('/')">
        <div class="brand-mark" aria-hidden="true">
          <img class="brand-mark__logo" :src="logo" alt="体育馆图标" />
        </div>
        <div>
          <h1>体育馆预约与仓库管理系统</h1>
          <p>统一认证入口 · 支持角色分离 · JWT 无状态登录</p>
        </div>
        <span class="home-hint" aria-hidden="true">返回首页</span>
      </div>
    </header>

    <section class="auth-card" :class="{ 'agreement-open': showAgreementPanel }">
      <div class="card auth-main-card">
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
                @open-agreement="openAgreementPanel"
              />
            </div>
          </div>
        </div>
      </div>

      <transition name="agreement-slide">
        <div v-if="showAgreementPanel" class="card agreement-side-card" role="dialog" aria-modal="true">
          <div class="agreement-side-header">
            <h3>用户协议与隐私政策</h3>
            <button type="button" class="ghost" @click="closeAgreementPanel">关闭</button>
          </div>
          <div class="agreement-side-content">
            <h4>一、总则</h4>
            <p>欢迎使用体育馆预约与仓库管理系统。您注册、登录或使用本平台服务，即视为已阅读并同意本协议与隐私条款。</p>

            <h4>二、账号与安全</h4>
            <p>您应妥善保管账号、密码与验证码信息，并对账号下操作行为负责。因保管不善造成的风险由您自行承担。</p>

            <h4>三、服务使用规范</h4>
            <p>您承诺遵守法律法规及平台规则，不得利用本平台从事违法违规活动，不得干扰平台正常运行。</p>

            <h4>四、信息收集与使用</h4>
            <p>为实现注册登录、预约管理与通知服务，我们会收集必要信息（如用户名、邮箱、手机号、角色信息及日志信息），仅用于身份认证、业务处理和安全风控。</p>

            <h4>五、信息保护与共享</h4>
            <p>我们将采取合理安全措施保护您的个人信息。除法律要求或经您授权外，不会向无关第三方披露您的个人信息。</p>

            <h4>六、条款更新</h4>
            <p>我们可能根据业务和法规要求更新本条款。更新后在平台公示，您继续使用即视为同意更新内容。</p>
          </div>
        </div>
      </transition>
    </section>

  </div>
</template>
