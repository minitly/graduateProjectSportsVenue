<script setup>
import { computed, reactive } from 'vue'
import { useQuery, useQueryClient } from '@tanstack/vue-query'
import {
  NButton,
  NCard,
  NInput,
  NModal,
  NSelect,
  NTag
} from 'naive-ui'
import api from '../services/api'
import { useToast } from '../composables/useToast'

const { pushToast } = useToast()
const queryClient = useQueryClient()

const filters = reactive({
  keyword: '',
  status: ''
})

const pagination = reactive({
  pageNo: 1,
  pageSize: 10
})

const statusOptions = [
  { label: '全部', value: '' },
  { label: '正常', value: 1 },
  { label: '禁用', value: 0 }
]

const roleOptions = [
  { label: '普通用户 USER', value: 'USER' },
  { label: '场馆管理员 OWNER', value: 'OWNER' }
]

const createModal = reactive({
  show: false,
  submitting: false,
  form: {
    username: '',
    password: '',
    realName: '',
    role: 'USER',
    status: 1,
    phone: '',
    email: ''
  }
})

const detailModal = reactive({
  show: false,
  loading: false,
  user: null
})

const editModal = reactive({
  show: false,
  loading: false,
  submitting: false,
  form: {
    id: null,
    username: '',
    role: '',
    realName: '',
    status: 1,
    phone: '',
    email: '',
    password: ''
  }
})

const usersQuery = useQuery({
  queryKey: computed(() => ['adminUsers', filters.keyword, filters.status, pagination.pageNo, pagination.pageSize]),
  queryFn: async () => {
    const useSearch = filters.keyword || filters.status !== ''
    const response = useSearch
      ? await api.get('/users/search', {
          params: {
            keyword: filters.keyword || undefined,
            status: filters.status === '' ? undefined : filters.status,
            pageNo: pagination.pageNo,
            pageSize: pagination.pageSize
          }
        })
      : await api.get('/users', {
          params: {
            pageNo: pagination.pageNo,
            pageSize: pagination.pageSize
          }
        })

    if (response.code !== 200) {
      throw new Error(response.message || '用户列表加载失败')
    }

    return response.data || { records: [], total: 0 }
  },
  keepPreviousData: true,
  staleTime: 30000
})

const isUsersFetching = computed(() => Boolean(usersQuery.isFetching?.value ?? usersQuery.isFetching))
const users = computed(() => usersQuery.data?.records || usersQuery.data?.value?.records || [])
const total = computed(() => usersQuery.data?.total || usersQuery.data?.value?.total || 0)

const stats = computed(() => {
  const rows = users.value
  const enabledCount = rows.filter((item) => item.status === 1).length
  const disabledCount = rows.filter((item) => item.status === 0).length
  return [
    { label: '当前页用户', value: rows.length },
    { label: '正常账号', value: enabledCount },
    { label: '禁用账号', value: disabledCount }
  ]
})

function handleSearch() {
  pagination.pageNo = 1
  usersQuery.refetch()
}

function resetFilters() {
  filters.keyword = ''
  filters.status = ''
  handleSearch()
}

function resetCreateForm() {
  createModal.form = {
    username: '',
    password: '',
    realName: '',
    role: 'USER',
    status: 1,
    phone: '',
    email: ''
  }
}

function openCreateModal() {
  resetCreateForm()
  createModal.show = true
}

function closeCreateModal() {
  createModal.show = false
}

function validateCreateForm() {
  if (!createModal.form.username.trim()) {
    pushToast('请输入用户名', 'warning')
    return false
  }
  if (!createModal.form.password.trim()) {
    pushToast('请输入初始密码', 'warning')
    return false
  }
  if (!createModal.form.realName.trim()) {
    pushToast('请输入真实姓名', 'warning')
    return false
  }
  return true
}

