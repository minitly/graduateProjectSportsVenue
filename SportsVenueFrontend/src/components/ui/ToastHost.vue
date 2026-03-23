<script setup>
import { computed } from 'vue'
import { useToast } from '../../composables/useToast'

const { toasts } = useToast()
const toastList = computed(() => toasts.value)
</script>

<template>
  <div class="toast-host">
    <transition-group name="toast" tag="div">
      <div v-for="toast in toastList" :key="toast.id" class="toast" :class="toast.type">
        <span>{{ toast.message }}</span>
      </div>
    </transition-group>
  </div>
</template>
