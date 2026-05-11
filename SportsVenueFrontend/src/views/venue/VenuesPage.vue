<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useQuery, useQueryClient } from '@tanstack/vue-query'
import { useDialog, NButton, NDivider, NInput, NInputNumber, NModal, NSelect, NTag, NTabs, NTabPane } from 'naive-ui'
import { useToast } from '../../composables/useToast'
import api from '../../services/api'
import { useAuthStore } from '../../stores/auth'
import { getStatusText } from '../../constants/statusMap'
import { formatDisplayDateTime } from '../../utils/dateFormat'
import FloorPlanCanvasPreview from '../../components/floorPlan/FloorPlanCanvasPreview.vue'
import { parseFloorPlanContent } from '../../utils/floorPlan'

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
  submitting: false,
  floorPlanLoading: false,
  floorPlanTitle: '',
  floorPlanModel: null,
  /** 场地图绑定的场地 id → status，用于禁用不可预约区域的点击 */
  floorPlanVenueStatusById: {}
})

const venueNameMap = reactive({})
const venueImageMap = reactive({})

/** 预约弹窗打开时轮询已占用时段，避免并发预约后列表不更新 */
const bookingOccupiedPollTimer = ref(null)

function stopBookingOccupiedPoll() {
  if (bookingOccupiedPollTimer.value != null) {
    clearInterval(bookingOccupiedPollTimer.value)
    bookingOccupiedPollTimer.value = null
  }
}

function resetBookingFloorPlanVenueStatuses() {
  for (const k of Object.keys(bookingModal.floorPlanVenueStatusById)) {
    delete bookingModal.floorPlanVenueStatusById[k]
  }
}

function isVenueStatusBookable(status) {
  return String(status || '').toUpperCase() === 'AVAILABLE'
}

async function hydrateBookingFloorPlanVenueStatuses() {
  const model = bookingModal.floorPlanModel
  if (!model?.items?.length) return
  const ids = [
    ...new Set(model.items.map((it) => Number(it.venueId)).filter((n) => Number.isFinite(n) && n > 0))
  ]
  await Promise.all(
    ids.map(async (vid) => {
      const existing = bookingModal.floorPlanVenueStatusById[vid]
      if (existing != null && existing !== '') return
      try {
        const response = await api.get(`/venues/${vid}`)
        if (response.code === 200 && response.data?.status != null && response.data.status !== '') {
          bookingModal.floorPlanVenueStatusById[vid] = response.data.status
        }
      } catch {
        /* 忽略：点击时再请求 */
      }
    })
  )
}

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

function primeBookingFloorPlanVenueStatusesFromList() {
  const model = bookingModal.floorPlanModel
  if (!model?.items?.length) return
  const ids = new Set(
    model.items.map((it) => Number(it.venueId)).filter((n) => Number.isFinite(n) && n > 0)
  )
  for (const v of venuesData.value) {
    if (v?.id && ids.has(v.id) && v.status != null && v.status !== '') {
      bookingModal.floorPlanVenueStatusById[v.id] = v.status
    }
  }
  if (bookingModal.venue?.id != null && bookingModal.venue.status != null && bookingModal.venue.status !== '') {
    bookingModal.floorPlanVenueStatusById[bookingModal.venue.id] = bookingModal.venue.status
  }
}

watch(
    venuesData,
    (records) => {
      hydrateVenueImages(records)
    },
    { immediate: true }
)

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
    code: '',
    type: '',
    description: '',
    capacity: 0,
    price: 0,
    openTime: '08:00',
    closeTime: '22:00',
    openTimeDesc: '',
    remark: '',
    status: 'AVAILABLE',
    coverImageUrl: '',
    floorPlanId: null,
    floorPlanItemUid: ''
  }
})

const floorPlanOptions = ref([])
const floorPlanLoading = ref(false)
const floorPlanItemOptions = ref([])
const floorPlanItemLoading = ref(false)

async function loadFloorPlanOptions() {
  floorPlanLoading.value = true
  try {
    const response = await api.get('/floor-plans', { params: { pageNo: 1, pageSize: 200 } })
    if (response.code !== 200) {
      throw new Error(response.message || '场地图加载失败')
    }
    const records = response.data?.records || []
    floorPlanOptions.value = records
      .map((fp) => ({ label: fp.title || `场地图 ${fp.id}`, value: fp.id }))
      .filter((opt) => opt.value != null)
  } catch (e) {
    pushToast(e?.message || '场地图加载失败', 'error')
    floorPlanOptions.value = []
  } finally {
    floorPlanLoading.value = false
  }
}

async function loadFloorPlanItemOptions(floorPlanId) {
  if (!floorPlanId) {
    floorPlanItemOptions.value = []
    return
  }
  floorPlanItemLoading.value = true
  try {
    const response = await api.get(`/floor-plans/${floorPlanId}/items`, {
      params: { venueId: venueManageModal.editingId || undefined }
    })
    if (response.code !== 200) {
      throw new Error(response.message || '场地图区域加载失败')
    }
    const items = response.data || []
    floorPlanItemOptions.value = items
      .map((it) => ({ label: it.label || it.itemUid || it.itemUid, value: it.itemUid }))
      .filter((opt) => opt.value)
  } catch (e) {
    pushToast(e?.message || '场地图区域加载失败', 'error')
    floorPlanItemOptions.value = []
  } finally {
    floorPlanItemLoading.value = false
  }
}

const venueFormErrors = reactive({
  name: '',
  type: '',
  capacity: '',
  price: '',
  openTime: '',
  openTimeDesc: '',
  remark: ''
})

const coverUploadFile = ref(null)
const coverUploadPreview = ref('')
const coverPreviewObjectUrl = ref('')
const isCoverUploading = ref(false)
const isCodeGenerating = ref(false)
const originalVenueCode = ref('')

