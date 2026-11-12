<template>
  <div class="layout">
    <!-- ================= 顶部横向导航 ================= -->
    <header class="topnav">
      <div class="nav-glow" />

      <!-- 品牌区 -->
      <router-link to="/" class="nav-brand">
        <div class="nav-logo">修</div>
        <div class="nav-brand-text">
          <strong>校园报修</strong>
          <span>Campus Repair</span>
        </div>
      </router-link>

      <!-- 横向菜单：按角色过滤，分组之间用细分割线隔开 -->
      <nav class="nav-menu">
        <template v-for="menu in menuItems" :key="menu.path">
          <span v-if="menu.showDivider" class="nav-divider" />
          <router-link
            :to="menu.path"
            class="nav-item"
            :class="{ active: activeMenu === menu.path }"
          >
            <el-icon class="nav-icon"><component :is="menu.icon" /></el-icon>
            <span class="nav-text">{{ menu.title }}</span>
            <span class="nav-underline" />
          </router-link>
        </template>
      </nav>

      <!-- 右侧：角色胶囊（身份切换） + 头像 + 退出 -->
      <div class="nav-right">
        <el-dropdown trigger="click" :disabled="switchingRole" @command="handleSwitchRole">
          <span
            class="role-chip"
            :class="[`role-${userStore.role.toLowerCase()}`, { 'is-switching': switchingRole }]"
          >
            {{ roleLabel }}
            <el-icon class="role-caret"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-for="item in roleOptions"
                :key="item.value"
                :command="item.value"
              >
                <span class="role-option">
                  <span>{{ item.label }}</span>
                  <el-icon v-if="item.value === userStore.role" class="role-check">
                    <Check />
                  </el-icon>
                </span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <div class="nav-user">
          <span class="topbar-avatar">{{ avatarText }}</span>
          <div class="nav-user-info">
            <strong>{{ displayName }}</strong>
            <span>{{ roleLabel }}</span>
          </div>
        </div>

        <el-tooltip content="退出登录" placement="bottom">
          <button class="logout-btn" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
          </button>
        </el-tooltip>
      </div>
    </header>

    <!-- ================= 主区域 ================= -->
    <section class="main-wrap">
      <!-- 页面标题：跟随路由变化 -->
      <header class="page-head">
        <h1>{{ currentTitle }}</h1>
        <p>{{ currentDesc }}</p>
      </header>

      <!-- 路由内容：以角色为 key，切换身份后强制重挂载页面以刷新数据 -->
      <main class="main-content">
        <router-view :key="userStore.role" />
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  HomeFilled,
  EditPen,
  Tickets,
  DataAnalysis,
  List,
  Menu,
  User,
  Tools,
  SwitchButton,
  ArrowDown,
  Check
} from '@element-plus/icons-vue'
import { useUserStore } from '../../store/user'
import { ROLE_MAP } from '../../utils/dict'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 身份切换中标记：防止重复点击
const switchingRole = ref(false)

// 可切换的角色列表（与角色字典保持一致）
const roleOptions = Object.keys(ROLE_MAP).map((value) => ({
  value,
  label: ROLE_MAP[value].label
}))

// 当前角色中文名（来自公共字典）
const roleLabel = computed(() => ROLE_MAP[userStore.role]?.label ?? userStore.role)

// 昵称 / 手机号，作为展示名
const displayName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.phone || '未命名用户'
)

// 头像显示：昵称首字（无昵称时取手机号末两位）
const avatarText = computed(() => {
  const name = userStore.userInfo?.nickname
  if (name) return name.slice(0, 1)
  const phone = userStore.userInfo?.phone ?? ''
  return phone.slice(-2) || 'U'
})

// 全部菜单项（含分组），roles 限定可见角色
const allGroups = [
  {
    label: '报修服务',
    items: [
      { path: '/', title: '工作台', icon: HomeFilled, roles: ['USER', 'VIP', 'ADMIN'] },
      { path: '/repair/submit', title: '提交报修', icon: EditPen, roles: ['USER', 'VIP'] },
      { path: '/repair/my', title: '我的工单', icon: Tickets, roles: ['USER', 'VIP', 'ADMIN'] },
      { path: '/stats', title: '报修统计', icon: DataAnalysis, roles: ['VIP'] }
    ]
  },
  {
    label: '维修工作',
    items: [
      { path: '/', title: '工作台', icon: HomeFilled, roles: ['WORKER'] },
      { path: '/worker/tasks', title: '我的任务', icon: Tickets, roles: ['WORKER'] },
      { path: '/worker/pool', title: '抢单大厅', icon: List, roles: ['WORKER'] },
      { path: '/worker/profile', title: '我的资料', icon: User, roles: ['WORKER'] }
    ]
  },
  {
    label: '维修管理',
    items: [
      { path: '/admin/orders', title: '工单管理', icon: List, roles: ['ADMIN'] },
      { path: '/admin/workers', title: '维修工管理', icon: Tools, roles: ['ADMIN'] },
      { path: '/admin/categories', title: '故障分类', icon: Menu, roles: ['ADMIN'] },
      { path: '/admin/users', title: '用户管理', icon: User, roles: ['ADMIN'] }
    ]
  }
]

