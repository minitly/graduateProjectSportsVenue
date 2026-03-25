<script setup>
import { computed, reactive, ref } from 'vue'

const props = defineProps({
  apiBase: {
    type: String,
    required: true
  },
  loading: {
    type: Boolean,
    required: true
  },
  rememberMe: {
    type: Boolean,
    required: true
  }
})

const emit = defineEmits(['update:loading', 'update:remember-me', 'toast', 'success'])

const loginForm = reactive({
  username: '',
  password: '',
  role: 'USER'
})

const errors = reactive({
  username: '',
  password: ''
})

const showPassword = ref(false)

const roleOptions = [
  { label: '普通用户（USER）', value: 'USER' },
  { label: '场馆管理员（OWNER）', value: 'OWNER' },
  { label: '系统管理员（ADMIN）', value: 'ADMIN' }
]

const canSubmit = computed(() => !props.loading)

function setError(field, message) {
  errors[field] = message
}

function resetErrors() {
  errors.username = ''
  errors.password = ''
}

function validate() {
  resetErrors()
  let valid = true
  if (!loginForm.username.trim()) {
    setError('username', '请输入用户名')
    valid = false
  }
  if (!loginForm.password.trim()) {
    setError('password', '请输入密码')
    valid = false
  }
  return valid
}

