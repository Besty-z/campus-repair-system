<template>
  <div class="dashboard">
    <!-- ========== 服务商城：广告轮播 ========== -->
    <section class="ad-carousel">
      <el-carousel height="248px" :interval="5000" arrow="hover">
        <el-carousel-item v-for="ad in adSlides" :key="ad.id">
          <div
            class="ad-slide"
            :class="{ 'is-fallback': !ad.imageUrl }"
            :style="ad.imageUrl ? { backgroundImage: `url(${ad.imageUrl})` } : {}"
          >
            <div class="ad-shade" />
            <div class="ad-copy">
              <h2>{{ ad.title }}</h2>
              <p v-if="ad.subtitle">{{ ad.subtitle }}</p>
              <el-button class="ad-btn" @click="goSubmit()">立即报修</el-button>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </section>

    <!-- ========== 服务保障条 ========== -->
    <section class="service-bar">
      <div v-for="item in guarantees" :key="item.text" class="guarantee-item">
        <span class="guarantee-icon">
          <el-icon><component :is="item.icon" /></el-icon>
        </span>
        <span>{{ item.text }}</span>
      </div>
    </section>

    <!-- ========== 服务分类网格（含价格与会员价） ========== -->
    <section class="panel service-panel">
      <div class="panel-hd">
        <div class="panel-title">维修服务</div>
        <span class="service-note">明码标价 · 认证师傅上门</span>
      </div>

      <div v-if="serviceList.length" class="service-grid">
        <article
          v-for="item in serviceList"
          :key="item.id"
          class="service-card"
          @click="goSubmit(item.id)"
        >
          <span class="service-badge">8 折</span>
          <div class="service-thumb">
            <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.name" />
            <span v-else class="thumb-fallback">{{ (item.name || '服').slice(0, 1) }}</span>
          </div>
          <div class="service-info">
            <h4 class="service-name">{{ item.name }}</h4>
            <div class="service-price">
              <span class="price-now">¥{{ formatPrice(item.price) }}<em>起</em></span>
              <span class="price-vip">会员 ¥{{ formatPrice(item.price * 0.8) }}</span>
            </div>
            <el-button class="service-btn" @click.stop="goSubmit(item.id)">立即报修</el-button>
          </div>
        </article>
      </div>

      <div v-else class="empty-state">
        <div class="empty-icon">🛠️</div>
        暂无可预约的维修服务
      </div>
    </section>

    <!-- ========== 顶部欢迎横幅 ========== -->
    <section class="hero-banner welcome">
      <div class="welcome-text">
        <span class="welcome-badge">{{ greeting }}，{{ displayName }}</span>
        <h3>{{ heroTitle }}</h3>
        <p>{{ heroDesc }}</p>
        <div class="welcome-actions">
          <template v-if="userStore.role === 'ADMIN'">
            <el-button class="ghost-btn" @click="router.push('/admin/orders')">去处理工单</el-button>
            <el-button class="plain-btn" @click="router.push('/admin/users')">用户管理</el-button>
          </template>
          <template v-else-if="userStore.role === 'WORKER'">
            <el-button class="ghost-btn" @click="router.push('/worker/tasks')">我的任务</el-button>
            <el-button class="plain-btn" @click="router.push('/worker/pool')">去抢单</el-button>
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
            <em>{{ userStore.role === 'ADMIN' ? '待接单工单' : userStore.role === 'WORKER' ? '待接单任务' : '待接单' }}</em>
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
          <div class="panel-title">{{ recentTitle }}</div>
          <el-button link type="primary" @click="router.push(recentLink)">全部工单</el-button>
        </div>

        <div v-loading="loading" class="recent-list">
          <div v-if="!loading && recentOrders.length === 0" class="empty-state">
            <img
              class="empty-image"
              :src="EMPTY_IMAGE"
              alt="暂无工单插图"
              loading="lazy"
            />
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

        <!-- 维修工资料卡：姓名 / 擅长分类 / 完成数 / 评分 -->
        <div v-if="userStore.role === 'WORKER'" class="panel worker-panel">
          <div class="panel-hd">
            <div class="panel-title">维修工资料</div>
            <el-button link type="primary" @click="router.push('/worker/profile')">编辑</el-button>
          </div>
          <div v-if="workerProfile" class="worker-body">
            <div class="worker-head">
              <span class="worker-avatar">{{ (workerProfile.name || '维').slice(0, 1) }}</span>
              <div class="worker-id">
                <strong>{{ workerProfile.name || '-' }}</strong>
                <span>{{ workerProfile.phone || '-' }}</span>
              </div>
              <span
                class="status-pill"
                :class="workerProfile.status === 1 ? 'st-finished' : 'st-rejected'"
              >
                {{ workerProfile.status === 1 ? '在职' : '停用' }}
              </span>
            </div>
            <div class="worker-metrics">
              <div>
                <em>{{ workerProfile.completedCount ?? 0 }}</em>
                <span>完成工单</span>
              </div>
              <div>
                <em>{{ workerRatingText }}</em>
                <span>平均评分</span>
              </div>
            </div>
            <div class="worker-cats">
              <span
                v-for="name in workerProfile.categoryNames || []"
                :key="name"
                class="cat-tag"
              >
                {{ name }}
              </span>
              <span v-if="!(workerProfile.categoryNames || []).length" class="muted">
                未设置擅长分类
              </span>
            </div>
          </div>
          <div v-else class="empty-state">暂无维修工资料</div>
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
            <p v-else-if="userStore.role === 'WORKER'">
              维修工可查看分派任务、在抢单大厅接单，完工后填写维修备注，用户评价将计入评分。
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
import { ElMessage } from 'element-plus'
import {
  Tickets,
  Timer,
  Tools,
  CircleCheck,
  EditPen,
  DataAnalysis,
  List,
  User,
  Menu,
  Location,
  Avatar,
  Star
} from '@element-plus/icons-vue'
import { useUserStore } from '../../store/user'
import { getMyOrders, getAdminOrderPage, getWorkerTasks } from '../../api/order'
import { getMyWorkerProfile } from '../../api/worker'
import { getAds } from '../../api/ad'
import { listCategories } from '../../api/category'
import { STATUS_MAP, ROLE_MAP, dictLabel, formatLocation } from '../../utils/dict'
import { EMPTY_IMAGE } from '../../utils/images'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
// 最近工单（最多 5 条），用于首页概览
const recentOrders = ref([])
// 按状态统计的数量
const statusCount = ref({ PENDING: 0, IN_PROGRESS: 0, FINISHED: 0, REJECTED: 0 })
const total = ref(0)
// 维修工本人资料（仅 WORKER 角色加载）
const workerProfile = ref(null)

