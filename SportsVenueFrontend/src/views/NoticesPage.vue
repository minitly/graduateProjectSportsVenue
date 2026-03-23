<script setup>
import { computed, onBeforeUnmount, reactive, shallowRef } from 'vue'
import { useQuery, useQueryClient } from '@tanstack/vue-query'
import { NButton, NCard, NInput, NModal, NSelect, NTag, useDialog } from 'naive-ui'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import api from '../services/api'
import { useToast } from '../composables/useToast'
import { useAuthStore } from '../stores/auth'

const MAX_TITLE_LENGTH = 80
const MAX_CONTENT_TEXT_LENGTH = 5000
const MAX_IMAGE_SIZE_BYTES = 2 * 1024 * 1024
const ALLOWED_IMAGE_MIME_TYPES = ['image/jpeg', 'image/png', 'image/webp']
const ALLOWED_IMAGE_EXTENSIONS = ['jpg', 'jpeg', 'png', 'webp']

const { pushToast } = useToast()
const queryClient = useQueryClient()
const authStore = useAuthStore()
const dialog = useDialog()

const isOwner = computed(() => authStore.role === 'OWNER')
const editorRef = shallowRef()

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

const toolbarConfig = {
  excludeKeys: ['group-video']
}

function getFileExtension(fileName) {
  const suffix = fileName?.split('.')?.pop()?.toLowerCase()
  return suffix || ''
}

function validateImageFileBeforeUpload(file) {
  const ext = getFileExtension(file?.name)
  const mimeType = file?.type || ''
  const isMimeAllowed = ALLOWED_IMAGE_MIME_TYPES.includes(mimeType)
  const isExtAllowed = ALLOWED_IMAGE_EXTENSIONS.includes(ext)

  if (!isMimeAllowed && !isExtAllowed) {
    pushToast('仅支持上传 JPG / PNG / WEBP 格式图片', 'warning')
    return false
  }

  if ((file?.size || 0) > MAX_IMAGE_SIZE_BYTES) {
    pushToast('图片大小不能超过 2MB', 'warning')
    return false
  }

  return true
}

const editorConfig = {
  placeholder: '请输入公告正文内容（支持富文本）',
  MENU_CONF: {
    uploadImage: {
      customUpload: async (file, insertFn) => {
        if (!validateImageFileBeforeUpload(file)) {
          return
        }

        try {
          const formData = new FormData()
          formData.append('file', file)
          formData.append('biz', 'notice')

          const response = await api.post('/files/upload', formData, {
            headers: {
              'Content-Type': 'multipart/form-data'
            }
          })

          if (response.code !== 200) {
            pushToast(response.message || '图片上传失败', 'error')
            return
          }

          const uploadedPath = response.data?.urls?.[0]
          if (!uploadedPath) {
            pushToast('上传成功但未返回图片地址', 'error')
            return
          }

          const imageUrl = uploadedPath.startsWith('http')
            ? uploadedPath
            : `http://localhost:9999/sportsVenue/files/serve?path=${encodeURIComponent(uploadedPath)}`

          insertFn(imageUrl, file.name, imageUrl)
          pushToast('图片上传成功', 'success')
        } catch (error) {
          pushToast('图片上传失败，请稍后重试', 'error')
        }
      }
    }
  }
}

const manageQuery = useQuery({
  queryKey: computed(() => ['noticesManage', filters.keyword, filters.status, pagination.pageNo, pagination.pageSize]),
  queryFn: async () => {
    const endpoint = isOwner.value ? '/notices/manage' : '/notices'
    const params = {
      keyword: filters.keyword || undefined,
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize
    }
    if (isOwner.value) {
      params.status = filters.status || undefined
    }
    const response = await api.get(endpoint, { params })
    if (response.code !== 200) {
      throw new Error(response.message || '公告列表加载失败')
    }
    return response.data || { records: [], total: 0 }
  },
  keepPreviousData: true,
  staleTime: 30000
})

const notices = computed(() => manageQuery.data?.records || manageQuery.data?.value?.records || [])
const total = computed(() => manageQuery.data?.total || manageQuery.data?.value?.total || 0)

const detailModal = reactive({
  show: false,
  loading: false,
  data: null
})

const editModal = reactive({
  show: false,
  submitting: false,
  form: {
    id: null,
    title: '',
    content: '',
    status: 'DRAFT'
  }
})

