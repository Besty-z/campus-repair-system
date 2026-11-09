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
    // 首页不固定重定向：由守卫根据角色跳转到各自首页（管理员 -> 工单管理，普通用户/会员 -> 提交报修）
    path: '/',
    component: () => import('../views/layout/Layout.vue'),
    children: [
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
        // 工单管理（仅 ADMIN）
        path: 'admin/orders',
        name: 'AdminOrders',
        component: () => import('../views/admin/Orders.vue'),
        meta: { title: '工单管理', roles: ['ADMIN'] }
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

  // 访问根路径：按角色跳转到对应首页
  if (to.path === '/') {
    return { path: userStore.role === 'ADMIN' ? '/admin/orders' : '/repair/submit' }
  }

  // 角色权限校验（无权限时跳回本角色首页并提示）
  const allowRoles = to.meta.roles
  if (allowRoles && allowRoles.length > 0 && !allowRoles.includes(userStore.role)) {
    ElMessage.warning('没有访问该页面的权限')
    return { path: userStore.role === 'ADMIN' ? '/admin/orders' : '/repair/submit' }
  }

  return true
})

export default router
