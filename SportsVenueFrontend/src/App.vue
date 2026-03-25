<script setup>
import { NConfigProvider, zhCN, dateZhCN } from 'naive-ui'
import ToastHost from './components/ui/ToastHost.vue'
</script>

<template>
  <NConfigProvider :locale="zhCN" :date-locale="dateZhCN">
    <div class="app-root">
      <RouterView />
      <ToastHost />
    </div>
  </NConfigProvider>
</template>
