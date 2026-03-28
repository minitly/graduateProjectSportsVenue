<script setup>
import { computed, reactive, ref } from 'vue'
import { useQuery, useQueryClient } from '@tanstack/vue-query'
import { NButton, NCard, NDatePicker, NInput, NInputNumber, NModal, NSelect, NTag } from 'naive-ui'
import api from '../../services/api'
import { useToast } from '../../composables/useToast'
import { getStatusText } from '../../constants/statusMap'

const { pushToast } = useToast()
const queryClient = useQueryClient()

const ownerFilters = reactive({ status: '', keyword: '', userId: null, range: null, startTime: null, endTime: null })
const ownerPagination = reactive({ pageNo: 1, pageSize: 6 })
const ownerActionModal = reactive({ show: false, type: 'approve', record: null, condition: 'GOOD', remark: '', submitting: false })
const itemNameMap = reactive({})
const expandedRecordIds = ref([])

const ownerStatusOptions = [
  { label: '全部', value: '' },
  { label: '申请中', value: 'REQUESTED' },
  { label: '使用中', value: 'USING' },
  { label: '已归还', value: 'RETURNED' }
]

const pageSizeOptions = [
  { label: '6条/页', value: 6 },
  { label: '10条/页', value: 10 },
  { label: '20条/页', value: 20 }
]

const conditionOptions = [
  { label: '完好', value: 'GOOD' },
  { label: '损坏', value: 'DAMAGED' },
  { label: '丢失', value: 'LOST' }
]

const ownerBorrowsQuery = useQuery({
  queryKey: computed(() => ['ownerBorrows', ownerFilters.status, ownerFilters.startTime, ownerFilters.endTime, ownerFilters.userId, ownerFilters.keyword, ownerPagination.pageNo, ownerPagination.pageSize]),
  queryFn: async () => {
    const keyword = ownerFilters.keyword?.trim()?.toLowerCase()

    // 有器材关键词时先拉取大列表再前端分页，避免分页页内数据不满的问题
    if (keyword) {
      const response = await api.get('/borrows', {
        params: {
          status: ownerFilters.status || undefined,
          userId: ownerFilters.userId || undefined,
          startTime: ownerFilters.startTime || undefined,
          endTime: ownerFilters.endTime || undefined,
          pageNo: 1,
          pageSize: 2000
        }
      })
      if (response.code !== 200) throw new Error(response.message || '管理借用记录加载失败')
      const allData = response.data || { records: [], total: 0 }
      await hydrateItemNames(allData.records || [])
      const filtered = (allData.records || []).filter((record) => {
        const itemName = (itemNameMap[record.itemId] || `器材 ${record.itemId}`).toLowerCase()
        return itemName.includes(keyword)
      })
      const startIndex = (ownerPagination.pageNo - 1) * ownerPagination.pageSize
      const endIndex = startIndex + ownerPagination.pageSize
      return {
        ...allData,
        records: filtered.slice(startIndex, endIndex),
        total: filtered.length
      }
    }

    const response = await api.get('/borrows', {
      params: {
        status: ownerFilters.status || undefined,
        userId: ownerFilters.userId || undefined,
        startTime: ownerFilters.startTime || undefined,
        endTime: ownerFilters.endTime || undefined,
        pageNo: ownerPagination.pageNo,
        pageSize: ownerPagination.pageSize
      }
    })
    if (response.code !== 200) throw new Error(response.message || '管理借用记录加载失败')
    const data = response.data || { records: [], total: 0 }
    await hydrateItemNames(data.records || [])
    return data
  },
  keepPreviousData: true,
  staleTime: 30000
})

const pendingCountQuery = useQuery({
  queryKey: ['ownerStatBorrows', 'REQUESTED'],
  queryFn: async () => {
    const response = await api.get('/borrows', { params: { status: 'REQUESTED', pageNo: 1, pageSize: 1 } })
    if (response.code !== 200) return 0
    return response.data?.total || 0
  },
  staleTime: 30000
})

const usingCountQuery = useQuery({
  queryKey: ['ownerStatBorrows', 'USING'],
  queryFn: async () => {
    const response = await api.get('/borrows', { params: { status: 'USING', pageNo: 1, pageSize: 1 } })
    if (response.code !== 200) return 0
    return response.data?.total || 0
  },
  staleTime: 30000
})

const todayReturnedCountQuery = useQuery({
  queryKey: ['ownerStatBorrows', 'RETURNED', 'today'],
  queryFn: async () => {
    const now = new Date()
    const start = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    const end = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59)
    const response = await api.get('/borrows', {
      params: {
        status: 'RETURNED',
        startTime: start.toISOString().slice(0, 19),
        endTime: end.toISOString().slice(0, 19),
        pageNo: 1,
        pageSize: 1
      }
    })
    if (response.code !== 200) return 0
    return response.data?.total || 0
  },
  staleTime: 30000
})