/** 用户端 hero「可预约场地」：固定统计 status=AVAILABLE 的数量，与列表筛选「全部」时的 total 解耦 */
const availableVenuesStatsQuery = useQuery({
  queryKey: ['venues', 'available-total'],
  queryFn: async () => {
    const response = await api.get('/venues', {
      params: { status: 'AVAILABLE', pageNo: 1, pageSize: 1 }
    })
    if (response.code !== 200) throw new Error(response.message || '场地统计失败')
    const data = response.data || {}
    return data.total ?? 0
  },
  enabled: computed(() => !isOwner.value),
  staleTime: 30000,
  refetchOnWindowFocus: false
})

const availableVenuesTotal = computed(() => {
  const raw = availableVenuesStatsQuery.data
  const v = raw?.value !== undefined ? raw.value : raw
  return typeof v === 'number' ? v : 0
})

const stats = computed(() => [
  { label: '可预约场地', value: availableVenuesTotal.value },
  { label: '预约窗口', value: '未来 7 天' },
  { label: '当前状态', value: filters.status ? getStatusText(filters.status) : '全部' }
])

const ownerVenueStats = computed(() => [
  { label: '场地总数', value: venuesTotal.value },
  { label: '筛选状态', value: filters.status ? getStatusText(filters.status) : '全部状态' },
  { label: '每页数量', value: pagination.pageSize }
])

const deleteModal = reactive({
  show: false,
  venue: null,
  adminPassword: '',
  submitting: false
})

const venueDetailModal = reactive({
  show: false,
  loading: false,
  data: null
})

watch(
  () => venueManageModal.form.floorPlanId,
  async (next, old) => {
    if (next === old) return
    venueManageModal.form.floorPlanItemUid = ''
    if (!next) {
      floorPlanItemOptions.value = []
      return
    }
    await loadFloorPlanItemOptions(next)
  }
)

const debouncedSearch = ref(null)
const currentTimeTick = ref(Date.now())
const nowTimer = ref(null)

