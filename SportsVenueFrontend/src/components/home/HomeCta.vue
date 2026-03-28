<template>
  <section class="home-cta">
    <div class="cta-card">
      <div>
        <span class="section-kicker">新手福利</span>
        <h3>免费注册，解锁预约与器材借用</h3>
        <p>
          创建账号即可使用场地预约、器材申请与公告浏览；随时查看「我的预约」与借用进度，不错过平台动态。
        </p>
      </div>
      <RouterLink class="cta-button" to="/auth?tab=register">立即注册</RouterLink>
    </div>
  </section>
</template>

<script setup>
import { RouterLink } from 'vue-router'
</script>