const ownerBorrowsData = computed(() => ownerBorrowsQuery.data?.records || ownerBorrowsQuery.data?.value?.records || [])
const ownerBorrowsTotal = computed(() => ownerBorrowsQuery.data?.total || ownerBorrowsQuery.data?.value?.total || 0)
const ownerTotalPages = computed(() => Math.max(1, Math.ceil(ownerBorrowsTotal.value / ownerPagination.pageSize)))
const isOwnerFetching = computed(() => Boolean(ownerBorrowsQuery.isFetching?.value ?? ownerBorrowsQuery.isFetching))

const summaryCards = computed(() => [
  { label: '待审批', value: pendingCountQuery.data?.value ?? pendingCountQuery.data ?? 0 },
  { label: '使用中', value: usingCountQuery.data?.value ?? usingCountQuery.data ?? 0 },
  { label: '今日归还', value: todayReturnedCountQuery.data?.value ?? todayReturnedCountQuery.data ?? 0 }
])

const actionTitle = computed(() => (ownerActionModal.type === 'approve' ? '确认借出' : '确认归还'))
const actionButtonText = computed(() => (ownerActionModal.type === 'approve' ? '确认借出' : '确认归还'))

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

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function handleOwnerRangeChange(value) {
  if (!value || value.length !== 2) {
    ownerFilters.startTime = null
    ownerFilters.endTime = null
    return
  }
  const [start, end] = value
  ownerFilters.startTime = new Date(start).toISOString().slice(0, 19)
  ownerFilters.endTime = new Date(end).toISOString().slice(0, 19)
}

function applyFilters() {
  ownerPagination.pageNo = 1
  ownerBorrowsQuery.refetch()
}

function resetFilters() {
  ownerFilters.status = ''
  ownerFilters.keyword = ''
  ownerFilters.userId = null
  ownerFilters.range = null
  ownerFilters.startTime = null
  ownerFilters.endTime = null
  ownerPagination.pageNo = 1
  ownerBorrowsQuery.refetch()
}

function toggleRecordExpand(id) {
  if (expandedRecordIds.value.includes(id)) {
    expandedRecordIds.value = expandedRecordIds.value.filter((item) => item !== id)
  } else {
    expandedRecordIds.value = [...expandedRecordIds.value, id]
  }
}

function openOwnerAction(type, record) {
  ownerActionModal.show = true
  ownerActionModal.type = type
  ownerActionModal.record = record
  ownerActionModal.condition = 'GOOD'
  ownerActionModal.remark = ''
}

function closeOwnerAction() {
  ownerActionModal.show = false
}

async function submitOwnerAction() {
  if (!ownerActionModal.record) return
  ownerActionModal.submitting = true
  try {
    const recordId = ownerActionModal.record.id
    const endpoint = ownerActionModal.type === 'approve' ? `/borrows/${recordId}/approve` : `/borrows/${recordId}/return`
    const response = await api.put(endpoint, {
      remark: ownerActionModal.remark,
      conditionOnBorrow: ownerActionModal.type === 'approve' ? ownerActionModal.condition : undefined,
      conditionOnReturn: ownerActionModal.type === 'return' ? ownerActionModal.condition : undefined
    })
    if (response.code !== 200) {
      pushToast(response.message || '提交失败', 'error')
      return
    }
    pushToast(ownerActionModal.type === 'approve' ? '已确认借出' : '已确认归还', 'success')
    closeOwnerAction()
    await ownerBorrowsQuery.refetch()
    pendingCountQuery.refetch()
    usingCountQuery.refetch()
    todayReturnedCountQuery.refetch()
    queryClient.invalidateQueries({ queryKey: ['myBorrows'] })
    queryClient.invalidateQueries({ queryKey: ['items'] })
  } catch (error) {
    const backendMessage = error?.response?.data?.message
    pushToast(backendMessage || '操作失败，请稍后再试', 'error')
  } finally {
    ownerActionModal.submitting = false
  }
}
</script>

