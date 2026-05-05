<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useQuery, useQueryClient } from '@tanstack/vue-query'
import { useDialog, NButton, NCard, NDatePicker, NInput, NInputNumber, NSelect, NTag } from 'naive-ui'
import { useToast } from '../../composables/useToast'
import api from '../../services/api'
import { useAuthStore } from '../../stores/auth'
import { getStatusText } from '../../constants/statusMap'
import { formatDisplayDateTime } from '../../utils/dateFormat'

const { pushToast } = useToast()
const queryClient = useQueryClient()
const dialog = useDialog()
const authStore = useAuthStore()

const isOwner = computed(() => authStore.role === 'OWNER')

const venueNameMap = reactive({})

const myBookings = reactive({
  pagination: {
    pageNo: 1,
    pageSize: 12
  }
})

const cancelingIds = ref(new Set())

function formatLocalDateString(date) {
  if (!date || Number.isNaN(date.getTime())) return ''
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function formatDate(timestamp) {
  if (timestamp == null || timestamp === undefined) return null
  const d = new Date(timestamp)
  if (Number.isNaN(d.getTime())) return null
  return formatLocalDateString(d)
}

const bookingStatusOptions = [
  { label: '全部', value: '' },
  { label: getStatusText('APPLIED'), value: 'APPLIED' },
  { label: getStatusText('CANCELED'), value: 'CANCELED' },
  { label: getStatusText('VERIFIED'), value: 'VERIFIED' },
  { label: getStatusText('VIOLATION'), value: 'VIOLATION' }
]

const bookingFilters = reactive({
  venueName: '',
  status: '',
  startDate: null,
  endDate: null
})

const bookingDateRange = computed({
  get() {
    if (!bookingFilters.startDate || !bookingFilters.endDate) return null
    return [new Date(bookingFilters.startDate).getTime(), new Date(bookingFilters.endDate).getTime()]
  },
  set(value) {
    if (!value) {
      bookingFilters.startDate = null
      bookingFilters.endDate = null
      return
    }
    bookingFilters.startDate = formatDate(value[0])
    bookingFilters.endDate = formatDate(value[1])
  }
})

const bookingsQueryKey = computed(() => [
  'myBookings',
  bookingFilters.venueName,
  bookingFilters.status,
  bookingFilters.startDate,
  bookingFilters.endDate,
  myBookings.pagination.pageNo,
  myBookings.pagination.pageSize
])

const bookingsQuery = useQuery({
  queryKey: bookingsQueryKey,
  queryFn: async () => {
    const response = await api.get('/bookings/my', {
      params: {
        venueName: bookingFilters.venueName || undefined,
        status: bookingFilters.status || undefined,
        startDate: bookingFilters.startDate || undefined,
        endDate: bookingFilters.endDate || undefined,
        pageNo: myBookings.pagination.pageNo,
        pageSize: myBookings.pagination.pageSize
      }
    })
    if (response.code !== 200) {
      throw new Error(response.message || '预约记录加载失败')
    }
    const data = response.data || {}
    await hydrateVenueNames(data.records || [])
    return {
      records: data.records || [],
      total: data.total || 0
    }
  },
  keepPreviousData: true,
  staleTime: 30000,
  enabled: computed(() => !isOwner.value)
})

const myBookingsData = computed(() => bookingsQuery.data?.records || bookingsQuery.data?.value?.records || [])
const myBookingsTotal = computed(() => bookingsQuery.data?.total || bookingsQuery.data?.value?.total || 0)
const isBookingsFetching = computed(() => Boolean(bookingsQuery.isFetching?.value ?? bookingsQuery.isFetching))

const bookingSummary = computed(() => [
  { label: '我的预约', value: myBookingsTotal.value },
  { label: '筛选状态', value: bookingFilters.status ? getStatusText(bookingFilters.status) : '全部' }
])

const ownerBookingFilters = reactive({
  venueName: '',
  username: '',
  status: '',
  startDate: null,
  endDate: null
})

const ownerBookingStatusOptions = [
  { label: '全部状态', value: '' },
  { label: getStatusText('APPLIED'), value: 'APPLIED' },
  { label: getStatusText('CANCELED'), value: 'CANCELED' },
  { label: getStatusText('VERIFIED'), value: 'VERIFIED' },
  { label: getStatusText('VIOLATION'), value: 'VIOLATION' }
]

const ownerBookingPagination = reactive({
  pageNo: 1,
  pageSize: 12
})

const ownerBookingDateRange = computed({
  get() {
    if (!ownerBookingFilters.startDate || !ownerBookingFilters.endDate) return null
    return [new Date(ownerBookingFilters.startDate).getTime(), new Date(ownerBookingFilters.endDate).getTime()]
  },
  set(value) {
    if (!value) {
      ownerBookingFilters.startDate = null
      ownerBookingFilters.endDate = null
      return
    }
    ownerBookingFilters.startDate = formatDate(value[0])
    ownerBookingFilters.endDate = formatDate(value[1])
  }
})

const ownerBookingsQuery = useQuery({
  queryKey: computed(() => [
    'ownerBookings',
    ownerBookingFilters.venueName,
    ownerBookingFilters.username,
    ownerBookingFilters.status,
    ownerBookingFilters.startDate,
    ownerBookingFilters.endDate,
    ownerBookingPagination.pageNo,
    ownerBookingPagination.pageSize
  ]),
  queryFn: async () => {
    const response = await api.get('/bookings', {
      params: {
        venueName: ownerBookingFilters.venueName || undefined,
        username: ownerBookingFilters.username || undefined,
        status: ownerBookingFilters.status || undefined,
        startDate: ownerBookingFilters.startDate || undefined,
        endDate: ownerBookingFilters.endDate || undefined,
        pageNo: ownerBookingPagination.pageNo,
        pageSize: ownerBookingPagination.pageSize
      }
    })
    if (response.code !== 200) {
      throw new Error(response.message || '预约审核列表加载失败')
    }
    const data = response.data || {}
    await hydrateVenueNames(data.records || [])
    return { records: data.records || [], total: data.total || 0 }
  },
  enabled: isOwner,
  keepPreviousData: true,
  staleTime: 30000
})

const ownerBookingsData = computed(() => ownerBookingsQuery.data?.records || ownerBookingsQuery.data?.value?.records || [])
const ownerBookingsTotal = computed(() => ownerBookingsQuery.data?.total || ownerBookingsQuery.data?.value?.total || 0)
const isOwnerBookingsFetching = computed(
    () => Boolean(ownerBookingsQuery.isFetching?.value ?? ownerBookingsQuery.isFetching)
)
const ownerBookingSummary = computed(() => [
  { label: '预约总数', value: ownerBookingsTotal.value },
  { label: '当前筛选', value: ownerBookingFilters.status ? getStatusText(ownerBookingFilters.status) : '全部状态' },
  { label: '分页大小', value: ownerBookingPagination.pageSize }
])

async function hydrateVenueNames(records) {
  const ids = [...new Set(records.map((item) => item.venueId).filter(Boolean))].filter(
      (id) => !venueNameMap[id]
  )
  if (!ids.length) return
  await Promise.all(
      ids.map(async (id) => {
        try {
          const response = await api.get(`/venues/${id}`)
          if (response.code === 200 && response.data) {
            venueNameMap[id] = response.data.name || `场地 ${id}`
          }
        } catch (error) {
          venueNameMap[id] = `场地 ${id}`
        }
      })
  )
}

async function nextBookingPage() {
  if (myBookings.pagination.pageNo * myBookings.pagination.pageSize >= myBookingsTotal.value) return
  myBookings.pagination.pageNo += 1
  await bookingsQuery.refetch()
}

async function prevBookingPage() {
  if (myBookings.pagination.pageNo <= 1) return
  myBookings.pagination.pageNo -= 1
  await bookingsQuery.refetch()
}

function resetBookingFilters() {
  bookingFilters.venueName = ''
  bookingFilters.status = ''
  bookingFilters.startDate = null
  bookingFilters.endDate = null
  myBookings.pagination.pageNo = 1
  queryClient.invalidateQueries({ queryKey: ['myBookings'] })
}

async function refreshMyBookings() {
  await bookingsQuery.refetch()
}

async function refreshOwnerBookings() {
  await ownerBookingsQuery.refetch()
}

function resetOwnerBookingFilters() {
  ownerBookingFilters.venueName = ''
  ownerBookingFilters.username = ''
  ownerBookingFilters.status = ''
  ownerBookingFilters.startDate = null
  ownerBookingFilters.endDate = null
  ownerBookingPagination.pageNo = 1
  refreshOwnerBookings()
}

function getHoursUntilBookingStart(item) {
  const startAt = new Date(item?.startTime).getTime()
  if (!Number.isFinite(startAt)) return Infinity
  return (startAt - Date.now()) / (1000 * 60 * 60)
}

function getViolationCountFromMyBookings() {
  return myBookingsData.value.filter((booking) => booking?.status === 'VIOLATION').length
}

async function runCancelBooking(item) {
  cancelingIds.value.add(item.id)
  try {
    const response = await api.put(`/bookings/${item.id}/cancel`, {
      remark: '用户取消'
    })
    if (response.code !== 200) {
      pushToast(response.message || '取消失败', 'error')
      return
    }
    pushToast('预约已取消', 'success')
    queryClient.invalidateQueries({ queryKey: ['myBookings'] })
  } catch (error) {
    const backendMessage = error?.response?.data?.message
    pushToast(backendMessage || '无法连接后端服务', 'error')
  } finally {
    cancelingIds.value.delete(item.id)
  }
}

async function cancelBooking(item) {
  if (!item?.id || cancelingIds.value.has(item.id)) return

  const hoursUntilStart = getHoursUntilBookingStart(item)
  if (hoursUntilStart < 2) {
    const currentViolationCount = getViolationCountFromMyBookings()
    const nextViolationCount = currentViolationCount + 1
    dialog.warning({
      title: '临近开始时间取消确认',
      content: `当前预约距离开始不足 2 小时，取消将计为违规。\n当前为第 ${nextViolationCount} 次违规；累计 3 次违规将无法再预约。`,
      positiveText: '仍要取消',
      negativeText: '返回',
      type: nextViolationCount >= 3 ? 'error' : 'warning',
      onPositiveClick: () => runCancelBooking(item)
    })
    return
  }

  runCancelBooking(item)
}

function verifyBooking(item) {
  if (!item?.id) return
  dialog.warning({
    title: '确认核销',
    content: `确认核销预约 #${item.id} 吗？`,
    positiveText: '确认核销',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const response = await api.put(`/bookings/${item.id}/verify`)
        if (response.code !== 200) {
          pushToast(response.message || '核销失败', 'error')
          return
        }
        pushToast('预约已核销', 'success')
        refreshOwnerBookings()
        queryClient.invalidateQueries({ queryKey: ['myBookings'] })
      } catch (error) {
        const backendMessage = error?.response?.data?.message
        pushToast(backendMessage || '核销失败，请稍后再试', 'error')
      }
    }
  })
}

