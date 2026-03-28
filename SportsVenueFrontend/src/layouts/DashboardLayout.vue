<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NConfigProvider, NDialogProvider, NTooltip } from 'naive-ui'
import {
  MapPinned,
  CalendarCheck2,
  Package,
  ClipboardList,
  ClipboardCheck,
  UserCircle2,
  Bell,
  Users,
  BarChart3,
  PencilRuler,
  Cuboid,
  LogOut,
  Sun,
  CloudSun,
  Cloud,
  CloudFog,
  CloudDrizzle,
  CloudRain,
  CloudSnow,
  CloudLightning
} from 'lucide-vue-next'
import { useAuthStore } from '../stores/auth'
import { useNaiveTheme } from '../composables/useNaiveTheme'
import logo from '../assets/logo.svg'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { themeOverrides } = useNaiveTheme()

const SIDEBAR_COLLAPSED_KEY = 'sports-venue:dashboard-sidebar-collapsed'

const collapsed = ref(localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === '1')

const locationInfo = ref({
  city: '定位中',
  region: '',
  weather: '获取中',
  temperature: '--',
  weatherCode: null
})

const locationError = ref('')

watch(collapsed, (value) => {
  localStorage.setItem(SIDEBAR_COLLAPSED_KEY, value ? '1' : '0')
})

const weatherCodeMap = {
  0: '晴',
  1: '大部晴朗',
  2: '局部多云',
  3: '阴天',
  45: '有雾',
  48: '冻雾',
  51: '小毛雨',
  53: '毛雨',
  55: '强毛雨',
  56: '冻毛雨',
  57: '强冻毛雨',
  61: '小雨',
  63: '中雨',
  65: '大雨',
  66: '冻雨',
  67: '强冻雨',
  71: '小雪',
  73: '中雪',
  75: '大雪',
  77: '冰粒',
  80: '阵雨',
  81: '强阵雨',
  82: '暴雨',
  85: '阵雪',
  86: '强阵雪',
  95: '雷雨',
  96: '雷雨伴小冰雹',
  99: '强雷雨伴冰雹'
}

function getWeatherText(code) {
  if (code === undefined || code === null) return '实时天气'
  return weatherCodeMap[code] || `天气码 ${code}`
}

function getWeatherIcon(code) {
  if (code === 0) return Sun
  if (code === 1 || code === 2) return CloudSun
  if (code === 3) return Cloud
  if (code === 45 || code === 48) return CloudFog
  if ([51, 53, 55, 56, 57].includes(code)) return CloudDrizzle
  if ([61, 63, 65, 66, 67, 80, 81, 82].includes(code)) return CloudRain
  if ([71, 73, 75, 77, 85, 86].includes(code)) return CloudSnow
  if ([95, 96, 99].includes(code)) return CloudLightning
  return Cloud
}

const weatherIcon = computed(() => getWeatherIcon(locationInfo.value.weatherCode))

const weatherIconColor = computed(() => {
  const code = locationInfo.value.weatherCode
  if (code === 0 || code === 1 || code === 2) return '#f7c948'
  if ([95, 96, 99].includes(code)) return '#9a7cff'
  if ([61, 63, 65, 66, 67, 80, 81, 82].includes(code)) return '#66b2ff'
  if ([51, 53, 55, 56, 57].includes(code)) return '#7fc3ff'
  if ([71, 73, 75, 77, 85, 86].includes(code)) return '#8ec7ff'
  if (code === 45 || code === 48 || code === 3) return '#94a8c6'
  return '#c6d6ee'
})

