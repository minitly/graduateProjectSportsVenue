<script setup>
import { computed, reactive, ref } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { NButton, NCard, NInput, NInputNumber, NModal, NTag } from 'naive-ui'
import api from '../../services/api'
import { useToast } from '../../composables/useToast'
import FloorPlanCanvasPreview from './FloorPlanCanvasPreview.vue'
import { parseFloorPlanContent } from '../../utils/floorPlan'

const { pushToast } = useToast()

const filters = reactive({
  keyword: ''
})

const pagination = reactive({
  pageNo: 1,
  pageSize: 10
})

const detailModal = reactive({
  show: false,
  loading: false,
  record: null,
  model: null
})

const floorPlansQuery = useQuery({
  queryKey: computed(() => ['floorPlans', 'user', filters.keyword, pagination.pageNo, pagination.pageSize]),
  queryFn: async () => {
    const response = await api.get('/floor-plans', {
      params: {
        keyword: filters.keyword || undefined,
        status: 'PUBLISHED',
        pageNo: pagination.pageNo,
        pageSize: pagination.pageSize
      }
    })
    if (response.code !== 200) throw new Error(response.message || '场地图加载失败')
    return response.data || { records: [], total: 0 }
  },
  keepPreviousData: true,
  staleTime: 30000,
  refetchOnWindowFocus: false
})

const records = computed(() => floorPlansQuery.data?.records || floorPlansQuery.data?.value?.records || [])
const total = computed(() => floorPlansQuery.data?.total || floorPlansQuery.data?.value?.total || 0)
const isFetching = computed(() => Boolean(floorPlansQuery.isFetching?.value ?? floorPlansQuery.isFetching))
const totalPages = computed(() => Math.ceil(total.value / pagination.pageSize) || 1)
const userStats = computed(() => [
  { label: '可查看场地图', value: total.value },
  { label: '当前状态', value: '已发布' },
  { label: '关键词筛选', value: filters.keyword?.trim() || '全部' }
])

function formatTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(
    date.getHours()
  ).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function handleSearch() {
  pagination.pageNo = 1
  floorPlansQuery.refetch()
}

function handleReset() {
  filters.keyword = ''
  pagination.pageNo = 1
  floorPlansQuery.refetch()
}

