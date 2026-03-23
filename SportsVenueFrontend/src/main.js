import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { VueQueryPlugin } from '@tanstack/vue-query'
import App from './App.vue'
import router from './router'
import Particles from '@tsparticles/vue3'
import { loadFull } from 'tsparticles'
import './style.css'
import 'animate.css'

createApp(App)
  .use(createPinia())
  .use(VueQueryPlugin)
  .use(router)
  .use(Particles, {
    init: async (engine) => {
      await loadFull(engine)
    }
  })
  .mount('#app')