watch(
    () => myBookings.pagination.pageSize,
    (value, oldValue) => {
      if (value === oldValue) return
      if (!Number.isFinite(value) || value <= 0) {
        myBookings.pagination.pageSize = oldValue || 12
        return
      }
      myBookings.pagination.pageSize = Math.min(50, Math.max(1, Math.floor(value)))
      myBookings.pagination.pageNo = 1
    }
)

watch(
    () => ownerBookingPagination.pageSize,
    (value, oldValue) => {
      if (value === oldValue) return
      if (!Number.isFinite(value) || value <= 0) {
        ownerBookingPagination.pageSize = oldValue || 12
        return
      }
      ownerBookingPagination.pageSize = Math.min(50, Math.max(1, Math.floor(value)))
      ownerBookingPagination.pageNo = 1
    }
)
</script>

<template>
  <div class="venues-page">
    <section v-if="isOwner" class="card booking-panel module-tier module-tier--summary">
      <div class="booking-panel__header">
        <div>
          <p class="section-kicker">预约审核管理</p>
          <h3>查看全部用户预约并执行审核操作</h3>
          <p class="text-muted">按场地、用户、状态和日期筛选预约并执行核销。</p>
        </div>
        <div class="booking-panel__summary">
          <div v-for="stat in ownerBookingSummary" :key="stat.label" class="summary-card">
            <span>{{ stat.label }}</span>
            <strong>{{ stat.value }}</strong>
          </div>
        </div>
      </div>
    </section>

    <section v-if="isOwner" class="card booking-panel module-tier module-tier--filters">
      <div class="booking-panel__filters">
        <div>
          <label>场地名称</label>
          <NInput v-model:value="ownerBookingFilters.venueName" placeholder="按场地名称过滤（模糊）" />
        </div>
        <div>
          <label>用户名</label>
          <NInput v-model:value="ownerBookingFilters.username" placeholder="按用户名过滤（模糊）" />
        </div>
        <div>
          <label>状态</label>
          <NSelect v-model:value="ownerBookingFilters.status" :options="ownerBookingStatusOptions" />
        </div>
        <div>
          <label>预约日期范围</label>
          <NDatePicker v-model:value="ownerBookingDateRange" type="daterange" clearable />
        </div>
        <div class="booking-panel__actions">
          <NButton type="primary" :loading="isOwnerBookingsFetching" @click="refreshOwnerBookings">查询</NButton>
          <NButton tertiary @click="resetOwnerBookingFilters">重置</NButton>
        </div>
      </div>
    </section>

    <section v-if="isOwner" class="card booking-panel module-tier module-tier--data">
      <div class="booking-panel__list">
        <NCard v-for="item in ownerBookingsData" :key="`owner-${item.id}`" size="small" class="booking-card">
          <template #header>
            <div class="booking-card__header">
              <div>
                <strong>预约编号 #{{ item.id }}</strong>
                <p class="text-muted owner-booking-meta">
                  <span class="owner-booking-meta__item">
                    <span class="owner-booking-meta__label">场地：</span>
                    <span>{{ item.venueName || `场地 ${item.venueId || '-'}` }}</span>
                  </span>
                  <span class="owner-booking-meta__item">
                    <span class="owner-booking-meta__label">用户：</span>
                    <span>{{ item.userName || item.username || '-' }}</span>
                  </span>
                </p>
              </div>
              <NTag :type="item.status === 'APPLIED' ? 'info' : item.status === 'VERIFIED' ? 'success' : item.status === 'CANCELED' ? 'warning' : 'error'">
                {{ getStatusText(item.status) }}
              </NTag>
            </div>
          </template>

          <div class="booking-card__body">
            <div><span>开始时间</span><strong>{{ formatDisplayDateTime(item.startTime) }}</strong></div>
            <div><span>结束时间</span><strong>{{ formatDisplayDateTime(item.endTime) }}</strong></div>
            <div><span>创建时间</span><strong>{{ formatDisplayDateTime(item.createTime) }}</strong></div>
          </div>
          <div class="booking-card__actions">
            <NButton size="small" type="primary" :disabled="item.status !== 'APPLIED'" @click="verifyBooking(item)">
              核销预约
            </NButton>
          </div>
        </NCard>
        <div v-if="!ownerBookingsData.length && !isOwnerBookingsFetching" class="empty-state">
          <h3>暂无预约记录</h3>
          <p>用户预约场地后将展示在这里，您可执行核销；若无数据，也可尝试调整筛选条件。</p>
        </div>
      </div>

      <div class="pagination">
        <NButton tertiary :disabled="ownerBookingPagination.pageNo <= 1" @click="ownerBookingPagination.pageNo -= 1">上一页</NButton>
        <span>第 {{ ownerBookingPagination.pageNo }} 页 / 共 {{ Math.ceil(ownerBookingsTotal / ownerBookingPagination.pageSize) || 1 }} 页</span>
        <span style="display: inline-flex; align-items: center; gap: 8px;">
          <span>每页</span>
          <NInputNumber
              v-model:value="ownerBookingPagination.pageSize"
              :min="1"
              :max="50"
              :step="1"
              style="width: 100px;"
          />
          <span>条</span>
        </span>
        <NButton tertiary :disabled="ownerBookingPagination.pageNo * ownerBookingPagination.pageSize >= ownerBookingsTotal" @click="ownerBookingPagination.pageNo += 1">下一页</NButton>
      </div>
    </section>

    <section v-if="!isOwner" class="card booking-panel">
      <div class="booking-panel__header">
        <div>
          <p class="section-kicker">我的预约</p>
          <h3>随时查看你的预约进度</h3>
          <p class="text-muted">快速查看状态变化与预约记录。</p>
        </div>
        <div class="booking-panel__summary">
          <div v-for="stat in bookingSummary" :key="stat.label" class="summary-card">
            <span>{{ stat.label }}</span>
            <strong>{{ stat.value }}</strong>
          </div>
        </div>
      </div>
      <p v-if="bookingsQuery.error" class="error-text">{{ bookingsQuery.error.message }}</p>

      <div class="booking-panel__filters">
        <div>
          <label>场地名称</label>
          <NInput v-model:value="bookingFilters.venueName" placeholder="按场地名称过滤（模糊）" />
        </div>
        <div>
          <label>状态</label>
          <NSelect v-model:value="bookingFilters.status" :options="bookingStatusOptions" />
        </div>
        <div>
          <label>时间范围</label>
          <NDatePicker v-model:value="bookingDateRange" type="daterange" clearable />
        </div>
        <div class="booking-panel__actions">
          <NButton type="primary" @click="refreshMyBookings" :loading="false">
            查询
          </NButton>
          <NButton tertiary @click="resetBookingFilters">重置</NButton>
        </div>
      </div>

      <div class="booking-panel__list">
        <NCard
            v-for="item in myBookingsData"
            :key="item.id"
            size="small"
            class="booking-card"
        >
          <template #header>
            <div class="booking-card__header">
              <div>
                <strong>预约编号 #{{ item.id }}</strong>
                <p class="text-muted">{{ item.venueName || venueNameMap[item.venueId] || `场地 ${item.venueId}` }}</p>
              </div>
              <NTag :type="item.status === 'APPLIED' ? 'info' : item.status === 'VERIFIED' ? 'success' : item.status === 'CANCELED' ? 'warning' : 'error'">
                {{ getStatusText(item.status) }}
              </NTag>
            </div>
          </template>
          <div class="booking-card__body">
            <div>
              <span>开始时间</span>
              <strong>{{ formatDisplayDateTime(item.startTime) }}</strong>
            </div>
            <div>
              <span>结束时间</span>
              <strong>{{ formatDisplayDateTime(item.endTime) }}</strong>
            </div>
            <div>
              <span>创建时间</span>
              <strong>{{ formatDisplayDateTime(item.createTime) }}</strong>
            </div>
          </div>
          <div class="booking-card__actions">
            <NButton
                size="small"
                tertiary
                type="error"
                :disabled="item.status !== 'APPLIED'"
                :loading="cancelingIds.has(item.id)"
                @click="cancelBooking(item)"
            >
              取消预约
            </NButton>
          </div>
        </NCard>
        <div v-if="!myBookingsData.length && !isBookingsFetching" class="empty-state">
          <h3>暂无预约记录</h3>
          <p>完成一次预约后会展示在这里。</p>
        </div>
      </div>

      <div class="pagination">
        <NButton tertiary @click="prevBookingPage" :disabled="myBookings.pagination.pageNo <= 1">上一页</NButton>
        <span>
          第 {{ myBookings.pagination.pageNo }} 页 / 共 {{ Math.ceil(myBookingsTotal / myBookings.pagination.pageSize) || 1 }} 页
        </span>
        <span style="display: inline-flex; align-items: center; gap: 8px;">
          <span>每页</span>
          <NInputNumber
              v-model:value="myBookings.pagination.pageSize"
              :min="1"
              :max="50"
              :step="1"
              style="width: 100px;"
          />
          <span>条</span>
        </span>
        <NButton
            tertiary
            @click="nextBookingPage"
            :disabled="myBookings.pagination.pageNo * myBookings.pagination.pageSize >= myBookingsTotal"
        >
          下一页
        </NButton>
      </div>
    </section>
  </div>
</template>