// 广告位（启用中，后端按 sort 升序返回）
const ads = ref([])
// 维修服务分类（含服务价格与服务图片）
const serviceList = ref([])

// 服务保障条配置
const guarantees = [
  { text: '300+ 校园点位', icon: Location },
  { text: '认证维修师傅', icon: Avatar },
  { text: '好评率 98%', icon: Star },
  { text: '1 小时响应', icon: Timer }
]

// 无广告数据时的兜底轮播，保证首屏仍有服务商城氛围
const DEFAULT_AD = {
  id: 'default',
  title: '校园维修 · 上门服务',
  subtitle: '报修不过夜，认证师傅 1 小时响应',
  imageUrl: ''
}

// 轮播数据：优先使用后端广告位
const adSlides = computed(() => (ads.value.length ? ads.value : [DEFAULT_AD]))

// 展示名：昵称优先，其次手机号
const displayName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.phone || '同学'
)

// 角色中文名
const roleLabel = computed(() => ROLE_MAP[userStore.role]?.label ?? userStore.role)

// 欢迎横幅文案：按角色给出场景化说明
const heroTitle = computed(() => {
  if (userStore.role === 'ADMIN') return '今天的维修任务已就绪'
  if (userStore.role === 'WORKER') return '待办维修任务已就绪'
  return '有故障？交给我们'
})

const heroDesc = computed(() => {
  if (userStore.role === 'ADMIN') {
    return '紧急工单已在列表优先展示，请及时接单并更新维修进度。'
  }
  if (userStore.role === 'WORKER') {
    return '分派任务与抢单池已同步，紧急工单优先处理，完工后请填写维修备注。'
  }
  return '拍照上传故障照片，选择位置与故障类型，维修管理员会尽快处理。'
})

// 最近工单面板标题与「全部工单」跳转地址
const recentTitle = computed(() => {
  if (userStore.role === 'ADMIN') return '最新报修工单'
  if (userStore.role === 'WORKER') return '我的最近任务'
  return '我的最近工单'
})

const recentLink = computed(() => {
  if (userStore.role === 'ADMIN') return '/admin/orders'
  if (userStore.role === 'WORKER') return '/worker/tasks'
  return '/repair/my'
})