function normalizeTimeInput(value) {
  if (!value) return ''
  const parts = String(value).split(':')
  if (parts.length < 2) return ''
  const hour = Number(parts[0])
  const minute = Number(parts[1])
  if (!Number.isFinite(hour) || !Number.isFinite(minute)) return ''
  if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return ''
  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`
}

function validateTimeRange() {
  const openTime = normalizeTimeInput(venueManageModal.form.openTime)
  const closeTime = normalizeTimeInput(venueManageModal.form.closeTime)
  venueManageModal.form.openTime = openTime
  venueManageModal.form.closeTime = closeTime
  if (!openTime || !closeTime) {
    venueFormErrors.openTime = '请选择开放时间与关闭时间'
    return false
  }
  if (openTime >= closeTime) {
    venueFormErrors.openTime = '关闭时间必须晚于开放时间'
    return false
  }
  venueFormErrors.openTime = ''
  return true
}

function getTodayDateString() {
  const now = new Date(currentTimeTick.value)
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

/** 本地日历的 YYYY-MM-DD（不要用 toISOString().slice(0,10)，UTC 会在东八区出现「选 30 日发 29 日」） */
function formatLocalDateString(date) {
  if (!date || Number.isNaN(date.getTime())) return ''
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function getEarliestStartHourForToday() {
  const now = new Date(currentTimeTick.value)
  return now.getHours() + 1
}

function isPastStartSlot(slotValue) {
  if (!bookingModal.date || bookingModal.date !== getTodayDateString()) return false
  const [hour] = slotValue.split(':').map(Number)
  return hour < getEarliestStartHourForToday()
}

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
  return bookingModal.occupied.map((slot) => {
    const d = new Date(slot.slotStartTime)
    if (Number.isNaN(d.getTime())) return ''
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  }).filter(Boolean)
})

/** 某整点时段是否与已占用区间重叠（比仅比对 HH:mm 更可靠） */
function slotHourOverlapsOccupied(slotValue) {
  if (!bookingModal.date || !slotValue || !bookingModal.occupied?.length) return false
  const slotStart = new Date(`${bookingModal.date}T${slotValue}:00`).getTime()
  const slotEnd = slotStart + 60 * 60 * 1000
  if (!Number.isFinite(slotStart)) return false
  return bookingModal.occupied.some((occ) => {
    const os = new Date(occ.slotStartTime).getTime()
    const oe = new Date(occ.slotEndTime).getTime()
    return Number.isFinite(os) && Number.isFinite(oe) && slotStart < oe && slotEnd > os
  })
}

/** 预约区间 [start,end) 是否与任意已占用区间重叠 */
function bookingRangeOverlapsOccupied() {
  if (!bookingModal.date || !bookingModal.startTime || !bookingModal.endTime) return false
  const rangeStart = new Date(`${bookingModal.date}T${bookingModal.startTime}:00`).getTime()
  const rangeEnd = new Date(`${bookingModal.date}T${bookingModal.endTime}:00`).getTime()
  if (!Number.isFinite(rangeStart) || !Number.isFinite(rangeEnd)) return true
  if (rangeEnd <= rangeStart) return false
  return bookingModal.occupied.some((occ) => {
    const os = new Date(occ.slotStartTime).getTime()
    const oe = new Date(occ.slotEndTime).getTime()
    return Number.isFinite(os) && Number.isFinite(oe) && rangeStart < oe && rangeEnd > os
  })
}

/** 结束时间是否晚于开始时间（同日） */
function bookingEndAfterStart() {
  if (!bookingModal.date || !bookingModal.startTime || !bookingModal.endTime) return false
  const a = new Date(`${bookingModal.date}T${bookingModal.startTime}:00`).getTime()
  const b = new Date(`${bookingModal.date}T${bookingModal.endTime}:00`).getTime()
  return Number.isFinite(a) && Number.isFinite(b) && b > a
}

const timeSlotOptions = computed(() =>
    timeSlots.value.map((slot) => ({
      ...slot,
      disabled:
          unavailableSlots.value.includes(slot.value) ||
          slotHourOverlapsOccupied(slot.value) ||
          isPastStartSlot(slot.value)
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
    if (isPastStartSlot(slot.value)) {
      status = 'disabled'
    } else if (occupiedSet.has(slot.value) || slotHourOverlapsOccupied(slot.value)) {
      status = 'occupied'
    } else if (bookingModal.startTime && !bookingModal.endTime) {
      const startIndex = timeSlots.value.findIndex((item) => item.value === bookingModal.startTime)
      const currentIndex = timeSlots.value.findIndex((item) => item.value === slot.value)
      if (currentIndex === startIndex) {
        status = 'selected'
      }
    } else if (bookingModal.startTime && bookingModal.endTime) {
      const startIndex = timeSlots.value.findIndex((item) => item.value === bookingModal.startTime)
      let endIndex = timeSlots.value.findIndex((item) => item.value === bookingModal.endTime)
      const currentIndex = timeSlots.value.findIndex((item) => item.value === slot.value)
      if (endIndex === -1 && bookingModal.endTime === bookingModal.venue?.closeTime) {
        endIndex = timeSlots.value.length
      }
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
      value: formatLocalDateString(target)
    })
  }
  return dates
})

const hasBookingFloorPlan = computed(() => Boolean(bookingModal.floorPlanLoading || bookingModal.floorPlanModel))

function getSlotEndBoundary(slotIndex) {
  return timeSlots.value[slotIndex + 1]?.value || bookingModal.venue?.closeTime || timeSlots.value[slotIndex]?.value
}

function handleSlotSelect(slot) {
  if (slot.status === 'occupied' || isPastStartSlot(slot.value)) return

  const clickedIndex = timeSlots.value.findIndex((item) => item.value === slot.value)
  if (clickedIndex === -1) return

  // 第一次点击：设置开始时段；如果已有完整区间，则重新开始选择
  if (!bookingModal.startTime || bookingModal.endTime) {
    bookingModal.startTime = slot.value
    bookingModal.endTime = null
    return
  }

  // 第二次点击：设置结束时段（包含点击的这个时段）
  const startIndex = timeSlots.value.findIndex((item) => item.value === bookingModal.startTime)
  if (startIndex === -1) {
    bookingModal.startTime = slot.value
    bookingModal.endTime = null
    return
  }

  if (clickedIndex < startIndex) {
    bookingModal.startTime = slot.value
    bookingModal.endTime = null
    return
  }

  const hasOccupiedBetween = timeSlots.value
      .slice(startIndex, clickedIndex + 1)
      .some((item) => unavailableSlots.value.includes(item.value))

  if (hasOccupiedBetween) {
    pushToast('选中区间包含已占用时段，请重新选择', 'warning')
    bookingModal.startTime = slot.value
    bookingModal.endTime = null
    return
  }

  bookingModal.endTime = getSlotEndBoundary(clickedIndex)
}

function clearSlotSelection() {
  bookingModal.startTime = null
  bookingModal.endTime = null
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

function showViolationBlockedDialog(violationData = {}, fallbackMessage = '') {
  if (!Boolean(violationData?.isViolationUser)) return false

  const violationCountMonth = Number(violationData?.violationCountMonth || 0)
  const violationMonth = violationData?.violationMonth || '当月'
  const bookingBannedUntil = violationData?.bookingBannedUntil
  const bannedUntilText = bookingBannedUntil ? formatDisplayDateTime(bookingBannedUntil) : '请联系管理员'

  dialog.warning({
    title: '当前账号已违规，暂不可预约',
    content: `${fallbackMessage || '你当前处于违规限制状态，暂时无法发起预约。'}\n违规月份：${violationMonth}\n当月违规次数：${violationCountMonth}\n限制截止：${bannedUntilText}`,
    positiveText: '我知道了',
    type: 'warning'
  })
  return true
}

/** 预约业务失败（时段冲突、校验不通过、禁约等）：展示后端返回的 message，不用违规模板 */
function showBookingRejectedDialog(message) {
  const text = (message && String(message).trim()) || '预约失败，请稍后重试'
  dialog.warning({
    title: '预约失败',
    content: text,
    positiveText: '我知道了',
    type: 'warning'
  })
}

async function checkBookingEligibility() {
  try {
    const response = await api.get('/bookings/my/violation-status')
    if (response.code !== 200) {
      pushToast(response.message || '校验预约资格失败', 'error')
      return false
    }
    if (response.data?.isViolationUser) {
      showViolationBlockedDialog(response.data, '你已违规，无法预约场地。')
      return false
    }
    return true
  } catch (error) {
    const backendMessage = error?.response?.data?.message
    pushToast(backendMessage || '无法连接后端服务', 'error')
    return false
  }
}

async function openBookingModal(venue) {
  if (!venue?.id) return
  const canBook = await checkBookingEligibility()
  if (!canBook) return

  bookingModal.show = true
  resetBookingFloorPlanVenueStatuses()
  bookingModal.venue = venue
  bookingModal.date = bookingDates.value[0]?.value || null
  bookingModal.startTime = null
  bookingModal.endTime = null
  bookingModal.occupied = []
  bookingModal.floorPlanLoading = false
  bookingModal.floorPlanTitle = ''
  bookingModal.floorPlanModel = null
  if (venue.status != null && venue.status !== '') {
    bookingModal.floorPlanVenueStatusById[venue.id] = venue.status
  }
  await loadBookingFloorPlan(venue)
}

async function loadBookingFloorPlan(venue) {
  if (!venue?.floorPlanId) return
  bookingModal.floorPlanLoading = true
  try {
    const response = await api.get(`/floor-plans/${venue.floorPlanId}`)
    if (response.code !== 200) {
      throw new Error(response.message || '场地图加载失败')
    }
    const detail = response.data || {}
    bookingModal.floorPlanTitle = detail.title || ''
    bookingModal.floorPlanModel = parseFloorPlanContent(detail.contentJson)
    primeBookingFloorPlanVenueStatusesFromList()
    await hydrateBookingFloorPlanVenueStatuses()
  } catch (error) {
    pushToast(error?.message || '场地图加载失败', 'warning')
    bookingModal.floorPlanModel = null
  } finally {
    bookingModal.floorPlanLoading = false
  }
}

async function generateVenueCode() {
  if (isCodeGenerating.value) return
  isCodeGenerating.value = true
  try {
    const response = await api.get('/venues/code/generate')
    if (response.code !== 200 || !response.data?.code) {
      throw new Error(response.message || '生成场地编号失败')
    }
    venueManageModal.form.code = response.data.code
  } catch (error) {
    pushToast(error?.message || '生成场地编号失败', 'error')
  } finally {
    isCodeGenerating.value = false
  }
}

function resetVenueForm() {
  venueManageModal.form = {
    name: '',
    code: '',
    type: '',
    description: '',
    capacity: 0,
    price: 0,
    openTime: '08:00',
    closeTime: '22:00',
    openTimeDesc: '',
    remark: '',
    status: 'AVAILABLE',
    coverImageUrl: '',
    floorPlanId: null,
    floorPlanItemUid: ''
  }
  venueFormErrors.name = ''
  venueFormErrors.type = ''
  venueFormErrors.name = ''
  venueFormErrors.type = ''
  venueFormErrors.capacity = ''
  venueFormErrors.price = ''
  venueFormErrors.openTime = ''
  venueFormErrors.openTimeDesc = ''
  venueFormErrors.remark = ''
  venueFormErrors.openTimeDesc = ''
  venueFormErrors.remark = ''
  cleanupCoverObjectUrl()
  coverUploadFile.value = null
  coverUploadPreview.value = ''
  originalVenueCode.value = ''
}

function openCreateVenue() {
  venueManageModal.show = true
  venueManageModal.editingId = null
  resetVenueForm()
  loadFloorPlanOptions()
  generateVenueCode()
}

function openEditVenue(venue) {
  venueManageModal.show = true
  venueManageModal.editingId = venue.id
  venueFormErrors.capacity = ''
  venueFormErrors.price = ''
  venueFormErrors.openTime = ''
  venueManageModal.form = {
    name: venue.name || '',
    code: venue.code || '',
    type: venue.type || '',
    description: venue.description || '',
    capacity: venue.capacity || 0,
    price: venue.price || 0,
    openTime: venue.openTime || '08:00',
    closeTime: venue.closeTime || '22:00',
    openTimeDesc: venue.openTimeDesc || '',
    remark: venue.remark || '',
    status: venue.status || 'AVAILABLE',
    coverImageUrl: venue.coverImageUrl || '',
    floorPlanId: venue.floorPlanId ?? null,
    floorPlanItemUid: venue.floorPlanItemUid || ''
  }
  loadFloorPlanOptions()
  if (venue.floorPlanId) {
    loadFloorPlanItemOptions(venue.floorPlanId)
  } else {
    floorPlanItemOptions.value = []
  }
  originalVenueCode.value = venue.code || ''
  cleanupCoverObjectUrl()
  coverUploadFile.value = null
  coverUploadPreview.value = venueImageMap[venue.id] || ''
}

function cleanupCoverObjectUrl() {
  if (coverPreviewObjectUrl.value) {
    URL.revokeObjectURL(coverPreviewObjectUrl.value)
    coverPreviewObjectUrl.value = ''
  }
}

function closeVenueModal() {
  venueManageModal.show = false
  cleanupCoverObjectUrl()
  coverUploadFile.value = null
  coverUploadPreview.value = ''
}

function handleCoverFileChange(event) {
  const file = event?.target?.files?.[0]
  if (!file) return
  if (!file.type?.startsWith('image/')) {
    pushToast('请上传图片文件', 'warning')
    event.target.value = ''
    return
  }
  const maxSize = 3 * 1024 * 1024
  if (file.size > maxSize) {
    pushToast('图片大小不能超过 3MB', 'warning')
    event.target.value = ''
    return
  }

  cleanupCoverObjectUrl()
  coverUploadFile.value = file
  coverPreviewObjectUrl.value = URL.createObjectURL(file)
  coverUploadPreview.value = coverPreviewObjectUrl.value
}

function clearCoverFile() {
  cleanupCoverObjectUrl()
  coverUploadFile.value = null
  coverUploadPreview.value = ''
  venueManageModal.form.coverImageUrl = ''
}

async function uploadCoverIfNeeded() {
  if (!coverUploadFile.value) return venueManageModal.form.coverImageUrl || ''
  isCoverUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', coverUploadFile.value)
    formData.append('biz', 'venue')
    const response = await api.post('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    if (response.code !== 200) {
      throw new Error(response.message || '封面上传失败')
    }
    const uploadedPath = response.data?.urls?.[0]
    if (!uploadedPath) {
      throw new Error('封面上传失败：未返回文件路径')
    }
    return uploadedPath
  } finally {
    isCoverUploading.value = false
  }
}

async function submitVenue() {
  const name = venueManageModal.form.name?.trim() || ''
  const type = venueManageModal.form.type?.trim() || ''
  const openTimeDesc = venueManageModal.form.openTimeDesc?.trim() || ''
  const remark = venueManageModal.form.remark?.trim() || ''
  const capacity = Number(venueManageModal.form.capacity)
  const price = Number(venueManageModal.form.price)

  venueFormErrors.name = ''
  venueFormErrors.type = ''
  venueFormErrors.capacity = ''
  venueFormErrors.price = ''
  venueFormErrors.openTime = ''
  venueFormErrors.openTimeDesc = ''
  venueFormErrors.remark = ''

  if (!name) {
    venueFormErrors.name = '场地名称不能为空'
    return
  }
  if (venueManageModal.editingId) {
    venueManageModal.form.code = originalVenueCode.value || venueManageModal.form.code
  } else {
    venueManageModal.form.code = venueManageModal.form.code?.trim()
  }
  if (!venueManageModal.form.code) {
    pushToast('场地编号生成失败，请关闭弹窗后重试', 'warning')
    return
  }
  if (!type) {
    venueFormErrors.type = '场地类型不能为空'
    return
  }
  if (!Number.isInteger(capacity) || capacity <= 0) {
    venueFormErrors.capacity = '容量必须为正整数'
    return
  }
  if (!Number.isFinite(price) || price <= 0) {
    venueFormErrors.price = '价格必须为大于 0 的数字'
    return
  }
  if (!validateTimeRange()) {
    return
  }
  if (!openTimeDesc) {
    venueFormErrors.openTimeDesc = '开放时间说明不能为空'
    return
  }
  if (!remark) {
    venueFormErrors.remark = '备注不能为空'
    return
  }

  venueManageModal.form.name = name
  venueManageModal.form.type = type
  venueManageModal.form.openTimeDesc = openTimeDesc
  venueManageModal.form.remark = remark

  venueManageModal.submitting = true
  try {
    const coverImageUrl = await uploadCoverIfNeeded()
    const payload = {
      ...venueManageModal.form,
      ...(venueManageModal.editingId ? { id: venueManageModal.editingId } : {}),
      capacity,
      price,
      coverImageUrl,
      openTimeDesc,
      imageUrls: coverImageUrl ? [coverImageUrl] : [],
      remark
    }
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
    pushToast(error?.message || '保存失败，请稍后再试', 'error')
  } finally {
    venueManageModal.submitting = false
  }
}

async function quickChangeVenueStatus(venue, status) {
  if (!venue?.id) return
  try {
    const response = await api.put(`/venues/${venue.id}`, {
      ...venue,
      status,
      floorPlanId: venue.floorPlanId ?? null,
      floorPlanItemUid: venue.floorPlanItemUid ?? null
    })
    if (response.code !== 200) {
      pushToast(response.message || '状态切换失败', 'error')
      return
    }
    pushToast(`场地状态已切换为 ${getStatusText(status)}`, 'success')
    queryClient.invalidateQueries({ queryKey: ['venues'] })
  } catch (error) {
    const backendMessage = error?.response?.data?.message
    pushToast(backendMessage || '状态切换失败，请稍后再试', 'error')
  }
}

async function openVenueDetail(venue) {
  if (!venue?.id) return
  venueDetailModal.show = true
  venueDetailModal.loading = true
  venueDetailModal.data = null
  try {
    const response = await api.get(`/venues/${venue.id}`)
    if (response.code !== 200) {
      pushToast(response.message || '加载场地详情失败', 'error')
      return
    }
    venueDetailModal.data = response.data
  } catch (error) {
    const backendMessage = error?.response?.data?.message
    pushToast(backendMessage || '加载场地详情失败', 'error')
  } finally {
    venueDetailModal.loading = false
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
    const backendMessage = error?.response?.data?.message
    pushToast(backendMessage || '删除失败，请稍后再试', 'error')
  } finally {
    deleteModal.submitting = false
  }
}

function confirmDeleteVenue() {
  if (!deleteModal.venue?.id) return
  dialog.warning({
    title: '确认删除',
    content: `确认删除场地「${deleteModal.venue.name}」吗？`,
    positiveText: '确认',
    negativeText: '取消',
    onPositiveClick: () => {
      runDeleteVenue()
    }
  })
}

function closeBookingModal() {
  bookingModal.show = false
  resetBookingFloorPlanVenueStatuses()
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
    const backendMessage = error?.response?.data?.message
    pushToast(backendMessage || '无法连接后端服务', 'error')
  }
}

async function jumpToVenueBooking(venueId) {
  const id = Number(venueId)
  if (!Number.isFinite(id) || id <= 0) return
  if (bookingModal.venue?.id === id) return
  try {
    const response = await api.get(`/venues/${id}`)
    if (response.code !== 200) {
      pushToast(response.message || '加载场地失败', 'error')
      return
    }
    const venue = response.data
    if (!venue?.id) return
    if (venue.status != null && venue.status !== '') {
      bookingModal.floorPlanVenueStatusById[id] = venue.status
    }
    if (!isVenueStatusBookable(venue.status)) {
      pushToast(`该场地不可预约（${getStatusText(venue.status)}）`, 'warning')
      return
    }
    bookingModal.venue = venue
    bookingModal.date = bookingDates.value[0]?.value || null
    bookingModal.startTime = null
    bookingModal.endTime = null
    bookingModal.occupied = []
    bookingModal.floorPlanTitle = ''
    bookingModal.floorPlanModel = null
    await loadBookingFloorPlan(venue)
    await loadOccupiedSlots()
  } catch (error) {
    pushToast(error?.response?.data?.message || '加载场地失败', 'error')
  }
}

const unboundMapHintAt = ref(0)
function showUnboundMapHint(item) {
  const now = Date.now()
  if (now - (unboundMapHintAt.value || 0) < 900) return
  unboundMapHintAt.value = now
  const label = item?.label || '该区域'
  pushToast(`${label} 未绑定场地，无法跳转`, 'info')
}

function handleBookingMapItemClick(item) {
  if (!item?.venueId) {
    showUnboundMapHint(item)
    return
  }
  const vid = Number(item.venueId)
  const cached = bookingModal.floorPlanVenueStatusById[vid]
  if (cached != null && cached !== '' && !isVenueStatusBookable(cached)) {
    pushToast(`该场地不可预约（${getStatusText(cached)}）`, 'warning')
    return
  }
  jumpToVenueBooking(item.venueId)
}
function computeEndTimeOptions() {
  if (!bookingModal.startTime) return []
  const startIndex = timeSlots.value.findIndex((slot) => slot.value === bookingModal.startTime)
  if (startIndex === -1) return []

  const options = timeSlots.value
      .slice(startIndex + 1)
      .filter((slot) => !isPastStartSlot(slot.value))
      .map((slot) => ({
        label: slot.label,
        value: slot.value
      }))

  const closeTime = bookingModal.venue?.closeTime
  const [closeHour] = (closeTime || '').split(':').map(Number)
  const closeTimeAllowed =
      bookingModal.date !== getTodayDateString() || Number.isNaN(closeHour) || closeHour >= getEarliestStartHourForToday()

  if (closeTime && closeTimeAllowed && !options.some((item) => item.value === closeTime)) {
    options.push({ label: closeTime, value: closeTime })
  }

  return options
}

const endTimeOptions = computed(() => computeEndTimeOptions())

async function submitBooking() {
  if (!bookingModal.venue || !bookingModal.date || !bookingModal.startTime || !bookingModal.endTime) {
    pushToast('请选择预约日期与时间', 'warning')
    return
  }
  if (!bookingEndAfterStart()) {
    dialog.warning({
      title: '时间无效',
      content: '结束时间必须晚于开始时间，请重新选择时段。',
      positiveText: '确定',
      onPositiveClick: () => {
        clearSlotSelection()
      }
    })
    return
  }
  if (isPastStartSlot(bookingModal.startTime)) {
    pushToast('开始时间不能早于当前时间后 1 小时', 'warning')
    return
  }
  if (bookingRangeOverlapsOccupied()) {
    dialog.warning({
      title: '时段与已占用冲突',
      content: '所选时间段与已有预约重叠，请重新选择。',
      positiveText: '确定',
      onPositiveClick: () => {
        clearSlotSelection()
      }
    })
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
      await loadOccupiedSlots()
      const conflict =
          response.code === 409 || String(response.message || '').includes('已被预约')
      if (conflict) {
        clearSlotSelection()
      }
      if (showViolationBlockedDialog(response.data, response.message || '')) {
        return
      }
      showBookingRejectedDialog(response.message || '预约失败')
      return
    }
    pushToast('预约申请已提交', 'success')
    closeBookingModal()
    queryClient.invalidateQueries({ queryKey: ['myBookings'] })
  } catch (error) {
    await loadOccupiedSlots()
    const payload = error?.response?.data
    const violationData = payload?.data
    const violationMessage = payload?.message
    const conflict = payload?.code === 409 || String(violationMessage || '').includes('已被预约')
    if (conflict) {
      clearSlotSelection()
    }
    if (showViolationBlockedDialog(violationData, violationMessage || '')) {
      return
    }
    showBookingRejectedDialog(violationMessage || '无法连接后端服务')
  } finally {
    bookingModal.submitting = false
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
    () => bookingModal.show,
    (show) => {
      stopBookingOccupiedPoll()
      if (!show) return
      loadOccupiedSlots()
      bookingOccupiedPollTimer.value = setInterval(() => {
        if (bookingModal.show && bookingModal.venue?.id && bookingModal.date) {
          loadOccupiedSlots()
        }
      }, 12000)
    }
)

watch(
    () => [bookingModal.date, bookingModal.startTime],
    () => {
      if (!bookingModal.startTime || !bookingModal.endTime) return
      if (!bookingEndAfterStart()) {
        bookingModal.endTime = null
      }
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

function handleRouteQuickBooking() {
  if (window.location.search.includes('action=quick-booking')) {
    handleQuickBooking()
  }
}

watch(
  () => pagination.pageSize,
  (value, oldValue) => {
    if (value === oldValue) return
    if (!Number.isFinite(value) || value <= 0) {
      pagination.pageSize = oldValue || 6
      return
    }
    pagination.pageSize = Math.min(50, Math.max(1, Math.floor(value)))
    pagination.pageNo = 1
  }
)

onMounted(() => {
  searchDisabled.value = false
  window.addEventListener('quick-booking', handleQuickBooking)
  handleRouteQuickBooking()
  nowTimer.value = setInterval(() => {
    currentTimeTick.value = Date.now()
  }, 30000)
})

onUnmounted(() => {
  window.removeEventListener('quick-booking', handleQuickBooking)
  if (debouncedSearch.value) {
    clearTimeout(debouncedSearch.value)
  }
  if (nowTimer.value) {
    clearInterval(nowTimer.value)
  }
  stopBookingOccupiedPoll()
})
</script>

<template>
  <div class="venues-page">
    <section v-if="!isOwner" class="card venues-hero module-tier module-tier--summary">
      <div>
        <p class="section-kicker">场地预约</p>
        <h2>找到最合适的场地，在线预约并管理你的计划</h2>
        <p class="text-muted">
          根据开放时间与状态筛选，系统会实时校验 7 天内预约规则。
        </p>
      </div>
      <div class="hero-metrics">
        <div v-for="stat in stats" :key="stat.label">
          <span>{{ stat.label }}</span>
          <strong>{{ stat.value }}</strong>
        </div>
      </div>
    </section>

    <section v-if="isOwner" class="card venues-hero module-tier module-tier--summary">
      <div>
        <p class="section-kicker">场地管理</p>
        <h2>统一维护场地信息、状态与预约资源</h2>
        <p class="text-muted">通过筛选快速定位场地，进行编辑、状态调整与详情查看。</p>
      </div>
      <div class="hero-metrics">
        <div v-for="stat in ownerVenueStats" :key="stat.label">
          <span>{{ stat.label }}</span>
          <strong>{{ stat.value }}</strong>
        </div>
      </div>
    </section>

    <p v-if="venuesQuery.error" class="error-text">{{ venuesQuery.error.message }}</p>

    <section class="card venues-filters module-tier module-tier--filters">
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

    <section class="card venues-panel module-tier module-tier--data">
      <div v-if="venuesData.length || isVenuesFetching" class="venues-grid">
        <article v-for="venue in venuesData" :key="venue.id" class="venue-card">
          <div class="venue-card__media">
            <div class="venue-card__badge" :class="venue.status?.toLowerCase()">
              {{ getStatusText(venue.status, '未知状态') }}
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
              <p class="text-muted">编号 {{ venue.code || '-' }} · {{ venue.type }} · 容量 {{ venue.capacity || '-' }}</p>
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
              <NButton tertiary @click="openVenueDetail(venue)">查看详情</NButton>
              <NButton
                  v-if="!isOwner && venue.status === 'AVAILABLE'"
                  type="primary"
                  @click="openBookingModal(venue)"
              >
                预约时段
              </NButton>
              <NButton v-if="isOwner" tertiary @click="openEditVenue(venue)">编辑场地</NButton>
              <NButton v-if="isOwner" type="error" tertiary @click="openDeleteVenue(venue)">删除</NButton>
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
                  :type="venue.status === 'SUSPEND' ? 'info' : 'default'"
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
      </div>

      <div v-else class="empty-state venues-panel__empty">
        <h3>暂无场地</h3>
        <p>尝试调整筛选条件或稍后再来。</p>
      </div>

      <section class="pagination">
        <NButton tertiary @click="prevPage" :disabled="pagination.pageNo <= 1">上一页</NButton>
        <span>第 {{ pagination.pageNo }} 页 / 共 {{ Math.ceil(venuesTotal / pagination.pageSize) || 1 }} 页</span>
        <span style="display: inline-flex; align-items: center; gap: 8px;">
          <span>每页</span>
          <NInputNumber
            v-model:value="pagination.pageSize"
            :min="1"
            :max="50"
            :step="1"
            style="width: 100px;"
          />
          <span>条</span>
        </span>
        <NButton
            tertiary
            @click="nextPage"
            :disabled="pagination.pageNo * pagination.pageSize >= venuesTotal"
        >
          下一页
        </NButton>
      </section>
    </section>

    <NModal v-model:show="venueManageModal.show" preset="card" class="booking-modal" title="场地管理">
      <div class="booking-modal__section">
        <label>场地名称</label>
        <NInput v-model:value="venueManageModal.form.name" placeholder="请输入场地名称（例如：一号篮球馆）" :status="venueFormErrors.name ? 'error' : undefined" />
        <p v-if="venueFormErrors.name" class="error-text">{{ venueFormErrors.name }}</p>
      </div>
      <div class="booking-modal__section"><label>场地编号</label><NInput v-model:value="venueManageModal.form.code" placeholder="系统自动生成" readonly :disabled="isCodeGenerating || !!venueManageModal.editingId" /></div>
      <div class="booking-modal__section">
        <label>场地类型</label>
        <NInput v-model:value="venueManageModal.form.type" placeholder="请输入场地类型（例如：篮球场）" :status="venueFormErrors.type ? 'error' : undefined" />
        <p v-if="venueFormErrors.type" class="error-text">{{ venueFormErrors.type }}</p>
      </div>
      <div class="booking-modal__section"><label>场地描述</label><NInput v-model:value="venueManageModal.form.description" type="textarea" placeholder="请输入场地描述（选填）" /></div>
      <div class="booking-modal__section two-col">
        <div>
          <label>容量（场地最多支持人数）</label>
          <NInput v-model:value="venueManageModal.form.capacity" placeholder="请输入大于 0 的人数" :status="venueFormErrors.capacity ? 'error' : undefined" />
          <p v-if="venueFormErrors.capacity" class="error-text">{{ venueFormErrors.capacity }}</p>
        </div>
        <div>
          <label>价格（每小时）</label>
          <NInput v-model:value="venueManageModal.form.price" placeholder="请输入大于 0 的价格（元）" :status="venueFormErrors.price ? 'error' : undefined" />
          <p v-if="venueFormErrors.price" class="error-text">{{ venueFormErrors.price }}</p>
        </div>
      </div>
      <div class="booking-modal__section two-col">
        <div>
          <label>开放时间</label>
          <NInput v-model:value="venueManageModal.form.openTime" placeholder="例如：08:00" />
        </div>
        <div>
          <label>关闭时间</label>
          <NInput v-model:value="venueManageModal.form.closeTime" placeholder="例如：22:00" />
        </div>
      </div>
      <p class="hint">请输入当天时间，格式为 HH:mm，且关闭时间必须晚于开放时间</p>
      <p v-if="venueFormErrors.openTime" class="error-text">{{ venueFormErrors.openTime }}</p>
      <div class="booking-modal__section">
        <label>开放时间说明（openTimeDesc）</label>
        <NInput v-model:value="venueManageModal.form.openTimeDesc" placeholder="例如：工作日 08:00-22:00，周末 09:00-21:00" :status="venueFormErrors.openTimeDesc ? 'error' : undefined" />
        <p v-if="venueFormErrors.openTimeDesc" class="error-text">{{ venueFormErrors.openTimeDesc }}</p>
      </div>
      <div class="booking-modal__section">
        <label>备注（remark）</label>
        <NInput v-model:value="venueManageModal.form.remark" type="textarea" placeholder="请输入备注信息" :status="venueFormErrors.remark ? 'error' : undefined" />
        <p v-if="venueFormErrors.remark" class="error-text">{{ venueFormErrors.remark }}</p>
      </div>

      <div class="booking-modal__section">
        <label>场地图绑定（选填）</label>
        <NTabs type="line" animated>
          <NTabPane name="floorPlan" tab="选择场地图">
            <NSelect
              v-model:value="venueManageModal.form.floorPlanId"
              clearable
              :loading="floorPlanLoading"
              :options="floorPlanOptions"
              placeholder="选择场地图标题（可选）"
            />
            <p class="hint" style="margin-top: 10px;">
              先选择场地图标题，才可在下一个选项卡选择区域 item。
            </p>
          </NTabPane>
          <NTabPane name="floorPlanItem" tab="选择区域">
            <NSelect
              v-model:value="venueManageModal.form.floorPlanItemUid"
              clearable
              :loading="floorPlanItemLoading"
              :disabled="!venueManageModal.form.floorPlanId"
              :options="floorPlanItemOptions"
              placeholder="选择区域（仅未绑定 + 当前场地已绑定）"
            />
            <p class="hint" style="margin-top: 10px;">
              未选择区域将视为不绑定/解绑；选择区域后将与当前场地一一绑定。
            </p>
          </NTabPane>
        </NTabs>
      </div>
      <div class="booking-modal__section"><label>状态</label><NSelect v-model:value="venueManageModal.form.status" :options="venueStatusOptions" /></div>
      <div class="booking-modal__section">
        <label>封面图片</label>
        <div class="upload-control">
          <label class="upload-trigger">
            <input class="upload-input" type="file" accept="image/*" @change="handleCoverFileChange" />
            <span>{{ coverUploadFile ? '重新选择图片' : '选择图片' }}</span>
          </label>
          <span v-if="coverUploadFile" class="upload-filename">{{ coverUploadFile.name }}</span>
        </div>
        <div v-if="coverUploadPreview" class="venue-cover-preview">
          <img :src="coverUploadPreview" alt="cover preview" />
          <div class="venue-cover-actions">
            <NButton size="small" tertiary @click="clearCoverFile">移除图片</NButton>
          </div>
        </div>
      </div>
      <div class="booking-modal__actions">
        <NButton :disabled="venueManageModal.submitting || isCoverUploading" @click="closeVenueModal">取消</NButton>
        <NButton type="primary" :loading="venueManageModal.submitting || isCoverUploading" :disabled="venueManageModal.submitting || isCoverUploading" @click="submitVenue">
          {{ isCoverUploading ? '上传中...' : venueManageModal.editingId ? '保存修改' : '确认新增' }}
        </NButton>
      </div>
    </NModal>

    <NModal v-model:show="deleteModal.show" preset="card" class="booking-modal" title="删除场地">
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

    <NModal v-model:show="venueDetailModal.show" preset="card" class="booking-modal" title="场地详情">
      <div v-if="venueDetailModal.loading" class="text-muted">加载中...</div>
      <template v-else-if="venueDetailModal.data">
        <div class="venue-detail-grid">
          <div><span>场地ID</span><strong>{{ venueDetailModal.data.id }}</strong></div>
          <div><span>场地名称</span><strong>{{ venueDetailModal.data.name || '—' }}</strong></div>
          <div><span>场地编号</span><strong>{{ venueDetailModal.data.code || '—' }}</strong></div>
          <div><span>场地类型</span><strong>{{ venueDetailModal.data.type || '—' }}</strong></div>
          <div><span>状态</span><strong>{{ getStatusText(venueDetailModal.data.status, '未知状态') }}</strong></div>
          <div><span>容量</span><strong>{{ venueDetailModal.data.capacity ?? '—' }}</strong></div>
          <div><span>价格</span><strong>{{ venueDetailModal.data.price ? `¥${venueDetailModal.data.price}/小时` : '咨询' }}</strong></div>
          <div><span>开放时间</span><strong>{{ venueDetailModal.data.openTime || '—' }}</strong></div>
          <div><span>关闭时间</span><strong>{{ venueDetailModal.data.closeTime || '—' }}</strong></div>
          <div><span>开放时间说明</span><strong>{{ venueDetailModal.data.openTimeDesc || '—' }}</strong></div>
          <div class="full"><span>描述</span><strong>{{ venueDetailModal.data.description || '暂无描述' }}</strong></div>
          <div class="full"><span>备注</span><strong>{{ venueDetailModal.data.remark || '—' }}</strong></div>
          <div class="full" v-if="venueImageMap[venueDetailModal.data.id]">
            <span>封面</span>
            <img class="venue-detail-image" :src="venueImageMap[venueDetailModal.data.id]" alt="venue detail cover" />
          </div>
        </div>
      </template>
      <div class="booking-modal__actions">
        <NButton type="primary" @click="venueDetailModal.show = false">关闭</NButton>
      </div>
    </NModal>

    <NModal
      v-model:show="bookingModal.show"
      preset="card"
      class="booking-modal booking-modal--booking"
      :class="{ 'booking-modal--with-map': hasBookingFloorPlan }"
      :style="{ width: hasBookingFloorPlan ? 'min(1360px, 96vw)' : 'min(540px, 92vw)' }"
      :mask-closable="false"
      title="预约时段"
    >
      <div class="booking-modal__layout" :class="{ 'booking-modal__layout--with-map': hasBookingFloorPlan }">
        <div class="booking-modal__main">
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
                <span class="legend disabled">不可用</span>
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
                  {{
                    slot.status === 'available'
                        ? '可预约'
                        : slot.status === 'selected'
                            ? '已选择'
                            : slot.status === 'occupied'
                                ? '已占用'
                                : '不可用'
                  }}
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
        </div>

        <aside v-if="bookingModal.floorPlanLoading || bookingModal.floorPlanModel" class="booking-modal__side">
          <h4>场地图预览</h4>
          <p class="text-muted" style="margin-bottom: 10px;">
            {{ bookingModal.floorPlanTitle || '该场地已绑定场地图区域' }}
          </p>
          <div v-if="bookingModal.floorPlanLoading" class="text-muted">场地图加载中...</div>
          <FloorPlanCanvasPreview
            v-else-if="bookingModal.floorPlanModel"
            :model="bookingModal.floorPlanModel"
            :venue-status-by-id="bookingModal.floorPlanVenueStatusById"
            :highlight-item-uid="bookingModal.venue?.floorPlanItemUid || ''"
            :max-height="620"
            interactive
            @item-click="handleBookingMapItemClick"
          />
        </aside>
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
