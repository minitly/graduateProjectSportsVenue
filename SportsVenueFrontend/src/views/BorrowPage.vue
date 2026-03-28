<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import BorrowUserPanel from '../components/borrow/BorrowUserPanel.vue'
import BorrowOwnerPanel from '../components/borrow/BorrowOwnerPanel.vue'
import BorrowAdminPanel from '../components/borrow/BorrowAdminPanel.vue'

const route = useRoute()
const authStore = useAuthStore()
const adminPanelRef = ref(null)

const isOwner = computed(() => authStore.role === 'OWNER')
const isAdmin = computed(() => authStore.role === 'ADMIN')
const isOwnerOrAdmin = computed(() => isOwner.value || isAdmin.value)

/** 用户端：/app/borrow 为可借列表，/app/my-borrows 为我的记录 */
const userBorrowSection = computed(() => (route.path === '/app/my-borrows' ? 'my' : 'browse'))

onMounted(() => {
  authStore.hydrate()
})

function handleEditFromUser(item) {
  adminPanelRef.value?.openAdminEdit(item)
}
</script>

<template>
  <div class="borrow-page">
    <BorrowAdminPanel v-if="isOwnerOrAdmin" ref="adminPanelRef" />
    <BorrowUserPanel v-if="!isOwner && !isAdmin" :section="userBorrowSection" @edit-item="handleEditFromUser" />
  </div>
</template>

<style scoped>
.borrow-page {
  min-height: 860px;
}
</style>
