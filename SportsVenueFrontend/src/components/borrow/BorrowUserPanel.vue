<script setup>
import { computed, nextTick, reactive, ref, unref, watch } from 'vue'
import { useQuery, useQueryClient } from '@tanstack/vue-query'
import { NButton, NCard, NDatePicker, NInput, NInputNumber, NModal, NSelect, NTag } from 'naive-ui'
import api from '../../services/api'
import { useToast } from '../../composables/useToast'
import { getStatusText } from '../../constants/statusMap'

const props = defineProps({
  /** browse：可借器材列表；my：我的借用记录 */
  section: {
    type: String,
    default: 'browse',
    validator: (v) => ['browse', 'my'].includes(v)
  }
})

const { pushToast } = useToast()
const queryClient = useQueryClient()

const itemFilters = reactive({ keyword: '', type: '', onlyAvailable: false })
const itemPagination = reactive({ pageNo: 1, pageSize: 6 })
const borrowFilters = reactive({ status: '', keyword: '', range: null, startTime: null, endTime: null })
const borrowPagination = reactive({ pageNo: 1, pageSize: 5 })
const itemNameMap = reactive({})
const expandedRecordIds = ref([])
const debouncedSearch = ref(null)
/** 重置筛选时跳过 watch，避免与手动 refetch 重复请求 */
const suppressItemFiltersWatch = ref(false)

const borrowModal = reactive({ show: false, item: null, quantity: 1, remark: '', submitting: false })

const statusOptions = [
  { label: '全部', value: '' },
  { label: '申请中', value: 'REQUESTED' },
  { label: '使用中', value: 'USING' },
  { label: '已归还', value: 'RETURNED' }
]

const itemTypeOptions = [
  { label: '全部类型', value: '' },
  { label: '球类', value: '球类' },
  { label: '球拍', value: '球拍' },
  { label: '护具', value: '护具' }
]

const itemsQuery = useQuery({
  queryKey: computed(() => ['items', itemFilters.keyword, itemFilters.type, itemFilters.onlyAvailable, itemPagination.pageNo, itemPagination.pageSize]),
  enabled: computed(() => props.section === 'browse'),
  queryFn: async () => {
    const response = await api.get('/items', {
      params: {
        keyword: itemFilters.keyword || undefined,
        type: itemFilters.type || undefined,
        onlyAvailable: itemFilters.onlyAvailable,
        pageNo: itemPagination.pageNo,
        pageSize: itemPagination.pageSize
      }
    })
    if (response.code !== 200) throw new Error(response.message || '器材加载失败')
    return response.data || { records: [], total: 0 }
  },
  keepPreviousData: true,
  staleTime: 30000
})

const borrowsQuery = useQuery({
  queryKey: computed(() => ['myBorrows', borrowFilters.status, borrowFilters.keyword, borrowFilters.startTime, borrowFilters.endTime, borrowPagination.pageNo, borrowPagination.pageSize]),
  enabled: computed(() => props.section === 'my'),
  queryFn: async () => {
    const response = await api.get('/borrows/my', {
      params: {
        status: borrowFilters.status || undefined,
        startTime: borrowFilters.startTime || undefined,
        endTime: borrowFilters.endTime || undefined,
        pageNo: borrowPagination.pageNo,
        pageSize: borrowPagination.pageSize
      }
    })
    if (response.code !== 200) throw new Error(response.message || '借用记录加载失败')
    const data = response.data || { records: [], total: 0 }
    await hydrateItemNames(data.records || [])
    const keyword = borrowFilters.keyword.trim().toLowerCase()
    if (!keyword) return data
    const filtered = (data.records || []).filter((record) =>
      (itemNameMap[record.itemId] || `器材 ${record.itemId}`).toLowerCase().includes(keyword)
    )
    return { ...data, records: filtered, total: filtered.length }
  },
  keepPreviousData: true,
  staleTime: 30000
})

