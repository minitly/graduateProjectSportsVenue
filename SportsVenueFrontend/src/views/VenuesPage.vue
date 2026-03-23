<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useQuery, useQueryClient } from '@tanstack/vue-query'
import { useDialog, NButton, NCard, NDatePicker, NDivider, NInput, NModal, NSelect, NTag } from 'naive-ui'
import { useToast } from '../composables/useToast'
import api from '../services/api'
import { useAuthStore } from '../stores/auth'

const { pushToast } = useToast()
const queryClient = useQueryClient()
const dialog = useDialog()
const authStore = useAuthStore()

const pagination = reactive({
  pageNo: 1,
  pageSize: 6
})
const filters = reactive({
  keyword: '',
  type: '',
  status: 'AVAILABLE'
})

const searchFilters = reactive({
  keyword: '',
  type: '',
  status: 'AVAILABLE'
})

const searchDisabled = ref(true)

const bookingModal = reactive({
  show: false,
  venue: null,
  date: null,
  startTime: null,
  endTime: null,
  occupied: [],
  dragging: false,
  submitting: false
})

const venueNameMap = reactive({})
const venueImageMap = reactive({})

const myBookings = reactive({
  pagination: {
    pageNo: 1,
    pageSize: 5
  }
})

const cancelingIds = ref(new Set())

const venueQueryKey = computed(() => [
  'venues',
  searchFilters.keyword,
  searchFilters.type,
  searchFilters.status,
  pagination.pageNo,
  pagination.pageSize
])

const fetchVenuesData = async (pageNoOverride = pagination.pageNo) => {
  const response = await api.get('/venues', {
    params: {
      keyword: searchFilters.keyword || undefined,
      type: searchFilters.type || undefined,
      status: searchFilters.status || undefined,
      pageNo: pageNoOverride,
      pageSize: pagination.pageSize
    }
  })
  if (response.code !== 200) {
    throw new Error(response.message || '场地加载失败')
  }
  const data = response.data || {}
  const records = data.records || []
  records.forEach((venue) => {
    if (venue?.id) {
      venueNameMap[venue.id] = venue.name || `场地 ${venue.id}`
    }
  })
  return {
    records,
    total: data.total || 0
  }
}

const venuesQuery = useQuery({
  queryKey: venueQueryKey,
  queryFn: () => fetchVenuesData(),
  keepPreviousData: true,
  staleTime: 30000,
  refetchOnWindowFocus: false,
  refetchOnReconnect: false,
  refetchInterval: false,
  retry: false,
  enabled: true,
  onSuccess: (data) => {
    console.log('[VenuesPage] venues fetched', data)
  },
  onError: (error) => {
    console.error('[VenuesPage] venues fetch error', error)
  }
})

const venuesData = computed(() => venuesQuery.data?.records || venuesQuery.data?.value?.records || [])
const venuesTotal = computed(() => venuesQuery.data?.total || venuesQuery.data?.value?.total || 0)
const isVenuesFetching = computed(() => Boolean(venuesQuery.isFetching?.value ?? venuesQuery.isFetching))

watch(
  venuesData,
  (records) => {
    hydrateVenueImages(records)
  },
  { immediate: true }
)

const stats = computed(() => [
  { label: '可预约场地', value: venuesTotal.value },
  { label: '预约窗口', value: '未来 7 天' },
  { label: '当前状态', value: filters.status || '全部' }
])

const bookingSummary = computed(() => [
  { label: '我的预约', value: myBookingsTotal.value },
  { label: '筛选状态', value: bookingFilters.status || '全部' }
])

const statusOptions = [
  { label: '可预约', value: 'AVAILABLE' },
  { label: '维护中', value: 'MAINTAIN' },
  { label: '暂停预约', value: 'SUSPEND' },
  { label: '已停用', value: 'DISABLED' },
  { label: '全部', value: '' }
]

const venueStatusOptions = [
  { label: '可预约', value: 'AVAILABLE' },
  { label: '维护中', value: 'MAINTAIN' },
  { label: '暂停预约', value: 'SUSPEND' },
  { label: '已停用', value: 'DISABLED' }
]

const isOwner = computed(() => authStore.role === 'OWNER')