const weatherIconBgColor = computed(() => {
  const code = locationInfo.value.weatherCode
  if (code === 0 || code === 1 || code === 2) return 'rgba(247, 201, 72, 0.2)'
  if ([95, 96, 99].includes(code)) return 'rgba(154, 124, 255, 0.2)'
  if ([61, 63, 65, 66, 67, 80, 81, 82].includes(code)) return 'rgba(102, 178, 255, 0.2)'
  if ([51, 53, 55, 56, 57].includes(code)) return 'rgba(127, 195, 255, 0.22)'
  if ([71, 73, 75, 77, 85, 86].includes(code)) return 'rgba(142, 199, 255, 0.2)'
  if (code === 45 || code === 48 || code === 3) return 'rgba(148, 168, 198, 0.2)'
  return 'rgba(198, 214, 238, 0.18)'
})

async function fetchLocationAndWeather() {
  try {
    locationError.value = ''
    const ipResponse = await fetch('https://ipapi.co/json/')
    if (!ipResponse.ok) {
      throw new Error('IP定位失败')
    }
    const ipData = await ipResponse.json()
    locationInfo.value = {
      ...locationInfo.value,
      city: ipData.city || '未知城市',
      region: ipData.region || ipData.country_name || ''
    }

    if (ipData.latitude && ipData.longitude) {
      const weatherUrl = `https://api.open-meteo.com/v1/forecast?latitude=${ipData.latitude}&longitude=${ipData.longitude}&current_weather=true&timezone=auto`
      const weatherResponse = await fetch(weatherUrl)
      if (!weatherResponse.ok) {
        throw new Error('天气获取失败')
      }
      const weatherData = await weatherResponse.json()
      const current = weatherData.current_weather || {}
      locationInfo.value = {
        ...locationInfo.value,
        temperature: typeof current.temperature === 'number' ? `${Math.round(current.temperature)}°C` : '--',
        weather: getWeatherText(current.weathercode),
        weatherCode: current.weathercode
      }
    }
  } catch (error) {
    locationError.value = '获取失败'
  }
}

onMounted(() => {
  fetchLocationAndWeather()
})

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
  const expanded = []
  for (const item of withUserLabels) {
    if (item.path === '/app/borrow') {
      expanded.push(
        {
          label: '器材借用',
          description: '查看可借器材并提交借用申请',
          path: '/app/borrow'
        },
        {
          label: '我的借用',
          description: '查看借用记录与状态',
          path: '/app/my-borrows'
        }
      )
    } else {
      expanded.push(item)
    }
  }
  const notices = expanded.find((item) => item.path === '/app/notices')
  const rest = expanded.filter((item) => item.path !== '/app/notices')
  return notices ? [notices, ...rest] : [...expanded]
})()

const adminMenuBase = baseMenuItems
  .filter((item) => item.path !== '/app/profile' && item.path !== '/app/notices')
  .map((item) =>
    item.path === '/app/borrow'
      ? {
          ...item,
          label: '器材管理',
          description: '维护器材台账与库存'
        }
      : item
  )

const noticesMenuItem = {
  label: '公告中心',
  description: '查看平台公告与运营通知',
  path: '/app/notices'
}

