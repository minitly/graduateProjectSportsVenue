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
  </form>
</template>