const itemsData = computed(() => itemsQuery.data?.records || itemsQuery.data?.value?.records || [])
const itemsTotal = computed(() => itemsQuery.data?.total || itemsQuery.data?.value?.total || 0)
const borrowsData = computed(() => borrowsQuery.data?.records || borrowsQuery.data?.value?.records || [])
const borrowsTotal = computed(() => borrowsQuery.data?.total || borrowsQuery.data?.value?.total || 0)

const itemStats = computed(() => [
  { label: '可借器材', value: itemsTotal.value },
  { label: '当前筛选', value: itemFilters.type || '全部' }
])

const borrowStats = computed(() => [
  { label: '我的借用', value: borrowsTotal.value },
  { label: '当前状态', value: borrowFilters.status ? getStatusText(borrowFilters.status) : '全部' }
])

/** useQuery 的 isFetching 为 Ref，直接传给 NButton :loading 会恒为真，需解包 */
const isItemsFetching = computed(() => unref(itemsQuery.isFetching))
const isBorrowsFetching = computed(() => unref(borrowsQuery.isFetching))

watch(() => [itemFilters.keyword, itemFilters.type, itemFilters.onlyAvailable], () => {
  if (suppressItemFiltersWatch.value) return
  itemPagination.pageNo = 1
  if (debouncedSearch.value) clearTimeout(debouncedSearch.value)
  debouncedSearch.value = setTimeout(() => itemsQuery.refetch(), 400)
})

watch(
  () => itemPagination.pageSize,
  (value, oldValue) => {
    if (value === oldValue) return
    if (!Number.isFinite(value) || value <= 0) {
      itemPagination.pageSize = oldValue || 6
      return
    }
    itemPagination.pageSize = Math.min(50, Math.max(1, Math.floor(value)))
    itemPagination.pageNo = 1
  }
)

watch(
  () => borrowPagination.pageSize,
  (value, oldValue) => {
    if (value === oldValue) return
    if (!Number.isFinite(value) || value <= 0) {
      borrowPagination.pageSize = oldValue || 5
      return
    }
    borrowPagination.pageSize = Math.min(50, Math.max(1, Math.floor(value)))
    borrowPagination.pageNo = 1
  }
)

async function hydrateItemNames(records) {
  const ids = [...new Set(records.map((record) => record.itemId).filter(Boolean))].filter((id) => !itemNameMap[id])
  if (!ids.length) return
  await Promise.all(ids.map(async (id) => {
    try {
      const response = await api.get(`/items/${id}`)
      if (response.code === 200 && response.data) itemNameMap[id] = response.data.name || `器材 ${id}`
    } catch {
      itemNameMap[id] = `器材 ${id}`
    }
  }))
}

function handleRangeChange(value) {
  if (!value || value.length !== 2) {
    borrowFilters.startTime = null
    borrowFilters.endTime = null
    return
  }
  const [start, end] = value
  borrowFilters.startTime = new Date(start).toISOString().slice(0, 19)
  borrowFilters.endTime = new Date(end).toISOString().slice(0, 19)
}

function formatDateTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function toggleRecordExpand(id) {
  if (expandedRecordIds.value.includes(id)) expandedRecordIds.value = expandedRecordIds.value.filter((item) => item !== id)
  else expandedRecordIds.value = [...expandedRecordIds.value, id]
}

function queryBorrows() {
  queryClient.invalidateQueries({ queryKey: ['myBorrows'] })
}

function resetItemFilters() {
  suppressItemFiltersWatch.value = true
  if (debouncedSearch.value) clearTimeout(debouncedSearch.value)
  debouncedSearch.value = null
  itemFilters.keyword = ''
  itemFilters.type = ''
  itemFilters.onlyAvailable = false
  itemPagination.pageNo = 1
  itemsQuery.refetch()
  nextTick(() => {
    suppressItemFiltersWatch.value = false
  })
}

function resetBorrowFilters() {
  borrowFilters.status = ''
  borrowFilters.keyword = ''
  borrowFilters.range = null
  borrowFilters.startTime = null
  borrowFilters.endTime = null
  borrowPagination.pageNo = 1
  borrowsQuery.refetch()
}

