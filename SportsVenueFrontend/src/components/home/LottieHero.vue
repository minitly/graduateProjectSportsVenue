<script setup>
import { Vue3Lottie } from 'vue3-lottie'

const animationUrl = 'https://assets9.lottiefiles.com/packages/lf20_1pxqjqps.json'
</script>

<template>
  <div class="lottie-hero">
    <Vue3Lottie :animation-link="animationUrl" :height="320" :width="320" />
  </div>
</template>
