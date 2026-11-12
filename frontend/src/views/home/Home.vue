<template>
  <div class="dashboard">
    <!-- ========== 顶部欢迎横幅 ========== -->
    <section class="hero-banner welcome">
      <div class="welcome-text">
        <span class="welcome-badge">{{ greeting }}，{{ displayName }}</span>
        <h3>{{ userStore.role === 'ADMIN' ? '今天的维修任务已就绪' : '有故障？交给我们' }}</h3>
        <p>
          {{ userStore.role === 'ADMIN'
            ? '紧急工单已在列表优先展示，请及时接单并更新维修进度。'
            : '拍照上传故障照片，选择位置与故障类型，维修管理员会尽快处理。' }}
        </p>
        <div class="welcome-actions">
          <template v-if="userStore.role === 'ADMIN'">
            <el-button class="ghost-btn" @click="router.push('/admin/orders')">去处理工单</el-button>
            <el-button class="plain-btn" @click="router.push('/admin/users')">用户管理</el-button>
          </template>
          <template v-else>
            <el-button class="ghost-btn" @click="router.push('/repair/submit')">立即报修</el-button>
            <el-button class="plain-btn" @click="router.push('/repair/my')">查看我的工单</el-button>
          </template>
        </div>
      </div>
      <div class="welcome-visual">
        <div class="vs-ring vs-ring-1" />
        <div class="vs-ring vs-ring-2" />
        <div class="vs-card">
          <span class="vs-dot" />
          <div>
            <strong>{{ statList[1]?.value ?? 0 }}</strong>
            <em>{{ userStore.role === 'ADMIN' ? '待接单工单' : '待接单' }}</em>
          </div>
        </div>
      </div>
    </section>

    <!-- ========== 数据磁贴 ========== -->
    <div class="stat-grid">
      <div
        v-for="stat in statList"
        :key="stat.label"
        class="stat-tile"
        :class="stat.tone"
      >
        <div class="stat-tile-label">{{ stat.label }}</div>
        <div class="stat-tile-value">{{ stat.value }}</div>
        <div class="stat-tile-icon">
          <el-icon><component :is="stat.icon" /></el-icon>
        </div>
      </div>
    </div>

    <!-- ========== 两栏：最近工单 + 快捷入口 ========== -->
    <div class="dashboard-grid">
      <!-- 最近工单 -->
      <section class="panel recent-panel">
        <div class="panel-hd">
          <div class="panel-title">
            {{ userStore.role === 'ADMIN' ? '最新报修工单' : '我的最近工单' }}
          </div>
          <el-button link type="primary" @click="router.push(userStore.role === 'ADMIN' ? '/admin/orders' : '/repair/my')">
            全部工单
          </el-button>
        </div>

        <div v-loading="loading" class="recent-list">
          <div v-if="!loading && recentOrders.length === 0" class="empty-state">
            <div class="empty-icon">📋</div>
            暂无工单记录
          </div>

          <div v-for="order in recentOrders" :key="order.id" class="recent-item">
            <div class="recent-main">
              <div class="recent-title">
                <span class="order-no">{{ order.orderNo }}</span>
                <span v-if="order.urgent === 1" class="urgent-flag">紧急</span>
              </div>
              <p class="recent-desc">
                {{ order.categoryName }} · {{ formatLocation(order) }}
              </p>
            </div>
            <div class="recent-side">
              <span class="status-pill" :class="`st-${order.status.toLowerCase()}`">
                {{ dictLabel(STATUS_MAP, order.status) }}
              </span>
              <time>{{ formatTime(order.createTime) }}</time>
            </div>
          </div>
        </div>
      </section>

      <!-- 快捷入口 & 角色提示 -->
      <section class="quick-wrap">
        <div class="panel">
          <div class="panel-hd">
            <div class="panel-title">快捷入口</div>
          </div>
          <div class="quick-grid">
            <button
              v-for="item in quickLinks"
              :key="item.path"
              class="quick-item"
              @click="router.push(item.path)"
            >
              <span class="quick-icon" :style="{ background: item.bg }">
                <el-icon><component :is="item.icon" /></el-icon>
              </span>
              <span class="quick-label">{{ item.title }}</span>
              <span class="quick-desc">{{ item.desc }}</span>
            </button>
          </div>
        </div>

        <div class="panel member-panel">
          <div class="panel-hd">
            <div class="panel-title">身份权益</div>
          </div>
          <div class="member-body">
            <span class="member-tag">{{ roleLabel }}</span>
            <p v-if="userStore.role === 'VIP'">
              已解锁：单次 5 张故障图、自选上门时间、紧急标记、历史报修统计。
            </p>
            <p v-else-if="userStore.role === 'USER'">
              普通用户单次可上传 1 张图片，仅可查看自己的工单。升级会员请联系维修管理员。
            </p>
            <p v-else>
              管理员可管理全部工单、故障分类与注册用户，并可为用户开通会员。
            </p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Tickets,
  Timer,
  Tools,
  CircleCheck,
  EditPen,
  DataAnalysis,
  List,
  User,
  Menu
} from '@element-plus/icons-vue'
import { useUserStore } from '../../store/user'
import { getMyOrders, getAdminOrderPage } from '../../api/order'
import { STATUS_MAP, ROLE_MAP, dictLabel, formatLocation } from '../../utils/dict'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
// 最近工单（最多 5 条），用于首页概览
const recentOrders = ref([])
// 按状态统计的数量
const statusCount = ref({ PENDING: 0, IN_PROGRESS: 0, FINISHED: 0, REJECTED: 0 })
const total = ref(0)