const venueManageModal = reactive({
  show: false,
  editingId: null,
  submitting: false,
  form: {
    name: '',
    type: '',
    description: '',
    capacity: 0,
    price: 0,
    openTime: '08:00',
    closeTime: '22:00',
    status: 'AVAILABLE',
    coverImageUrl: ''
  }
})

const deleteModal = reactive({
  show: false,
  venue: null,
  adminPassword: '',
  submitting: false
})

const bookingStatusOptions = [
  { label: '全部', value: '' },
  { label: '申请中', value: 'APPLIED' },
  { label: '已取消', value: 'CANCELED' },
  { label: '已核销', value: 'VERIFIED' },
  { label: '违规', value: 'VIOLATION' }
]

const bookingFilters = reactive({
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

const debouncedSearch = ref(null)

const timeSlots = computed(() => {
  if (!bookingModal.venue?.openTime || !bookingModal.venue?.closeTime) return []
  const [startHour] = bookingModal.venue.openTime.split(':').map(Number)
  const [endHour] = bookingModal.venue.closeTime.split(':').map(Number)
  const slots = []
  for (let hour = startHour; hour < endHour; hour += 1) {
    const label = `${String(hour).padStart(2, '0')}:00`
    slots.push({ label, value: label })
  }
  return slots
})

const unavailableSlots = computed(() => {
  if (!bookingModal.date || !bookingModal.occupied.length) return []
  return bookingModal.occupied.map((slot) => new Date(slot.slotStartTime).toTimeString().slice(0, 5))
})

const timeSlotOptions = computed(() =>
  timeSlots.value.map((slot) => ({
    ...slot,
    disabled: unavailableSlots.value.includes(slot.value)
  }))
)

const occupiedRanges = computed(() =>
  bookingModal.occupied.map((slot) => ({
    start: slot.slotStartTime,
    end: slot.slotEndTime
  }))
)

const slotStatusList = computed(() => {
  const occupiedSet = new Set(unavailableSlots.value)
  return timeSlots.value.map((slot, index) => {
    let status = 'available'
    if (occupiedSet.has(slot.value)) {
      status = 'occupied'
    } else if (bookingModal.startTime && bookingModal.endTime) {
      const startIndex = timeSlots.value.findIndex((item) => item.value === bookingModal.startTime)
      const endIndex = timeSlots.value.findIndex((item) => item.value === bookingModal.endTime)
      const currentIndex = timeSlots.value.findIndex((item) => item.value === slot.value)
      if (currentIndex >= startIndex && currentIndex < endIndex) {
        status = 'selected'
      }
    }
    return {
      ...slot,
      status,
      range: `${slot.value} - ${timeSlots.value[index + 1]?.value || bookingModal.venue?.closeTime}`
    }
  })
})

const bookingDates = computed(() => {
  const today = new Date()
  const dates = []
  for (let i = 0; i < 7; i += 1) {
    const target = new Date(today)
    target.setDate(today.getDate() + i)
    dates.push({
      label: target.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', weekday: 'short' }),
      value: target.toISOString().slice(0, 10)
    })
  }
  return dates
})

function formatDate(timestamp) {
  if (!timestamp) return null
  return new Date(timestamp).toISOString().slice(0, 10)
}

function handleSlotSelect(slot) {
  if (slot.status === 'occupied') return
  if (!bookingModal.startTime || bookingModal.endTime) {
    bookingModal.startTime = slot.value
    bookingModal.endTime = null
    return
  }
  const startIndex = timeSlots.value.findIndex((item) => item.value === bookingModal.startTime)
  const endIndex = timeSlots.value.findIndex((item) => item.value === slot.value)
  if (endIndex <= startIndex) {
    bookingModal.startTime = slot.value
    bookingModal.endTime = null
    return
  }
  const hasOccupiedBetween = timeSlots.value
    .slice(startIndex, endIndex)
    .some((item) => unavailableSlots.value.includes(item.value))
  if (hasOccupiedBetween) {
    pushToast('选中区间包含已占用时段，请重新选择', 'warning')
    bookingModal.startTime = slot.value
    bookingModal.endTime = null
    return
  }
  bookingModal.endTime = slot.value
}

function clearSlotSelection() {
  bookingModal.startTime = null
  bookingModal.endTime = null
}


const bookingsQueryKey = computed(() => [
  'myBookings',
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
  enabled: true
})

const myBookingsData = computed(() => bookingsQuery.data?.records || bookingsQuery.data?.value?.records || [])
const myBookingsTotal = computed(() => bookingsQuery.data?.total || bookingsQuery.data?.value?.total || 0)
const isBookingsFetching = computed(() => Boolean(bookingsQuery.isFetching?.value ?? bookingsQuery.isFetching))

const ownerBookingFilters = reactive({
  venueId: '',
  username: '',
  status: '',
  startDate: null,
  endDate: null
})

const ownerBookingStatusOptions = [
  { label: '全部状态', value: '' },
  { label: '申请中', value: 'APPLIED' },
  { label: '已取消', value: 'CANCELED' },
  { label: '已核销', value: 'VERIFIED' },
  { label: '违规', value: 'VIOLATION' }
]

const ownerBookingPagination = reactive({
  pageNo: 1,
  pageSize: 8
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
    ownerBookingFilters.venueId,
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
        venueId: ownerBookingFilters.venueId || undefined,
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

async function hydrateVenueImages(records) {
  const tasks = records
    .map((venue) => ({ id: venue.id, url: venue.coverImageUrl }))
    .filter((item) => item.id && item.url && !venueImageMap[item.id])
  if (!tasks.length) return
  await Promise.all(
    tasks.map(async (item) => {
      try {
        const response = await api.get('/files/serve', {
          params: {
            path: item.url
          }
        })
        if (response.code === 200 && response.data?.images?.length) {
          const image = response.data.images[0]
          venueImageMap[item.id] = `data:${image.contentType};base64,${image.content}`
        }
      } catch (error) {
        venueImageMap[item.id] = ''
      }
    })
  )
}

async function handleSearch() {
  console.log('[VenuesPage] handleSearch clicked')
  pagination.pageNo = 1
  searchFilters.keyword = filters.keyword
  searchFilters.type = filters.type
  searchFilters.status = filters.status
  await venuesQuery.refetch()
}

async function resetFilters() {
  filters.keyword = ''
  filters.type = ''
  filters.status = 'AVAILABLE'
  await handleSearch()
}

async function nextPage() {
  if (pagination.pageNo * pagination.pageSize >= venuesTotal.value) return
  pagination.pageNo += 1
  await venuesQuery.refetch()
}

async function prevPage() {
  if (pagination.pageNo <= 1) return
  pagination.pageNo -= 1
  await venuesQuery.refetch()
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
  bookingFilters.status = ''
  bookingFilters.startDate = null
  bookingFilters.endDate = null
  myBookings.pagination.pageNo = 1
  queryClient.invalidateQueries({ queryKey: ['myBookings'] })
}

function openBookingModal(venue) {
  bookingModal.show = true
  bookingModal.venue = venue
  bookingModal.date = bookingDates.value[0]?.value || null
  bookingModal.startTime = null
  bookingModal.endTime = null
  bookingModal.occupied = []
}

function resetVenueForm() {
  venueManageModal.form = {
    name: '',
    type: '',
    description: '',
    capacity: 0,
    price: 0,
    openTime: '08:00',
    closeTime: '22:00',
    status: 'AVAILABLE',
    coverImageUrl: ''
  }
}

function openCreateVenue() {
  venueManageModal.show = true
  venueManageModal.editingId = null
  resetVenueForm()
}

function openEditVenue(venue) {
  venueManageModal.show = true
  venueManageModal.editingId = venue.id
  venueManageModal.form = {
    name: venue.name || '',
    type: venue.type || '',
    description: venue.description || '',
    capacity: venue.capacity || 0,
    price: venue.price || 0,
    openTime: venue.openTime || '08:00',
    closeTime: venue.closeTime || '22:00',
    status: venue.status || 'AVAILABLE',
    coverImageUrl: venue.coverImageUrl || ''
  }
}

function closeVenueModal() {
  venueManageModal.show = false
}

async function submitVenue() {
  venueManageModal.submitting = true
  try {
    const payload = { ...venueManageModal.form }
    const response = venueManageModal.editingId
      ? await api.put(`/venues/${venueManageModal.editingId}`, payload)
      : await api.post('/venues', payload)
    if (response.code !== 200) {
      pushToast(response.message || '保存场地失败', 'error')
      return
    }
    pushToast(venueManageModal.editingId ? '场地已更新' : '场地已新增', 'success')
    closeVenueModal()
    queryClient.invalidateQueries({ queryKey: ['venues'] })
  } catch (error) {
    pushToast('保存失败，请稍后再试', 'error')
  } finally {
    venueManageModal.submitting = false
  }
}

async function quickChangeVenueStatus(venue, status) {
  if (!venue?.id) return
  try {
    const response = await api.put(`/venues/${venue.id}`, {
      ...venue,
      status
    })
    if (response.code !== 200) {
      pushToast(response.message || '状态切换失败', 'error')
      return
    }
    pushToast(`场地状态已切换为 ${status}`, 'success')
    queryClient.invalidateQueries({ queryKey: ['venues'] })
  } catch (error) {
    pushToast('状态切换失败，请稍后再试', 'error')
  }
}

function openDeleteVenue(venue) {
  deleteModal.show = true
  deleteModal.venue = venue
  deleteModal.adminPassword = ''
}

function closeDeleteVenue() {
  deleteModal.show = false
}

async function runDeleteVenue() {
  deleteModal.submitting = true
  try {
    const response = await api.delete(`/venues/${deleteModal.venue.id}`, {
      data: { adminPassword: deleteModal.adminPassword || undefined }
    })
    if (response.code !== 200) {
      pushToast(response.message || '删除失败', 'error')
      return
    }
    pushToast('场地已删除', 'success')
    closeDeleteVenue()
    queryClient.invalidateQueries({ queryKey: ['venues'] })
  } catch (error) {
    pushToast('删除失败，请稍后再试', 'error')
  } finally {
    deleteModal.submitting = false
  }
}

function confirmDeleteVenue() {
  if (!deleteModal.venue?.id) return
  dialog.warning({
    title: '确认删除/停用',
    content: `确认删除/停用场地「${deleteModal.venue.name}」吗？`,
    positiveText: '确认',
    negativeText: '取消',
    onPositiveClick: () => {
      runDeleteVenue()
    }
  })
}

function closeBookingModal() {
  bookingModal.show = false
}

async function loadOccupiedSlots() {
  if (!bookingModal.venue || !bookingModal.date) return
  try {
    const response = await api.get('/bookings/occupied', {
      params: {
        venueId: bookingModal.venue.id,
        startDate: bookingModal.date,
        endDate: bookingModal.date
      }
    })
    if (response.code !== 200) {
      pushToast(response.message || '加载已预约时段失败', 'error')
      return
    }
    bookingModal.occupied = response.data || []
  } catch (error) {
    pushToast('无法连接后端服务', 'error')
  }
}

function computeEndTimeOptions() {
  if (!bookingModal.startTime) return []
  const startIndex = timeSlots.value.findIndex((slot) => slot.value === bookingModal.startTime)
  if (startIndex === -1) return []
  return timeSlots.value.slice(startIndex + 1)
}

const endTimeOptions = computed(() => computeEndTimeOptions())

async function submitBooking() {
  if (!bookingModal.venue || !bookingModal.date || !bookingModal.startTime || !bookingModal.endTime) {
    pushToast('请选择预约日期与时间', 'warning')
    return
  }
  bookingModal.submitting = true
  try {
    const response = await api.post('/bookings', {
      venueId: bookingModal.venue.id,
      startTime: `${bookingModal.date}T${bookingModal.startTime}:00`,
      endTime: `${bookingModal.date}T${bookingModal.endTime}:00`
    })
    if (response.code !== 200) {
      pushToast(response.message || '预约失败', 'error')
      return
    }
    pushToast('预约申请已提交', 'success')
    closeBookingModal()
    refreshMyBookings()
  } catch (error) {
    pushToast('无法连接后端服务', 'error')
  } finally {
    bookingModal.submitting = false
  }
}


async function cancelBooking(item) {
  if (!item?.id || cancelingIds.value.has(item.id)) return
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
    pushToast('无法连接后端服务', 'error')
  } finally {
    cancelingIds.value.delete(item.id)
  }
}

watch(
  () => bookingModal.date,
  () => {
    bookingModal.startTime = null
    bookingModal.endTime = null
    loadOccupiedSlots()
  }
)

watch(
  () => [filters.keyword, filters.type, filters.status],
  () => {
    if (searchDisabled.value) return
    pagination.pageNo = 1
    if (debouncedSearch.value) {
      clearTimeout(debouncedSearch.value)
    }
    debouncedSearch.value = setTimeout(() => {
      searchFilters.keyword = filters.keyword
      searchFilters.type = filters.type
      searchFilters.status = filters.status
      console.log('[VenuesPage] auto search triggered', { ...searchFilters })
      venuesQuery.refetch()
    }, 400)
  }
)

watch(
  () => venuesQuery.data,
  (data) => {
    if (!data) return
    const totalPages = Math.ceil((data.total || 0) / pagination.pageSize)
    if (pagination.pageNo < totalPages) {
      queryClient.prefetchQuery({
        queryKey: [
          'venues',
          searchFilters.keyword,
          searchFilters.type,
          searchFilters.status,
          pagination.pageNo + 1,
          pagination.pageSize
        ],
        queryFn: () => fetchVenuesData(pagination.pageNo + 1)
      })
    }
  }
)

function handleQuickBooking() {
  if (!venuesData.value.length) {
    pushToast('暂无可预约场地，请先刷新列表', 'warning')
    return
  }
  openBookingModal(venuesData.value[0])
}

async function refreshMyBookings() {
  await bookingsQuery.refetch()
}

async function refreshOwnerBookings() {
  await ownerBookingsQuery.refetch()
}

function resetOwnerBookingFilters() {
  ownerBookingFilters.venueId = ''
  ownerBookingFilters.username = ''
  ownerBookingFilters.status = ''
  ownerBookingFilters.startDate = null
  ownerBookingFilters.endDate = null
  ownerBookingPagination.pageNo = 1
  refreshOwnerBookings()
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
        refreshMyBookings()
      } catch (error) {
        pushToast('核销失败，请稍后再试', 'error')
      }
    }
  })
}

