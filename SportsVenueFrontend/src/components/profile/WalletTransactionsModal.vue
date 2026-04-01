<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { NButton, NDatePicker, NInputNumber, NModal, NSelect } from 'naive-ui'
import api from '../../services/api'
import { useToast } from '../../composables/useToast'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:show'])
const { pushToast } = useToast()

const loading = ref(false)
const records = ref([])
const total = ref(0)

const filters = reactive({
  type: '',
  startDate: null,
  endDate: null
})

const pagination = reactive({
  pageNo: 1,
  pageSize: 10
})

const typeOptions = [
  { label: '全部类型', value: '' },
  { label: '充值', value: 'RECHARGE' },
  { label: '预约扣费', value: 'BOOKING_DEBIT' },
  { label: '借用租金扣费', value: 'BORROW_RENT_DEBIT' },
  { label: '借用押金扣费', value: 'BORROW_DEPOSIT_DEBIT' },
  { label: '退款', value: 'REFUND' }
]

const totalPages = computed(() => Math.max(1, Math.ceil((total.value || 0) / pagination.pageSize)))

const dateRange = computed({
  get() {
    if (!filters.startDate || !filters.endDate) return null
    return [new Date(filters.startDate).getTime(), new Date(filters.endDate).getTime()]
  },
  set(value) {
    if (!value || value.length !== 2) {
      filters.startDate = null
      filters.endDate = null
      return
    }
    filters.startDate = formatLocalDate(value[0])
    filters.endDate = formatLocalDate(value[1])
  }
})