// 展示名：昵称优先，其次手机号
const displayName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.phone || '同学'
)

// 角色中文名
const roleLabel = computed(() => ROLE_MAP[userStore.role]?.label ?? userStore.role)

// 根据当前时间生成问候语
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

// 统计磁贴配置
const statList = computed(() => {
  const isAdmin = userStore.role === 'ADMIN'
  return [
    {
      label: isAdmin ? '全部工单' : '我的工单',
      value: total.value,
      tone: 'is-brand',
      icon: Tickets
    },
    { label: '待接单', value: statusCount.value.PENDING, tone: 'is-amber', icon: Timer },
    { label: '维修中', value: statusCount.value.IN_PROGRESS, tone: 'is-cyan', icon: Tools },
    { label: '已完成', value: statusCount.value.FINISHED, tone: 'is-mint', icon: CircleCheck }
  ]
})

// 快捷入口：按角色给出不同的功能入口
const quickLinks = computed(() => {
  if (userStore.role === 'ADMIN') {
    return [
      { path: '/admin/orders', title: '工单管理', desc: '接单与状态更新', icon: List, bg: 'linear-gradient(135deg,#2c6bf2,#17bfd4)' },
      { path: '/admin/categories', title: '故障分类', desc: '维护分类项', icon: Menu, bg: 'linear-gradient(135deg,#f7b955,#f5a524)' },
      { path: '/admin/users', title: '用户管理', desc: '会员开通', icon: User, bg: 'linear-gradient(135deg,#3fd0a2,#0fb583)' }
    ]
  }
  const links = [
    { path: '/repair/submit', title: '提交报修', desc: '拍照上传故障', icon: EditPen, bg: 'linear-gradient(135deg,#2c6bf2,#17bfd4)' },
    { path: '/repair/my', title: '我的工单', desc: '查看维修进度', icon: Tickets, bg: 'linear-gradient(135deg,#3fd0a2,#0fb583)' }
  ]
  if (userStore.role === 'VIP') {
    links.push({
      path: '/stats',
      title: '报修统计',
      desc: '历史数据图表',
      icon: DataAnalysis,
      bg: 'linear-gradient(135deg,#f7b955,#f5a524)'
    })
  }
  return links
})

// 加载首页数据：拉取一页工单，本地统计各状态数量
async function loadDashboard() {
  loading.value = true
  try {
    const params = { pageNum: 1, pageSize: 100 }
    const res = userStore.role === 'ADMIN'
      ? await getAdminOrderPage(params)
      : await getMyOrders(params)

    const records = res.data?.records ?? []
    total.value = res.data?.total ?? 0

    // 统计四种状态的数量
    const counter = { PENDING: 0, IN_PROGRESS: 0, FINISHED: 0, REJECTED: 0 }
    records.forEach((item) => {
      if (counter[item.status] !== undefined) counter[item.status] += 1
    })
    statusCount.value = counter
    recentOrders.value = records.slice(0, 5)
  } finally {
    loading.value = false
  }
}

// 时间格式化：只保留 MM-DD HH:mm
function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(5, 16)
}

onMounted(loadDashboard)
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* ========== 欢迎横幅 ========== */
.welcome {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 28px 30px;
  margin-bottom: 18px;
}