function handleRouteQuickBooking() {
  if (window.location.search.includes('action=quick-booking')) {
    handleQuickBooking()
  }
}

onMounted(() => {
  searchDisabled.value = false
  window.addEventListener('quick-booking', handleQuickBooking)
  handleRouteQuickBooking()
})

onUnmounted(() => {
  window.removeEventListener('quick-booking', handleQuickBooking)
  if (debouncedSearch.value) {
    clearTimeout(debouncedSearch.value)
  }
})
</script>

<template>
  <div class="venues-page">
    <section class="card venues-hero">
      <div>
        <p class="section-kicker">场地预约</p>
        <h2>找到最合适的场地，在线预约并管理你的计划</h2>
        <p class="text-muted">
          根据开放时间与状态筛选，系统会实时校验 7 天游预约规则。
        </p>
      </div>
      <div class="hero-metrics">
        <div v-for="stat in stats" :key="stat.label">
          <span>{{ stat.label }}</span>
          <strong>{{ stat.value }}</strong>
        </div>
      </div>
    </section>

    <p v-if="venuesQuery.error" class="error-text">{{ venuesQuery.error.message }}</p>

    <section class="card venues-filters">
      <div class="field">
        <label>关键词</label>
        <NInput v-model:value="filters.keyword" placeholder="场地名称 / 编号" />
      </div>
      <div class="field">
        <label>场地类型</label>
        <NInput v-model:value="filters.type" placeholder="例如：篮球场" />
      </div>
      <div class="field">
        <label>状态</label>
        <NSelect v-model:value="filters.status" :options="statusOptions" />
      </div>
      <div class="venues-filters__actions">
        <NButton type="primary" @click="handleSearch" :loading="false">
          查询
        </NButton>
        <NButton @click="resetFilters" tertiary>重置</NButton>
        <NButton v-if="isOwner" type="info" @click="openCreateVenue">新增场地</NButton>
      </div>
    </section>

    <section class="venues-grid">
      <article v-for="venue in venuesData" :key="venue.id" class="venue-card">
        <div class="venue-card__media">
          <div class="venue-card__badge" :class="venue.status?.toLowerCase()">
            {{ venue.status || 'UNKNOWN' }}
          </div>
          <img
            v-if="venueImageMap[venue.id]"
            :src="venueImageMap[venue.id]"
            alt="venue cover"
          />
          <div v-else class="venue-card__placeholder">暂无封面</div>
        </div>
        <div class="venue-card__content">
          <div>
            <h3>{{ venue.name }}</h3>
            <p class="text-muted">{{ venue.type }} · 容量 {{ venue.capacity || '-' }}</p>
          </div>
          <p class="venue-card__desc">
            {{ venue.description || '暂无描述' }}
          </p>
          <div class="venue-card__meta">
            <div>
              <span>开放时间</span>
              <strong>{{ venue.openTime }} - {{ venue.closeTime }}</strong>
            </div>
            <div>
              <span>价格</span>
              <strong>{{ venue.price ? `¥${venue.price}/小时` : '咨询' }}</strong>
            </div>
          </div>
          <div class="venue-card__actions">
            <NButton type="primary" @click="openBookingModal(venue)">预约时段</NButton>
            <NButton v-if="isOwner" tertiary @click="openEditVenue(venue)">编辑场地</NButton>
            <NButton v-if="isOwner" type="error" tertiary @click="openDeleteVenue(venue)">删除/停用</NButton>
          </div>
          <div v-if="isOwner" class="venue-card__quick-status">
            <span>快速状态：</span>
            <NButton
              size="tiny"
              tertiary
              :type="venue.status === 'AVAILABLE' ? 'success' : 'default'"
              @click="quickChangeVenueStatus(venue, 'AVAILABLE')"
            >可预约</NButton>
            <NButton
              size="tiny"
              tertiary
              :type="venue.status === 'MAINTAIN' ? 'warning' : 'default'"
              @click="quickChangeVenueStatus(venue, 'MAINTAIN')"
            >维护中</NButton>
            <NButton
              size="tiny"
              tertiary
              :type="venue.status === 'SUSPEND' ? 'default' : 'default'"
              @click="quickChangeVenueStatus(venue, 'SUSPEND')"
            >暂停</NButton>
            <NButton
              size="tiny"
              tertiary
              :type="venue.status === 'DISABLED' ? 'error' : 'default'"
              @click="quickChangeVenueStatus(venue, 'DISABLED')"
            >停用</NButton>
          </div>
        </div>
      </article>

      <div v-if="!venuesData.length && !isVenuesFetching" class="empty-state">
        <h3>暂无场地</h3>
        <p>尝试调整筛选条件或稍后再来。</p>
      </div>
    </section>

    <section class="pagination">
      <NButton tertiary @click="prevPage" :disabled="pagination.pageNo <= 1">上一页</NButton>
      <span>第 {{ pagination.pageNo }} 页 / 共 {{ Math.ceil(venuesTotal / pagination.pageSize) || 1 }} 页</span>
      <NButton
        tertiary
        @click="nextPage"
        :disabled="pagination.pageNo * pagination.pageSize >= venuesTotal"
      >
        下一页
      </NButton>
    </section>

    <section v-if="isOwner" class="card booking-panel">
      <div class="booking-panel__header">
        <div>
          <p class="section-kicker">预约审核管理</p>
          <h3>查看全部用户预约并执行审核操作</h3>
        </div>
      </div>

      <div class="booking-panel__filters">
        <div>
          <label>场地ID</label>
          <NInput v-model:value="ownerBookingFilters.venueId" placeholder="按场地ID过滤" />
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
          <NButton type="primary" :loading="isOwnerBookingsFetching" @click="refreshOwnerBookings">刷新审核列表</NButton>
          <NButton tertiary @click="resetOwnerBookingFilters">快速重置筛选</NButton>
        </div>
      </div>

      <div class="booking-panel__list">
        <NCard v-for="item in ownerBookingsData" :key="`owner-${item.id}`" size="small" class="booking-card">
          <template #header>
            <div class="booking-card__header">
              <div>
                <strong>预约编号 #{{ item.id }}</strong>
                <p class="text-muted">场地 {{ item.venueId }} · 用户 {{ item.username || item.userId }}</p>
              </div>
              <NTag :type="item.status === 'APPLIED' ? 'info' : item.status === 'VERIFIED' ? 'success' : item.status === 'CANCELED' ? 'warning' : 'error'">
                {{ item.status }}
              </NTag>
            </div>
          </template>
          <div class="booking-card__body">
            <div><span>开始时间</span><strong>{{ item.startTime }}</strong></div>
            <div><span>结束时间</span><strong>{{ item.endTime }}</strong></div>
            <div><span>创建时间</span><strong>{{ item.createTime }}</strong></div>
          </div>
          <div class="booking-card__actions">
            <NButton size="small" type="primary" :disabled="item.status !== 'APPLIED'" @click="verifyBooking(item)">
              核销预约
            </NButton>
          </div>
        </NCard>
        <div v-if="!ownerBookingsData.length && !ownerBookingsQuery.isFetching" class="empty-state">
          <h3>暂无审核数据</h3>
          <p>可调整筛选条件后重试。</p>
        </div>
      </div>

      <div class="pagination">
        <NButton tertiary :disabled="ownerBookingPagination.pageNo <= 1" @click="ownerBookingPagination.pageNo -= 1">上一页</NButton>
        <span>第 {{ ownerBookingPagination.pageNo }} 页 / 共 {{ Math.ceil(ownerBookingsTotal / ownerBookingPagination.pageSize) || 1 }} 页</span>
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
          <label>状态</label>
          <NSelect v-model:value="bookingFilters.status" :options="bookingStatusOptions" />
        </div>
        <div>
          <label>时间范围</label>
          <NDatePicker v-model:value="bookingDateRange" type="daterange" clearable />
        </div>
        <div class="booking-panel__actions">
          <NButton type="primary" @click="refreshMyBookings" :loading="false">
            刷新
          </NButton>
          <NButton tertiary @click="resetBookingFilters">清空筛选</NButton>
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
                <p class="text-muted">{{ venueNameMap[item.venueId] || `场地 ${item.venueId}` }}</p>
              </div>
              <NTag :type="item.status === 'APPLIED' ? 'info' : item.status === 'VERIFIED' ? 'success' : item.status === 'CANCELED' ? 'warning' : 'error'">
                {{ item.status }}
              </NTag>
            </div>
          </template>
          <div class="booking-card__body">
            <div>
              <span>开始时间</span>
              <strong>{{ item.startTime }}</strong>
            </div>
            <div>
              <span>结束时间</span>
              <strong>{{ item.endTime }}</strong>
            </div>
            <div>
              <span>创建时间</span>
              <strong>{{ item.createTime }}</strong>
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
        <NButton
          tertiary
          @click="nextBookingPage"
          :disabled="myBookings.pagination.pageNo * myBookings.pagination.pageSize >= myBookingsTotal"
        >
          下一页
        </NButton>
      </div>
    </section>

    <NModal v-model:show="venueManageModal.show" preset="card" class="booking-modal" title="场地管理">
      <div class="booking-modal__section"><label>场地名称</label><NInput v-model:value="venueManageModal.form.name" /></div>
      <div class="booking-modal__section"><label>场地类型</label><NInput v-model:value="venueManageModal.form.type" /></div>
      <div class="booking-modal__section"><label>场地描述</label><NInput v-model:value="venueManageModal.form.description" type="textarea" /></div>
      <div class="booking-modal__section two-col">
        <div><label>容量</label><NInput v-model:value="venueManageModal.form.capacity" /></div>
        <div><label>价格（每小时）</label><NInput v-model:value="venueManageModal.form.price" /></div>
      </div>
      <div class="booking-modal__section two-col">
        <div><label>开放时间</label><NInput v-model:value="venueManageModal.form.openTime" placeholder="08:00" /></div>
        <div><label>关闭时间</label><NInput v-model:value="venueManageModal.form.closeTime" placeholder="22:00" /></div>
      </div>
      <div class="booking-modal__section"><label>状态</label><NSelect v-model:value="venueManageModal.form.status" :options="venueStatusOptions" /></div>
      <div class="booking-modal__section"><label>封面路径</label><NInput v-model:value="venueManageModal.form.coverImageUrl" /></div>
      <div class="booking-modal__actions">
        <NButton @click="closeVenueModal">取消</NButton>
        <NButton type="primary" :loading="venueManageModal.submitting" @click="submitVenue">
          {{ venueManageModal.editingId ? '保存修改' : '确认新增' }}
        </NButton>
      </div>
    </NModal>

    <NModal v-model:show="deleteModal.show" preset="card" class="booking-modal" title="删除/停用场地">
      <p class="text-muted">确认删除场地：{{ deleteModal.venue?.name }}。如后端要求，可输入管理员密码。</p>
      <div class="booking-modal__section">
        <label>管理员密码（可选）</label>
        <NInput v-model:value="deleteModal.adminPassword" type="password" placeholder="按后端要求填写" />
      </div>
      <div class="booking-modal__actions">
        <NButton @click="closeDeleteVenue">取消</NButton>
        <NButton type="error" :loading="deleteModal.submitting" @click="confirmDeleteVenue">确认删除</NButton>
      </div>
    </NModal>

    <NModal v-model:show="bookingModal.show" preset="card" class="booking-modal" :mask-closable="false" title="预约时段">
      <div class="booking-modal__header">
        <div>
          <h3>{{ bookingModal.venue?.name }}</h3>
          <p class="text-muted">{{ bookingModal.venue?.openTime }} - {{ bookingModal.venue?.closeTime }}</p>
        </div>
        <NTag type="info">{{ bookingModal.venue?.type }}</NTag>
      </div>

      <NDivider />

      <div class="booking-modal__section">
        <label>预约日期</label>
        <NSelect v-model:value="bookingModal.date" :options="bookingDates" />
      </div>

      <div class="booking-modal__section">
        <label>开始时间</label>
        <NSelect
          v-model:value="bookingModal.startTime"
          :options="timeSlotOptions"
          :disabled="!bookingModal.date"
        />
        <p class="hint">不可选时间已被占用</p>
      </div>

      <div class="booking-modal__section">
        <label>结束时间</label>
        <NSelect
          v-model:value="bookingModal.endTime"
          :options="endTimeOptions"
          :disabled="!bookingModal.startTime"
        />
        <p class="hint">点击两个可预约时段即可快速圈选连续区间</p>
      </div>

      <div class="booking-modal__slots">
        <div class="booking-modal__slot-header">
          <p>可选时段表格</p>
          <div class="slot-legend">
            <span class="legend available">可预约</span>
            <span class="legend selected">已选择</span>
            <span class="legend occupied">已占用</span>
          </div>
        </div>
        <div class="booking-modal__table">
          <div
            v-for="slot in slotStatusList"
            :key="slot.value"
            class="slot-row"
            :class="slot.status"
            @click="handleSlotSelect(slot)"
          >
            <span>{{ slot.range }}</span>
            <span class="slot-status">
              {{ slot.status === 'available' ? '可预约' : slot.status === 'selected' ? '已选择' : '已占用' }}
            </span>
          </div>
        </div>
        <div class="booking-modal__occupied">
          <p>已占用区间</p>
          <div class="booking-modal__tags">
            <NTag v-for="slot in occupiedRanges" :key="slot.start" type="warning" size="small">
              {{ slot.start }} ~ {{ slot.end }}
            </NTag>
            <span v-if="!occupiedRanges.length" class="text-muted">暂无占用</span>
          </div>
        </div>
      </div>

      <div class="booking-modal__rules">
        <p>预约规则提示</p>
        <ul>
          <li>预约时间必须为整点。</li>
          <li>每个时段为 60 分钟粒度，需至少选择 1 个时段。</li>
          <li>开始与结束时间必须在同一天内。</li>
        </ul>
      </div>

      <div class="booking-modal__actions">
        <NButton tertiary @click="clearSlotSelection">清空时段</NButton>
        <NButton @click="closeBookingModal">取消</NButton>
        <NButton type="primary" :loading="bookingModal.submitting" @click="submitBooking">
          提交预约
        </NButton>
      </div>
    </NModal>
  </div>
</template>
