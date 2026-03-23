<script setup>
import { RouterLink } from 'vue-router'
import LottieHero from './LottieHero.vue'
</script>

<template>
  <section class="home-hero">
    <div class="home-hero__content">
      <div class="brand">
        <div class="brand-mark">SV</div>
        <div>
          <h1>体育馆预约与仓库管理系统</h1>
          <p>一体化预约、审批与仓储管理平台，支持多角色权限与统一认证。</p>
        </div>
      </div>
      <div class="hero-metrics">
        <div>
          <span>场馆</span>
          <strong>120+</strong>
        </div>
        <div>
          <span>活跃用户</span>
          <strong>12,000+</strong>
        </div>
        <div>
          <span>预约成功率</span>
          <strong>98.7%</strong>
        </div>
      </div>
    </div>
    <div class="home-hero__panel">
      <h2>统一认证入口</h2>
      <p>支持普通用户、场馆管理员、系统管理员登录与权限隔离。</p>
      <ul>
        <li>统一账号体系，安全可控</li>
        <li>审批流程可视化</li>
        <li>预约排期自动化</li>
      </ul>
      <RouterLink class="text-link" to="/auth?tab=login">进入登录</RouterLink>
      <LottieHero />
    </div>
  </section>
</template>
