import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '../views/HomePage.vue'
import AuthPage from '../views/AuthPage.vue'
import DashboardLayout from '../layouts/DashboardLayout.vue'
import VenuesPage from '../views/VenuesPage.vue'
import BorrowPage from '../views/BorrowPage.vue'
import ProfilePage from '../views/ProfilePage.vue'
import AdminUsersPage from '../views/AdminUsersPage.vue'
import AdminAnalyticsPage from '../views/AdminAnalyticsPage.vue'
import AdminModel2DPage from '../views/AdminModel2DPage.vue'
import AdminModel3DPreviewPage from '../views/AdminModel3DPreviewPage.vue'
import NoticesPage from '../views/NoticesPage.vue'
import { useAuthStore } from '../stores/auth'
import { useToast } from '../composables/useToast'

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomePage
  },
  {
    path: '/auth',
    name: 'auth',
    component: AuthPage,
    props: (route) => ({ tab: route.query.tab || 'login' })
  },
  {
    path: '/app',
    component: DashboardLayout,
    children: [
      {
        path: 'venues',
        name: 'venues',
        component: VenuesPage,
        meta: {
          title: '场地预约',
          subtitle: '选择场地与时段，在线完成预约申请'
        }
      },
      {
        path: 'borrow',
        name: 'borrow',
        component: BorrowPage,
        meta: {
          title: '器材借用',
          subtitle: '查看可借器材并发起借用流程'
        }
      },
      {
        path: 'profile',
        name: 'profile',
        component: ProfilePage,
        meta: {
          title: '用户中心',
          subtitle: '查看账号状态与使用记录'
        }
      },
      {
        path: 'notices',
        name: 'notices',
        component: NoticesPage,
        meta: {
          title: '公告中心',
          subtitle: '查看平台已发布公告与活动通知'
        }
      },
      {
        path: 'admin-notices',
        name: 'admin-notices',
        component: NoticesPage,
        meta: {
          title: '公告管理',
          subtitle: '创建、发布、下线和维护公告内容',
          roles: ['OWNER']
        }
      },
      {
        path: 'admin-users',
        name: 'admin-users',
        component: AdminUsersPage,
        meta: {
          title: '用户管理',
          subtitle: '管理员查看与维护用户状态',
          roles: ['ADMIN', 'OWNER']
        }
      },
      {
        path: 'admin-analytics',
        name: 'admin-analytics',
        component: AdminAnalyticsPage,
        meta: {
          title: '数据分析管理',
          subtitle: '查看平台级预约与运营分析',
          roles: ['ADMIN', 'OWNER']
        }
      },
      {
        path: 'admin-model-2d',
        name: 'admin-model-2d',
        component: AdminModel2DPage,
        meta: {
          title: '2D建模管理',
          subtitle: '在数据分析管理下进行2D建模配置与预览',
          roles: ['ADMIN', 'OWNER']
        }
      },
      {
        path: 'admin-model-3d',
        name: 'admin-model-3d',
        component: AdminModel3DPreviewPage,
        meta: {
          title: '3D场馆预览',
          subtitle: '基于2D语义模型生成3D场馆可视化预览',
          roles: ['ADMIN', 'OWNER']
        }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  const { pushToast } = useToast()
  authStore.hydrate()
  if (to.path.startsWith('/app') && !authStore.isAuthed) {
    pushToast('请先登录后访问控制台', 'warning')
    return {
      path: '/auth',
      query: { tab: 'login' }
    }
  }
  if (to.path === '/auth' && authStore.isAuthed) {
    return '/app/venues'
  }
  const allowedRoles = to.meta?.roles
  if (allowedRoles?.length && !allowedRoles.includes(authStore.role)) {
    pushToast('当前账号无权限访问该页面', 'warning')
    return '/app/venues'
  }
  return true
})

export default router