// 维修工平均评分：null 表示暂无评价
const workerRatingText = computed(() => {
  const rating = workerProfile.value?.avgRating
  return rating == null ? '暂无' : Number(rating).toFixed(1)
})

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
  const isWorker = userStore.role === 'WORKER'
  return [
    {
      label: isAdmin ? '全部工单' : isWorker ? '我的任务' : '我的工单',
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
      { path: '/admin/workers', title: '维修工管理', desc: '账号与派单', icon: Tools, bg: 'linear-gradient(135deg,#f77b7b,#ef4b4b)' },
      { path: '/admin/categories', title: '故障分类', desc: '维护分类项', icon: Menu, bg: 'linear-gradient(135deg,#f7b955,#f5a524)' },
      { path: '/admin/users', title: '用户管理', desc: '会员开通', icon: User, bg: 'linear-gradient(135deg,#3fd0a2,#0fb583)' }
    ]
  }
  if (userStore.role === 'WORKER') {
    return [
      { path: '/worker/tasks', title: '我的任务', desc: '接单与完工', icon: Tickets, bg: 'linear-gradient(135deg,#2c6bf2,#17bfd4)' },
      { path: '/worker/pool', title: '抢单大厅', desc: '紧急工单优先', icon: List, bg: 'linear-gradient(135deg,#f7b955,#f5a524)' },
      { path: '/worker/profile', title: '我的资料', desc: '擅长分类设置', icon: User, bg: 'linear-gradient(135deg,#3fd0a2,#0fb583)' }
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

    // 维修工：我的任务 + 本人资料
    if (userStore.role === 'WORKER') {
      const [taskRes, profileRes] = await Promise.all([
        getWorkerTasks(params),
        getMyWorkerProfile()
      ])
      const records = taskRes.data?.records ?? []
      total.value = taskRes.data?.total ?? 0

      const counter = { PENDING: 0, IN_PROGRESS: 0, FINISHED: 0, REJECTED: 0 }
      records.forEach((item) => {
        if (counter[item.status] !== undefined) counter[item.status] += 1
      })
      statusCount.value = counter
      recentOrders.value = records.slice(0, 5)
      workerProfile.value = profileRes.data || null
      return
    }

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

/**
 * 价格格式化：最多保留两位小数并去掉多余的 0
 * @param {number|string} value 价格
 * @returns {string} 展示文本
 */
function formatPrice(value) {
  const num = Number(value)
  if (!Number.isFinite(num)) return '0'
  return num.toFixed(2).replace(/\.?0+$/, '') || '0'
}

// 加载商城内容：广告位 + 服务分类（价格 / 图片）
async function loadStorefront() {
  try {
    const [adRes, categoryRes] = await Promise.all([getAds(), listCategories()])
    ads.value = adRes.data || []
    serviceList.value = categoryRes.data || []
  } catch {
    // 广告或分类加载失败时保留空态，不影响工作台其它内容展示
  }
}

/**
 * 跳转报修页：携带分类 ID 供提交页自动预选
 * @param {number} [categoryId] 服务分类 ID
 */
function goSubmit(categoryId) {
  // 维修工 / 管理员没有报修权限，给出提示而不是跳转后被路由拦截
  if (userStore.role !== 'USER' && userStore.role !== 'VIP') {
    ElMessage.warning('当前身份不支持提交报修，请先切换为普通用户或会员')
    return
  }
  if (categoryId) {
    router.push({ path: '/repair/submit', query: { categoryId } })
  } else {
    router.push('/repair/submit')
  }
}

// 时间格式化：只保留 MM-DD HH:mm
function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(5, 16)
}

onMounted(() => {
  loadDashboard()
  loadStorefront()
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 0;
  /* 价格强调色：与极客修风格一致的橙色 */
  --price-accent: #ff6b00;
}

/* ========== 服务商城：广告轮播 ========== */
.ad-carousel {
  margin-bottom: 16px;
  border-radius: var(--r-lg);
  overflow: hidden;
  box-shadow: var(--shadow-card);
}

.ad-slide {
  position: relative;
  height: 100%;
  background-color: #10294a;
  background-size: cover;
  background-position: center;
}

.ad-slide.is-fallback {
  background-image: var(--grad-brand);
}

.ad-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    90deg,
    rgba(8, 20, 38, 0.78) 0%,
    rgba(8, 20, 38, 0.36) 58%,
    rgba(8, 20, 38, 0.04) 100%
  );
}

.ad-copy {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
  height: 100%;
  max-width: 620px;
  padding: 0 44px;
  color: #fff;
}