const safeDetailHtml = computed(() => sanitizeNoticeHtml(detailModal.data?.content || ''))

function sanitizeNoticeHtml(html) {
  if (!html) return ''
  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')

  const allowedTags = new Set([
    'p', 'br', 'strong', 'b', 'em', 'i', 'u', 's',
    'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
    'ul', 'ol', 'li', 'blockquote',
    'a', 'img', 'span', 'div', 'pre', 'code'
  ])

  const allowedAttrs = {
    a: ['href', 'target', 'rel'],
    img: ['src', 'alt', 'title'],
    p: ['style'],
    span: ['style'],
    div: ['style'],
    pre: ['style'],
    code: ['style'],
    h1: ['style'],
    h2: ['style'],
    h3: ['style'],
    h4: ['style'],
    h5: ['style'],
    h6: ['style'],
    li: ['style'],
    ul: ['style'],
    ol: ['style'],
    blockquote: ['style']
  }

  const walker = doc.createTreeWalker(doc.body, NodeFilter.SHOW_ELEMENT)
  const removeNodes = []

  while (walker.nextNode()) {
    const node = walker.currentNode
    const tagName = node.tagName.toLowerCase()

    if (!allowedTags.has(tagName)) {
      removeNodes.push(node)
      continue
    }

    const attrs = [...node.attributes]
    for (const attr of attrs) {
      const attrName = attr.name.toLowerCase()
      const attrValue = attr.value || ''

      if (attrName.startsWith('on')) {
        node.removeAttribute(attr.name)
        continue
      }

      const tagAllowedAttrs = allowedAttrs[tagName] || []
      if (!tagAllowedAttrs.includes(attrName)) {
        node.removeAttribute(attr.name)
        continue
      }

      if (attrName === 'href' || attrName === 'src') {
        const lower = attrValue.trim().toLowerCase()
        const isSafe =
          lower.startsWith('http://') ||
          lower.startsWith('https://') ||
          lower.startsWith('/') ||
          lower.startsWith('data:image/')
        if (!isSafe) {
          node.removeAttribute(attr.name)
        }
      }

      if (attrName === 'style') {
        node.removeAttribute(attr.name)
      }
    }

    if (tagName === 'a') {
      node.setAttribute('rel', 'noopener noreferrer')
      node.setAttribute('target', '_blank')
    }
  }

  removeNodes.forEach((node) => {
    const parent = node.parentNode
    if (!parent) return
    while (node.firstChild) {
      parent.insertBefore(node.firstChild, node)
    }
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

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function statusTagType(status) {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'DRAFT') return 'warning'
  if (status === 'OFFLINE') return 'default'
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
  } catch (error) {
    pushToast('加载公告详情失败', 'error')
  } finally {
    detailModal.loading = false
  }
}

function closeDetail() {
  detailModal.show = false
}

function resetEditForm() {
  editModal.form = {
    id: null,
    title: '',
    content: '<p></p>',
    status: 'DRAFT'
  }
}

function openCreate() {
  resetEditForm()
  editModal.show = true
}

function openEdit(item) {
  editModal.form = {
    id: item.id,
    title: item.title || '',
    content: item.content || '<p></p>',
    status: item.status || 'DRAFT'
  }
  editModal.show = true
}

function closeEdit() {
  editModal.show = false
}

function validateNoticeForm() {
  const title = editModal.form.title?.trim() || ''
  if (!title) {
    pushToast('请输入公告标题', 'warning')
    return false
  }

  if (title.length > MAX_TITLE_LENGTH) {
    pushToast(`标题不能超过 ${MAX_TITLE_LENGTH} 个字符`, 'warning')
    return false
  }

  const contentText = extractPlainText(editModal.form.content)
  if (!contentText) {
    pushToast('请输入公告正文内容', 'warning')
    return false
  }

  if (contentText.length > MAX_CONTENT_TEXT_LENGTH) {
    pushToast(`正文纯文本不能超过 ${MAX_CONTENT_TEXT_LENGTH} 个字符`, 'warning')
    return false
  }

  return true
}

async function submitNotice() {
  if (!validateNoticeForm()) return
  editModal.submitting = true
  try {
    const payload = {
      title: editModal.form.title.trim(),
      content: sanitizeNoticeHtml(editModal.form.content),
      status: editModal.form.status
    }

    let response
    if (editModal.form.id) {
      response = await api.put(`/notices/${editModal.form.id}`, {
        id: editModal.form.id,
        ...payload
      })
    } else {
      response = await api.post('/notices', payload)
    }

    if (response.code !== 200) {
      pushToast(response.message || '保存公告失败', 'error')
      return
    }

    pushToast(editModal.form.id ? '公告更新成功' : '公告创建成功', 'success')
    closeEdit()
    queryClient.invalidateQueries({ queryKey: ['noticesManage'] })
  } catch (error) {
    pushToast('保存失败，请稍后重试', 'error')
  } finally {
    editModal.submitting = false
  }
}

function onEditorCreated(editor) {
  editorRef.value = editor
}

function confirmSwitchStatus(item, status) {
  const actionText = status === 'PUBLISHED' ? '发布' : '下线'
  dialog.warning({
    title: `确认${actionText}`,
    content: `确认要${actionText}公告「${item.title}」吗？`,
    positiveText: `确认${actionText}`,
    negativeText: '取消',
    onPositiveClick: async () => {
      await switchStatus(item, status)
    }
  })
}

async function switchStatus(item, status) {
  try {
    const response = await api.put(`/notices/${item.id}/status`, { status })
    if (response.code !== 200) {
      pushToast(response.message || '状态切换失败', 'error')
      return
    }
    pushToast(status === 'PUBLISHED' ? '公告已发布' : '公告已下线', 'success')
    queryClient.invalidateQueries({ queryKey: ['noticesManage'] })
  } catch (error) {
    pushToast('状态切换失败，请稍后重试', 'error')
  }
}

function confirmRemoveNotice(item) {
  dialog.error({
    title: '确认删除',
    content: `删除后公告不可恢复，确认删除「${item.title}」吗？`,
    positiveText: '确认删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      await removeNotice(item)
    }
  })
}