async function submitCreateUser() {
  if (!validateCreateForm()) return
  createModal.submitting = true
  try {
    const response = await api.post('/users', {
      username: createModal.form.username.trim(),
      password: createModal.form.password,
      realName: createModal.form.realName.trim(),
      role: createModal.form.role,
      status: createModal.form.status,
      phone: createModal.form.phone?.trim() || undefined,
      email: createModal.form.email?.trim() || undefined
    })

    if (response.code !== 200) {
      pushToast(response.message || '创建用户失败', 'error')
      return
    }

    pushToast('创建用户成功', 'success')
    closeCreateModal()
    queryClient.invalidateQueries({ queryKey: ['adminUsers'] })
  } catch (error) {
    pushToast('创建失败，请稍后重试', 'error')
  } finally {
    createModal.submitting = false
  }
}

async function loadUserDetail(id) {
  const response = await api.get(`/users/${id}`)
  if (response.code !== 200) {
    throw new Error(response.message || '用户详情加载失败')
  }
  return response.data
}

async function openDetailModal(user) {
  detailModal.show = true
  detailModal.loading = true
  detailModal.user = null
  try {
    detailModal.user = await loadUserDetail(user.id)
  } catch (error) {
    pushToast(error.message || '加载用户详情失败', 'error')
  } finally {
    detailModal.loading = false
  }
}

function closeDetailModal() {
  detailModal.show = false
}

async function openEditModal(user) {
  editModal.show = true
  editModal.loading = true
  try {
    const detail = await loadUserDetail(user.id)
    editModal.form = {
      id: detail.id,
      username: detail.username || '',
      role: detail.role || 'USER',
      realName: detail.realName || '',
      status: detail.status ?? 1,
      phone: detail.phone || '',
      email: detail.email || '',
      password: ''
    }
  } catch (error) {
    pushToast(error.message || '加载编辑数据失败', 'error')
    editModal.show = false
  } finally {
    editModal.loading = false
  }
}

function closeEditModal() {
  editModal.show = false
}

function validateEditForm() {
  if (!editModal.form.realName.trim()) {
    pushToast('请输入真实姓名', 'warning')
    return false
  }
  return true
}

async function submitEditUser() {
  if (!validateEditForm()) return
  editModal.submitting = true
  try {
    const payload = {
      id: editModal.form.id,
      username: editModal.form.username,
      realName: editModal.form.realName.trim(),
      role: editModal.form.role,
      status: editModal.form.status,
      phone: editModal.form.phone?.trim() || '',
      email: editModal.form.email?.trim() || ''
    }

    if (editModal.form.password?.trim()) {
      payload.password = editModal.form.password
    }

    const response = await api.put(`/users/${editModal.form.id}`, payload)
    if (response.code !== 200) {
      pushToast(response.message || '更新用户失败', 'error')
      return
    }

    pushToast('更新用户成功', 'success')
    closeEditModal()
    queryClient.invalidateQueries({ queryKey: ['adminUsers'] })
  } catch (error) {
    pushToast('更新失败，请稍后重试', 'error')
  } finally {
    editModal.submitting = false
  }
}

async function toggleUserStatus(user) {
  try {
    const detail = await loadUserDetail(user.id)
    const response = await api.put(`/users/${user.id}`, {
      id: detail.id,
      username: detail.username,
      realName: detail.realName,
      role: detail.role,
      status: detail.status === 1 ? 0 : 1,
      phone: detail.phone || '',
      email: detail.email || ''
    })

    if (response.code !== 200) {
      pushToast(response.message || '用户状态更新失败', 'error')
      return
    }

    pushToast(detail.status === 1 ? '用户已禁用' : '用户已启用', 'success')
    queryClient.invalidateQueries({ queryKey: ['adminUsers'] })
  } catch (error) {
    pushToast('更新失败，请稍后再试', 'error')
  }
}

function nextPage() {
  if (pagination.pageNo * pagination.pageSize >= total.value) return
  pagination.pageNo += 1
}

