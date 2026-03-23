<template>
  <section class="home-cta">
    <div class="cta-card">
      <div>
        <span class="section-kicker">新手福利</span>
        <h3>注册即领新手券</h3>
        <p>现在加入，预约更省心</p>
      </div>
      <RouterLink class="cta-button" to="/auth?tab=register">马上注册</RouterLink>
    </div>
  </section>
</template>

<script setup>
import { RouterLink } from 'vue-router'
</script>
