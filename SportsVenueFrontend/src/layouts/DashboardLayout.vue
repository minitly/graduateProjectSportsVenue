<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NConfigProvider, NDialogProvider } from 'naive-ui'
import { useAuthStore } from '../stores/auth'
import { useNaiveTheme } from '../composables/useNaiveTheme'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { themeOverrides } = useNaiveTheme()

const baseMenuItems = [
  {
    label: '场地预约',
    description: '发现可预约场地，管理我的预约',
    path: '/app/venues'
  },
  {
    label: '器材借用',
    description: '查看器材库存并提交借用申请',
    path: '/app/borrow'
  },
  {
    label: '用户中心',
    description: '完善个人资料、查看账号状态',
    path: '/app/profile'
  },
  {
    label: '公告中心',
    description: '查看平台公告与运营通知',
    path: '/app/notices'
  }
]

const menuItems = computed(() => {
  if (authStore.role === 'ADMIN') {
    const managedBaseItems = baseMenuItems.map((item) => ({
      ...item,
      label: `${item.label}管理`
    }))
    return [
      ...managedBaseItems,
      {
        label: '用户管理',
        description: '查看用户状态并执行启用/禁用',
        path: '/app/admin-users'
      },
      {
        label: '公告管理',
        description: '创建、发布、下线与维护公告',
        path: '/app/admin-notices'
      },
      {
        label: '数据分析管理',
        description: '查看全局预约与运营数据分析',
        path: '/app/admin-analytics'
      },
      {
        label: '2D建模管理',
        description: '进行2D建模配置与画布预览',
        path: '/app/admin-model-2d'
      },
      {
        label: '3D场馆预览',
        description: '查看2D模型转化的3D场馆效果',
        path: '/app/admin-model-3d'
      }
    ]
  }

  if (authStore.role === 'OWNER') {
    const managedBaseItems = baseMenuItems.map((item) => ({
      ...item,
      label: `${item.label}管理`
    }))
    return [
      ...managedBaseItems,
      {
        label: '用户管理',
        description: '查看用户状态并执行启用/禁用',
        path: '/app/admin-users'
      },
      {
        label: '公告管理',
        description: '创建、发布、下线与维护公告',
        path: '/app/admin-notices'
      },
      {
        label: '数据分析管理',
        description: '查看全局预约与运营数据分析',
        path: '/app/admin-analytics'
      },
      {
        label: '2D建模管理',
        description: '进行2D建模配置与画布预览',
        path: '/app/admin-model-2d'
      },
      {
        label: '3D场馆预览',
        description: '查看2D模型转化的3D场馆效果',
        path: '/app/admin-model-3d'
      }
    ]
  }

  return baseMenuItems
})

const activePath = computed(() => route.path)
const displayName = computed(() => authStore.user?.realName || authStore.user?.username || '访客')
const roleLabel = computed(() => {
  if (authStore.role === 'ADMIN') return '系统管理员'
  if (authStore.role === 'OWNER') return '场馆管理员'
  return '普通用户'
})

function handleNavigate(path) {
  if (path !== route.path) {
    router.push(path)
  }
}

function handleLogout() {
  authStore.clear()
  router.replace('/auth')
}

function handleQuickBooking() {
  if (route.path === '/app/venues') {
    window.dispatchEvent(new CustomEvent('quick-booking'))
    return
  }
  router.push({
    path: '/app/venues',
    query: { action: 'quick-booking' }
  })
}
</script>

<template>
  <div class="dashboard">
    <aside class="dashboard__aside">
      <div class="dashboard__brand">
        <div class="brand-mark">SV</div>
        <div>
          <strong>体育馆管理中台</strong>
          <span>Sports Venue</span>
        </div>
      </div>

      <div class="dashboard__user">
        <div class="avatar">{{ displayName.charAt(0) }}</div>
        <div>
          <h4>{{ displayName }}</h4>
          <p>{{ roleLabel }}</p>
        </div>
      </div>

      <nav class="dashboard__nav">
        <button
          v-for="item in menuItems"
          :key="item.path"
          type="button"
          class="nav-item"
          :class="{ active: activePath === item.path }"
          @click="handleNavigate(item.path)"
        >
          <span>{{ item.label }}</span>
          <small>{{ item.description }}</small>
        </button>
      </nav>

      <div class="dashboard__footer">
        <button class="ghost" type="button" @click="handleLogout">退出登录</button>
      </div>
    </aside>

    <main class="dashboard__main">
      <header class="dashboard__header">
        <div>
          <h2>{{ route.meta.title || '控制台' }}</h2>
          <p>{{ route.meta.subtitle || '管理你的体育馆预约与器材借用' }}</p>
        </div>
        <div class="dashboard__actions">
          <button class="ghost" type="button">帮助中心</button>
          <button class="primary" type="button" @click="handleQuickBooking">
            立即预约
          </button>
        </div>
      </header>

      <section class="dashboard__content">
        <NConfigProvider :theme-overrides="themeOverrides">
          <NDialogProvider>
            <RouterView />
          </NDialogProvider>
        </NConfigProvider>
      </section>
    </main>
  </div>
</template>