function prevPage() {
  if (pagination.pageNo <= 1) return
  pagination.pageNo -= 1
}

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}
</script>

<template>
  <div class="admin-users-page">
    <section class="card profile-hero">
      <div>
        <p class="section-kicker">用户管理</p>
        <h2>按文档完成用户全流程管理</h2>
        <p class="text-muted">支持创建、详情查看、分页筛选、编辑与启用/禁用。</p>
      </div>
      <div class="hero-metrics">
        <div v-for="stat in stats" :key="stat.label">
          <span>{{ stat.label }}</span>
          <strong>{{ stat.value }}</strong>
        </div>
      </div>
    </section>

    <section class="card borrow-filters">
      <div class="field">
        <label>关键词</label>
        <NInput v-model:value="filters.keyword" placeholder="用户名关键字" />
      </div>
      <div class="field">
        <label>状态</label>
        <NSelect v-model:value="filters.status" :options="statusOptions" />
      </div>
      <div class="borrow-filters__actions">
        <NButton type="primary" :loading="isUsersFetching" @click="handleSearch">查询</NButton>
        <NButton tertiary @click="resetFilters">重置</NButton>
        <NButton type="info" @click="openCreateModal">创建用户</NButton>
      </div>
    </section>

    <section class="borrow-panel__list">
      <NCard v-for="user in users" :key="user.id" size="small" class="borrow-record">
        <template #header>
          <div class="borrow-record__header">
            <div>
              <strong>{{ user.realName || user.username }}</strong>
              <p class="text-muted">@{{ user.username }} · {{ user.role }}</p>
            </div>
            <NTag :type="user.status === 1 ? 'success' : 'error'">{{ user.status === 1 ? '正常' : '禁用' }}</NTag>
          </div>
        </template>

        <div class="borrow-record__body">
          <div>
            <span>手机号</span>
            <strong>{{ user.phone || '—' }}</strong>
          </div>
          <div>
            <span>邮箱</span>
            <strong>{{ user.email || '—' }}</strong>
          </div>
          <div>
            <span>本月违规</span>
            <strong>{{ user.violationCountMonth ?? 0 }}</strong>
          </div>
          <div>
            <span>禁用截止</span>
            <strong>{{ formatDateTime(user.bookingBannedUntil) }}</strong>
          </div>
        </div>

        <div class="borrow-record__actions">
          <NButton size="small" tertiary @click="openDetailModal(user)">详情</NButton>
          <NButton size="small" type="primary" tertiary @click="openEditModal(user)">编辑</NButton>
          <NButton
            size="small"
            :type="user.status === 1 ? 'error' : 'success'"
            @click="toggleUserStatus(user)"
          >
            {{ user.status === 1 ? '禁用用户' : '启用用户' }}
          </NButton>
        </div>
      </NCard>

      <div v-if="!users.length && !isUsersFetching" class="empty-state">
        <h3>暂无用户数据</h3>
        <p>请调整筛选条件后重试。</p>
      </div>
    </section>

    <section class="pagination">
      <NButton tertiary @click="prevPage" :disabled="pagination.pageNo <= 1">上一页</NButton>
      <span>第 {{ pagination.pageNo }} 页 / 共 {{ Math.ceil(total / pagination.pageSize) || 1 }} 页</span>
      <NButton tertiary @click="nextPage" :disabled="pagination.pageNo * pagination.pageSize >= total">下一页</NButton>
    </section>

    <NModal v-model:show="createModal.show" preset="card" title="创建用户" class="booking-modal">
      <div class="booking-modal__section"><label>用户名 *</label><NInput v-model:value="createModal.form.username" /></div>
      <div class="booking-modal__section"><label>初始密码 *</label><NInput v-model:value="createModal.form.password" type="password" /></div>
      <div class="booking-modal__section"><label>真实姓名 *</label><NInput v-model:value="createModal.form.realName" /></div>
      <div class="booking-modal__section two-col">
        <div><label>角色</label><NSelect v-model:value="createModal.form.role" :options="roleOptions" /></div>
        <div><label>状态</label><NSelect v-model:value="createModal.form.status" :options="statusOptions.filter((item) => item.value !== '')" /></div>
      </div>
      <div class="booking-modal__section two-col">
        <div><label>手机号</label><NInput v-model:value="createModal.form.phone" /></div>
        <div><label>邮箱</label><NInput v-model:value="createModal.form.email" /></div>
      </div>
      <div class="booking-modal__actions">
        <NButton @click="closeCreateModal">取消</NButton>
        <NButton type="primary" :loading="createModal.submitting" @click="submitCreateUser">确认创建</NButton>
      </div>
    </NModal>

    <NModal v-model:show="detailModal.show" preset="card" title="用户详情" class="booking-modal">
      <div v-if="detailModal.loading" class="text-muted">加载中...</div>
      <div v-else-if="detailModal.user" class="detail-grid">
        <div><span>用户ID</span><strong>{{ detailModal.user.id }}</strong></div>
        <div><span>用户名</span><strong>{{ detailModal.user.username }}</strong></div>
        <div><span>真实姓名</span><strong>{{ detailModal.user.realName || '—' }}</strong></div>
        <div><span>角色</span><strong>{{ detailModal.user.role }}</strong></div>
        <div><span>状态</span><strong>{{ detailModal.user.status === 1 ? '正常' : '禁用' }}</strong></div>
        <div><span>手机号</span><strong>{{ detailModal.user.phone || '—' }}</strong></div>
        <div><span>邮箱</span><strong>{{ detailModal.user.email || '—' }}</strong></div>
        <div><span>本月违规</span><strong>{{ detailModal.user.violationCountMonth ?? 0 }}</strong></div>
        <div><span>违规月份</span><strong>{{ detailModal.user.violationMonth || '—' }}</strong></div>
        <div><span>预约禁用截止</span><strong>{{ formatDateTime(detailModal.user.bookingBannedUntil) }}</strong></div>
        <div><span>创建时间</span><strong>{{ formatDateTime(detailModal.user.createTime) }}</strong></div>
        <div><span>更新时间</span><strong>{{ formatDateTime(detailModal.user.updateTime) }}</strong></div>
      </div>
      <div class="booking-modal__actions">
        <NButton type="primary" @click="closeDetailModal">关闭</NButton>
      </div>
    </NModal>

    <NModal v-model:show="editModal.show" preset="card" title="编辑用户" class="booking-modal">
      <div v-if="editModal.loading" class="text-muted">加载中...</div>
      <template v-else>
        <div class="booking-modal__section two-col">
          <div><label>用户名（不可改）</label><NInput :value="editModal.form.username" disabled /></div>
          <div><label>角色（不可改）</label><NInput :value="editModal.form.role" disabled /></div>
        </div>
        <div class="booking-modal__section"><label>真实姓名 *</label><NInput v-model:value="editModal.form.realName" /></div>
        <div class="booking-modal__section two-col">
          <div><label>状态</label><NSelect v-model:value="editModal.form.status" :options="statusOptions.filter((item) => item.value !== '')" /></div>
          <div><label>重置密码（可选）</label><NInput v-model:value="editModal.form.password" type="password" placeholder="留空表示不修改" /></div>
        </div>
        <div class="booking-modal__section two-col">
          <div><label>手机号</label><NInput v-model:value="editModal.form.phone" /></div>
          <div><label>邮箱</label><NInput v-model:value="editModal.form.email" /></div>
        </div>
        <div class="booking-modal__actions">
          <NButton @click="closeEditModal">取消</NButton>
          <NButton type="primary" :loading="editModal.submitting" @click="submitEditUser">保存修改</NButton>
        </div>
      </template>
    </NModal>
  </div>
</template>
