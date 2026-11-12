import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'

// 路由表说明：
// - /login 登录/注册页（无需登录）
// - 其余页面均挂在 Layout 布局下，需要登录
// - meta.roles 声明允许访问的角色；管理员路由统一放在 /admin 下
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/Login.vue'),
    meta: { title: '登录' }
  },
  {
    // 首页为工作台：展示概览统计、最近工单与快捷入口（各角色共用）
    path: '/',
    component: () => import('../views/layout/Layout.vue'),
    children: [
      {
        // 工作台首页（所有角色）：概览统计 + 快捷入口
        path: '',
        name: 'Home',
        component: () => import('../views/home/Home.vue'),
        meta: { title: '工作台' }
      },
      {
        // 提交报修（USER / VIP）
        path: 'repair/submit',
        name: 'RepairSubmit',
        component: () => import('../views/repair/Submit.vue'),
        meta: { title: '提交报修', roles: ['USER', 'VIP'] }
      },
      {
        // 我的工单（USER / VIP；ADMIN 有自己的工单时也可查看）
        path: 'repair/my',
        name: 'MyOrders',
        component: () => import('../views/repair/MyOrders.vue'),
        meta: { title: '我的工单', roles: ['USER', 'VIP', 'ADMIN'] }
      },
      {
        // 报修统计（仅 VIP）
        path: 'stats',
        name: 'Stats',
        component: () => import('../views/stats/Stats.vue'),
        meta: { title: '报修统计', roles: ['VIP'] }
      },
      {
        // 维修工：我的任务（仅 WORKER）
        path: 'worker/tasks',
        name: 'WorkerTasks',
        component: () => import('../views/worker/Tasks.vue'),
        meta: { title: '我的任务', roles: ['WORKER'] }
      },
      {
        // 维修工：抢单大厅（仅 WORKER）
        path: 'worker/pool',
        name: 'WorkerPool',
        component: () => import('../views/worker/Pool.vue'),
        meta: { title: '抢单大厅', roles: ['WORKER'] }
      },
      {
        // 维修工：我的资料（仅 WORKER）
        path: 'worker/profile',
        name: 'WorkerProfile',
        component: () => import('../views/worker/Profile.vue'),
        meta: { title: '我的资料', roles: ['WORKER'] }
      },
      {
        // 工单管理（仅 ADMIN）
        path: 'admin/orders',
        name: 'AdminOrders',
        component: () => import('../views/admin/Orders.vue'),
        meta: { title: '工单管理', roles: ['ADMIN'] }
      },
      {
        // 维修工管理（仅 ADMIN）
        path: 'admin/workers',
        name: 'AdminWorkers',
        component: () => import('../views/admin/Workers.vue'),
        meta: { title: '维修工管理', roles: ['ADMIN'] }
      },
      {
        // 故障分类管理（仅 ADMIN）
        path: 'admin/categories',
        name: 'AdminCategories',
        component: () => import('../views/admin/Categories.vue'),
        meta: { title: '故障分类管理', roles: ['ADMIN'] }
      },
      {
        // 用户管理（仅 ADMIN）
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('../views/admin/Users.vue'),
        meta: { title: '用户管理', roles: ['ADMIN'] }
      }
    ]
  },
  {
    // 兜底：未匹配路径重定向首页
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局路由守卫：
// 1. 未登录访问受保护页面 -> 跳登录页
// 2. 已登录访问登录页 -> 跳首页
// 3. 角色不在 meta.roles 内（如 USER 访问 /admin/*）-> 跳回首页并提示无权限
router.beforeEach((to) => {
  const userStore = useUserStore()
  document.title = to.meta.title ? `${to.meta.title} - 校园报修预约系统` : '校园报修预约系统'

  if (to.path === '/login') {
    // 已登录用户访问登录页直接回首页
    if (userStore.isLoggedIn) {
      return { path: '/' }
    }
    return true
  }

  // 未登录 -> 登录页
  if (!userStore.isLoggedIn) {
    return { path: '/login' }
  }

  // 访问根路径：展示工作台首页
  if (to.path === '/') {
    return true
  }

  // 角色权限校验（无权限时跳回首页并提示）
  const allowRoles = to.meta.roles
  if (allowRoles && allowRoles.length > 0 && !allowRoles.includes(userStore.role)) {
    ElMessage.warning('没有访问该页面的权限')
    return { path: '/' }
  }

  return true
})

export default router
