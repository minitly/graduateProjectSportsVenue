<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { NButton, NCard, NInput, NTag } from 'naive-ui'
import api from '../services/api'
import { useAuthStore } from '../stores/auth'
import { useToast } from '../composables/useToast'

const authStore = useAuthStore()
const { pushToast } = useToast()

const loading = ref(false)
const saving = ref(false)

const profile = ref(null)

const profileForm = reactive({
  realName: '',
  phone: '',
  email: '',
  password: ''
})

const bookingSummary = reactive({
  total: 0,
  applied: 0,
  verified: 0,
  canceled: 0,
  violation: 0
})

const borrowSummary = reactive({
  total: 0,
  requested: 0,
  using: 0,
  returned: 0
})

const roleLabel = computed(() => {
  const role = profile.value?.role || authStore.user?.role
  if (role === 'ADMIN') return '系统管理员'
  if (role === 'OWNER') return '场馆管理员'
  return '普通用户'
})

const statusLabel = computed(() => (profile.value?.status === 0 ? '已禁用' : '正常'))

const stats = computed(() => [
  { label: '预约总数', value: bookingSummary.total },
  { label: '借用总数', value: borrowSummary.total },
  { label: '违规次数', value: profile.value?.violationCountMonth ?? 0 }
])

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(
    date.getDate()
  ).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(
    date.getMinutes()
  ).padStart(2, '0')}`
}

function patchProfileForm(data) {
  profileForm.realName = data?.realName || ''
  profileForm.phone = data?.phone || ''
  profileForm.email = data?.email || ''
  profileForm.password = ''
}

async function fetchProfile() {
  const userId = authStore.user?.userId
  if (!userId) {
    pushToast('当前登录态缺少用户ID，请重新登录', 'error')
    return
  }
  const response = await api.get(`/users/${userId}`)
  if (response.code !== 200) {
    throw new Error(response.message || '用户信息加载失败')
  }
  profile.value = response.data
  patchProfileForm(response.data)
}

async function fetchBookingSummary() {
  const [allRes, appliedRes, verifiedRes, canceledRes, violationRes] = await Promise.all([
    api.get('/bookings/my', { params: { pageNo: 1, pageSize: 1 } }),
    api.get('/bookings/my', { params: { status: 'APPLIED', pageNo: 1, pageSize: 1 } }),
    api.get('/bookings/my', { params: { status: 'VERIFIED', pageNo: 1, pageSize: 1 } }),
    api.get('/bookings/my', { params: { status: 'CANCELED', pageNo: 1, pageSize: 1 } }),
    api.get('/bookings/my', { params: { status: 'VIOLATION', pageNo: 1, pageSize: 1 } })
  ])

  bookingSummary.total = allRes?.data?.total || 0
  bookingSummary.applied = appliedRes?.data?.total || 0
  bookingSummary.verified = verifiedRes?.data?.total || 0
  bookingSummary.canceled = canceledRes?.data?.total || 0
  bookingSummary.violation = violationRes?.data?.total || 0
}

async function fetchBorrowSummary() {
  const [allRes, requestedRes, usingRes, returnedRes] = await Promise.all([
    api.get('/borrows/my', { params: { pageNo: 1, pageSize: 1 } }),
    api.get('/borrows/my', { params: { status: 'REQUESTED', pageNo: 1, pageSize: 1 } }),
    api.get('/borrows/my', { params: { status: 'USING', pageNo: 1, pageSize: 1 } }),
    api.get('/borrows/my', { params: { status: 'RETURNED', pageNo: 1, pageSize: 1 } })
  ])

  borrowSummary.total = allRes?.data?.total || 0
  borrowSummary.requested = requestedRes?.data?.total || 0
  borrowSummary.using = usingRes?.data?.total || 0
  borrowSummary.returned = returnedRes?.data?.total || 0
}

async function fetchAll() {
  loading.value = true
  try {
    await Promise.all([fetchProfile(), fetchBookingSummary(), fetchBorrowSummary()])
  } catch (error) {
    pushToast(error.message || '用户中心加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  if (!profile.value?.id) return
  saving.value = true
  try {
    const payload = {
      id: profile.value.id,
      username: profile.value.username,
      role: profile.value.role,
      status: profile.value.status,
      realName: profileForm.realName,
      phone: profileForm.phone,
      email: profileForm.email,
      password: profileForm.password || undefined
    }
    const response = await api.put(`/users/${profile.value.id}`, payload)
    if (response.code !== 200) {
      pushToast(response.message || '保存失败', 'error')
      return
    }
    pushToast('资料更新成功', 'success')
    profile.value = response.data
    profileForm.password = ''
    authStore.setSession(authStore.token, {
      ...authStore.user,
      realName: response.data.realName
    })
  } catch (error) {
    pushToast('保存失败，请稍后再试', 'error')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchAll()
})
</script>

<template>
  <div class="profile-page">
    <div v-if="!loading && !profile" class="empty-state">
      <h3>暂无用户数据</h3>
      <p>请尝试重新登录后刷新页面。</p>
      <NButton type="primary" @click="fetchAll">重新加载</NButton>
    </div>

    <section class="card profile-hero" v-else>
      <div>
        <p class="section-kicker">用户中心</p>
        <h2>管理你的账号资料与使用状态</h2>
        <p class="text-muted">支持查看预约/借用统计，及时了解账号健康度。</p>
      </div>
      <div class="hero-metrics">
        <div v-for="stat in stats" :key="stat.label">
          <span>{{ stat.label }}</span>
          <strong>{{ stat.value }}</strong>
        </div>
      </div>
    </section>

    <section class="profile-grid">
      <NCard class="profile-card" title="账号信息" :loading="loading">
        <div class="profile-basic">
          <div class="profile-avatar">{{ (profile?.realName || profile?.username || 'U').charAt(0) }}</div>
          <div>
            <h3>{{ profile?.realName || profile?.username || '未命名用户' }}</h3>
            <p class="text-muted">{{ profile?.username || '—' }}</p>
            <div class="profile-tags">
              <NTag type="info">{{ roleLabel }}</NTag>
              <NTag :type="profile?.status === 0 ? 'error' : 'success'">{{ statusLabel }}</NTag>
            </div>
          </div>
        </div>

        <div class="profile-detail-grid">
          <div>
            <span>本月违规次数</span>
            <strong>{{ profile?.violationCountMonth ?? 0 }}</strong>
          </div>
          <div>
            <span>违规月份</span>
            <strong>{{ profile?.violationMonth || '—' }}</strong>
          </div>
          <div>
            <span>预约禁用截止</span>
            <strong>{{ formatDateTime(profile?.bookingBannedUntil) }}</strong>
          </div>
          <div>
            <span>创建时间</span>
            <strong>{{ formatDateTime(profile?.createTime) }}</strong>
          </div>
        </div>
      </NCard>

      <NCard class="profile-card" title="编辑资料" :loading="loading">
        <div class="profile-form">
          <div class="field">
            <label>真实姓名</label>
            <NInput v-model:value="profileForm.realName" placeholder="请输入真实姓名" />
          </div>
          <div class="field">
            <label>手机号</label>
            <NInput v-model:value="profileForm.phone" placeholder="请输入手机号" />
          </div>
          <div class="field">
            <label>邮箱</label>
            <NInput v-model:value="profileForm.email" placeholder="请输入邮箱" />
          </div>
          <div class="field">
            <label>新密码（可选）</label>
            <NInput v-model:value="profileForm.password" type="password" placeholder="留空表示不修改" />
          </div>
          <div class="profile-form__actions">
            <NButton tertiary @click="fetchAll">重置</NButton>
            <NButton type="primary" :loading="saving" @click="saveProfile">保存修改</NButton>
          </div>
        </div>
      </NCard>
    </section>

    <section class="profile-grid">
      <NCard class="profile-card" title="预约统计" :loading="loading">
        <div class="profile-stat-grid">
          <div>
            <span>申请中</span>
            <strong>{{ bookingSummary.applied }}</strong>
          </div>
          <div>
            <span>已核销</span>
            <strong>{{ bookingSummary.verified }}</strong>
          </div>
          <div>
            <span>已取消</span>
            <strong>{{ bookingSummary.canceled }}</strong>
          </div>
          <div>
            <span>违规</span>
            <strong>{{ bookingSummary.violation }}</strong>
          </div>
        </div>
      </NCard>

      <NCard class="profile-card" title="借用统计" :loading="loading">
        <div class="profile-stat-grid">
          <div>
            <span>申请中</span>
            <strong>{{ borrowSummary.requested }}</strong>
          </div>
          <div>
            <span>使用中</span>
            <strong>{{ borrowSummary.using }}</strong>
          </div>
          <div>
            <span>已归还</span>
            <strong>{{ borrowSummary.returned }}</strong>
          </div>
          <div>
            <span>总借用</span>
            <strong>{{ borrowSummary.total }}</strong>
          </div>
        </div>
      </NCard>
    </section>
  </div>
</template>