async function removeNotice(item) {
  try {
    const response = await api.delete(`/notices/${item.id}`)
    if (response.code !== 200) {
      pushToast(response.message || '删除公告失败', 'error')
      return
    }
    pushToast('公告已删除', 'success')
    queryClient.invalidateQueries({ queryKey: ['noticesManage'] })
  } catch (error) {
    pushToast('删除失败，请稍后重试', 'error')
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

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor) {
    editor.destroy()
  }
})
</script>

<template>
  <div class="admin-users-page">
    <section class="card profile-hero">
      <div>
        <p class="section-kicker">公告模块</p>
        <h2>{{ isOwner ? '公告管理 + 用户侧展示联动' : '公告中心' }}</h2>
        <p class="text-muted">
          {{ isOwner ? '支持草稿、发布、下线、删除与详情查看。' : '查看已发布公告列表与详情。' }}
        </p>
      </div>
      <div class="hero-metrics">
        <div>
          <span>当前页公告</span>
          <strong>{{ notices.length }}</strong>
        </div>
        <div>
          <span>总公告数</span>
          <strong>{{ total }}</strong>
        </div>
      </div>
    </section>

    <section class="card borrow-filters">
      <div class="field">
        <label>关键词</label>
        <NInput v-model:value="filters.keyword" placeholder="按标题搜索" />
      </div>
      <div class="field" v-if="isOwner">
        <label>状态</label>
        <NSelect v-model:value="filters.status" :options="statusOptionsManage" />
      </div>
      <div class="borrow-filters__actions">
        <NButton type="primary" :loading="manageQuery.isFetching" @click="handleSearch">查询</NButton>
        <NButton tertiary @click="resetFilters">重置</NButton>
        <NButton v-if="isOwner" type="info" @click="openCreate">新建公告</NButton>
      </div>
    </section>

    <section class="borrow-panel__list">
      <NCard v-for="item in notices" :key="item.id" size="small" class="borrow-record">
        <template #header>
          <div class="borrow-record__header">
            <div>
              <strong>{{ item.title }}</strong>
              <p class="text-muted">发布时间：{{ formatDateTime(item.publishTime) }}</p>
            </div>
            <NTag :type="statusTagType(item.status)">{{ item.status }}</NTag>
          </div>
        </template>

        <div class="borrow-record__body">
          <div>
            <span>创建时间</span>
            <strong>{{ formatDateTime(item.createTime) }}</strong>
          </div>
          <div>
            <span>更新时间</span>
            <strong>{{ formatDateTime(item.updateTime) }}</strong>
          </div>
          <div v-if="isOwner">
            <span>创建人</span>
            <strong>{{ item.createBy ?? '—' }}</strong>
          </div>
          <div v-if="isOwner">
            <span>更新人</span>
            <strong>{{ item.updateBy ?? '—' }}</strong>
          </div>
        </div>

        <div class="borrow-record__actions" style="gap: 8px; justify-content: flex-end; flex-wrap: wrap;">
          <NButton size="small" tertiary @click="openDetail(item)">查看详情</NButton>
          <template v-if="isOwner">
            <NButton size="small" type="primary" tertiary @click="openEdit(item)">编辑</NButton>
            <NButton
              size="small"
              type="success"
              tertiary
              :disabled="item.status === 'PUBLISHED'"
              @click="confirmSwitchStatus(item, 'PUBLISHED')"
            >发布</NButton>
            <NButton
              size="small"
              tertiary
              :disabled="item.status === 'OFFLINE'"
              @click="confirmSwitchStatus(item, 'OFFLINE')"
            >下线</NButton>
            <NButton size="small" type="error" tertiary @click="confirmRemoveNotice(item)">删除</NButton>
          </template>
        </div>
      </NCard>

      <div v-if="!notices.length && !manageQuery.isFetching" class="empty-state">
        <h3>暂无公告数据</h3>
        <p>请调整筛选条件后重试。</p>
      </div>
    </section>

    <section class="pagination">
      <NButton tertiary @click="prevPage" :disabled="pagination.pageNo <= 1">上一页</NButton>
      <span>第 {{ pagination.pageNo }} 页 / 共 {{ Math.ceil(total / pagination.pageSize) || 1 }} 页</span>
      <NButton tertiary @click="nextPage" :disabled="pagination.pageNo * pagination.pageSize >= total">下一页</NButton>
    </section>

    <NModal v-model:show="detailModal.show" preset="card" title="公告详情" class="booking-modal">
      <div v-if="detailModal.loading" class="text-muted">加载中...</div>
      <div v-else-if="detailModal.data" class="detail-grid">
        <div><span>公告ID</span><strong>{{ detailModal.data.id }}</strong></div>
        <div><span>标题</span><strong>{{ detailModal.data.title }}</strong></div>
        <div><span>状态</span><strong>{{ detailModal.data.status }}</strong></div>
        <div><span>发布时间</span><strong>{{ formatDateTime(detailModal.data.publishTime) }}</strong></div>
        <div><span>创建时间</span><strong>{{ formatDateTime(detailModal.data.createTime) }}</strong></div>
        <div><span>更新时间</span><strong>{{ formatDateTime(detailModal.data.updateTime) }}</strong></div>
      </div>
      <div v-if="detailModal.data" class="booking-modal__section">
        <label>正文（HTML渲染）</label>
        <div class="notice-html-preview" v-html="safeDetailHtml"></div>
      </div>
      <div class="booking-modal__actions">
        <NButton type="primary" @click="closeDetail">关闭</NButton>
      </div>
    </NModal>

    <NModal v-model:show="editModal.show" preset="card" title="公告编辑" class="booking-modal">
      <div class="booking-modal__section">
        <label>标题 *（{{ editModal.form.title.length }}/{{ MAX_TITLE_LENGTH }}）</label>
        <NInput v-model:value="editModal.form.title" :maxlength="MAX_TITLE_LENGTH" show-count />
      </div>
      <div class="booking-modal__section">
        <label>
          正文富文本 *（纯文本上限 {{ MAX_CONTENT_TEXT_LENGTH }} 字）
        </label>
        <div class="notice-editor-wrapper">
          <Toolbar :editor="editorRef" :default-config="toolbarConfig" mode="default" />
          <Editor
            v-model="editModal.form.content"
            :default-config="editorConfig"
            mode="default"
            @onCreated="onEditorCreated"
          />
        </div>
      </div>
      <div class="booking-modal__section">
        <label>状态</label>
        <NSelect v-model:value="editModal.form.status" :options="statusOptionsEdit" />
      </div>
      <div class="booking-modal__actions">
        <NButton @click="closeEdit">取消</NButton>
        <NButton type="primary" :loading="editModal.submitting" @click="submitNotice">保存</NButton>
      </div>
    </NModal>
  </div>
</template>
