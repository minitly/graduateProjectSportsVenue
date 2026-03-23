<script setup>
import { computed, reactive } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { NButton, NCard, NDatePicker, NSelect, NTag } from 'naive-ui'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import api from '../services/api'

use([CanvasRenderer, BarChart, LineChart, GridComponent, LegendComponent, TooltipComponent])

const filters = reactive({
  range: null,
  startDate: null,
  endDate: null,
  bookingStatus: '',
  borrowStatus: ''
})

const bookingStatusOptions = [
  { label: '全部预约状态', value: '' },
  { label: '申请中', value: 'APPLIED' },
  { label: '已核销', value: 'VERIFIED' },
  { label: '已取消', value: 'CANCELED' },
  { label: '违规', value: 'VIOLATION' }
]

const borrowStatusOptions = [
  { label: '全部借用状态', value: '' },
  { label: '申请中', value: 'REQUESTED' },
  { label: '使用中', value: 'USING' },
  { label: '已归还', value: 'RETURNED' }
]

function formatDate(date) {
  return date.toISOString().slice(0, 10)
}

const defaultRange = (() => {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - 6)
  return [start.getTime(), end.getTime()]
})()

if (!filters.range) {
  filters.range = defaultRange
  filters.startDate = formatDate(new Date(defaultRange[0]))
  filters.endDate = formatDate(new Date(defaultRange[1]))
}

function handleRangeChange(value) {
  if (!value || value.length !== 2) {
    filters.startDate = null
    filters.endDate = null
    return
  }
  filters.startDate = formatDate(new Date(value[0]))
  filters.endDate = formatDate(new Date(value[1]))
}

function percentage(part, total) {
  if (!total) return '0.0'
  return ((part / total) * 100).toFixed(1)
}

const bookingStatsQuery = useQuery({
  queryKey: computed(() => ['analyticsBookings', filters.startDate, filters.endDate, filters.bookingStatus]),
  queryFn: async () => {
    const [totalRes, appliedRes, verifiedRes, canceledRes, violationRes] = await Promise.all([
      api.get('/bookings', {
        params: {
          startDate: filters.startDate || undefined,
          endDate: filters.endDate || undefined,
          status: filters.bookingStatus || undefined,
          pageNo: 1,
          pageSize: 1
        }
      }),
      api.get('/bookings', { params: { startDate: filters.startDate || undefined, endDate: filters.endDate || undefined, status: 'APPLIED', pageNo: 1, pageSize: 1 } }),
      api.get('/bookings', { params: { startDate: filters.startDate || undefined, endDate: filters.endDate || undefined, status: 'VERIFIED', pageNo: 1, pageSize: 1 } }),
      api.get('/bookings', { params: { startDate: filters.startDate || undefined, endDate: filters.endDate || undefined, status: 'CANCELED', pageNo: 1, pageSize: 1 } }),
      api.get('/bookings', { params: { startDate: filters.startDate || undefined, endDate: filters.endDate || undefined, status: 'VIOLATION', pageNo: 1, pageSize: 1 } })
    ])

    const total = totalRes?.data?.total || 0
    const applied = appliedRes?.data?.total || 0
    const verified = verifiedRes?.data?.total || 0
    const canceled = canceledRes?.data?.total || 0
    const violation = violationRes?.data?.total || 0

    const end = new Date(filters.endDate || formatDate(new Date()))
    const daily = []
    for (let i = 6; i >= 0; i -= 1) {
      const current = new Date(end)
      current.setDate(end.getDate() - i)
      const day = formatDate(current)
      const [allDay, verifiedDay, violationDay] = await Promise.all([
        api.get('/bookings', { params: { startDate: day, endDate: day, pageNo: 1, pageSize: 1 } }),
        api.get('/bookings', { params: { startDate: day, endDate: day, status: 'VERIFIED', pageNo: 1, pageSize: 1 } }),
        api.get('/bookings', { params: { startDate: day, endDate: day, status: 'VIOLATION', pageNo: 1, pageSize: 1 } })
      ])
      daily.push({
        date: day,
        total: allDay?.data?.total || 0,
        verified: verifiedDay?.data?.total || 0,
        violation: violationDay?.data?.total || 0
      })
    }

    return {
      total,
      applied,
      verified,
      canceled,
      violation,
      violationRate: percentage(violation, total),
      verifyRate: percentage(verified, total),
      daily
    }
  },
  staleTime: 30000,
  keepPreviousData: true
})