async function openDetail(item) {
  detailModal.show = true
  detailModal.loading = true
  detailModal.record = null
  detailModal.model = null
  try {
    const response = await api.get(`/floor-plans/${item.id}`)
    if (response.code !== 200) {
      pushToast(response.message || '加载详情失败', 'error')
      return
    }
    const detail = response.data || {}
    if (detail.status !== 'PUBLISHED') {
      pushToast('该场地图暂未发布', 'warning')
      return
    }
    detailModal.record = detail
    detailModal.model = parseFloorPlanContent(detail.contentJson)
  } catch (error) {
    pushToast(error?.response?.data?.message || '加载详情失败', 'error')
  } finally {
    detailModal.loading = false
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
</script>

<template>
  <div class="floor-plan-page">
    <section class="card venues-hero">
      <div>
        <p class="section-kicker">场地图展示</p>
        <h2>查看场馆空间分区与布局信息</h2>
        <p class="text-muted">仅展示已发布的场地图，点击卡片可查看具体画布内容。</p>
      </div>
      <div class="hero-metrics">
        <div v-for="stat in userStats" :key="stat.label">
          <span>{{ stat.label }}</span>
          <strong>{{ stat.value }}</strong>
        </div>
      </div>
    </section>

    <NCard title="场地图展示" class="floor-plan-panel">
      <div class="filter-row">
        <div class="field">
          <label>场地图名称</label>
          <NInput v-model:value="filters.keyword" class="keyword-input" clearable placeholder="按名称模糊查询场地图" @keyup.enter="handleSearch" />
        </div>
        <div class="filter-row__actions">
          <NButton type="primary" @click="handleSearch">查询</NButton>
          <NButton @click="handleReset">重置</NButton>
        </div>
      </div>

      <div v-if="records.length" class="card-grid">
        <NCard
          v-for="item in records"
          :key="item.id"
          size="small"
          class="floor-plan-card floor-plan-card--clickable"
          @click="openDetail(item)"
        >
          <template #header>
            <div class="card-title">
              <strong>{{ item.title || '未命名场地图' }}</strong>
              <NTag type="success" size="small">PUBLISHED</NTag>
            </div>
          </template>
          <div class="card-body">
            <p class="card-desc">{{ item.description || '暂无说明' }}</p>
            <p class="card-meta">更新时间：{{ formatTime(item.updateTime) }}</p>
          </div>
          <template #action>
            <div class="card-actions">
              <NButton size="small" @click.stop="openDetail(item)">查看详情</NButton>
            </div>
          </template>
        </NCard>
      </div>
      <div v-else class="empty-panel">暂无已发布场地图</div>

      <section class="pagination">
        <NButton tertiary :disabled="pagination.pageNo <= 1" @click="prevPage">上一页</NButton>
        <span>第 {{ pagination.pageNo }} 页 / 共 {{ totalPages }} 页（共 {{ total }} 条）</span>
        <label class="page-size-control">
          <span>每页</span>
          <NInputNumber
            v-model:value="pagination.pageSize"
            :min="1"
            :max="50"
            :precision="0"
            :show-button="false"
            size="small"
            style="width: 88px"
          />
          <span>条</span>
        </label>
        <NButton tertiary :disabled="pagination.pageNo * pagination.pageSize >= total" @click="nextPage">下一页</NButton>
      </section>
    </NCard>

    <NModal
      :show="detailModal.show"
      preset="card"
      title="场地图详情"
      class="floor-plan-modal"
      :style="{ width: 'min(1300px, 92vw)' }"
      @close="detailModal.show = false"
    >
      <div v-if="detailModal.loading" class="loading-panel">加载中...</div>
      <div v-else-if="detailModal.record && detailModal.model" class="detail-layout">
        <div class="detail-meta">
          <h3>{{ detailModal.record.title }}</h3>
          <p>{{ detailModal.record.description || '暂无说明' }}</p>
          <small>发布时间：{{ formatTime(detailModal.record.updateTime) }}</small>
        </div>
        <FloorPlanCanvasPreview :model="detailModal.model" :max-height="560" />
      </div>
      <div v-else class="empty-panel">暂无可展示内容</div>
    </NModal>
  </div>
</template>

<style scoped>
.floor-plan-page {
  display: grid;
  gap: 16px;
}

.floor-plan-panel {
  border-radius: 18px;
}

.filter-row {
  display: grid;
  grid-template-columns: minmax(260px, 420px) auto;
  gap: 12px;
  align-items: end;
}

.keyword-input {
  width: 100%;
}

.filter-row__actions {
  display: flex;
  justify-content: flex-start;
  gap: 8px;
  flex-wrap: wrap;
}

.card-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
}

.floor-plan-card {
  border-radius: 14px;
}

.floor-plan-card--clickable {
  cursor: pointer;
}

.floor-plan-card--clickable:hover {
  box-shadow: 0 12px 28px rgba(59, 130, 246, 0.14);
}

.card-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.card-title strong {
  font-size: 14px;
}

.card-body {
  display: grid;
  gap: 8px;
}

.card-desc {
  margin: 0;
  color: #475569;
  min-height: 38px;
}

.card-meta {
  margin: 0;
  font-size: 12px;
  color: #94a3b8;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
}

.empty-panel {
  margin-top: 16px;
  padding: 36px 20px;
  text-align: center;
  border: 1px dashed #c9d6e8;
  border-radius: 12px;
  color: #64748b;
}

.loading-panel {
  padding: 26px;
  text-align: center;
  color: #64748b;
}

.detail-layout {
  display: grid;
  gap: 14px;
}

.detail-meta h3 {
  margin: 0;
  font-size: 18px;
}

.detail-meta p {
  margin: 6px 0;
  color: #64748b;
}

.detail-meta small {
  color: #94a3b8;
}

.page-size-control {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 1024px) {
  .filter-row {
    grid-template-columns: 1fr;
  }

  .filter-row__actions {
    justify-content: flex-start;
  }

  :deep(.floor-plan-modal) {
    width: min(700px, 92vw) !important;
  }
}
</style>