function openBorrowModal(item) {
  borrowModal.show = true
  borrowModal.item = item
  borrowModal.quantity = 1
  borrowModal.remark = ''
}

function closeBorrowModal() {
  borrowModal.show = false
}

async function submitBorrow() {
  if (!borrowModal.item) return
  if (!borrowModal.quantity || borrowModal.quantity <= 0) {
    pushToast('请输入正确的借用数量', 'warning')
    return
  }
  borrowModal.submitting = true
  try {
    const response = await api.post('/borrows', {
      itemId: borrowModal.item.id,
      quantity: borrowModal.quantity,
      remark: borrowModal.remark
    })
    if (response.code !== 200) {
      pushToast(response.message || '借用申请失败', 'error')
      return
    }
    pushToast('借用申请已提交', 'success')
    closeBorrowModal()
    queryClient.invalidateQueries({ queryKey: ['myBorrows'] })
    queryClient.invalidateQueries({ queryKey: ['items'] })
  } catch (error) {
    const backendMessage = error?.response?.data?.message
    pushToast(backendMessage || '无法连接后端服务', 'error')
  } finally {
    borrowModal.submitting = false
  }
}
</script>

<template>
  <template v-if="section === 'browse'">
  <section class="card borrow-hero">
    <div>
      <p class="section-kicker">器材借用</p>
      <h2>快速查看器材库存，提交借用申请</h2>
      <p class="text-muted">借用流程：申请 → 管理员确认借出 → 确认归还。</p>
    </div>
    <div class="hero-metrics">
      <div v-for="stat in itemStats" :key="stat.label">
        <span>{{ stat.label }}</span>
        <strong>{{ stat.value }}</strong>
      </div>
    </div>
  </section>

  <section class="card borrow-filters">
    <div class="field"><label>关键词</label><NInput v-model:value="itemFilters.keyword" placeholder="器材名称 / 型号" /></div>
    <div class="field"><label>类型</label><NSelect v-model:value="itemFilters.type" :options="itemTypeOptions" /></div>
    <div class="field">
      <label>只看可借</label>
      <NSelect v-model:value="itemFilters.onlyAvailable" :options="[{ label: '全部', value: false }, { label: '仅可借', value: true }]" />
    </div>
    <div class="borrow-filters__actions">
      <NButton type="primary" :loading="isItemsFetching" @click="itemsQuery.refetch()">查询</NButton>
      <NButton tertiary @click="resetItemFilters">重置</NButton>
    </div>
  </section>

  <section class="borrow-grid">
    <article v-for="item in itemsData" :key="item.id" class="borrow-card">
      <div class="borrow-card__header">
        <div class="borrow-card__title-wrap">
          <h3>{{ item.name }}</h3>
          <p class="text-muted">{{ item.type || '未分类' }} · {{ item.model || '无型号' }}</p>
        </div>
        <NTag type="info">押金 ¥{{ item.depositAmount || 0 }}</NTag>
      </div>
      <div class="borrow-card__meta">
        <div><span>库存</span><strong>{{ item.totalQuantity }}</strong></div>
        <div><span>可借</span><strong>{{ item.availableQuantity }}</strong></div>
        <div><span>损坏</span><strong>{{ item.damagedQuantity || 0 }}</strong></div>
      </div>
      <p class="borrow-card__desc">{{ item.description || '暂无描述' }}</p>
      <div class="borrow-card__actions">
        <NButton type="primary" :disabled="item.availableQuantity === 0" @click="openBorrowModal(item)">申请借用</NButton>
      </div>
    </article>

    <div v-if="!itemsData.length && !isItemsFetching" class="empty-state">
      <h3>暂无器材</h3><p>尝试调整筛选条件或稍后再来。</p>
    </div>
  </section>

  <section class="pagination">
    <NButton tertiary @click="itemPagination.pageNo = Math.max(1, itemPagination.pageNo - 1); itemsQuery.refetch()" :disabled="itemPagination.pageNo <= 1">上一页</NButton>
    <span>第 {{ itemPagination.pageNo }} 页 / 共 {{ Math.ceil(itemsTotal / itemPagination.pageSize) || 1 }} 页</span>
    <span style="display: inline-flex; align-items: center; gap: 8px;">
      <span>每页</span>
      <NInputNumber
        v-model:value="itemPagination.pageSize"
        :min="1"
        :max="50"
        :step="1"
        style="width: 100px;"
      />
      <span>条</span>
    </span>
    <NButton tertiary @click="itemPagination.pageNo += 1; itemsQuery.refetch()" :disabled="itemPagination.pageNo * itemPagination.pageSize >= itemsTotal">下一页</NButton>
  </section>
  </template>

  <section v-if="section === 'my'" class="card borrow-panel borrow-panel--standalone">
    <div class="borrow-panel__header">
      <div><p class="section-kicker">我的借用</p><h3>借用记录与状态追踪</h3></div>
      <div class="borrow-panel__summary"><div v-for="stat in borrowStats" :key="stat.label" class="summary-card"><span>{{ stat.label }}</span><strong>{{ stat.value }}</strong></div></div>
    </div>

    <div class="borrow-panel__filters">
      <div><label>状态</label><NSelect v-model:value="borrowFilters.status" :options="statusOptions" /></div>
      <div><label>器材名称</label><NInput v-model:value="borrowFilters.keyword" placeholder="按器材名称筛选" /></div>
      <div><label>申请时间范围</label><NDatePicker v-model:value="borrowFilters.range" type="datetimerange" clearable @update:value="handleRangeChange" /></div>
      <div class="borrow-panel__actions">
        <NButton type="primary" :loading="isBorrowsFetching" @click="queryBorrows">查询</NButton>
        <NButton tertiary @click="resetBorrowFilters">重置</NButton>
      </div>
    </div>

    <div class="borrow-panel__list borrow-my-records">
      <NCard v-for="record in borrowsData" :key="record.id" size="small" class="borrow-record borrow-record--user-my" :bordered="true">
        <template #header>
          <div class="borrow-record__header">
            <div><strong>借用单 #{{ record.id }}</strong><p class="text-muted">{{ itemNameMap[record.itemId] || `器材 ${record.itemId}` }}</p></div>
            <NTag :type="record.status === 'REQUESTED' ? 'info' : record.status === 'USING' ? 'warning' : 'success'">{{ getStatusText(record.status) }}</NTag>
          </div>
        </template>
        <div class="borrow-record__body borrow-record__body--compact">
          <div><span>数量</span><strong>{{ record.quantity }}</strong></div>
          <div><span>申请时间</span><strong>{{ formatDateTime(record.requestedTime || record.createTime) }}</strong></div>
          <div><span>备注</span><strong>{{ record.remark || '—' }}</strong></div>
        </div>
        <div class="borrow-record__actions">
          <NButton size="small" tertiary type="primary" @click="toggleRecordExpand(record.id)">
            {{ expandedRecordIds.includes(record.id) ? '收起详情' : '展开详情' }}
          </NButton>
        </div>
        <div v-if="expandedRecordIds.includes(record.id)" class="borrow-record__details">
          <div><span>借出确认时间</span><strong>{{ formatDateTime(record.approvedTime) || '—' }}</strong></div>
          <div><span>归还确认时间</span><strong>{{ formatDateTime(record.returnedTime) || '—' }}</strong></div>
          <div><span>借出时器材状态</span><strong>{{ getStatusText(record.conditionOnBorrow, '—') }}</strong></div>
          <div><span>归还时器材状态</span><strong>{{ getStatusText(record.conditionOnReturn, '—') }}</strong></div>
        </div>
      </NCard>
      <div v-if="!borrowsData.length && !isBorrowsFetching" class="empty-state">
        <h3>暂无借用记录</h3>
        <p>申请借用后会展示在这里。</p>
      </div>
    </div>

    <section class="pagination">
      <NButton
        tertiary
        @click="borrowPagination.pageNo = Math.max(1, borrowPagination.pageNo - 1); borrowsQuery.refetch()"
        :disabled="borrowPagination.pageNo <= 1"
      >
        上一页
      </NButton>
      <span>第 {{ borrowPagination.pageNo }} 页 / 共 {{ Math.ceil(borrowsTotal / borrowPagination.pageSize) || 1 }} 页</span>
      <span style="display: inline-flex; align-items: center; gap: 8px;">
        <span>每页</span>
        <NInputNumber
          v-model:value="borrowPagination.pageSize"
          :min="1"
          :max="50"
          :step="1"
          style="width: 100px;"
        />
        <span>条</span>
      </span>
      <NButton
        tertiary
        @click="borrowPagination.pageNo += 1; borrowsQuery.refetch()"
        :disabled="borrowPagination.pageNo * borrowPagination.pageSize >= borrowsTotal"
      >
        下一页
      </NButton>
    </section>
  </section>

  <NModal v-if="section === 'browse'" v-model:show="borrowModal.show" preset="card" class="booking-modal" title="提交借用申请">
    <div class="booking-modal__header">
      <div><h3>{{ borrowModal.item?.name }}</h3><p class="text-muted">可借数量：{{ borrowModal.item?.availableQuantity }}</p></div>
      <NTag type="info">{{ borrowModal.item?.type || '未分类' }}</NTag>
    </div>
    <div class="booking-modal__section"><label>借用数量</label><NInputNumber v-model:value="borrowModal.quantity" :min="1" :max="borrowModal.item?.availableQuantity || 1" /></div>
    <div class="booking-modal__section"><label>用途说明</label><NInput v-model:value="borrowModal.remark" type="textarea" placeholder="例如：篮球训练" /></div>
    <div class="booking-modal__actions"><NButton @click="closeBorrowModal">取消</NButton><NButton type="primary" :loading="borrowModal.submitting" @click="submitBorrow">提交申请</NButton></div>
  </NModal>