const menuItems = computed(() => {
  if (authStore.role === 'ADMIN' || authStore.role === 'OWNER') {
    return [
      ...adminMenuBase,
      ...(authStore.role === 'OWNER'
        ? [
            {
              label: '借用审批',
              description: '审批借用申请并确认归还',
              path: '/app/borrow-approval'
            },
            noticesMenuItem
          ]
        : [noticesMenuItem]),
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

const pathIconMap = {
  '/app/venues': MapPinned,
  '/app/bookings': CalendarCheck2,
  '/app/borrow': Package,
  '/app/my-borrows': ClipboardList,
  '/app/borrow-approval': ClipboardCheck,
  '/app/profile': UserCircle2,
  '/app/notices': Bell,
  '/app/admin-users': Users,
  '/app/admin-analytics': BarChart3,
  '/app/admin-model-2d': PencilRuler,
  '/app/admin-model-3d': Cuboid
}

const navItems = computed(() =>
  menuItems.value.map((item) => ({
    ...item,
    icon: pathIconMap[item.path] || Bell
  }))
)

const activePath = computed(() => route.path)

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
    if (path === '/app/borrow') {
      return {
        title: '器材借用',
        subtitle: '查看可借器材并提交借用申请'
      }
    }
    if (path === '/app/my-borrows') {
      return {
        title: '我的借用',
        subtitle: '查看借用记录与状态'
      }
    }
  }
  if ((role === 'ADMIN' || role === 'OWNER') && path === '/app/borrow') {
    return {
      title: '器材管理',
      subtitle: '维护器材台账与库存'
    }
  }
  if (role === 'OWNER' && path === '/app/borrow-approval') {
    return {
      title: '借用审批',
      subtitle: '审批借用申请并确认归还'
    }
  }
  return {
    title: meta.title || '控制台',
    subtitle: meta.subtitle || '管理你的体育馆预约与器材借用'
  }
})

const sidebarBrandTitle = computed(() =>
  authStore.role === 'ADMIN' || authStore.role === 'OWNER' ? '体育馆管理中台' : '体育馆预约服务'
)

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

function toggleCollapsed() {
  collapsed.value = !collapsed.value
}
</script>

<template>
  <div class="dashboard" :class="{ 'dashboard--collapsed': collapsed }">
    <aside class="dashboard__aside">
      <div class="dashboard__aside-top">
        <button class="collapse-btn" type="button" @click="toggleCollapsed">
          <span>{{ collapsed ? '»' : '«' }}</span>
        </button>

        <div class="dashboard__brand" :class="{ compact: collapsed }">
          <div class="brand-mark">
            <img class="brand-mark__logo" :src="logo" alt="体育馆图标" />
          </div>
          <div class="sidebar-fade" :class="{ 'is-hidden': collapsed }">
            <strong>{{ sidebarBrandTitle }}</strong>
            <span>Sports Venue</span>
          </div>
        </div>

        <div class="dashboard__user" :class="{ compact: collapsed }">
          <div class="avatar">{{ displayName.charAt(0) }}</div>
          <div class="sidebar-fade" :class="{ 'is-hidden': collapsed }">
            <h4>{{ displayName }}</h4>
            <p>{{ roleLabel }}</p>
          </div>
        </div>
      </div>

      <nav class="dashboard__nav" :class="{ compact: collapsed }">
        <NTooltip
          v-for="item in navItems"
          :key="item.path"
          trigger="hover"
          placement="right"
          :disabled="!collapsed"
        >
          <template #trigger>
            <button
              type="button"
              class="nav-item"
              :class="{ active: activePath === item.path, compact: collapsed }"
              @click="handleNavigate(item.path)"
            >
              <span class="nav-item__icon">
                <component :is="item.icon" :size="16" :stroke-width="2.25" />
              </span>
              <div class="nav-item__text sidebar-fade" :class="{ 'is-hidden': collapsed }">
                <span>{{ item.label }}</span>
                <small>{{ item.description }}</small>
              </div>
            </button>
          </template>
          {{ item.label }}
        </NTooltip>
      </nav>

      <div class="dashboard__footer">
        <div class="sidebar-meta" :class="{ compact: collapsed }">
          <div class="sidebar-meta__title sidebar-fade" :class="{ 'is-hidden': collapsed }">当前环境</div>
          <div v-if="!collapsed" class="sidebar-meta__row">
            <span class="sidebar-meta__label">IP属地</span>
            <strong :title="`${locationInfo.city} ${locationInfo.region}`">
              {{ `${locationInfo.city}${locationInfo.region ? ` · ${locationInfo.region}` : ''}` }}
            </strong>
          </div>
          <div class="sidebar-meta__row">
            <span class="sidebar-meta__label sidebar-fade" :class="{ 'is-hidden': collapsed }">当前天气</span>
            <strong class="weather-value" :title="`${locationInfo.weather} ${locationInfo.temperature}`">
              <span class="weather-icon-chip" :style="{ backgroundColor: weatherIconBgColor }">
                <component :is="weatherIcon" :size="14" :stroke-width="2.2" :style="{ color: weatherIconColor }" />
              </span>
              <span v-if="!collapsed">{{ `${locationInfo.weather} ${locationInfo.temperature}` }}</span>
            </strong>
          </div>
          <p v-if="locationError && !collapsed" class="sidebar-meta__error">{{ locationError }}</p>
        </div>

        <button class="ghost" type="button" @click="handleLogout">
          <LogOut :size="15" :stroke-width="2.3" />
          <span class="sidebar-fade" :class="{ 'is-hidden': collapsed }">退出登录</span>
        </button>
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

<style scoped>
.dashboard {
  --aside-width: 280px;
  --aside-compact-width: 96px;
  display: grid;
  grid-template-columns: var(--aside-width) minmax(0, 1fr);
  min-height: 100vh;
  background: #f4f6fb;
  transition: grid-template-columns 180ms ease;
}

.dashboard--collapsed {
  grid-template-columns: var(--aside-compact-width) minmax(0, 1fr);
}

.dashboard__aside {
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
  height: 100vh;
  padding: 14px;
  border-right: 1px solid #1e2a3f;
  background: linear-gradient(180deg, #0f1726 0%, #10192c 100%);
  transition: padding 180ms ease;
  overflow: hidden;
}

.dashboard__aside-top {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.collapse-btn {
  align-self: flex-end;
  width: 28px;
  height: 28px;
  border: 1px solid #30415f;
  border-radius: 8px;
  background: #17233a;
  color: #d6e1f3;
  cursor: pointer;
}

.collapse-btn:hover {
  background: #1c2c48;
}

.dashboard__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
}

.dashboard__brand.compact {
  justify-content: center;
}

.sidebar-fade {
  opacity: 1;
  transform: translateX(0);
  max-width: 220px;
  max-height: 56px;
  overflow: hidden;
  white-space: nowrap;
  transition: opacity 180ms ease, transform 180ms ease, max-width 180ms ease, max-height 180ms ease;
}

.sidebar-fade.is-hidden {
  opacity: 0;
  transform: translateX(-6px);
  max-width: 0;
  max-height: 0;
}

.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: #e8f0ff;
}

.brand-mark__logo {
  width: 20px;
  height: 20px;
}

.dashboard__brand strong {
  display: block;
  font-size: 14px;
  color: #f4f8ff;
}

.dashboard__brand span {
  font-size: 12px;
  color: #9fb1ce;
}

.dashboard__user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 12px;
  background: rgba(42, 59, 92, 0.55);
  border: 1px solid #243552;
}

