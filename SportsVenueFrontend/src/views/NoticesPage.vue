<script setup>
import { computed, reactive, ref } from 'vue'
import { useQuery, useQueryClient } from '@tanstack/vue-query'
import { NButton, NCard, NInput, NModal, NSelect, NTag, useDialog } from 'naive-ui'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import api from '../services/api'
import { useToast } from '../composables/useToast'
import { useAuthStore } from '../stores/auth'
import { getStatusText } from '../constants/statusMap'
import { formatDisplayDateTime } from '../utils/dateFormat'

const MAX_TITLE_LENGTH = 80
const MAX_CONTENT_TEXT_LENGTH = 5000

const { pushToast } = useToast()
const queryClient = useQueryClient()
const authStore = useAuthStore()
const dialog = useDialog()

const isOwner = computed(() => authStore.role === 'OWNER')
const pageMode = ref('list')

const statusOptionsManage = [
  { label: '全部状态', value: '' },
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已下线', value: 'OFFLINE' }
]

const statusOptionsEdit = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已下线', value: 'OFFLINE' }
]

const filters = reactive({
  keyword: '',
  status: ''
})

const pagination = reactive({
  pageNo: 1,
  pageSize: 10
})

const manageQuery = useQuery({
  queryKey: computed(() => ['noticesManage', filters.keyword, filters.status, pagination.pageNo, pagination.pageSize]),
  queryFn: async () => {
    const endpoint = isOwner.value ? '/notices/manage' : '/notices'
    const params = {
      keyword: filters.keyword || undefined,
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize
    }
    if (isOwner.value) params.status = filters.status || undefined

    const response = await api.get(endpoint, { params })
    if (response.code !== 200) throw new Error(response.message || '公告列表加载失败')
    return response.data || { records: [], total: 0 }
  },
  keepPreviousData: true,
  staleTime: 30000,
  refetchOnWindowFocus: false,
  refetchOnReconnect: false,
  retry: false
})

const isQueryLoading = computed(() => Boolean(manageQuery.isFetching?.value ?? manageQuery.isFetching))
const notices = computed(() => manageQuery.data?.records || manageQuery.data?.value?.records || [])
const total = computed(() => manageQuery.data?.total || manageQuery.data?.value?.total || 0)

const detailModal = reactive({
  show: false,
  loading: false,
  data: null
})

const editForm = reactive({
  id: null,
  title: '',
  content: '<p></p>',
  status: 'DRAFT'
})

const saving = ref(false)
const safeDetailHtml = computed(() => sanitizeNoticeHtml(detailModal.data?.content || ''))

function sanitizeNoticeHtml(html) {
  if (!html) return ''
  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')
  const allowedTags = new Set(['p', 'br', 'strong', 'b', 'em', 'i', 'u', 's', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'ul', 'ol', 'li', 'blockquote', 'a', 'img', 'span', 'div', 'pre', 'code'])
  const walker = doc.createTreeWalker(doc.body, NodeFilter.SHOW_ELEMENT)
  const removeNodes = []

  while (walker.nextNode()) {
    const node = walker.currentNode
    const tagName = node.tagName.toLowerCase()
    if (!allowedTags.has(tagName)) {
      removeNodes.push(node)
      continue
    }
    ;[...node.attributes].forEach((attr) => {
      const name = attr.name.toLowerCase()
      const value = (attr.value || '').toLowerCase().trim()
      if (name.startsWith('on') || name === 'style') node.removeAttribute(attr.name)
      if ((name === 'href' || name === 'src') && !(value.startsWith('http://') || value.startsWith('https://') || value.startsWith('/') || value.startsWith('data:image/'))) {
        node.removeAttribute(attr.name)
      }
    })
    if (tagName === 'a') {
      node.setAttribute('rel', 'noopener noreferrer')
      node.setAttribute('target', '_blank')
    }
  }

  removeNodes.forEach((node) => {
    const parent = node.parentNode
    if (!parent) return
    while (node.firstChild) parent.insertBefore(node.firstChild, node)
    parent.removeChild(node)
  })

  return doc.body.innerHTML
}

