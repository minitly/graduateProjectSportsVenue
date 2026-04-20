<script setup>
import { computed, reactive, ref } from 'vue'
import { useDialog, NButton, NCard, NInput, NInputNumber, NModal, NSelect, NTag } from 'naive-ui'
import { useQuery, useQueryClient } from '@tanstack/vue-query'
import api from '../../services/api'
import { useToast } from '../../composables/useToast'
import FloorPlanCanvasEditor from './FloorPlanCanvasEditor.vue'
import { createDefaultFloorPlanModel, parseFloorPlanContent, stringifyFloorPlanContent } from '../../utils/floorPlan'

const { pushToast } = useToast()
const queryClient = useQueryClient()
const dialog = useDialog()

const filters = reactive({
  keyword: '',
  status: ''
})

const pagination = reactive({
  pageNo: 1,
  pageSize: 10
})

const editorModal = reactive({
  show: false,
  saving: false,
  id: null,
  title: '',
  description: '',
  status: 'PUBLISHED',
  canvasModel: createDefaultFloorPlanModel()
})

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已下线', value: 'OFFLINE' }
]

const statusTagTypeMap = {
  DRAFT: 'warning',
  PUBLISHED: 'success',
  OFFLINE: 'default'
}

const floorPlansQuery = useQuery({
  queryKey: computed(() => ['floorPlans', 'admin', filters.keyword, filters.status, pagination.pageNo, pagination.pageSize]),
  queryFn: async () => {
    const response = await api.get('/floor-plans', {
      params: {
        keyword: filters.keyword || undefined,
        status: filters.status || undefined,
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
const summaryCards = computed(() => [
  { label: '场地图总数', value: total.value },
  { label: '当前状态筛选', value: filters.status ? statusOptions.find((item) => item.value === filters.status)?.label || filters.status : '全部状态' },
  { label: '每页条数', value: pagination.pageSize }
])

function formatTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(
    date.getHours()
  ).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function refreshList() {
  queryClient.invalidateQueries({ queryKey: ['floorPlans', 'admin'] })
}

function handleSearch() {
  pagination.pageNo = 1
  floorPlansQuery.refetch()
}

function handleReset() {
  filters.keyword = ''
  filters.status = ''
  pagination.pageNo = 1
  floorPlansQuery.refetch()
}

function openCreate() {
  editorModal.show = true
  editorModal.saving = false
  editorModal.id = null
  editorModal.title = ''
  editorModal.description = ''
  editorModal.status = 'PUBLISHED'
  editorModal.canvasModel = createDefaultFloorPlanModel()
}

async function openEdit(record) {
  try {
    const response = await api.get(`/floor-plans/${record.id}`)
    if (response.code !== 200) {
      pushToast(response.message || '加载场地图失败', 'error')
      return
    }
    const detail = response.data || {}
    editorModal.show = true
    editorModal.saving = false
    editorModal.id = detail.id || record.id
    editorModal.title = detail.title || ''
    editorModal.description = detail.description || ''
    editorModal.status = detail.status || 'DRAFT'
    editorModal.canvasModel = parseFloorPlanContent(detail.contentJson)
  } catch (error) {
    pushToast(error?.response?.data?.message || '加载场地图失败', 'error')
  }
}

function closeEditor() {
  if (editorModal.saving) return
  editorModal.show = false
}

function validateEditor() {
  if (!editorModal.title.trim()) {
    pushToast('请输入场地图标题', 'warning')
    return false
  }
  return true
}

async function submitEditor() {
  if (!validateEditor()) return
  editorModal.saving = true
  try {
    const payload = {
      title: editorModal.title.trim(),
      description: editorModal.description?.trim() || '',
      status: editorModal.status,
      contentJson: stringifyFloorPlanContent(editorModal.canvasModel)
    }
    let response
    if (editorModal.id) {
      response = await api.put(`/floor-plans/${editorModal.id}`, payload)
    } else {
      response = await api.post('/floor-plans', payload)
    }
    if (response.code !== 200) {
      pushToast(response.message || '保存失败', 'error')
      return
    }
    pushToast(editorModal.id ? '场地图更新成功' : '场地图创建成功', 'success')
    editorModal.show = false
    refreshList()
  } catch (error) {
    pushToast(error?.response?.data?.message || '保存失败，请稍后重试', 'error')
  } finally {
    editorModal.saving = false
  }
}

function removeFloorPlan(record) {
  dialog.warning({
    title: '删除场地图',
    content: `确定删除“${record.title || '未命名场地图'}”吗？删除后用户端将不可见。`,
    positiveText: '确认删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const response = await api.delete(`/floor-plans/${record.id}`)
        if (response.code !== 200) {
          pushToast(response.message || '删除失败', 'error')
          return
        }
        pushToast('场地图删除成功', 'success')
        refreshList()
      } catch (error) {
        pushToast(error?.response?.data?.message || '删除失败，请稍后重试', 'error')
      }
    }
  })
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
    <NCard class="floor-plan-panel module-tier module-tier--summary">
      <div class="hero-head">
        <div>
          <p class="section-kicker">场地图管理</p>
          <h3>维护场地图模板并控制发布状态</h3>
          <p class="text-muted">先查看概览，再按条件查询并进入编辑。</p>
        </div>
      </div>
      <div class="hero-metrics">
        <div v-for="stat in summaryCards" :key="stat.label">
          <span>{{ stat.label }}</span>
          <strong>{{ stat.value }}</strong>
        </div>
      </div>
    </NCard>

    <NCard class="floor-plan-panel module-tier module-tier--filters">
      <div class="filter-row">
        <NInput v-model:value="filters.keyword" class="keyword-input" clearable placeholder="按名称模糊查询" @keyup.enter="handleSearch" />
        <NSelect v-model:value="filters.status" clearable :options="statusOptions" placeholder="状态筛选" />
        <div class="filter-row__actions">
          <NButton type="primary" @click="handleSearch">查询</NButton>
          <NButton @click="handleReset">重置</NButton>
          <NButton type="success" @click="openCreate">新增场地图</NButton>
        </div>
      </div>
    </NCard>

    <NCard class="floor-plan-panel module-tier module-tier--data">
      <div class="meta-row">
        <span>共 {{ total }} 条</span>
        <span v-if="isFetching">加载中...</span>
      </div>

      <div v-if="records.length" class="card-grid">
        <NCard v-for="item in records" :key="item.id" size="small" class="floor-plan-card">
          <template #header>
            <div class="card-title">
              <strong>{{ item.title || '未命名场地图' }}</strong>
              <NTag size="small" :type="statusTagTypeMap[item.status] || 'default'">{{ item.status || 'UNKNOWN' }}</NTag>
            </div>
          </template>
          <div class="card-body">
            <p class="card-desc">{{ item.description || '暂无说明' }}</p>
            <p class="card-meta">更新时间：{{ formatTime(item.updateTime) }}</p>
          </div>
          <template #action>
            <div class="card-actions">
              <NButton size="small" @click="openEdit(item)">编辑</NButton>
              <NButton size="small" type="error" ghost @click="removeFloorPlan(item)">删除</NButton>
            </div>
          </template>
        </NCard>
      </div>
      <div v-else class="empty-panel">暂无场地图数据</div>

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
      :show="editorModal.show"
      preset="card"
      title="场地图编辑"
      class="floor-plan-modal"
      :style="{ width: 'min(1400px, 98vw)' }"
      @close="closeEditor"
    >
      <div class="editor-form">
        <div class="editor-form__top">
          <NInput v-model:value="editorModal.title" placeholder="请输入场地图标题" />
          <NSelect
            v-model:value="editorModal.status"
            :options="statusOptions.filter((item) => item.value)"
            placeholder="请选择状态"
          />
        </div>
        <NInput v-model:value="editorModal.description" type="textarea" :rows="2" placeholder="场地图说明（可选）" />
        <FloorPlanCanvasEditor v-model="editorModal.canvasModel" />
      </div>
      <template #action>
        <div class="modal-actions">
          <NButton @click="closeEditor">取消</NButton>
          <NButton type="primary" :loading="editorModal.saving" @click="submitEditor">保存</NButton>
        </div>
      </template>
    </NModal>
  </div>
</template>

<style scoped>
.floor-plan-page {
  display: grid;
  gap: 16px;
}

.floor-plan-panel {
  border-radius: 22px;
}

.hero-head h3 {
  margin: 6px 0 4px;
  font-size: 24px;
}

.hero-metrics {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, minmax(160px, 1fr));
  gap: 12px;
}

.hero-metrics > div {
  min-height: 92px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 14px;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.82);
  display: grid;
  gap: 4px;
}

.hero-metrics span {
  font-size: 12px;
  color: #64748b;
}

.hero-metrics strong {
  font-size: 24px;
  color: #0f172a;
}

.filter-row {
  display: grid;
  grid-template-columns: minmax(260px, 420px) 180px auto;
  gap: 12px;
  align-items: center;
}

.keyword-input {
  width: 100%;
}

.filter-row__actions {
  display: flex;
  gap: 8px;
  justify-content: flex-start;
  flex-wrap: wrap;
}

.meta-row {
  margin-top: 14px;
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #64748b;
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
  gap: 8px;
}

.empty-panel {
  margin-top: 16px;
  padding: 36px 20px;
  text-align: center;
  border: 1px dashed #c9d6e8;
  border-radius: 12px;
  color: #64748b;
}

.editor-form {
  display: grid;
  gap: 12px;
}

.editor-form__top {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 12px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
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

  .hero-metrics {
    grid-template-columns: 1fr;
  }

  .filter-row__actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .editor-form__top {
    grid-template-columns: 1fr;
  }

  :deep(.floor-plan-modal) {
    width: min(720px, 92vw) !important;
  }
}
</style>
