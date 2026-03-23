<script setup>
import { computed, onMounted, ref } from 'vue'
import { useAuthStore } from '../stores/auth'
import BorrowUserPanel from '../components/borrow/BorrowUserPanel.vue'
import BorrowOwnerPanel from '../components/borrow/BorrowOwnerPanel.vue'
import BorrowAdminPanel from '../components/borrow/BorrowAdminPanel.vue'

const authStore = useAuthStore()
const adminPanelRef = ref(null)

const isOwner = computed(() => authStore.role === 'OWNER')
const isAdmin = computed(() => authStore.role === 'ADMIN')

onMounted(() => {
  authStore.hydrate()
})

function handleEditFromUser(item) {
  adminPanelRef.value?.openAdminEdit(item)
}
</script>

<template>
  <div class="borrow-page">
    <BorrowAdminPanel v-if="isAdmin" ref="adminPanelRef" />
    <BorrowUserPanel v-if="!isOwner && !isAdmin" @edit-item="handleEditFromUser" />
    <BorrowOwnerPanel v-if="isOwner" />
  </div>
</template>
