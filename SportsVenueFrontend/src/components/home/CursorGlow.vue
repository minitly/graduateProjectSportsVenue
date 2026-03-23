<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

const glowRef = ref(null)

function handleMove(event) {
  if (!glowRef.value) return
  const { clientX, clientY } = event
  glowRef.value.style.setProperty('--x', `${clientX}px`)
  glowRef.value.style.setProperty('--y', `${clientY}px`)
}

onMounted(() => {
  window.addEventListener('pointermove', handleMove)
})

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', handleMove)
})
</script>

<template>
  <div ref="glowRef" class="cursor-glow" aria-hidden="true"></div>
</template>