const borrowStatsQuery = useQuery({
  queryKey: computed(() => ['analyticsBorrows', filters.borrowStatus]),
  queryFn: async () => {
    const [totalRes, requestedRes, usingRes, returnedRes] = await Promise.all([
      api.get('/borrows', { params: { status: filters.borrowStatus || undefined, pageNo: 1, pageSize: 1 } }),
      api.get('/borrows', { params: { status: 'REQUESTED', pageNo: 1, pageSize: 1 } }),
      api.get('/borrows', { params: { status: 'USING', pageNo: 1, pageSize: 1 } }),
      api.get('/borrows', { params: { status: 'RETURNED', pageNo: 1, pageSize: 1 } })
    ])

    const total = totalRes?.data?.total || 0
    const requested = requestedRes?.data?.total || 0
    const using = usingRes?.data?.total || 0
    const returned = returnedRes?.data?.total || 0

    return {
      total,
      requested,
      using,
      returned,
      turnoverRate: percentage(returned, total)
    }
  },
  staleTime: 30000,
  keepPreviousData: true
})

const userRiskQuery = useQuery({
  queryKey: ['analyticsUserRisk'],
  queryFn: async () => {
    const response = await api.get('/users', { params: { pageNo: 1, pageSize: 100 } })
    const rows = response?.data?.records || []
    return rows
      .filter((user) => (user.violationCountMonth || 0) > 0)
      .sort((a, b) => (b.violationCountMonth || 0) - (a.violationCountMonth || 0))
      .slice(0, 8)
  },
  staleTime: 60000
})

const bookingStats = computed(() => bookingStatsQuery.data || bookingStatsQuery.data?.value || {})
const borrowStats = computed(() => borrowStatsQuery.data || borrowStatsQuery.data?.value || {})
const riskUsers = computed(() => userRiskQuery.data || userRiskQuery.data?.value || [])

const isAnalyticsFetching = computed(
  () =>
    Boolean(bookingStatsQuery.isFetching?.value ?? bookingStatsQuery.isFetching) ||
    Boolean(borrowStatsQuery.isFetching?.value ?? borrowStatsQuery.isFetching) ||
    Boolean(userRiskQuery.isFetching?.value ?? userRiskQuery.isFetching)
)

const kpis = computed(() => [
  { label: '预约总量', value: bookingStats.value.total || 0, tip: '选定日期范围内' },
  { label: '违规率', value: `${bookingStats.value.violationRate || '0.0'}%`, tip: 'VIOLATION / 总预约' },
  { label: '核销率', value: `${bookingStats.value.verifyRate || '0.0'}%`, tip: 'VERIFIED / 总预约' },
  { label: '借用周转率', value: `${borrowStats.value.turnoverRate || '0.0'}%`, tip: 'RETURNED / 总借用' }
])

const compareCards = computed(() => {
  const daily = bookingStats.value.daily || []
  const today = daily[daily.length - 1]?.total || 0
  const yesterday = daily[daily.length - 2]?.total || 0
  const weekAvg = daily.length ? daily.reduce((sum, d) => sum + (d.total || 0), 0) / daily.length : 0
  const dayDelta = yesterday === 0 ? 0 : ((today - yesterday) / yesterday) * 100
  const weekDelta = weekAvg === 0 ? 0 : ((today - weekAvg) / weekAvg) * 100
  return [
    {
      label: '较昨日',
      value: `${dayDelta >= 0 ? '+' : ''}${dayDelta.toFixed(1)}%`,
      positive: dayDelta >= 0
    },
    {
      label: '较近7日均值',
      value: `${weekDelta >= 0 ? '+' : ''}${weekDelta.toFixed(1)}%`,
      positive: weekDelta >= 0
    }
  ]
})

const trendChartOption = computed(() => {
  const rows = bookingStats.value.daily || []
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['总预约', '已核销', '违规'] },
    grid: { left: 24, right: 20, top: 36, bottom: 24, containLabel: true },
    xAxis: { type: 'category', data: rows.map((r) => r.date.slice(5)) },
    yAxis: { type: 'value' },
    series: [
      { name: '总预约', type: 'line', smooth: true, data: rows.map((r) => r.total) },
      { name: '已核销', type: 'line', smooth: true, data: rows.map((r) => r.verified) },
      { name: '违规', type: 'line', smooth: true, data: rows.map((r) => r.violation) }
    ]
  }
})

const bookingStackChartOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  legend: { data: ['申请中', '已核销', '已取消', '违规'] },
  grid: { left: 24, right: 20, top: 36, bottom: 24, containLabel: true },
  xAxis: { type: 'category', data: ['预约状态分布'] },
  yAxis: { type: 'value' },
  series: [
    { name: '申请中', type: 'bar', stack: 'total', data: [bookingStats.value.applied || 0] },
    { name: '已核销', type: 'bar', stack: 'total', data: [bookingStats.value.verified || 0] },
    { name: '已取消', type: 'bar', stack: 'total', data: [bookingStats.value.canceled || 0] },
    { name: '违规', type: 'bar', stack: 'total', data: [bookingStats.value.violation || 0] }
  ]
}))