</template>

<style scoped>
/* 列表内卡片按内容高度，避免 Grid 同行拉伸 / n-card-content flex:1 在底部留白 */
.borrow-my-records {
  align-items: start;
}

.borrow-my-records :deep(.borrow-record--user-my.n-card) {
  --n-padding-top: 10px;
  --n-padding-bottom: 10px;
  --n-padding-left: 14px;
  width: 100%;
  max-width: 100%;
  height: fit-content;
  align-self: start;
}

/* Naive Card 默认 .n-card-content { flex: 1 } 会撑满卡片剩余高度，产生「展开详情」下方的空白带 */
.borrow-my-records :deep(.borrow-record--user-my .n-card-content) {
  flex: 0 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  /* 与主题左右留白一致，略补上下使主信息与操作区节奏均匀 */
  padding: 4px var(--n-padding-left) 4px var(--n-padding-left);
}

.borrow-my-records :deep(.borrow-record--user-my .n-card-header) {
  padding-top: 10px;
  padding-bottom: 10px;
}

.borrow-my-records :deep(.borrow-record__body--compact) {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px 12px;
  align-items: start;
  justify-items: stretch;
  font-size: 13px;
}

.borrow-my-records :deep(.borrow-record__body--compact > div) {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 0;
}

.borrow-my-records :deep(.borrow-record__body--compact span) {
  font-size: 11px;
  line-height: 1.2;
}

.borrow-my-records :deep(.borrow-record__body--compact strong) {
  font-size: 13px;
  line-height: 1.25;
}

.borrow-my-records :deep(.borrow-record__header .text-muted) {
  margin-top: 2px;
  margin-bottom: 0;
  font-size: 12px;
  line-height: 1.25;
}

.borrow-my-records :deep(.borrow-record__actions) {
  margin-top: 0;
  padding-top: 2px;
}

.borrow-my-records :deep(.borrow-record__details) {
  margin-top: 0;
}

@media (max-width: 520px) {
  .borrow-my-records :deep(.borrow-record__body--compact) {
    grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  }
}
</style>