function formatLocalDate(value) {
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return null
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function formatDateTime(value) {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(
    d.getHours()
  ).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function mapTypeLabel(type) {
  if (type === 'RECHARGE') return '充值'
  if (type === 'BOOKING_DEBIT') return '预约扣费'
  if (type === 'BORROW_RENT_DEBIT') return '借用租金扣费'
  if (type === 'BORROW_DEPOSIT_DEBIT') return '借用押金扣费'
  if (type === 'REFUND') return '退款'
  return type || '—'
}

function formatAmount(value) {
  const num = Number(value || 0)
  const abs = Math.abs(num).toFixed(2)
  if (num > 0) return `+${abs}`
  if (num < 0) return `-${abs}`
  return abs
}

function isPositiveAmount(value) {
  return Number(value || 0) > 0
}

function isNegativeAmount(value) {
  return Number(value || 0) < 0
}

async function fetchTransactions() {
  if (filters.startDate && filters.endDate && filters.startDate > filters.endDate) {
    pushToast('开始日期不能晚于结束日期', 'warning')
    return
  }
  loading.value = true
  try {
    const response = await api.get('/wallet/transactions/my', {
      params: {
      type: filters.type || undefined,
      startDate: filters.startDate || undefined,
      endDate: filters.endDate || undefined,
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize
      }
    })
    if (response.code !== 200) {
      pushToast(response.message || '流水加载失败', 'error')
      return
    }
    const data = response.data || {}
    total.value = data.total || 0
    records.value = data.records || []
  } catch (error) {
    const backendMessage = error?.response?.data?.message
    pushToast(backendMessage || '流水加载失败', 'error')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNo = 1
  fetchTransactions()
}

function resetFilters() {
  filters.type = ''
  filters.startDate = null
  filters.endDate = null
  pagination.pageNo = 1
  fetchTransactions()
}

function prevPage() {
  if (pagination.pageNo <= 1) return
  pagination.pageNo -= 1
  fetchTransactions()
}

function nextPage() {
  if (pagination.pageNo >= totalPages.value) return
  pagination.pageNo += 1
  fetchTransactions()
}

function closeModal() {
  emit('update:show', false)
}

watch(
  () => pagination.pageSize,
  (value, oldValue) => {
    if (value === oldValue) return
    if (!Number.isFinite(value) || value <= 0) {
      pagination.pageSize = oldValue || 10
      return
    }
    pagination.pageSize = Math.min(50, Math.max(1, Math.floor(value)))
    pagination.pageNo = 1
    fetchTransactions()
  }
)

watch(
  () => props.show,
  (show) => {
    if (!show) return
    pagination.pageNo = 1
    fetchTransactions()
  }
)

defineExpose({
  reloadFirstPage: () => {
    pagination.pageNo = 1
    return fetchTransactions()
  }
})
</script>

<template>
  <NModal
    :show="show"
    preset="card"
    class="wallet-transactions-modal"
    title="我的资金流水"
    :style="{ width: '62vw', height: '66vh' }"
    @close="closeModal"
  >
    <div class="booking-panel__filters">
      <div>
        <label>类型</label>
        <NSelect v-model:value="filters.type" :options="typeOptions" />
      </div>
      <div>
        <label>时间范围</label>
        <NDatePicker v-model:value="dateRange" type="daterange" clearable />
      </div>
      <div class="booking-panel__actions">
        <NButton type="primary" :loading="loading" @click="handleSearch">查询</NButton>
        <NButton tertiary @click="resetFilters">重置</NButton>
      </div>
    </div>

    <div class="wallet-table-wrap">
      <table class="wallet-table">
        <thead>
          <tr>
            <th>时间</th>
            <th>类型</th>
            <th>金额</th>
            <th>变动前</th>
            <th>变动后</th>
            <th>业务类型</th>
            <th>业务单号</th>
            <th>备注</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in records" :key="row.id">
            <td>{{ formatDateTime(row.createTime) }}</td>
            <td>{{ mapTypeLabel(row.type) }}</td>
            <td :class="{ 'wallet-amount--plus': isPositiveAmount(row.amount), 'wallet-amount--minus': isNegativeAmount(row.amount) }">
              {{ formatAmount(row.amount) }}
            </td>
            <td>{{ Number(row.beforeBalance || 0).toFixed(2) }}</td>
            <td>{{ Number(row.afterBalance || 0).toFixed(2) }}</td>
            <td>{{ row.bizType || '—' }}</td>
            <td>{{ row.bizId ?? '—' }}</td>
            <td>{{ row.remark || '—' }}</td>
          </tr>
          <tr v-if="!records.length && !loading">
            <td colspan="8">
              <div class="empty-state wallet-empty">
                <h3>暂无资金流水</h3>
                <p>可调整筛选条件后重试。</p>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <section class="pagination">
      <NButton tertiary @click="prevPage" :disabled="pagination.pageNo <= 1">上一页</NButton>
      <span>第 {{ pagination.pageNo }} 页 / 共 {{ totalPages }} 页（共 {{ total }} 条）</span>
      <span style="display: inline-flex; align-items: center; gap: 8px;">
        <span>每页</span>
        <NInputNumber v-model:value="pagination.pageSize" :min="1" :max="50" :step="1" style="width: 100px;" />
        <span>条</span>
      </span>
      <NButton tertiary @click="nextPage" :disabled="pagination.pageNo >= totalPages">下一页</NButton>
    </section>
  </NModal>
</template>

<style scoped>
.wallet-transactions-modal {
  width: 62vw !important;
  height: 66vh !important;
  max-width: 62vw !important;
  max-height: 66vh !important;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

:deep(.wallet-transactions-modal .n-card) {
  height: 100%;
}

.wallet-table-wrap {
  flex: 1;
  min-height: 0;
  margin-top: 16px;
  overflow: auto;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
}

.wallet-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 980px;
}

.wallet-table th,
.wallet-table td {
  border-bottom: 1px solid #e2e8f0;
  padding: 10px 12px;
  font-size: 13px;
  text-align: left;
  color: #334155;
}

.wallet-table thead th {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
}

.wallet-amount--plus {
  color: #16a34a;
  font-weight: 600;
}

.wallet-amount--minus {
  color: #dc2626;
  font-weight: 600;
}

.wallet-empty {
  box-shadow: none;
  background: transparent;
  padding: 20px 10px;
}
</style>
