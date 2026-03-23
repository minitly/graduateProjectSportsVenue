<script setup>
import HeroSection from '../components/home/HeroSection.vue'
import ParticlesBackground from '../components/home/ParticlesBackground.vue'
import CursorGlow from '../components/home/CursorGlow.vue'
import StepsFlow from '../components/home/StepsFlow.vue'
import FeatureHighlights from '../components/home/FeatureHighlights.vue'
import HomeCta from '../components/home/HomeCta.vue'
</script>

<template>
  <div class="home-page">
    <ParticlesBackground />
    <div class="home-bg">
      <div class="home-blob blob-one"></div>
      <div class="home-blob blob-two"></div>
      <div class="home-grid"></div>
    </div>
    <CursorGlow />
    <HeroSection />
    <StepsFlow />
    <FeatureHighlights />
    <HomeCta />
  </div>
</template>
