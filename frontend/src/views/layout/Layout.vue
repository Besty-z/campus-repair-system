<template>
  <div class="layout">
    <!-- ================= 左侧深色导航 ================= -->
    <aside class="sidebar">
      <div class="side-glow" />

      <!-- 品牌区 -->
      <div class="side-brand">
        <div class="side-logo">修</div>
        <div class="side-brand-text">
          <strong>校园报修</strong>
          <span>Campus Repair</span>
        </div>
      </div>

      <!-- 菜单：按角色分组展示 -->
      <nav class="side-nav">
        <template v-for="group in menuGroups" :key="group.label">
          <p class="nav-group">{{ group.label }}</p>
          <router-link
            v-for="menu in group.items"
            :key="menu.path"
            :to="menu.path"
            class="nav-item"
            :class="{ active: activeMenu === menu.path }"
          >
            <span class="nav-icon">
              <el-icon><component :is="menu.icon" /></el-icon>
            </span>
            <span class="nav-text">{{ menu.title }}</span>
            <span class="nav-bar" />
          </router-link>
        </template>
      </nav>

      <!-- 侧边栏底部：当前登录用户卡片 -->
      <div class="side-user">
        <div class="side-avatar">{{ avatarText }}</div>
        <div class="side-user-info">
          <strong>{{ displayName }}</strong>
          <span>{{ roleLabel }}</span>
        </div>
        <el-tooltip content="退出登录" placement="top">
          <button class="logout-btn" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
          </button>
        </el-tooltip>
      </div>
    </aside>

    <!-- ================= 右侧主区域 ================= -->
    <section class="main-wrap">
      <!-- 顶部栏：页面标题 + 角色标识 -->
      <header class="topbar">
        <div class="topbar-title">
          <h1>{{ currentTitle }}</h1>
          <p>{{ currentDesc }}</p>
        </div>
        <div class="topbar-right">
          <span class="role-chip" :class="`role-${userStore.role.toLowerCase()}`">
            {{ roleLabel }}
          </span>
          <span class="topbar-avatar">{{ avatarText }}</span>
        </div>
      </header>

      <!-- 路由内容 -->
      <main class="main-content">
        <router-view />
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
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
  SwitchButton
} from '@element-plus/icons-vue'
import { useUserStore } from '../../store/user'
import { ROLE_MAP } from '../../utils/dict'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

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
    label: '维修管理',
    items: [
      { path: '/admin/orders', title: '工单管理', icon: List, roles: ['ADMIN'] },
      { path: '/admin/categories', title: '故障分类', icon: Menu, roles: ['ADMIN'] },
      { path: '/admin/users', title: '用户管理', icon: User, roles: ['ADMIN'] }
    ]
  }
]

// 按当前角色过滤：空分组不展示
const menuGroups = computed(() =>
  allGroups
    .map((group) => ({
      label: group.label,
      items: group.items.filter((item) => item.roles.includes(userStore.role))
    }))
    .filter((group) => group.items.length > 0)
)

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
    '/admin/categories': '维护故障分类，报修时下拉读取',
    '/admin/users': '查看注册用户，开通或取消会员身份'
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
</script>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
  background: #f4f7fc;
}

/* ================= 侧边栏 ================= */
.sidebar {
  position: sticky;
  top: 0;
  width: 232px;
  height: 100vh;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  padding: 22px 14px 16px;
  background: linear-gradient(180deg, #10294a 0%, #0a1728 100%);
  overflow: hidden;
}

/* 顶部柔光，避免纯色呆板 */
.side-glow {
  position: absolute;
  top: -120px;
  left: -60px;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(44, 107, 242, 0.42) 0%, transparent 70%);
  pointer-events: none;
}

.side-brand {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 8px 22px;
  margin-bottom: 6px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.side-logo {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  font-size: 19px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #2c6bf2 0%, #17bfd4 100%);
  box-shadow: 0 8px 18px rgba(44, 107, 242, 0.4);
}

.side-brand-text strong {
  display: block;
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
}

.side-brand-text span {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.45);
  letter-spacing: 0.5px;
}

.side-nav {
  position: relative;
  flex: 1;
  padding-top: 10px;
  overflow-y: auto;
}

.nav-group {
  margin: 14px 0 8px;
  padding-left: 12px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 1px;
  color: rgba(255, 255, 255, 0.35);
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 11px;
  height: 42px;
  padding: 0 12px;
  margin-bottom: 6px;
  border-radius: 12px;
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  font-size: 14px;
  transition: background 0.22s ease, color 0.22s ease;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.07);
  color: #fff;
}

.nav-item.active {
  color: #fff;
  background: rgba(44, 107, 242, 0.32);
  box-shadow: inset 0 0 0 1px rgba(120, 165, 255, 0.32);
}

/* 激活项左侧发光竖条 */
.nav-bar {
  position: absolute;
  left: 0;
  top: 50%;
  width: 3px;
  height: 0;
  border-radius: 0 4px 4px 0;
  background: linear-gradient(180deg, #6ea2ff, #23d2e2);
  transform: translateY(-50%);
  transition: height 0.25s ease;
}

.nav-item.active .nav-bar {
  height: 20px;
}

.nav-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  font-size: 15px;
  background: rgba(255, 255, 255, 0.06);
}

.nav-item.active .nav-icon {
  background: rgba(255, 255, 255, 0.16);
  color: #8ff0e4;
}

/* 底部用户卡片 */
.side-user {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.side-avatar {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 11px;
  font-size: 14px;
  font-weight: 700;
  color: #0a1728;
  background: linear-gradient(135deg, #8ff0e4, #a9c8ff);
}

.side-user-info {
  flex: 1;
  min-width: 0;
}

.side-user-info strong {
  display: block;
  font-size: 13px;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.side-user-info span {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}

.logout-btn {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 9px;
  color: rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.06);
  cursor: pointer;
  transition: all 0.2s ease;
}

.logout-btn:hover {
  color: #fff;
  background: rgba(239, 75, 75, 0.8);
}

/* ================= 主区域 ================= */
.main-wrap {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 28px;
  background: rgba(255, 255, 255, 0.82);
  border-bottom: 1px solid #eef2f9;
  backdrop-filter: blur(12px);
}

.topbar-title h1 {
  margin: 0;
  font-size: 19px;
  font-weight: 700;
  color: #12294a;
  letter-spacing: -0.2px;
}

.topbar-title p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #7a879b;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 角色胶囊标签 */
.role-chip {
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #2c6bf2;
  background: #eef3ff;
}

.role-chip.role-vip {
  color: #a76a05;
  background: #fff5e3;
}

.role-chip.role-admin {
  color: #b03636;
  background: #fdeeee;
}

.topbar-avatar {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #2c6bf2, #17bfd4);
}

.main-content {
  flex: 1;
  padding: 22px 28px 40px;
}

/* 小屏：侧边栏收窄为图标栏 */
@media (max-width: 1080px) {
  .sidebar {
    width: 76px;
    padding: 18px 10px 14px;
  }

  .side-brand-text,
  .nav-group,
  .nav-text,
  .side-user-info {
    display: none;
  }

  .side-brand {
    justify-content: center;
    padding: 0 0 18px;
  }

  .nav-item {
    justify-content: center;
    padding: 0;
  }

  .side-user {
    justify-content: center;
    padding: 10px;
  }

  .logout-btn {
    display: none;
  }

  .main-content {
    padding: 18px 16px 30px;
  }

  .topbar {
    padding: 14px 16px;
  }
}
</style>