const bookingBars = computed(() => {
  const total = bookingStats.value.total || 0
  return [
    { label: '申请中', value: bookingStats.value.applied || 0 },
    { label: '已核销', value: bookingStats.value.verified || 0 },
    { label: '已取消', value: bookingStats.value.canceled || 0 },
    { label: '违规', value: bookingStats.value.violation || 0 }
  ].map((item) => ({
    ...item,
    pct: Number(percentage(item.value, total))
  }))
})

function exportCsv() {
  const daily = bookingStats.value.daily || []
  const header = ['date', 'bookings_total', 'bookings_verified', 'bookings_violation']
  const rows = daily.map((row) => [row.date, row.total, row.verified, row.violation])
  const csv = [header, ...rows].map((line) => line.join(',')).join('\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `analytics_${filters.startDate || 'start'}_${filters.endDate || 'end'}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

function refreshAll() {
  bookingStatsQuery.refetch()
  borrowStatsQuery.refetch()
  userRiskQuery.refetch()
}
</script>

<template>
  <div class="admin-analytics-page">
    <section class="card profile-hero">
      <div>
        <p class="section-kicker">数据分析管理</p>
        <h2>全局预约与运营分析看板</h2>
        <p class="text-muted">聚合预约、借用、用户风险数据，支持高频运营决策。</p>
      </div>
      <div class="hero-metrics">
        <div v-for="kpi in kpis" :key="kpi.label">
          <span>{{ kpi.label }}</span>
          <strong>{{ kpi.value }}</strong>
          <small>{{ kpi.tip }}</small>
        </div>
      </div>
    </section>

    <section class="card analytics-compare">
      <div class="compare-header">
        <div>
          <h3>预约变化趋势</h3>
          <p class="text-muted">与昨日、近 7 日均值对比</p>
        </div>
        <div class="compare-tags">
          <NTag v-for="card in compareCards" :key="card.label" :type="card.positive ? 'success' : 'error'">
            {{ card.label }} {{ card.value }}
          </NTag>
        </div>
      </div>
      <VChart class="chart" :option="trendChartOption" autoresize />
    </section>

    <section class="card borrow-filters">
      <div class="field">
        <label>预约日期范围</label>
        <NDatePicker v-model:value="filters.range" type="daterange" clearable @update:value="handleRangeChange" />
      </div>
      <div class="field">
        <label>预约主筛选</label>
        <NSelect v-model:value="filters.bookingStatus" :options="bookingStatusOptions" />
      </div>
      <div class="field">
        <label>借用主筛选</label>
        <NSelect v-model:value="filters.borrowStatus" :options="borrowStatusOptions" />
      </div>
      <div class="borrow-filters__actions">
        <NButton type="primary" :loading="isAnalyticsFetching" @click="refreshAll">
          刷新分析
        </NButton>
        <NButton tertiary @click="exportCsv">导出 CSV</NButton>
      </div>
    </section>

    <section class="analytics-grid">
      <NCard title="预约状态分布（堆叠柱）" class="analytics-card">
        <VChart class="chart" :option="bookingStackChartOption" autoresize />
      </NCard>

      <NCard title="借用状态总览" class="analytics-card">
        <div class="mini-stats">
          <div>
            <span>总借用</span>
            <strong>{{ borrowStats.total || 0 }}</strong>
          </div>
          <div>
            <span>申请中</span>
            <strong>{{ borrowStats.requested || 0 }}</strong>
          </div>
          <div>
            <span>使用中</span>
            <strong>{{ borrowStats.using || 0 }}</strong>
          </div>
          <div>
            <span>已归还</span>
            <strong>{{ borrowStats.returned || 0 }}</strong>
          </div>
        </div>
      </NCard>
    </section>

    <section class="analytics-grid">
      <NCard title="近7日预约趋势" class="analytics-card">
        <div class="trend-table">
          <div class="trend-row trend-head">
            <span>日期</span>
            <span>总预约</span>
            <span>已核销</span>
            <span>违规</span>
          </div>
          <div v-for="row in bookingStats.daily || []" :key="row.date" class="trend-row">
            <span>{{ row.date }}</span>
            <strong>{{ row.total }}</strong>
            <span>{{ row.verified }}</span>
            <span>{{ row.violation }}</span>
          </div>
        </div>
      </NCard>

      <NCard title="高风险用户（本月违规）" class="analytics-card">
        <div v-if="riskUsers.length" class="risk-list">
          <div v-for="user in riskUsers" :key="user.id" class="risk-item">
            <div>
              <strong>{{ user.realName || user.username }}</strong>
              <p class="text-muted">@{{ user.username }} · {{ user.role }}</p>
            </div>
            <NTag type="error">违规 {{ user.violationCountMonth || 0 }} 次</NTag>
          </div>
        </div>
        <div v-else class="empty-state small-empty">
          <h3>暂无风险用户</h3>
          <p>当前样本中未发现本月违规用户。</p>
        </div>
      </NCard>
    </section>
  </div>
</template>