<template>
  <section class="card borrow-panel">
    <div class="borrow-panel__header">
      <div><p class="section-kicker">借用管理</p><h3>审批借用与归还确认</h3></div>
    </div>

    <div class="borrow-panel__summary">
      <div v-for="stat in summaryCards" :key="stat.label" class="summary-card"><span>{{ stat.label }}</span><strong>{{ stat.value }}</strong></div>
    </div>

    <div class="borrow-panel__filters">
      <div><label>状态</label><NSelect v-model:value="ownerFilters.status" :options="ownerStatusOptions" /></div>
      <div><label>器材关键词</label><NInput v-model:value="ownerFilters.keyword" placeholder="器材名称" /></div>
      <div><label>用户ID</label><NInputNumber v-model:value="ownerFilters.userId" :min="1" clearable placeholder="请输入用户ID" /></div>
      <div><label>申请时间范围</label><NDatePicker v-model:value="ownerFilters.range" type="datetimerange" clearable @update:value="handleOwnerRangeChange" /></div>
      <div class="borrow-panel__actions">
        <NButton type="primary" :loading="isOwnerFetching" @click="applyFilters">查询</NButton>
        <NButton @click="resetFilters">重置</NButton>
      </div>
    </div>

    <div class="borrow-panel__list">
      <NCard v-for="record in ownerBorrowsData" :key="record.id" size="small" class="borrow-record">
        <template #header>
          <div class="borrow-record__header">
            <div><strong>借用单 #{{ record.id }}</strong><p class="text-muted">{{ itemNameMap[record.itemId] || `器材 ${record.itemId}` }}</p></div>
            <NTag :type="record.status === 'REQUESTED' ? 'info' : record.status === 'USING' ? 'warning' : 'success'">{{ getStatusText(record.status) }}</NTag>
          </div>
        </template>
        <div class="borrow-record__body">
          <div><span>用户ID</span><strong>{{ record.userId }}</strong></div>
          <div><span>数量</span><strong>{{ record.quantity }}</strong></div>
          <div><span>申请时间</span><strong>{{ formatDateTime(record.requestedTime || record.createTime) }}</strong></div>
        </div>
        <div class="borrow-record__actions">
          <NButton text type="primary" @click="toggleRecordExpand(record.id)">{{ expandedRecordIds.includes(record.id) ? '收起详情' : '展开详情' }}</NButton>
          <NButton v-if="record.status === 'REQUESTED'" size="small" type="primary" @click="openOwnerAction('approve', record)">确认借出</NButton>
          <NButton v-if="record.status === 'USING'" size="small" type="warning" @click="openOwnerAction('return', record)">确认归还</NButton>
        </div>
        <div v-if="expandedRecordIds.includes(record.id)" class="borrow-record__details">
          <div><span>借出确认时间</span><strong>{{ formatDateTime(record.approvedTime) }}</strong></div>
          <div><span>归还确认时间</span><strong>{{ formatDateTime(record.returnedTime) }}</strong></div>
          <div><span>借出时器材状态</span><strong>{{ getStatusText(record.conditionOnBorrow, '—') }}</strong></div>
          <div><span>归还时器材状态</span><strong>{{ getStatusText(record.conditionOnReturn, '—') }}</strong></div>
        </div>
      </NCard>
      <div v-if="!ownerBorrowsData.length && !ownerBorrowsQuery.isFetching" class="empty-state"><h3>暂无记录</h3><p>没有符合筛选条件的数据，试试清空筛选条件。</p></div>
    </div>

    <section class="pagination">
      <NButton tertiary @click="ownerPagination.pageNo = Math.max(1, ownerPagination.pageNo - 1); ownerBorrowsQuery.refetch()" :disabled="ownerPagination.pageNo <= 1">上一页</NButton>
      <span>第 {{ ownerPagination.pageNo }} 页 / 共 {{ ownerTotalPages }} 页（共 {{ ownerBorrowsTotal }} 条）</span>
      <NButton tertiary @click="ownerPagination.pageNo += 1; ownerBorrowsQuery.refetch()" :disabled="ownerPagination.pageNo >= ownerTotalPages">下一页</NButton>
      <NSelect v-model:value="ownerPagination.pageSize" :options="pageSizeOptions" style="width: 120px" @update:value="() => { ownerPagination.pageNo = 1; ownerBorrowsQuery.refetch() }" />
    </section>
  </section>

  <NModal v-model:show="ownerActionModal.show" preset="card" class="booking-modal" :title="actionTitle">
    <p class="text-muted" style="margin: 0 0 12px 0;">你正在处理借用单 #{{ ownerActionModal.record?.id }}，请确认器材状态并填写补充说明。</p>
    <div class="booking-modal__section"><label>器材状态</label><NSelect v-model:value="ownerActionModal.condition" :options="conditionOptions" /></div>
    <div class="booking-modal__section"><label>备注</label><NInput v-model:value="ownerActionModal.remark" type="textarea" placeholder="可填写处理说明" /></div>
    <div class="booking-modal__actions"><NButton @click="closeOwnerAction">取消</NButton><NButton type="primary" :loading="ownerActionModal.submitting" @click="submitOwnerAction">{{ actionButtonText }}</NButton></div>
  </NModal>
</template>
