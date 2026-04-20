import { createRouter, createWebHistory } from "vue-router";
import HomePage from "../views/HomePage.vue";
import AuthPage from "../views/AuthPage.vue";
import DashboardLayout from "../layouts/DashboardLayout.vue";
import VenuesPage from "../views/VenuesPage.vue";
import BorrowPage from "../views/BorrowPage.vue";
import BorrowApprovalPage from "../views/BorrowApprovalPage.vue";
import ProfilePage from "../views/ProfilePage.vue";
import AdminUsersPage from "../views/AdminUsersPage.vue";
import AdminAnalyticsPage from "../views/AdminAnalyticsPage.vue";
import NoticesPage from "../views/NoticesPage.vue";
import FloorPlanPage from "../views/FloorPlanPage.vue";
import { useAuthStore } from "../stores/auth";
import { useToast } from "../composables/useToast";

const routes = [
    {
        path: "/",
        name: "home",
        component: HomePage,
    },
    {
        path: "/auth",
        name: "auth",
        component: AuthPage,
        props: (route) => ({ tab: route.query.tab || "login" }),
    },
    {
        path: "/app",
        component: DashboardLayout,
        children: [
            {
                path: "venues",
                name: "venues",
                component: VenuesPage,
                props: { module: "venue" },
                meta: {
                    title: "场地管理",
                    subtitle: "维护场地信息、状态与可预约资源",
                },
            },
            {
                path: "bookings",
                name: "bookings",
                component: VenuesPage,
                props: { module: "booking" },
                meta: {
                    title: "预约管理",
                    subtitle: "查看预约记录并执行审核/核销操作",
                },
            },
            {
                path: "borrow",
                name: "borrow",
                component: BorrowPage,
                meta: {
                    title: "器材借用",
                    subtitle: "查看可借器材并发起借用流程",
                },
            },
            {
                path: "my-borrows",
                name: "my-borrows",
                component: BorrowPage,
                meta: {
                    title: "我的借用",
                    subtitle: "查看借用记录与状态",
                },
            },
            {
                path: "borrow-approval",
                name: "borrow-approval",
                component: BorrowApprovalPage,
                meta: {
                    title: "借用审批",
                    subtitle: "审批借用申请并确认归还",
                    roles: ["ADMIN", "OWNER"],
                },
            },
            {
                path: "profile",
                name: "profile",
                component: ProfilePage,
                meta: {
                    title: "用户中心",
                    subtitle: "查看账号状态与使用记录",
                },
            },
            {
                path: "notices",
                name: "notices",
                component: NoticesPage,
                meta: {
                    title: "公告中心",
                    subtitle: "查看平台已发布公告与活动通知",
                },
            },
            {
                path: "floor-plans",
                name: "floor-plans",
                component: FloorPlanPage,
                meta: {
                    title: "场地图展示",
                    subtitle: "查看体育馆场地图与分区信息",
                },
            },
            {
                path: "floor-plans-admin",
                name: "floor-plans-admin",
                component: FloorPlanPage,
                meta: {
                    title: "场地图管理",
                    subtitle: "维护场地图画布并发布可视化分区",
                    roles: ["ADMIN", "OWNER"],
                },
            },
            {
                path: "admin-users",
                name: "admin-users",
                component: AdminUsersPage,
                meta: {
                    title: "用户管理",
                    subtitle: "管理员查看与维护用户状态",
                    roles: ["ADMIN", "OWNER"],
                },
            },
            {
                path: "admin-analytics",
                name: "admin-analytics",
                component: AdminAnalyticsPage,
                meta: {
                    title: "数据分析管理",
                    subtitle: "查看平台级预约与运营分析",
                    roles: ["ADMIN", "OWNER"],
                },
            },
        ],
    },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

router.beforeEach((to) => {
    const authStore = useAuthStore();
    const { pushToast } = useToast();
    authStore.hydrate();
    if (to.path.startsWith("/app") && !authStore.isAuthed) {
        pushToast("请先登录后访问控制台", "warning");
        return {
            path: "/auth",
            query: { tab: "login" },
        };
    }
    if (to.path === "/auth" && authStore.isAuthed) {
        return authStore.role === "USER" ? "/app/notices" : "/app/venues";
    }
    const allowedRoles = to.meta?.roles;
    if (allowedRoles?.length && !allowedRoles.includes(authStore.role)) {
        pushToast("当前账号无权限访问该页面", "warning");
        return authStore.role === "USER" ? "/app/notices" : "/app/venues";
    }
    return true;
});

export default router;
