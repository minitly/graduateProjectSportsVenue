<script setup>
import { computed, reactive, ref, unref } from 'vue'
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
  endDate: null
})

const trendSort = reactive({
  key: 'date',
  order: 'desc'
})

const venueTopN = ref(8)
const venueTopNOptions = [
  { label: 'Top 5', value: 5 },
  { label: 'Top 8', value: 8 },
  { label: 'Top 10', value: 10 },
  { label: 'Top 20', value: 20 }
]

function formatDate(date) {
  return date.toISOString().slice(0, 10)
}

function toYYYYMMDD(value) {
  if (!value) return ''
  if (typeof value === 'string') return value.slice(0, 10)
  return formatDate(new Date(value))
}

function toNumber(value) {
  return Number(value || 0)
}

function percentage(part, total) {
  if (!total) return '0.0'
  return ((part / total) * 100).toFixed(1)
}

function rateNumber(part, total) {
  if (!total) return 0
  return Number(((part / total) * 100).toFixed(1))
}

function getRiskTag(rate) {
  if (rate >= 20) return { type: 'error', label: '异常偏高' }
  if (rate >= 10) return { type: 'warning', label: '重点关注' }
  return { type: 'success', label: '正常' }
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

function toggleTrendSort(key) {
  if (trendSort.key === key) {
    trendSort.order = trendSort.order === 'asc' ? 'desc' : 'asc'
    return
  }
  trendSort.key = key
  trendSort.order = key === 'date' ? 'desc' : 'asc'
}

const bookingStatsQuery = useQuery({
  queryKey: computed(() => ['analyticsReportsBooking', filters.startDate, filters.endDate]),
  enabled: computed(() => Boolean(filters.startDate && filters.endDate)),
  queryFn: async () => {
    const [dashboardRes, trendRes] = await Promise.all([
      api.get('/reports/dashboard', {
        params: {
          startDate: filters.startDate,
          endDate: filters.endDate
        }
      }),
      api.get('/reports/bookings/trend', {
        params: {
          startDate: filters.startDate,
          endDate: filters.endDate
        }
      })
    ])

    const dashboard = dashboardRes?.data || {}
    const trendRows = Array.isArray(trendRes?.data) ? trendRes.data : []

    const total = toNumber(dashboard.bookingTotal)
    const verified = toNumber(dashboard.bookingVerifiedTotal)
    const canceled = toNumber(dashboard.bookingCanceledTotal)
    const violation = toNumber(dashboard.bookingViolationTotal)
    const applied = Math.max(total - verified - canceled - violation, 0)

    const daily = trendRows.map((row) => {
      const total = toNumber(row.bookingTotal)
      const verified = toNumber(row.verifiedTotal)
      const canceled = toNumber(row.canceledTotal)
      const violation = toNumber(row.violationTotal)
      const applied = Math.max(total - verified - canceled - violation, 0)
      return {
        date: toYYYYMMDD(row.date),
        total,
        applied,
        verified,
        canceled,
        violation,
        violationRate: rateNumber(row.violationTotal, row.bookingTotal)
      }
    })

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

const venueRankQuery = useQuery({
  queryKey: computed(() => ['analyticsReportsVenueRank', filters.startDate, filters.endDate, venueTopN.value]),
  enabled: computed(() => Boolean(filters.startDate && filters.endDate)),
  queryFn: async () => {
    const response = await api.get('/reports/bookings/venue-rank', {
      params: {
        startDate: filters.startDate,
        endDate: filters.endDate,
        topN: venueTopN.value
      }
    })

    const rows = Array.isArray(response?.data) ? response.data : []
    return rows.map((row, index) => {
      const bookingTotal = toNumber(row.bookingTotal)
      const verifiedTotal = toNumber(row.verifiedTotal)
      const violationTotal = toNumber(row.violationTotal)
      const violationRate = rateNumber(violationTotal, bookingTotal)

      return {
        rank: index + 1,
        venueId: row.venueId,
        venueName: row.venueName,
        bookingTotal,
        verifiedTotal,
        violationTotal,
        verifyRate: rateNumber(verifiedTotal, bookingTotal),
        violationRate,
        riskTag: getRiskTag(violationRate)
      }
    })
  },
  staleTime: 30000,
  keepPreviousData: true
})

const borrowStatsQuery = useQuery({
  queryKey: ['analyticsBorrowsSnapshot'],
  queryFn: async () => {
    const response = await api.get('/borrows', { params: { pageNo: 1, pageSize: 1000 } })
    const rows = response?.data?.records || []

    const all = {
      total: rows.length,
      requested: rows.filter((item) => item.status === 'REQUESTED').length,
      using: rows.filter((item) => item.status === 'USING').length,
      returned: rows.filter((item) => item.status === 'RETURNED').length
    }

    return {
      ...all,
      turnoverRate: percentage(all.returned, all.total)
    }
  },
  staleTime: 30000,
  keepPreviousData: true
})

const bookingStats = computed(() => unref(bookingStatsQuery.data) || {})

/** 借用快照：始终按全部状态统计（界面不再提供状态筛选） */
const borrowStats = computed(() => unref(borrowStatsQuery.data) || {})

const venueRankList = computed(() => unref(venueRankQuery.data) || [])

const sortedTrendRows = computed(() => {
  const rows = [...(bookingStats.value.daily || [])]
  const key = trendSort.key
  const factor = trendSort.order === 'asc' ? 1 : -1

  return rows.sort((a, b) => {
    if (key === 'date') {
      return a.date > b.date ? factor : -factor
    }
    return (toNumber(a[key]) - toNumber(b[key])) * factor
  })
})

const trendSummaryRow = computed(() => {
  const rows = bookingStats.value.daily || []
  const total = rows.reduce((sum, row) => sum + (row.total || 0), 0)
  const applied = rows.reduce((sum, row) => sum + (row.applied || 0), 0)
  const verified = rows.reduce((sum, row) => sum + (row.verified || 0), 0)
  const canceled = rows.reduce((sum, row) => sum + (row.canceled || 0), 0)
  const violation = rows.reduce((sum, row) => sum + (row.violation || 0), 0)
  const avgTotal = rows.length ? (total / rows.length).toFixed(1) : '0.0'

  return {
    total,
    applied,
    verified,
    canceled,
    violation,
    avgTotal,
    verifyRate: percentage(verified, total),
    violationRate: percentage(violation, total)
  }
})

const isAnalyticsFetching = computed(
  () =>
    Boolean(bookingStatsQuery.isFetching?.value ?? bookingStatsQuery.isFetching) ||
    Boolean(venueRankQuery.isFetching?.value ?? venueRankQuery.isFetching) ||
    Boolean(borrowStatsQuery.isFetching?.value ?? borrowStatsQuery.isFetching)
)

const reportsErrorMessage = computed(() => {
  const bookingErr = bookingStatsQuery.error?.value ?? bookingStatsQuery.error
  const trendErr = venueRankQuery.error?.value ?? venueRankQuery.error
  const responseData = bookingErr?.response?.data || trendErr?.response?.data
  if (!responseData) return ''
  return responseData.message || '报表数据加载失败'
})

const kpis = computed(() => [
  { label: '预约总量', value: bookingStats.value.total || 0, tip: '选定日期范围内' },
  { label: '违规率', value: `${bookingStats.value.violationRate || '0.0'}%`, tip: '违规预约数 / 总预约' },
  { label: '核销率', value: `${bookingStats.value.verifyRate || '0.0'}%`, tip: '已核销数 / 总预约' },
  { label: '借用周转率', value: `${borrowStats.value.turnoverRate || '0.0'}%`, tip: '已归还数 / 总借用' }
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
    legend: {
      data: ['总预约', '已核销', '违规'],
      bottom: 0,
      left: 'center',
      padding: [12, 0, 0, 0]
    },
    grid: { left: 24, right: 20, top: 36, bottom: 56, containLabel: true },
    xAxis: { type: 'category', data: rows.map((r) => r.date.slice(5)) },
    yAxis: { type: 'value' },
    series: [
      { name: '总预约', type: 'line', smooth: true, data: rows.map((r) => r.total) },
      { name: '已核销', type: 'line', smooth: true, data: rows.map((r) => r.verified) },
      { name: '违规', type: 'line', smooth: true, data: rows.map((r) => r.violation) }
    ]
  }
})

/** 各状态数量对比：分类柱状图（非堆叠） */
const bookingStatusBarChartOption = computed(() => {
  const applied = bookingStats.value.applied || 0
  const verified = bookingStats.value.verified || 0
  const canceled = bookingStats.value.canceled || 0
  const violation = bookingStats.value.violation || 0
  const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666']
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: { left: 24, right: 20, top: 28, bottom: 32, containLabel: true },
    xAxis: {
      type: 'category',
      data: ['申请中', '已核销', '已取消', '违规'],
      axisLabel: { interval: 0 }
    },
    yAxis: { type: 'value' },
    series: [
      {
        type: 'bar',
        name: '预约量',
        data: [applied, verified, canceled, violation],
        barMaxWidth: 48,
        itemStyle: {
          color: (params) => colors[params.dataIndex] ?? colors[0]
        }
      }
    ]
  }
})

function sortArrow(key) {
  if (trendSort.key !== key) return '↕'
  return trendSort.order === 'asc' ? '↑' : '↓'
}

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
  venueRankQuery.refetch()
  borrowStatsQuery.refetch()
}
</script>

<template>
  <div class="admin-analytics-page">
    <section class="card profile-hero">
      <div>
        <p class="section-kicker">数据分析管理</p>
        <h2>全局预约与运营分析看板</h2>
        <p class="text-muted">聚合预约、借用等运营数据，支持高频运营决策。</p>
      </div>
      <div class="hero-metrics">
        <div v-for="kpi in kpis" :key="kpi.label">
          <span>{{ kpi.label }}</span>
          <strong>{{ kpi.value }}</strong>
          <small>{{ kpi.tip }}</small>
        </div>
      </div>
    </section>

    <section v-if="reportsErrorMessage" class="card error-banner">
      <strong>报表接口不可用：</strong>
      <span>{{ reportsErrorMessage }}</span>
    </section>

    <section class="card borrow-filters">
      <div class="field">
        <label>预约日期范围</label>
        <NDatePicker v-model:value="filters.range" type="daterange" clearable @update:value="handleRangeChange" />
      </div>
      <div class="field">
        <label>场馆排行</label>
        <NSelect v-model:value="venueTopN" :options="venueTopNOptions" />
      </div>
      <div class="borrow-filters__actions">
        <NButton type="primary" :loading="isAnalyticsFetching" @click="refreshAll">
          刷新分析
        </NButton>
        <NButton tertiary @click="exportCsv">导出 CSV</NButton>
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

    <section class="analytics-grid">
      <NCard title="预约状态分布" class="analytics-card">
        <VChart class="chart" :option="bookingStatusBarChartOption" autoresize />
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

    <section class="analytics-grid advanced-grid">
      <NCard title="高级趋势表（可排序）" class="analytics-card">
        <p class="trend-table-hint text-muted">
          总预约为当日开场时段的条数（与报表一致，按预约开始时间）；各状态互斥，满足
          <strong>总预约 = 申请中 + 已核销 + 已取消 + 违规</strong>。
        </p>
        <div class="table-wrap">
          <table class="pro-table">
            <thead>
              <tr>
                <th @click="toggleTrendSort('date')">日期 {{ sortArrow('date') }}</th>
                <th @click="toggleTrendSort('total')">总预约 {{ sortArrow('total') }}</th>
                <th @click="toggleTrendSort('applied')">申请中 {{ sortArrow('applied') }}</th>
                <th @click="toggleTrendSort('verified')">已核销 {{ sortArrow('verified') }}</th>
                <th @click="toggleTrendSort('canceled')">已取消 {{ sortArrow('canceled') }}</th>
                <th @click="toggleTrendSort('violation')">违规 {{ sortArrow('violation') }}</th>
                <th @click="toggleTrendSort('violationRate')">违规率 {{ sortArrow('violationRate') }}</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="row in sortedTrendRows"
                :key="row.date"
                :class="{ 'row-alert': Number(row.violationRate) >= 20 }"
              >
                <td>{{ row.date }}</td>
                <td>{{ row.total }}</td>
                <td>{{ row.applied ?? 0 }}</td>
                <td>{{ row.verified }}</td>
                <td>{{ row.canceled }}</td>
                <td>{{ row.violation }}</td>
                <td>{{ row.violationRate }}%</td>
                <td>
                  <NTag :type="getRiskTag(row.violationRate).type">{{ getRiskTag(row.violationRate).label }}</NTag>
                </td>
              </tr>
            </tbody>
            <tfoot>
              <tr class="summary-row">
                <td>统计行</td>
                <td>总计 {{ trendSummaryRow.total }} / 日均 {{ trendSummaryRow.avgTotal }}</td>
                <td>{{ trendSummaryRow.applied }}</td>
                <td>{{ trendSummaryRow.verified }}</td>
                <td>{{ trendSummaryRow.canceled }}</td>
                <td>{{ trendSummaryRow.violation }}</td>
                <td>{{ trendSummaryRow.violationRate }}%</td>
                <td>核销率 {{ trendSummaryRow.verifyRate }}%</td>
              </tr>
            </tfoot>
          </table>
        </div>
      </NCard>

      <NCard title="TopN 场馆预约排行" class="analytics-card analytics-card--venue-rank">
        <div class="table-wrap table-wrap--venue-rank">
          <table class="pro-table">
            <thead>
              <tr>
                <th>排名</th>
                <th>场馆</th>
                <th>预约总量</th>
                <th>核销率</th>
                <th>违规率</th>
                <th>风险标记</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="venue in venueRankList"
                :key="venue.venueId"
                :class="{ 'row-alert': venue.violationRate >= 20 }"
              >
                <td>
                  <span class="rank-pill" :class="`rank-${venue.rank}`">#{{ venue.rank }}</span>
                </td>
                <td>{{ venue.venueName }}</td>
                <td>{{ venue.bookingTotal }}</td>
                <td>{{ venue.verifyRate }}%</td>
                <td>{{ venue.violationRate }}%</td>
                <td>
                  <NTag :type="venue.riskTag.type">{{ venue.riskTag.label }}</NTag>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </NCard>
    </section>
  </div>
</template>

<style scoped>
.admin-analytics-page {
  display: grid;
  gap: 16px;
  color: #1f2937;
  background: #f5f7fb;
}

.card {
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.06);
  padding: 18px;
}

