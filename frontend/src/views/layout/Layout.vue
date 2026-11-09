<template>
  <el-container class="layout">
    <!-- 顶部导航栏 -->
    <el-header class="header">
      <div class="logo">校园报修预约系统</div>
      <div class="user-area">
        <el-tag :type="roleTagType" effect="dark" size="small">{{ roleLabel }}</el-tag>
        <span class="nickname">{{ userStore.userInfo?.nickname || userStore.userInfo?.phone }}</span>
        <el-button type="danger" plain size="small" @click="handleLogout">退出登录</el-button>
      </div>
    </el-header>

    <el-container class="body">
      <!-- 左侧菜单：根据登录角色动态生成 -->
      <el-aside width="200px" class="aside">
        <el-menu :default-active="activeMenu" router>
          <template v-for="menu in menus" :key="menu.path">
            <el-menu-item :index="menu.path">
              <el-icon><component :is="menu.icon" /></el-icon>
              <span>{{ menu.title }}</span>
            </el-menu-item>
          </template>
        </el-menu>
      </el-aside>

      <!-- 右侧内容区 -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  EditPen,
  Tickets,
  DataAnalysis,
  List,
  Menu,
  User
} from '@element-plus/icons-vue'
import { useUserStore } from '../../store/user'
import { ROLE_MAP } from '../../utils/dict'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 当前角色中文名与 tag 颜色（来自公共字典）
const roleLabel = computed(() => ROLE_MAP[userStore.role]?.label ?? userStore.role)
const roleTagType = computed(() => ROLE_MAP[userStore.role]?.tagType ?? 'info')

// 全部可选菜单（path 与路由一致，roles 限定可见角色）
const allMenus = [
  { path: '/repair/submit', title: '提交报修', icon: EditPen, roles: ['USER', 'VIP'] },
  // 管理员也可以查看自己的工单（管理员没有提交报修菜单）
  { path: '/repair/my', title: '我的工单', icon: Tickets, roles: ['USER', 'VIP', 'ADMIN'] },
  { path: '/stats', title: '报修统计', icon: DataAnalysis, roles: ['VIP'] },
  { path: '/admin/orders', title: '工单管理', icon: List, roles: ['ADMIN'] },
  { path: '/admin/categories', title: '故障分类管理', icon: Menu, roles: ['ADMIN'] },
  { path: '/admin/users', title: '用户管理', icon: User, roles: ['ADMIN'] }
]

// 按当前角色过滤菜单
const menus = computed(() => allMenus.filter((m) => m.roles.includes(userStore.role)))

// 当前激活菜单（与路由路径一致）
const activeMenu = computed(() => route.path)

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
  height: 100%;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #001529;
  color: #fff;
}

.logo {
  font-size: 18px;
  font-weight: 600;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nickname {
  font-size: 14px;
}

.body {
  height: calc(100% - 60px);
}

.aside {
  border-right: 1px solid #e4e7ed;
  background: #fff;
}

.aside .el-menu {
  border-right: none;
}

.main {
  background: #f5f7fa;
  overflow-y: auto;
}
</style>
