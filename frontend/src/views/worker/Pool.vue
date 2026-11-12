<template>
  <div class="page worker-pool">
    <section class="panel">
      <div class="panel-hd">
        <div class="panel-title">抢单大厅</div>
        <span class="urgent-note">
          <i class="urgent-dot" />
          待接单且未指派的工单，紧急优先
        </span>
      </div>

      <div v-loading="loading" class="pool-body">
        <div v-if="!loading && poolList.length === 0" class="empty-state">
          <div class="empty-icon">📭</div>
          暂无可抢工单，稍后再来看看吧
        </div>

        <div v-else class="pool-grid">
          <article
            v-for="order in poolList"
            :key="order.id"
            class="pool-card"
            :class="{ 'is-urgent': order.urgent === 1 }"
          >
            <div class="pool-card-hd">
              <span class="order-no">{{ order.orderNo }}</span>
              <span v-if="order.urgent === 1" class="urgent-tag">紧急</span>
            </div>

            <div class="loc-cell">
              <span class="loc-type" :class="order.locationType === 'DORM' ? 'dorm' : 'public'">
                {{ order.locationType === 'DORM' ? '宿舍' : '公共' }}
              </span>
              <span>{{ formatLocation(order) }}</span>
            </div>

            <p class="pool-desc">{{ order.description }}</p>

            <div class="cat-tags">
              <span v-for="(name, index) in orderCategoryNames(order)" :key="index" class="cat-tag">
                {{ name }}
              </span>
            </div>

            <div class="pool-meta">
              <span>报修人：{{ order.userNickname || '-' }}</span>
              <time>{{ formatTime(order.createTime) }}</time>
            </div>

            <el-button
              type="primary"
              class="grab-btn"
              :loading="acceptingId === order.id"
              @click="handleGrab(order)"
            >
              立即抢单
            </el-button>
          </article>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[9, 18, 36]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadData"
          @size-change="handleSizeChange"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWorkerPool, acceptWorkerOrder } from '../../api/order'
import { formatLocation } from '../../utils/dict'

const loading = ref(false)
const poolList = ref([])
const total = ref(0)
// 正在抢单的工单 ID，用于按钮 loading
const acceptingId = ref(null)

const query = reactive({
  pageNum: 1,
  pageSize: 9
})

// 加载抢单池分页（后端按紧急优先 + 创建时间倒序）
async function loadData() {
  loading.value = true
  try {
    const res = await getWorkerPool({
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    poolList.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

// 分页大小变化：回到第一页
function handleSizeChange() {
  query.pageNum = 1
  loadData()
}

// 抢单：成功后刷新列表
async function handleGrab(order) {
  await ElMessageBox.confirm(
    `确定抢下「${order.orderNo}」这单吗？抢单后请及时上门维修。`,
    '抢单确认',
    { type: 'info' }
  )
  acceptingId.value = order.id
  try {
    await acceptWorkerOrder(order.id)
    ElMessage.success('抢单成功')
    // 当前页只剩一条时回退一页，避免出现空页
    if (poolList.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
    await loadData()
  } finally {
    acceptingId.value = null
  }
}

// 时间格式化：T 换空格并去掉秒
function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

/**
 * 工单分类展示文本：优先使用 categoryNames（多选），否则回退 categoryName
 * @param {Object} order 工单对象
 * @returns {string[]} 分类名称数组
 */
function orderCategoryNames(order) {
  if (order?.categoryNames?.length) return order.categoryNames
  return order?.categoryName ? [order.categoryName] : []
}

onMounted(loadData)
</script>

<style scoped>
/* 紧急提示 */
.urgent-note {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #b03636;
}

.urgent-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #ef4b4b;
  box-shadow: 0 0 0 4px rgba(239, 75, 75, 0.14);
}

.pool-body {
  min-height: 180px;
}

/* 工单卡片网格 */
.pool-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 14px;
}

.pool-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px;
  border: 1px solid #eef2f9;
  border-radius: var(--r-md);
  background: #fff;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.pool-card:hover {
  transform: translateY(-2px);
  border-color: #d6e2fd;
  box-shadow: 0 12px 26px rgba(18, 41, 74, 0.08);
}

/* 紧急卡片：淡红底 + 红色左边条 */
.pool-card.is-urgent {
  border-color: #f8dcdc;
  background: #fffafa;
  box-shadow: inset 3px 0 0 #ef4b4b;
}

.pool-card-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.order-no {
  font-family: 'SF Mono', Consolas, monospace;
  font-size: 13px;
  font-weight: 600;
  color: #1b2b45;
}

.urgent-tag {
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  color: #b03636;
  background: #fdeeee;
  flex-shrink: 0;
}

.loc-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #1b2b45;
}

.loc-type {
  padding: 1px 7px;
  border-radius: 6px;
  font-size: 11px;
  flex-shrink: 0;
}

.loc-type.dorm {
  color: #1b56d0;
  background: #eef3ff;
}

.loc-type.public {
  color: #0a7f5e;
  background: #e7f8f2;
}

.pool-desc {
  margin: 0;
  font-size: 12px;
  line-height: 1.7;
  color: #5b6b80;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.cat-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.cat-tag {
  padding: 1px 9px;
  border-radius: 999px;
  font-size: 11px;
  color: #1b56d0;
  background: #eef3ff;
}

.pool-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 11px;
  color: #a9b4c6;
}

.grab-btn {
  width: 100%;
  margin-top: 2px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