.profile-hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.section-kicker {
  color: #6b7280;
  letter-spacing: 0.08em;
  margin: 0 0 8px;
  font-size: 12px;
}

.text-muted {
  color: #6b7280;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(130px, 1fr));
  gap: 12px;
  min-width: 320px;
}

.hero-metrics > div {
  border: 1px solid #eef2f7;
  background: #f8fafc;
  border-radius: 10px;
  padding: 12px;
  display: grid;
  gap: 4px;
}

.hero-metrics strong {
  font-size: 20px;
  color: #111827;
}

.analytics-compare .compare-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.compare-tags {
  display: flex;
  gap: 8px;
}

.chart {
  height: 280px;
}

.borrow-filters {
  display: grid;
  grid-template-columns: minmax(240px, 1.4fr) minmax(140px, 0.6fr) auto;
  gap: 12px;
  align-items: end;
}

.field {
  display: grid;
  gap: 8px;
}

.field label {
  font-size: 12px;
  color: #6b7280;
}

.borrow-filters__actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.analytics-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.advanced-grid {
  align-items: start;
}

.trend-table-hint {
  margin: 0 0 12px;
  font-size: 12px;
  line-height: 1.5;
}

.analytics-card {
  min-height: 220px;
}

.mini-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.mini-stats > div {
  border: 1px solid #eef2f7;
  background: #f8fafc;
  border-radius: 10px;
  padding: 12px;
  display: grid;
  gap: 5px;
}