// 横向菜单：按角色过滤后拍平，分组首项前加分割线
const menuItems = computed(() => {
  const result = []
  allGroups.forEach((group, groupIndex) => {
    const items = group.items.filter((item) => item.roles.includes(userStore.role))
    items.forEach((item, itemIndex) => {
      result.push({
        ...item,
        showDivider: groupIndex > 0 && itemIndex === 0
      })
    })
  })
  return result
})

// 当前激活菜单（与路由路径一致）
const activeMenu = computed(() => route.path)

// 顶部标题与副标题：按当前路由给出场景化说明
const currentTitle = computed(() => route.meta.title || '校园报修预约系统')

const currentDesc = computed(() => {
  const descMap = {
    '/': '工单概览、最近报修与快捷入口',
    '/repair/submit': '填写故障信息并上传照片，提交后由维修管理员处理',
    '/repair/my': '查看自己的报修记录与维修进度',
    '/stats': '会员专属：历史报修数据统计',
    '/admin/orders': '全部工单，紧急工单优先展示',
    '/admin/workers': '维护维修工账号、擅长分类与在职状态',
    '/admin/categories': '维护故障分类，报修时下拉读取',
    '/admin/users': '查看注册用户，开通或取消会员身份',
    '/worker/tasks': '查看分派给我的维修任务，接单并完成维修',
    '/worker/pool': '抢单大厅：待接单且未指派的工单，紧急优先',
    '/worker/profile': '维护个人简介与擅长分类，查看维修统计'
  }
  return descMap[route.path] || ''
})

// 退出登录：确认后清空登录态并回到登录页
async function handleLogout() {
  await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

// 切换身份（课程演示）：确认后更新角色，成功后回到工作台刷新菜单与数据
async function handleSwitchRole(role) {
  if (!role || role === userStore.role || switchingRole.value) return
  try {
    await ElMessageBox.confirm(
      '课程演示功能：切换身份会立即改变菜单与数据权限',
      '切换身份',
      { type: 'warning', confirmButtonText: '确认切换', cancelButtonText: '取消' }
    )
  } catch {
    return // 用户取消切换
  }

  switchingRole.value = true
  try {
    await userStore.switchRole(role)
    ElMessage.success(`已切换为${ROLE_MAP[role]?.label ?? role}`)
    router.replace('/')
  } catch {
    // 失败提示已由请求拦截器统一展示后端 message
  } finally {
    switchingRole.value = false
  }
}
</script>

<style scoped>
.layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #f4f7fc;
}

/* ================= 横向顶部导航 ================= */
.topnav {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 26px;
  height: 66px;
  padding: 0 28px;
  background: linear-gradient(90deg, #10294a 0%, #0d2038 55%, #0a1728 100%);
  box-shadow: 0 6px 24px rgba(10, 23, 40, 0.18);
}

/* 左侧柔光，避免纯色呆板 */
.nav-glow {
  position: absolute;
  top: -140px;
  left: 40px;
  width: 320px;
  height: 260px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(44, 107, 242, 0.45) 0%, transparent 70%);
  pointer-events: none;
}

.nav-brand {
  position: relative;
  display: flex;
  align-items: center;
  gap: 11px;
  flex-shrink: 0;
  text-decoration: none;
}

.nav-logo {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 11px;
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #2c6bf2 0%, #17bfd4 100%);
  box-shadow: 0 8px 18px rgba(44, 107, 242, 0.4);
}

.nav-brand-text strong {
  display: block;
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
  line-height: 1.2;
}

.nav-brand-text span {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.45);
  letter-spacing: 0.5px;
}

/* 横向菜单容器：窄屏可横向滚动 */
.nav-menu {
  position: relative;
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  overflow-x: auto;
  scrollbar-width: none;
}

