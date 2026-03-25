<script setup>
import { computed, reactive } from 'vue'
import { useQuery, useQueryClient } from '@tanstack/vue-query'
import { NButton, NCard, NDatePicker, NInput, NModal, NSelect, NTag } from 'naive-ui'
import api from '../../services/api'
import { useToast } from '../../composables/useToast'
import { getStatusText } from '../../constants/statusMap'

const { pushToast } = useToast()
const queryClient = useQueryClient()

const ownerFilters = reactive({ status: '', range: null, startTime: null, endTime: null })
const ownerPagination = reactive({ pageNo: 1, pageSize: 6 })
const ownerActionModal = reactive({ show: false, type: 'approve', record: null, condition: 'GOOD', remark: '', submitting: false })
const itemNameMap = reactive({})

const ownerStatusOptions = [
  { label: '全部', value: '' },
  { label: '申请中', value: 'REQUESTED' },
  { label: '使用中', value: 'USING' }
]

const conditionOptions = [
  { label: '完好', value: 'GOOD' },
  { label: '轻微磨损', value: 'WORN' },
  { label: '损坏', value: 'DAMAGED' }
]

const ownerBorrowsQuery = useQuery({
  queryKey: computed(() => ['ownerBorrows', ownerFilters.status, ownerFilters.startTime, ownerFilters.endTime, ownerPagination.pageNo, ownerPagination.pageSize]),
  queryFn: async () => {
    const response = await api.get('/borrows', {
      params: {
        status: ownerFilters.status || undefined,
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

const ownerBorrowsData = computed(() => ownerBorrowsQuery.data?.records || ownerBorrowsQuery.data?.value?.records || [])
const ownerBorrowsTotal = computed(() => ownerBorrowsQuery.data?.total || ownerBorrowsQuery.data?.value?.total || 0)
const isOwnerFetching = computed(() => Boolean(ownerBorrowsQuery.isFetching?.value ?? ownerBorrowsQuery.isFetching))

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
    queryClient.invalidateQueries({ queryKey: ['ownerBorrows'] })
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
    <div class="borrow-panel__header"><div><p class="section-kicker">借用管理</p><h3>审批借用与归还确认</h3></div></div>

    <div class="borrow-panel__filters">
      <div><label>状态</label><NSelect v-model:value="ownerFilters.status" :options="ownerStatusOptions" /></div>
      <div><label>申请时间范围</label><NDatePicker v-model:value="ownerFilters.range" type="datetimerange" clearable @update:value="handleOwnerRangeChange" /></div>
      <div class="borrow-panel__actions"><NButton type="primary" :loading="isOwnerFetching" @click="ownerBorrowsQuery.refetch()">刷新</NButton></div>
    </div>

    <div class="borrow-panel__list">
      <NCard v-for="record in ownerBorrowsData" :key="record.id" size="small" class="borrow-record">
        <template #header>
          <div class="borrow-record__header">
            <div><strong>借用单 #{{ record.id }}</strong><p class="text-muted">{{ itemNameMap[record.itemId] || `器材 ${record.itemId}` }}</p></div>
            <NTag :type="record.status === 'REQUESTED' ? 'info' : 'warning'">{{ getStatusText(record.status) }}</NTag>
          </div>
        </template>
        <div class="borrow-record__body">
          <div><span>用户ID</span><strong>{{ record.userId }}</strong></div>
          <div><span>数量</span><strong>{{ record.quantity }}</strong></div>
          <div><span>申请时间</span><strong>{{ formatDateTime(record.requestedTime || record.createTime) }}</strong></div>
        </div>
        <div class="borrow-record__actions">
          <NButton v-if="record.status === 'REQUESTED'" size="small" type="primary" @click="openOwnerAction('approve', record)">确认借出</NButton>
          <NButton v-if="record.status === 'USING'" size="small" type="warning" @click="openOwnerAction('return', record)">确认归还</NButton>
        </div>
      </NCard>
      <div v-if="!ownerBorrowsData.length && !ownerBorrowsQuery.isFetching" class="empty-state"><h3>暂无待处理记录</h3><p>可通过筛选查看申请中或使用中的借用单。</p></div>
    </div>
  </section>

  <NModal v-model:show="ownerActionModal.show" preset="card" class="booking-modal" title="借用流程处理">
    <div class="booking-modal__section"><label>器材状态</label><NSelect v-model:value="ownerActionModal.condition" :options="conditionOptions" /></div>
    <div class="booking-modal__section"><label>备注</label><NInput v-model:value="ownerActionModal.remark" type="textarea" placeholder="可填写处理说明" /></div>
    <div class="booking-modal__actions"><NButton @click="closeOwnerAction">取消</NButton><NButton type="primary" :loading="ownerActionModal.submitting" @click="submitOwnerAction">提交</NButton></div>
  </NModal>
</template>
