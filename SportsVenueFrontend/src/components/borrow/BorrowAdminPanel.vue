<script setup>
import { reactive } from 'vue'
import { useQueryClient } from '@tanstack/vue-query'
import { NButton, NInput, NInputNumber, NModal } from 'naive-ui'
import api from '../../services/api'
import { useToast } from '../../composables/useToast'

const props = defineProps({
  items: { type: Array, default: () => [] }
})

const { pushToast } = useToast()
const queryClient = useQueryClient()

const adminItemModal = reactive({
  show: false,
  editingId: null,
  submitting: false,
  form: {
    name: '',
    type: '',
    model: '',
    totalQuantity: 0,
    availableQuantity: 0,
    damagedQuantity: 0,
    depositAmount: 0,
    description: ''
  }
})

function resetForm() {
  adminItemModal.form = {
    name: '',
    type: '',
    model: '',
    totalQuantity: 0,
    availableQuantity: 0,
    damagedQuantity: 0,
    depositAmount: 0,
    description: ''
  }
}

function openAdminCreate() {
  adminItemModal.show = true
  adminItemModal.editingId = null
  resetForm()
}

function openAdminEdit(item) {
  adminItemModal.show = true
  adminItemModal.editingId = item.id
  adminItemModal.form = {
    name: item.name || '',
    type: item.type || '',
    model: item.model || '',
    totalQuantity: item.totalQuantity || 0,
    availableQuantity: item.availableQuantity || 0,
    damagedQuantity: item.damagedQuantity || 0,
    depositAmount: item.depositAmount || 0,
    description: item.description || ''
  }
}

function closeAdminModal() {
  adminItemModal.show = false
}

async function submitAdminItem() {
  if (!adminItemModal.form.name?.trim()) {
    pushToast('请填写器材名称', 'warning')
    return
  }
  adminItemModal.submitting = true
  try {
    const payload = { ...adminItemModal.form }
    const response = adminItemModal.editingId
      ? await api.put(`/items/${adminItemModal.editingId}`, payload)
      : await api.post('/items', payload)

    if (response.code !== 200) {
      pushToast(response.message || '提交失败', 'error')
      return
    }

    pushToast(adminItemModal.editingId ? '器材信息已更新' : '器材已新增', 'success')
    closeAdminModal()
    queryClient.invalidateQueries({ queryKey: ['items'] })
  } catch {
    pushToast('提交失败，请稍后重试', 'error')
  } finally {
    adminItemModal.submitting = false
  }
}

defineExpose({ openAdminCreate, openAdminEdit })
</script>

<template>
  <div class="borrow-admin-actions">
    <NButton type="primary" @click="openAdminCreate">新增器材</NButton>
  </div>

  <NModal v-model:show="adminItemModal.show" preset="card" class="booking-modal" title="器材台账管理">
    <div class="booking-modal__section"><label>器材名称</label><NInput v-model:value="adminItemModal.form.name" /></div>
    <div class="booking-modal__section"><label>器材类型</label><NInput v-model:value="adminItemModal.form.type" /></div>
    <div class="booking-modal__section"><label>型号</label><NInput v-model:value="adminItemModal.form.model" /></div>
    <div class="booking-modal__section two-col">
      <div><label>库存总量</label><NInputNumber v-model:value="adminItemModal.form.totalQuantity" :min="0" /></div>
      <div><label>可借数量</label><NInputNumber v-model:value="adminItemModal.form.availableQuantity" :min="0" /></div>
    </div>
    <div class="booking-modal__section two-col">
      <div><label>损坏数量</label><NInputNumber v-model:value="adminItemModal.form.damagedQuantity" :min="0" /></div>
      <div><label>押金金额</label><NInputNumber v-model:value="adminItemModal.form.depositAmount" :min="0" /></div>
    </div>
    <div class="booking-modal__section"><label>描述</label><NInput v-model:value="adminItemModal.form.description" type="textarea" /></div>
    <div class="booking-modal__actions">
      <NButton @click="closeAdminModal">取消</NButton>
      <NButton type="primary" :loading="adminItemModal.submitting" @click="submitAdminItem">{{ adminItemModal.editingId ? '保存修改' : '确认新增' }}</NButton>
    </div>
  </NModal>
</template>