.nav-menu::-webkit-scrollbar {
  display: none;
}

.nav-divider {
  flex-shrink: 0;
  width: 1px;
  height: 20px;
  margin: 0 10px;
  background: rgba(255, 255, 255, 0.14);
}

.nav-item {
  position: relative;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 14px;
  border-radius: 10px;
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  font-size: 14px;
  transition: background 0.22s ease, color 0.22s ease;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.nav-item.active {
  color: #fff;
  background: rgba(44, 107, 242, 0.32);
  box-shadow: inset 0 0 0 1px rgba(120, 165, 255, 0.3);
}

/* 激活项底部发光横条 */
.nav-underline {
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: -2px;
  height: 2px;
  border-radius: 2px;
  background: linear-gradient(90deg, #6ea2ff, #23d2e2);
  opacity: 0;
  transform: scaleX(0.4);
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.nav-item.active .nav-underline {
  opacity: 1;
  transform: scaleX(1);
}

.nav-icon {
  font-size: 15px;
}

.nav-item.active .nav-icon {
  color: #8ff0e4;
}

/* 右侧用户区 */
.nav-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

/* 角色胶囊标签：点击展开身份切换 */
.role-chip {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #cfe0ff;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.16);
  cursor: pointer;
  user-select: none;
  transition: opacity 0.2s ease, background 0.2s ease;
}

.role-chip:hover {
  background: rgba(255, 255, 255, 0.2);
}

.role-chip:focus-visible {
  outline: none;
}

/* 切换请求进行中：弱化并禁用点击反馈 */
.role-chip.is-switching {
  opacity: 0.6;
  cursor: not-allowed;
}

.role-caret {
  font-size: 12px;
}

/* 下拉菜单项（弹出层挂载在 body，需全局选择器） */
:global(.role-option) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  min-width: 104px;
}

:global(.role-check) {
  color: #2c6bf2;
  font-size: 14px;
}

/* 角色色调：浅色文字 + 半透明底色，与深色导航融合 */
.role-chip.role-vip {
  color: #ffd591;
  background: rgba(245, 165, 36, 0.2);
  border-color: rgba(245, 165, 36, 0.35);
}

.role-chip.role-worker {
  color: #9ff0d8;
  background: rgba(15, 181, 131, 0.2);
  border-color: rgba(15, 181, 131, 0.35);
}

.role-chip.role-admin {
  color: #ffb3b3;
  background: rgba(239, 75, 75, 0.2);
  border-color: rgba(239, 75, 75, 0.35);
}

.nav-user {
  display: flex;
  align-items: center;
  gap: 9px;
}

.topbar-avatar {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 700;
  color: #0a1728;
  background: linear-gradient(135deg, #8ff0e4, #a9c8ff);
}

.nav-user-info strong {
  display: block;
  max-width: 120px;
  font-size: 13px;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.nav-user-info span {
  display: block;
  margin-top: 1px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}

.logout-btn {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 9px;
  color: rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.08);
  cursor: pointer;
  transition: all 0.2s ease;
}

.logout-btn:hover {
  color: #fff;
  background: rgba(239, 75, 75, 0.85);
}

/* ================= 主区域 ================= */
.main-wrap {
  flex: 1;
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
}

.page-head {
  padding: 22px 28px 0;
}

.page-head h1 {
  margin: 0;
  font-size: 21px;
  font-weight: 700;
  color: #12294a;
  letter-spacing: -0.2px;
}

.page-head p {
  margin: 5px 0 0;
  font-size: 12px;
  color: #7a879b;
}

.main-content {
  flex: 1;
  padding: 18px 28px 40px;
}

/* 中等屏幕：隐藏用户文字，保留头像与角色胶囊 */
@media (max-width: 1180px) {
  .nav-user-info {
    display: none;
  }

  .topnav {
    gap: 18px;
    padding: 0 18px;
  }
}

/* 小屏：隐藏品牌副标题，菜单横向滚动 */
@media (max-width: 900px) {
  .nav-brand-text span {
    display: none;
  }

  .nav-item {
    padding: 0 11px;
  }

  .page-head {
    padding: 18px 16px 0;
  }

  .main-content {
    padding: 16px 16px 30px;
  }
}

/* 超小屏：仅保留图标菜单 */
@media (max-width: 720px) {
  .nav-brand-text,
  .nav-text,
  .nav-divider {
    display: none;
  }

  .nav-item {
    padding: 0 12px;
  }

  .role-chip {
    padding: 4px 9px;
  }
}
</style>
