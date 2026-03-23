<script setup>
import { computed, reactive } from 'vue'
import { useQuery, useQueryClient } from '@tanstack/vue-query'
import { NButton, NCard, NInput, NSelect, NTag } from 'naive-ui'
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

const users = computed(() => usersQuery.data?.records || usersQuery.data?.value?.records || [])
const total = computed(() => usersQuery.data?.total || usersQuery.data?.value?.total || 0)

const bookingStatsQuery = useQuery({
  queryKey: ['adminBookingStats'],
  queryFn: async () => {
    const [allRes, appliedRes, verifiedRes, canceledRes, violationRes, todayRes] = await Promise.all([
      api.get('/bookings', { params: { pageNo: 1, pageSize: 1 } }),
      api.get('/bookings', { params: { status: 'APPLIED', pageNo: 1, pageSize: 1 } }),
      api.get('/bookings', { params: { status: 'VERIFIED', pageNo: 1, pageSize: 1 } }),
      api.get('/bookings', { params: { status: 'CANCELED', pageNo: 1, pageSize: 1 } }),
      api.get('/bookings', { params: { status: 'VIOLATION', pageNo: 1, pageSize: 1 } }),
      api.get('/bookings', {
        params: {
          startDate: new Date().toISOString().slice(0, 10),
          endDate: new Date().toISOString().slice(0, 10),
          pageNo: 1,
          pageSize: 1
        }
      })
    ])

    const totalCount = allRes?.data?.total || 0
    const violationCount = violationRes?.data?.total || 0
    return {
      total: totalCount,
      applied: appliedRes?.data?.total || 0,
      verified: verifiedRes?.data?.total || 0,
      canceled: canceledRes?.data?.total || 0,
      violation: violationCount,
      today: todayRes?.data?.total || 0,
      violationRate: totalCount ? ((violationCount / totalCount) * 100).toFixed(1) : '0.0'
    }
  },
  staleTime: 30000
})

const stats = computed(() => {
  const rows = users.value
  const bookingStats = bookingStatsQuery.data || bookingStatsQuery.data?.value || {}
  return [
    { label: '当前页用户', value: rows.length },
    { label: '今日预约', value: bookingStats.today ?? 0 },
    { label: '违规率', value: `${bookingStats.violationRate ?? '0.0'}%` }
  ]
})

const bookingStatusStats = computed(() => {
  const data = bookingStatsQuery.data || bookingStatsQuery.data?.value || {}
  return [
    { label: '总预约', value: data.total ?? 0 },
    { label: '申请中', value: data.applied ?? 0 },
    { label: '已核销', value: data.verified ?? 0 },
    { label: '已取消', value: data.canceled ?? 0 },
    { label: '违规', value: data.violation ?? 0 }
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

async function toggleUserStatus(user) {
  try {
    const response = await api.put(`/users/${user.id}`, {
      ...user,
      status: user.status === 1 ? 0 : 1
    })
    if (response.code !== 200) {
      pushToast(response.message || '用户状态更新失败', 'error')
      return
    }
    pushToast(user.status === 1 ? '用户已禁用' : '用户已启用', 'success')
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
        <h2>查看并维护平台用户状态</h2>
        <p class="text-muted">支持关键词搜索、状态过滤、启用/禁用操作。</p>
      </div>
      <div class="hero-metrics">
        <div v-for="stat in stats" :key="stat.label">
          <span>{{ stat.label }}</span>
          <strong>{{ stat.value }}</strong>
        </div>
      </div>
    </section>

    <section class="card borrow-panel">
      <div class="borrow-panel__header">
        <div>
          <p class="section-kicker">全局预约统计看板</p>
          <h3>按状态计数 + 今日预约 + 违规率</h3>
        </div>
        <NButton tertiary :loading="bookingStatsQuery.isFetching" @click="bookingStatsQuery.refetch()">
          刷新统计
        </NButton>
      </div>
      <div class="profile-stat-grid">
        <div v-for="item in bookingStatusStats" :key="item.label" class="summary-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>
    </section>

    <section class="card borrow-filters">
      <div class="field">
        <label>关键词</label>
        <NInput v-model:value="filters.keyword" placeholder="用户名/姓名/邮箱/手机号" />
      </div>
      <div class="field">
        <label>状态</label>
        <NSelect v-model:value="filters.status" :options="statusOptions" />
      </div>
      <div class="borrow-filters__actions">
        <NButton type="primary" :loading="usersQuery.isFetching" @click="handleSearch">查询</NButton>
        <NButton tertiary @click="resetFilters">重置</NButton>
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
          <NButton
            size="small"
            :type="user.status === 1 ? 'error' : 'primary'"
            @click="toggleUserStatus(user)"
          >
            {{ user.status === 1 ? '禁用用户' : '启用用户' }}
          </NButton>
        </div>
      </NCard>

      <div v-if="!users.length && !usersQuery.isFetching" class="empty-state">
        <h3>暂无用户数据</h3>
        <p>请调整筛选条件后重试。</p>
      </div>
    </section>

    <section class="pagination">
      <NButton tertiary @click="prevPage" :disabled="pagination.pageNo <= 1">上一页</NButton>
      <span>第 {{ pagination.pageNo }} 页 / 共 {{ Math.ceil(total / pagination.pageSize) || 1 }} 页</span>
      <NButton tertiary @click="nextPage" :disabled="pagination.pageNo * pagination.pageSize >= total">下一页</NButton>
    </section>
  </div>
</template>
