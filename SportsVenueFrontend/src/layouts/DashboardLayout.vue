<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NConfigProvider, NDialogProvider } from 'naive-ui'
import { useAuthStore } from '../stores/auth'
import { useNaiveTheme } from '../composables/useNaiveTheme'
import logo from '../assets/logo.svg'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { themeOverrides } = useNaiveTheme()

const baseMenuItems = [
  {
    label: '场地管理',
    description: '维护场地信息与开放状态',
    path: '/app/venues'
  },
  {
    label: '预约管理',
    description: '查看预约记录与审核流程',
    path: '/app/bookings'
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

/** 普通用户侧文案：场地预约 / 我的预约；公告中心置顶 */
const userMenuItems = (() => {
  const withUserLabels = baseMenuItems.map((item) => {
    if (item.path === '/app/venues') {
      return {
        ...item,
        label: '场地预约',
        description: '浏览可预约场地并选择时段'
      }
    }
    if (item.path === '/app/bookings') {
      return {
        ...item,
        label: '我的预约',
        description: '查看预约记录与状态'
      }
    }
    return item
  })
  const notices = withUserLabels.find((item) => item.path === '/app/notices')
  const rest = withUserLabels.filter((item) => item.path !== '/app/notices')
  return notices ? [notices, ...rest] : [...withUserLabels]
})()

/** 管理端（ADMIN / OWNER）：不展示「用户中心」；器材借用文案改为「器材管理」 */
const adminMenuBase = baseMenuItems
  .filter((item) => item.path !== '/app/profile')
  .map((item) =>
    item.path === '/app/borrow'
      ? {
          ...item,
          label: '器材管理',
          description: '查看器材库存与借用审批'
        }
      : item
  )

const menuItems = computed(() => {
  if (authStore.role === 'ADMIN') {
    return [
      ...adminMenuBase,
      {
        label: '用户管理',
        description: '查看用户状态并执行启用/禁用',
        path: '/app/admin-users'
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
    return [
      ...adminMenuBase,
      {
        label: '用户管理',
        description: '查看用户状态并执行启用/禁用',
        path: '/app/admin-users'
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

  return userMenuItems
})

const activePath = computed(() => route.path)

/** 与侧边栏一致：用户端 / 管理端部分页面标题与路由 meta 区分 */
const pageHeader = computed(() => {
  const meta = route.meta || {}
  const path = route.path
  const role = authStore.role
  if (role === 'USER') {
    if (path === '/app/venues') {
      return {
        title: '场地预约',
        subtitle: '浏览可预约场地并选择时段'
      }
    }
    if (path === '/app/bookings') {
      return {
        title: '我的预约',
        subtitle: '查看预约记录与状态'
      }
    }
  }
  if ((role === 'ADMIN' || role === 'OWNER') && path === '/app/borrow') {
    return {
      title: '器材管理',
      subtitle: '查看器材库存与借用审批'
    }
  }
  return {
    title: meta.title || '控制台',
    subtitle: meta.subtitle || '管理你的体育馆预约与器材借用'
  }
})

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
</script>

<template>
  <div class="dashboard">
    <aside class="dashboard__aside">
      <div class="dashboard__brand">
        <div class="brand-mark">
          <img class="brand-mark__logo" :src="logo" alt="体育馆图标" />
        </div>
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
          <h2>{{ pageHeader.title }}</h2>
          <p>{{ pageHeader.subtitle }}</p>
        </div>
        <div class="dashboard__actions">
          <button class="ghost" type="button">帮助中心</button>
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