.dashboard__user.compact {
  justify-content: center;
  gap: 0;
}

.avatar {
  width: 36px;
  height: 36px;
  min-width: 36px;
  min-height: 36px;
  aspect-ratio: 1 / 1;
  border-radius: 9999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  background: #4a8bff;
  color: #fff;
  font-weight: 700;
  flex-shrink: 0;
}

.dashboard__user h4 {
  margin: 0;
  font-size: 14px;
  color: #f2f6ff;
}

.dashboard__user p {
  margin: 2px 0 0;
  font-size: 12px;
  color: #9fb0cc;
}

.dashboard__nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 2px;
  scrollbar-width: thin;
  scrollbar-color: #3d5a86 #16243b;
}

.dashboard__nav.compact {
  align-items: center;
}

.dashboard__nav::-webkit-scrollbar {
  width: 8px;
}

.dashboard__nav::-webkit-scrollbar-track {
  background: transparent;
  border-radius: 999px;
}

.dashboard__nav::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #4c74ab 0%, #375a8d 100%);
  border-radius: 999px;
  border: 1px solid #1b2c47;
}

.dashboard__nav::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, #5b84bc 0%, #42679e 100%);
}

.nav-item {
  width: 100%;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 12px;
  border: 1px solid #263754;
  border-radius: 12px;
  background: #131f34;
  cursor: pointer;
  color: #d9e6fb;
  transition: all 180ms ease;
}