function extractPlainText(html) {
  if (!html) return ''
  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')
  return (doc.body.textContent || '').replace(/\s+/g, ' ').trim()
}

function handleSearch() {
  pagination.pageNo = 1
  manageQuery.refetch()
}

function resetFilters() {
  filters.keyword = ''
  filters.status = ''
  handleSearch()
}


function statusTagType(status) {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'DRAFT') return 'warning'
  return 'default'
}

async function openDetail(item) {
  detailModal.show = true
  detailModal.loading = true
  detailModal.data = null
  try {
    const endpoint = isOwner.value ? `/notices/manage/${item.id}` : `/notices/${item.id}`
    const response = await api.get(endpoint)
    if (response.code !== 200) {
      pushToast(response.message || '加载公告详情失败', 'error')
      return
    }
    detailModal.data = response.data
  } catch {
    pushToast('加载公告详情失败', 'error')
  } finally {
    detailModal.loading = false
  }
}

function onUserNoticeCardActivate(item) {
  if (isOwner.value) return
  openDetail(item)
}

function openCreatePage() {
  editForm.id = null
  editForm.title = ''
  editForm.content = '<p></p>'
  editForm.status = 'DRAFT'
  pageMode.value = 'edit'
}

function openEditPage(item) {
  editForm.id = item.id
  editForm.title = item.title || ''
  editForm.content = item.content || '<p></p>'
  editForm.status = item.status || 'DRAFT'
  pageMode.value = 'edit'
}

function backToList() {
  pageMode.value = 'list'
}

function validateNoticeForm() {
  const title = editForm.title?.trim() || ''
  if (!title) return pushToast('请输入公告标题', 'warning'), false
  if (title.length > MAX_TITLE_LENGTH) return pushToast(`标题不能超过 ${MAX_TITLE_LENGTH} 个字符`, 'warning'), false
  const contentText = extractPlainText(editForm.content)
  if (!contentText) return pushToast('请输入公告正文内容', 'warning'), false
  if (contentText.length > MAX_CONTENT_TEXT_LENGTH) return pushToast(`正文纯文本不能超过 ${MAX_CONTENT_TEXT_LENGTH} 个字符`, 'warning'), false
  return true
}

async function submitNotice() {
  if (!validateNoticeForm()) return
  saving.value = true
  try {
    const payload = {
      title: editForm.title.trim(),
      content: sanitizeNoticeHtml(editForm.content),
      status: editForm.status
    }
    const response = editForm.id
      ? await api.put(`/notices/${editForm.id}`, { id: editForm.id, ...payload })
      : await api.post('/notices', payload)

    if (response.code !== 200) {
      pushToast(response.message || '保存公告失败', 'error')
      return
    }
    pushToast(editForm.id ? '公告更新成功' : '公告创建成功', 'success')
    pageMode.value = 'list'
    queryClient.invalidateQueries({ queryKey: ['noticesManage'] })
  } catch (error) {
    const backendMessage = error?.response?.data?.message
    pushToast(backendMessage || '保存失败，请稍后重试', 'error')
  } finally {
    saving.value = false
  }
}

function confirmSwitchStatus(item, status) {
  const actionText = status === 'PUBLISHED' ? '发布' : '下线'
  dialog.warning({
    title: `确认${actionText}`,
    content: `确认要${actionText}公告「${item.title}」吗？`,
    positiveText: `确认${actionText}`,
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const response = await api.put(`/notices/${item.id}/status`, { status })
        if (response.code !== 200) return pushToast(response.message || '状态切换失败', 'error')
        pushToast(status === 'PUBLISHED' ? '公告已发布' : '公告已下线', 'success')
        queryClient.invalidateQueries({ queryKey: ['noticesManage'] })
      } catch (error) {
        const backendMessage = error?.response?.data?.message
        pushToast(backendMessage || '状态切换失败，请稍后重试', 'error')
      }
    }
  })
}