.welcome-text {
  position: relative;
  z-index: 2;
}

.welcome-badge {
  display: inline-block;
  padding: 4px 12px;
  margin-bottom: 12px;
  border-radius: 999px;
  font-size: 12px;
  color: #fff;
  background: rgba(255, 255, 255, 0.18);
}

.welcome h3 {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.4px;
}

.welcome p {
  max-width: 520px;
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.85);
}

.welcome-actions {
  display: flex;
  gap: 10px;
  margin-top: 18px;
}

/* 横幅上的两种按钮：实心白 / 描边白 */
.ghost-btn {
  border: none;
  color: #1b47b8;
  background: #fff;
  font-weight: 600;
  box-shadow: 0 8px 20px rgba(8, 30, 70, 0.18);
}

.ghost-btn:hover {
  color: #1b47b8;
  background: #f4f8ff;
}

.plain-btn {
  color: #fff;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.4);
}

.plain-btn:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.24);
}

/* 右侧装饰：圆环 + 数据卡片 */
.welcome-visual {
  position: relative;
  width: 190px;
  height: 120px;
  flex-shrink: 0;
}

.vs-ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.28);
}

.vs-ring-1 {
  width: 140px;
  height: 140px;
  right: -20px;
  top: -20px;
}

.vs-ring-2 {
  width: 90px;
  height: 90px;
  right: 40px;
  bottom: -40px;
  border-color: rgba(255, 255, 255, 0.16);
}

.vs-card {
  position: absolute;
  right: 12px;
  top: 26px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.28);
  backdrop-filter: blur(8px);
}

.vs-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #8ff0e4;
  box-shadow: 0 0 0 5px rgba(143, 240, 228, 0.22);
}

.vs-card strong {
  display: block;
  font-size: 26px;
  line-height: 1;
  color: #fff;
}

.vs-card em {
  display: block;
  margin-top: 5px;
  font-size: 11px;
  font-style: normal;
  color: rgba(255, 255, 255, 0.8);
}

/* ========== 两栏布局 ========== */
.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr);
  gap: 16px;
}

/* 最近工单列表 */
.recent-list {
  min-height: 120px;
}

.recent-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 13px 12px;
  border-radius: 12px;
  transition: background 0.2s ease;
}

.recent-item:hover {
  background: #f7f9fd;
}

.recent-item + .recent-item {
  border-top: 1px dashed #eef2f9;
}

.recent-main {
  min-width: 0;
}

.recent-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-no {
  font-size: 13px;
  font-weight: 600;
  color: #1b2b45;
  font-family: 'SF Mono', Consolas, monospace;
}

.urgent-flag {
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  color: #b03636;
  background: #fdeeee;
}

.recent-desc {
  margin: 5px 0 0;
  font-size: 12px;
  color: #7a879b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.recent-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  flex-shrink: 0;
}

.recent-side time {
  font-size: 11px;
  color: #a9b4c6;
}

/* 状态胶囊 */
.status-pill {
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}

.st-pending {
  color: #a76a05;
  background: #fff5e3;
}

.st-in_progress {
  color: #1b56d0;
  background: #eef3ff;
}

.st-finished {
  color: #0a7f5e;
  background: #e7f8f2;
}

.st-rejected {
  color: #b03636;
  background: #fdeeee;
}

/* ========== 快捷入口 ========== */
.quick-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 14px;
  border: 1px solid #eef2f9;
  border-radius: 14px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: all 0.22s ease;
}

.quick-item:hover {
  border-color: #d6e2fd;
  background: #f8fbff;
  transform: translateY(-2px);
  box-shadow: 0 10px 22px rgba(18, 41, 74, 0.07);
}

.quick-icon {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 11px;
  color: #fff;
  font-size: 17px;
}

.quick-label {
  font-size: 13px;
  font-weight: 600;
  color: #1b2b45;
}

.quick-desc {
  font-size: 11px;
  color: #a9b4c6;
}

/* 会员权益 */
.member-body {
  font-size: 13px;
  line-height: 1.8;
  color: #5b6b80;
}

.member-tag {
  display: inline-block;
  padding: 3px 12px;
  margin-bottom: 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #2c6bf2;
  background: #eef3ff;
}

.member-body p {
  margin: 0;
}

/* 响应式 */
@media (max-width: 1100px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .welcome-visual {
    display: none;
  }
}
</style>