.nav-item__text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item:hover {
  background: #1a2842;
  border-color: #355281;
}

.nav-item.active {
  border-color: #4f7bc3;
  background: #1b2f52;
  box-shadow: inset 0 0 0 1px rgba(117, 160, 228, 0.25);
}

.nav-item.compact {
  width: 56px;
  height: 56px;
  padding: 0;
  gap: 0;
  justify-content: center;
  align-items: center;
}

.nav-item.compact.active {
  animation: pulseGlow 1.8s ease-in-out infinite;
}

.nav-item__icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: #e8f0ff;
  color: #29466f;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
  transition: all 0.2s ease;
}

.nav-item:hover .nav-item__icon,
.nav-item.active .nav-item__icon {
  background: #dbe8ff;
  color: #1f3e67;
}

.nav-item.compact .nav-item__icon {
  margin-bottom: 0;
}

.nav-item__text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item__text > span {
  font-size: 14px;
  font-weight: 600;
}

.nav-item__text > small {
  font-size: 12px;
  color: #9fb0cc;
}

.weather-value {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.weather-icon-chip {
  width: 22px;
  height: 22px;
  border-radius: 9999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.dashboard__footer {
  border-top: 1px solid #22324d;
  padding-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sidebar-meta {
  border: 1px solid #273754;
  border-radius: 12px;
  background: #121d32;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #cdd9ee;
}

.sidebar-meta.compact {
  align-items: center;
  gap: 4px;
}

.sidebar-meta.compact .sidebar-meta__row {
  width: 100%;
  justify-content: center;
}

.sidebar-meta.compact .weather-value {
  width: 100%;
  justify-content: center;
  transform: translateX(-3px);
}

.sidebar-meta__title {
  font-size: 12px;
  color: #8ea2c6;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.sidebar-meta__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
}

.sidebar-meta__label {
  color: #93a7c9;
}

.sidebar-meta__row strong {
  font-size: 12px;
  font-weight: 600;
  color: #e9f1ff;
}

.sidebar-meta__error {
  margin: 0;
  font-size: 11px;
  color: #f29ca3;
}

.dashboard__main {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.dashboard__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  background: #fff;
  border-bottom: 1px solid #e8edf5;
}

.dashboard__header h2 {
  margin: 0;
  font-size: 22px;
  color: #1f2937;
}

.dashboard__header p {
  margin: 4px 0 0;
  color: #778399;
  font-size: 13px;
}

.dashboard__content {
  min-width: 0;
  padding: 18px;
}

.ghost {
  border: 1px solid #2c3e5e;
  border-radius: 10px;
  background: #17233a;
  color: #d7e4f8;
  padding: 8px 12px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.ghost:hover {
  background: #1c2d4a;
}

.dashboard__actions .ghost {
  border-color: #dbe3f0;
  background: #fff;
  color: #34445c;
}

.dashboard__actions .ghost:hover {
  background: #f7f9fc;
}

@keyframes pulseGlow {
  0% {
    box-shadow: 0 0 0 0 rgba(85, 138, 224, 0.55);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(85, 138, 224, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(85, 138, 224, 0);
  }
}

@media (max-width: 1024px) {
  .dashboard {
    grid-template-columns: var(--aside-compact-width) minmax(0, 1fr);
  }

  .dashboard__aside {
    padding: 10px;
  }

  .dashboard__brand,
  .dashboard__user {
    justify-content: center;
  }

  .dashboard__brand > div:not(.brand-mark),
  .dashboard__user > div:not(.avatar) {
    display: none;
  }

  .dashboard__nav {
    align-items: center;
  }

  .nav-item {
    width: 56px;
    height: 56px;
    padding: 0;
    justify-content: center;
    align-items: center;
  }

  .nav-item small,
  .nav-item span:not(.nav-item__icon) {
    display: none;
  }

  .dashboard__footer .ghost {
    width: 56px;
    padding: 8px 0;
  }
}
</style>