async function handleLogin() {
  if (!validate() || !canSubmit.value) return
  emit('update:loading', true)
  try {
    const response = await fetch(`${props.apiBase}/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(loginForm)
    })
    const result = await response.json()
    if (result.code !== 200) {
      emit('toast', 'error', result.message || '登录失败')
      return
    }
    emit('success', {
      ...(result.data || {}),
      selectedRole: loginForm.role
    })
  } catch (error) {
    emit('toast', 'error', '无法连接后端服务')
  } finally {
    emit('update:loading', false)
  }
}
</script>

<template>
  <form class="form" @submit.prevent="handleLogin">
    <div class="field">
      <label for="login-username">用户名</label>
      <input
        id="login-username"
        v-model="loginForm.username"
        placeholder="请输入用户名"
        :class="{ error: errors.username }"
      />
      <span v-if="errors.username" class="error-text">{{ errors.username }}</span>
    </div>

    <div class="field">
      <label for="login-password">密码</label>
      <div class="password-field">
        <input
          id="login-password"
          v-model="loginForm.password"
          :type="showPassword ? 'text' : 'password'"
          placeholder="请输入密码"
          :class="{ error: errors.password }"
        />
        <button type="button" class="ghost" @click="showPassword = !showPassword">
          {{ showPassword ? '隐藏' : '显示' }}
        </button>
      </div>
      <span v-if="errors.password" class="error-text">{{ errors.password }}</span>
    </div>

    <div class="field">
      <label for="login-role">登录角色</label>
      <select id="login-role" v-model="loginForm.role">
        <option v-for="role in roleOptions" :key="role.value" :value="role.value">
          {{ role.label }}
        </option>
      </select>
    </div>

    <div class="form-actions">
      <label class="checkbox">
        <input
          type="checkbox"
          :checked="rememberMe"
          @change="emit('update:remember-me', $event.target.checked)"
        />
        记住我
      </label>
      <span class="text-muted">忘记密码请联系管理员</span>
    </div>

    <button class="primary" type="submit" :disabled="loading">
      {{ loading ? '登录中...' : '立即登录' }}
    </button>

    <div class="login-helper-card" aria-hidden="true">
      <div class="assistant-widget vertical">
        <div class="assistant-avatar" title="登录助手">
          <div class="avatar-antenna"></div>
          <div class="avatar-face">
            <span class="eye"></span>
            <span class="eye"></span>
          </div>
          <span class="avatar-mouth"></span>
        </div>

        <div class="assistant-bubble">
          <div class="login-helper-head">
            <span class="helper-dot"></span>
            <strong>登录小助手</strong>
          </div>
          <p>建议先确认角色与密码强度，连续输错可稍后再试。</p>
          <ul>
            <li>普通用户：选择 USER 角色</li>
            <li>场馆管理员：选择 OWNER 角色</li>
            <li>系统管理员：选择 ADMIN 角色</li>
          </ul>
        </div>
      </div>
    </div>
  </form>
</template>

<style scoped>
.login-helper-card {
  margin-top: 12px;
  border-radius: 14px;
  border: 1px solid #bfdbfe;
  background: linear-gradient(135deg, rgba(219, 234, 254, 0.98), rgba(191, 219, 254, 0.95));
  padding: 12px;
  color: #334155;
  box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.35);
  animation: helperCardIntro 0.6s ease-out both,
    helperCardPulse 1.9s ease-in-out infinite 0.6s;
}

.assistant-widget {
  display: grid;
  gap: 10px;
}

.assistant-widget.vertical {
  justify-items: center;
}

.assistant-avatar {
  position: relative;
  width: 70px;
  height: 82px;
  display: grid;
  place-items: end center;
  animation: botEntrance 0.6s ease-out both,
    botFloat 1.45s cubic-bezier(0.4, 0, 0.2, 1) infinite 0.6s,
    botSway 2.2s ease-in-out infinite 0.6s;
  filter: drop-shadow(0 12px 20px rgba(37, 99, 235, 0.35));
}

.avatar-antenna {
  position: absolute;
  top: 0;
  width: 6px;
  height: 18px;
  border-radius: 999px;
  background: #60a5fa;
}

.avatar-antenna::after {
  content: '';
  position: absolute;
  top: -10px;
  left: 50%;
  transform: translateX(-50%);
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #22d3ee;
  box-shadow: 0 0 0 0 rgba(34, 211, 238, 0.55);
  animation: antennaPulse 1.2s ease-out infinite;
}

.avatar-face {
  width: 62px;
  height: 46px;
  border-radius: 15px;
  background: linear-gradient(180deg, #3b82f6, #2563eb);
  border: 1px solid rgba(30, 64, 175, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  box-shadow: 0 12px 20px rgba(37, 99, 235, 0.42);
  animation: botGlow 1.35s ease-in-out infinite;
}

.eye {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #e0f2fe;
  animation: botBlink 2.2s ease-in-out infinite;
}

.avatar-mouth {
  position: absolute;
  bottom: 11px;
  width: 14px;
  height: 3px;
  border-radius: 999px;
  background: rgba(224, 242, 254, 0.9);
  animation: mouthPulse 1.1s ease-in-out infinite;
}

.assistant-bubble {
  position: relative;
  width: 100%;
  border-radius: 12px;
  background: white;
  border: 1px solid #e2e8f0;
  padding: 10px 12px 8px;
}

.assistant-bubble::before {
  content: '';
  position: absolute;
  left: 50%;
  top: -7px;
  width: 10px;
  height: 10px;
  background: white;
  border-top: 1px solid #e2e8f0;
  border-left: 1px solid #e2e8f0;
  transform: translateX(-50%) rotate(45deg);
}

.login-helper-head {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 6px;
}

.helper-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #22c55e;
  box-shadow: 0 0 0 6px rgba(34, 197, 94, 0.14);
}

.login-helper-card p {
  margin: 0 0 6px;
  font-size: 13px;
  text-align: center;
}

.login-helper-card ul {
  margin: 0;
  padding-left: 18px;
  display: grid;
  gap: 2px;
  font-size: 12px;
  color: #475569;
}

@keyframes helperCardIntro {
  0% {
    opacity: 0.2;
    transform: scale(0.94);
    box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.45);
  }

  55% {
    opacity: 1;
    transform: scale(1.04);
    box-shadow: 0 0 0 12px rgba(59, 130, 246, 0.16), 0 16px 28px rgba(59, 130, 246, 0.3);
  }

  100% {
    opacity: 1;
    transform: scale(1);
    box-shadow: 0 8px 18px rgba(59, 130, 246, 0.16);
  }
}

@keyframes helperCardPulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.2), 0 8px 18px rgba(59, 130, 246, 0.16);
  }

  50% {
    box-shadow: 0 0 0 6px rgba(59, 130, 246, 0.08), 0 14px 26px rgba(59, 130, 246, 0.24);
  }
}

@keyframes botEntrance {
  0% {
    opacity: 0;
    transform: scale(0.72) translateY(6px);
    filter: drop-shadow(0 0 0 rgba(34, 211, 238, 0));
  }

  60% {
    opacity: 1;
    transform: scale(1.12) translateY(-2px);
    filter: drop-shadow(0 0 16px rgba(34, 211, 238, 0.55));
  }

  100% {
    opacity: 1;
    transform: scale(1) translateY(0);
    filter: drop-shadow(0 12px 20px rgba(37, 99, 235, 0.35));
  }
}

@keyframes botFloat {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-8px);
  }
}

@keyframes botSway {
  0%,
  100% {
    rotate: -2deg;
  }

  50% {
    rotate: 2deg;
  }
}

@keyframes botGlow {
  0%,
  100% {
    box-shadow: 0 12px 20px rgba(37, 99, 235, 0.35);
    filter: saturate(1);
  }

  50% {
    box-shadow: 0 16px 28px rgba(56, 189, 248, 0.58);
    filter: saturate(1.2);
  }
}

@keyframes antennaPulse {
  0% {
    box-shadow: 0 0 0 0 rgba(34, 211, 238, 0.55);
  }

  70% {
    box-shadow: 0 0 0 12px rgba(34, 211, 238, 0);
  }

  100% {
    box-shadow: 0 0 0 0 rgba(34, 211, 238, 0);
  }
}

@keyframes mouthPulse {
  0%,
  100% {
    transform: scaleX(1);
    opacity: 0.8;
  }

  50% {
    transform: scaleX(1.25);
    opacity: 1;
  }
}

@keyframes botBlink {
  0%,
  44%,
  52%,
  100% {
    transform: scaleY(1);
  }

  46%,
  48%,
  50% {
    transform: scaleY(0.15);
  }
}
</style>