function confirmRemoveNotice(item) {
  dialog.error({
    title: '确认删除',
    content: `删除后公告不可恢复，确认删除「${item.title}」吗？`,
    positiveText: '确认删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const response = await api.delete(`/notices/${item.id}`)
        if (response.code !== 200) return pushToast(response.message || '删除公告失败', 'error')
        pushToast('公告已删除', 'success')
        queryClient.invalidateQueries({ queryKey: ['noticesManage'] })
      } catch (error) {
        const backendMessage = error?.response?.data?.message
        pushToast(backendMessage || '删除失败，请稍后重试', 'error')
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

function formatDateTime(value) {
  return formatDisplayDateTime(value)
}
</script>

<template>
  <div class="admin-users-page">
    <template v-if="pageMode === 'list'">
      <section class="card profile-hero">
        <div>
          <p class="section-kicker">公告模块</p>
          <h2>{{ isOwner ? '公告中心（管理 + 用户侧展示联动）' : '公告中心' }}</h2>
          <p class="text-muted">{{ isOwner ? '支持草稿、发布、下线、删除与详情查看。' : '查看已发布公告列表与详情。' }}</p>
        </div>
        <div class="hero-metrics"><div><span>当前页公告</span><strong>{{ notices.length }}</strong></div><div><span>总公告数</span><strong>{{ total }}</strong></div></div>
      </section>

      <section class="card borrow-filters">
        <div class="field"><label>关键词</label><NInput v-model:value="filters.keyword" placeholder="按标题搜索" /></div>
        <div class="field" v-if="isOwner"><label>状态</label><NSelect v-model:value="filters.status" :options="statusOptionsManage" /></div>
        <div class="borrow-filters__actions">
          <NButton type="primary" :loading="isQueryLoading" @click="handleSearch">查询</NButton>
          <NButton tertiary @click="resetFilters">重置</NButton>
          <NButton v-if="isOwner" type="info" @click="openCreatePage">新建公告</NButton>
        </div>
      </section>

      <section class="borrow-panel__list">
        <div
          v-for="item in notices"
          :key="item.id"
          class="notice-list-item"
          :class="{ 'notice-list-item--user-clickable': !isOwner }"
          :role="isOwner ? undefined : 'button'"
          :tabindex="isOwner ? undefined : 0"
          @click="onUserNoticeCardActivate(item)"
          @keyup.enter="onUserNoticeCardActivate(item)"
        >
          <NCard size="small" class="borrow-record">
            <template #header>
              <div class="borrow-record__header">
                <div>
                  <strong>{{ item.title }}</strong>
                  <p class="text-muted">发布时间：{{ formatDisplayDateTime(item.publishTime) }}</p>
                </div>
                <NTag v-if="isOwner" :type="statusTagType(item.status)">{{ getStatusText(item.status) }}</NTag>
              </div>
            </template>
            <div v-if="isOwner" class="borrow-record__body">
              <div><span>创建时间</span><strong>{{ formatDateTime(item.createTime) }}</strong></div>
              <div><span>更新时间</span><strong>{{ formatDateTime(item.updateTime) }}</strong></div>
              <div><span>创建人</span><strong>{{ item.createByUsername ?? '—' }}</strong></div>
              <div><span>更新人</span><strong>{{ item.updateByUsername ?? '—' }}</strong></div>
            </div>
            <div v-if="isOwner" class="borrow-record__actions" style="gap: 8px; justify-content: flex-end; flex-wrap: wrap;">
              <NButton size="small" tertiary @click.stop="openDetail(item)">查看详情</NButton>
              <NButton size="small" type="primary" tertiary @click.stop="openEditPage(item)">编辑</NButton>
              <NButton
                size="small"
                type="success"
                tertiary
                :disabled="item.status === 'PUBLISHED'"
                @click.stop="confirmSwitchStatus(item, 'PUBLISHED')"
              >
                发布
              </NButton>
              <NButton
                size="small"
                tertiary
                :disabled="item.status === 'OFFLINE'"
                @click.stop="confirmSwitchStatus(item, 'OFFLINE')"
              >
                下线
              </NButton>
              <NButton size="small" type="error" tertiary @click.stop="confirmRemoveNotice(item)">删除</NButton>
            </div>
          </NCard>
        </div>
        <div v-if="!notices.length && !isQueryLoading" class="empty-state"><h3>暂无公告数据</h3><p>请调整筛选条件后重试。</p></div>
      </section>

      <section class="pagination">
        <NButton tertiary @click="prevPage" :disabled="pagination.pageNo <= 1">上一页</NButton>
        <span>第 {{ pagination.pageNo }} 页 / 共 {{ Math.ceil(total / pagination.pageSize) || 1 }} 页</span>
        <NButton tertiary @click="nextPage" :disabled="pagination.pageNo * pagination.pageSize >= total">下一页</NButton>
      </section>
    </template>

    <template v-else>
      <section class="card">
        <div style="display:flex;align-items:center;gap:12px;margin-bottom:16px;">
          <NButton tertiary @click="backToList">← 返回</NButton>
          <h3 style="margin:0;">{{ editForm.id ? '编辑公告' : '新建公告' }}</h3>
        </div>

        <div class="booking-modal__section">
          <label>标题 *（{{ editForm.title.length }}/{{ MAX_TITLE_LENGTH }}）</label>
          <NInput v-model:value="editForm.title" :maxlength="MAX_TITLE_LENGTH" show-count />
        </div>

        <div class="booking-modal__section" style="margin-top:16px;">
          <label>正文富文本 *（纯文本上限 {{ MAX_CONTENT_TEXT_LENGTH }} 字）</label>
          <div class="notice-editor-wrapper" style="margin-top:8px;">
            <QuillEditor v-model:content="editForm.content" content-type="html" theme="snow" toolbar="full" />
          </div>
        </div>

        <div class="booking-modal__section" style="margin-top:16px;">
          <label>状态</label>
          <NSelect v-model:value="editForm.status" :options="statusOptionsEdit" />
        </div>

        <div class="booking-modal__actions" style="margin-top:20px;">
          <NButton @click="backToList">取消</NButton>
          <NButton type="primary" :loading="saving" @click="submitNotice">保存</NButton>
        </div>
      </section>
    </template>

    <NModal v-model:show="detailModal.show" preset="card" title="公告详情" class="booking-modal notice-detail-modal-host">
      <div v-if="detailModal.loading" class="text-muted">加载中...</div>
      <div v-else-if="detailModal.data" class="notice-detail-modal">
        <h2 class="notice-detail-modal__title">{{ detailModal.data.title }}</h2>

        <div class="notice-detail-modal__body">
          <div class="notice-detail-modal__body-label">正文</div>
          <div class="notice-detail-modal__content notice-html-preview" v-html="safeDetailHtml"></div>
        </div>

        <div
          class="notice-detail-modal__meta"
          :class="{ 'notice-detail-modal__meta--owner': isOwner }"
        >
          <template v-if="isOwner">
            <div class="notice-detail-meta-row">
              <span>状态</span>
              <strong>{{ getStatusText(detailModal.data.status) }}</strong>
            </div>
            <div class="notice-detail-meta-row">
              <span>发布时间</span>
              <strong>{{ formatDateTime(detailModal.data.publishTime) || '—' }}</strong>
            </div>
            <div class="notice-detail-meta-row">
              <span>创建时间</span>
              <strong>{{ formatDateTime(detailModal.data.createTime) || '—' }}</strong>
            </div>
            <div class="notice-detail-meta-row">
              <span>更新时间</span>
              <strong>{{ formatDateTime(detailModal.data.updateTime) || '—' }}</strong>
            </div>
            <div class="notice-detail-meta-row">
              <span>创建人</span>
              <strong>{{ detailModal.data.createByUsername ?? '—' }}</strong>
            </div>
            <div class="notice-detail-meta-row">
              <span>更新人</span>
              <strong>{{ detailModal.data.updateByUsername ?? '—' }}</strong>
            </div>
          </template>
          <template v-else>
            <div class="notice-detail-meta-row">
              <span>发布时间</span>
              <strong>{{ formatDateTime(detailModal.data.publishTime) || '—' }}</strong>
            </div>
          </template>
        </div>
      </div>
      <div class="booking-modal__actions"><NButton type="primary" @click="detailModal.show = false">关闭</NButton></div>
    </NModal>
  </div>
</template>