.mini-stats strong {
  font-size: 20px;
}

.table-wrap {
  overflow: auto;
}

/* TopN 场馆排行：限制高度，超出部分纵向滚动（表头 sticky 仍生效） */
.table-wrap--venue-rank {
  max-height: min(320px, 45vh);
  overflow-x: auto;
  overflow-y: auto;
  scrollbar-gutter: stable;
  border-radius: 8px;
  border: 1px solid #eef2f7;
}

.pro-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 13px;
  color: #111827;
}

.pro-table thead th {
  position: sticky;
  top: 0;
  background: #f3f4f6;
  color: #374151;
  text-align: left;
  font-weight: 600;
  border-bottom: 1px solid #e5e7eb;
  padding: 10px;
  cursor: pointer;
  user-select: none;
}

.pro-table tbody td,
.pro-table tfoot td {
  padding: 10px;
  border-bottom: 1px solid #eef2f7;
}

.pro-table tbody tr:hover {
  background: #f9fafb;
}

.row-alert {
  background: #fff1f2;
}

.summary-row {
  background: #f3f4f6;
  color: #111827;
  font-weight: 600;
}

.rank-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 40px;
  height: 22px;
  border-radius: 11px;
  background: #e5e7eb;
  color: #111827;
  font-weight: 700;
}

.rank-1,
.rank-2,
.rank-3 {
  background: #fde68a;
  color: #7c2d12;
}

.error-banner {
  border-color: #fecaca;
  background: #fef2f2;
  color: #991b1b;
  display: flex;
  gap: 8px;
  align-items: center;
}

@media (max-width: 1200px) {
  .analytics-grid,
  .borrow-filters,
  .profile-hero {
    grid-template-columns: 1fr;
    display: grid;
  }

  .hero-metrics {
    min-width: 100%;
  }
}
</style>