.ad-copy h2 {
  margin: 0;
  font-size: 30px;
  font-weight: 800;
  letter-spacing: -0.4px;
}

.ad-copy p {
  margin: 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.86);
}

.ad-btn {
  align-self: flex-start;
  height: 40px;
  margin-top: 10px;
  padding: 0 26px;
  border: none;
  border-radius: 999px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #ff8a3d, var(--price-accent));
  box-shadow: 0 10px 22px rgba(255, 107, 0, 0.34);
}

.ad-btn:hover,
.ad-btn:focus {
  color: #fff;
  background: linear-gradient(135deg, #ff9a52, #ff7a1a);
}

/* ========== 服务保障条 ========== */
.service-bar {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  padding: 14px 18px;
  margin-bottom: 16px;
  background: var(--surface);
  border-radius: var(--r-md);
  box-shadow: var(--shadow-card);
}

.guarantee-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  font-size: 13px;
  font-weight: 600;
  color: #465468;
}

.guarantee-item + .guarantee-item {
  border-left: 1px dashed #eef2f9;
}

.guarantee-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  border-radius: 10px;
  font-size: 15px;
  color: var(--price-accent);
  background: #fff3e9;
}

/* ========== 服务分类网格 ========== */
.service-panel {
  margin-bottom: 18px;
}

.service-note {
  font-size: 12px;
  color: var(--faint);
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 14px;
}

.service-card {
  position: relative;
  overflow: hidden;
  border: 1px solid #eef2f9;
  border-radius: 18px;
  background: #fff;
  cursor: pointer;
  transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease;
}

.service-card:hover {
  border-color: #ffd9bd;
  transform: translateY(-4px);
  box-shadow: var(--shadow-hover);
}

/* 右上角会员折扣角标 */
.service-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 2;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #ff8a3d, var(--price-accent));
  box-shadow: 0 6px 14px rgba(255, 107, 0, 0.28);
}

.service-thumb {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 132px;
  overflow: hidden;
  background: linear-gradient(135deg, #eef3ff, #e7f8f2);
}

.service-thumb img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.service-card:hover .service-thumb img {
  transform: scale(1.05);
}

.thumb-fallback {
  font-size: 36px;
  font-weight: 700;
  color: #96b5f9;
}

.service-info {
  padding: 14px 16px 16px;
}

.service-name {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--ink);
}

.service-price {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.price-now {
  font-size: 20px;
  font-weight: 800;
  line-height: 1;
  color: var(--price-accent);
}

.price-now em {
  margin-left: 2px;
  font-size: 11px;
  font-style: normal;
  font-weight: 500;
  color: #ff9a52;
}

.price-vip {
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  color: #a76a05;
  background: #fff5e3;
}

.service-btn {
  width: 100%;
  height: 36px;
  margin-top: 12px;
  border: none;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #ff8a3d, var(--price-accent));
  box-shadow: 0 8px 18px rgba(255, 107, 0, 0.24);
}

.service-btn:hover,
.service-btn:focus {
  color: #fff;
  background: linear-gradient(135deg, #ff9a52, #ff7a1a);
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

/* 空状态插图：居中缩略图，圆角 + 柔和阴影 */
.empty-image {
  display: block;
  width: 120px;
  height: 120px;
  margin: 0 auto 12px;
  border-radius: 12px;
  object-fit: cover;
  box-shadow: 0 8px 20px rgba(18, 41, 74, 0.08);
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

/* 维修工资料卡 */
.worker-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.worker-avatar {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #3fd0a2, #0fb583);
}

.worker-id {
  flex: 1;
  min-width: 0;
}

.worker-id strong {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #1b2b45;
}

.worker-id span {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: #a9b4c6;
}

.worker-metrics {
  display: flex;
  gap: 28px;
  padding: 14px 0 10px;
}

.worker-metrics em {
  display: block;
  font-size: 20px;
  font-style: normal;
  font-weight: 700;
  color: #12294a;
}

.worker-metrics span {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: #a9b4c6;
}

.worker-cats {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding-top: 10px;
  border-top: 1px dashed #eef2f9;
}

.cat-tag {
  padding: 1px 9px;
  border-radius: 999px;
  font-size: 11px;
  color: #1b56d0;
  background: #eef3ff;
}

.muted {
  font-size: 12px;
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

  .service-bar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px 8px;
  }

  .guarantee-item:nth-child(3) {
    border-left: none;
  }

  .ad-copy {
    padding: 0 26px;
  }

  .ad-copy h2 {
    font-size: 22px;
  }
}
</style>
