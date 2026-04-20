<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import FloorPlanAdminPanel from '../components/floorPlan/FloorPlanAdminPanel.vue'
import FloorPlanUserPanel from '../components/floorPlan/FloorPlanUserPanel.vue'

const route = useRoute()
const authStore = useAuthStore()

const isAdminPath = computed(() => route.path === '/app/floor-plans-admin')
const isOwnerOrAdmin = computed(() => authStore.role === 'OWNER' || authStore.role === 'ADMIN')

onMounted(() => {
  authStore.hydrate()
})
</script>

<template>
  <div class="floor-plan-view">
    <FloorPlanAdminPanel v-if="isAdminPath && isOwnerOrAdmin" />
    <FloorPlanUserPanel v-else />
  </div>
</template>

<style scoped>
.floor-plan-view {
  min-height: 860px;
}
</style>
