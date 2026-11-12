<template>
  <div class="page my-orders">
    <!-- 状态筛选胶囊 -->
    <div class="status-tabs">
      <button
        class="status-tab"
        :class="{ active: query.status === '' }"
        @click="handleStatusChange('')"
      >
        全部工单
      </button>
      <button
        v-for="item in STATUS_OPTIONS"
        :key="item.value"
        class="status-tab"
        :class="{ active: query.status === item.value }"
        @click="handleStatusChange(item.value)"
      >
        <i class="tab-dot" :class="`dot-${item.value.toLowerCase()}`" />
        {{ item.label }}
      </button>
      <span class="total-hint">共 {{ total }} 条记录</span>
    </div>

    <!-- 工单列表 -->
    <section class="panel">
      <el-table v-loading="loading" :data="tableData" class="order-table">
        <el-table-column prop="orderNo" label="工单号" width="165">
          <template #default="{ row }">
            <span class="order-no">{{ row.orderNo }}</span>
          </template>
        </el-table-column>

        <el-table-column label="报修地点" min-width="160">
          <template #default="{ row }">
            <div class="loc-cell">
              <span class="loc-type" :class="row.locationType === 'DORM' ? 'dorm' : 'public'">
                {{ row.locationType === 'DORM' ? '宿舍' : '公共' }}
              </span>
              <span>{{ formatLocation(row) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="故障分类" width="160">
          <template #default="{ row }">
            <div class="cat-tags">
              <span v-for="(name, index) in orderCategoryNames(row)" :key="index" class="cat-tag">
                {{ name }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="描述摘要" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description }}</template>
        </el-table-column>

        <el-table-column label="紧急" width="80" align="center">
          <template #default="{ row }">
            <span v-if="row.urgent === 1" class="urgent-tag">紧急</span>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>

        <el-table-column label="预估费用" width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.estimatedPrice != null" class="est-price">
              ¥{{ formatPrice(row.estimatedPrice) }}
            </span>
            <span v-else class="muted">面议</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <span class="status-pill" :class="`st-${row.status.toLowerCase()}`">
              {{ dictLabel(STATUS_MAP, row.status) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{ row }">
            <span class="muted">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">详情</el-button>
            <!-- 已完成且未评价的工单可评价维修 -->
            <el-button
              v-if="row.status === 'FINISHED' && row.ratingScore == null"
              type="warning"
              link
              size="small"
              @click="openRating(row)"
            >
              评价维修
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <div class="empty-state">
            <div class="empty-icon">🛠️</div>
            暂无工单，快去提交报修吧
          </div>
        </template>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadData"
          @size-change="handleSearch"
        />
      </div>
    </section>

    <!-- 工单详情弹窗（含图片预览、维修备注） -->
    <DetailDialog v-model:visible="detailVisible" :order-id="currentOrderId" />

    <!-- 评价维修弹窗：1-5 星 + 评价内容 -->
    <el-dialog v-model="ratingVisible" title="评价维修" width="460px">
      <div class="dlg-order">
        <span class="order-no">{{ ratingRow?.orderNo }}</span>
        <span class="status-pill" :class="`st-${(ratingRow?.status || '').toLowerCase()}`">
          {{ dictLabel(STATUS_MAP, ratingRow?.status) }}
        </span>
      </div>

      <el-form label-position="top">
        <el-form-item label="维修评分" required>
          <el-rate
            v-model="ratingForm.score"
            :texts="['很不满意', '不满意', '一般', '满意', '非常满意']"
            show-text
          />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input
            v-model="ratingForm.comment"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="说说本次维修体验（选填）"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="ratingVisible = false">取 消</el-button>
        <el-button type="primary" :loading="ratingSubmitting" @click="handleRating">
          提交评价
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyOrders, rateOrder } from '../../api/order'
import { STATUS_MAP, STATUS_OPTIONS, dictLabel, formatLocation } from '../../utils/dict'
import DetailDialog from './DetailDialog.vue'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)

// 查询参数：状态 + 分页
const query = reactive({
  status: '',
  pageNum: 1,
  pageSize: 10
})

// 详情弹窗状态
const detailVisible = ref(false)
const currentOrderId = ref(null)

// 评价弹窗状态
const ratingVisible = ref(false)
const ratingSubmitting = ref(false)
const ratingRow = ref(null)
const ratingForm = reactive({ score: 5, comment: '' })

// 加载我的工单列表（按创建时间倒序，后端默认）
async function loadData() {
  loading.value = true
  try {
    const res = await getMyOrders({
      status: query.status || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

// 切换状态筛选：回到第一页
function handleStatusChange(status) {
  query.status = status
  query.pageNum = 1
  loadData()
}

// 筛选/翻页大小变化：回到第一页重新查询
function handleSearch() {
  query.pageNum = 1
  loadData()
}

// 查看工单详情
function handleView(row) {
  currentOrderId.value = row.id
  detailVisible.value = true
}

// 打开评价弹窗
function openRating(row) {
  ratingRow.value = row
  ratingForm.score = 5
  ratingForm.comment = ''
  ratingVisible.value = true
}

// 提交评价
async function handleRating() {
  if (!ratingForm.score) {
    ElMessage.warning('请选择评分星级')
    return
  }
  ratingSubmitting.value = true
  try {
    await rateOrder(ratingRow.value.id, {
      score: ratingForm.score,
      comment: ratingForm.comment || undefined
    })
    ElMessage.success('评价成功，感谢你的反馈')
    ratingVisible.value = false
    loadData()
  } finally {
    ratingSubmitting.value = false
  }
}

// 时间格式化：T 换空格并去掉秒
function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
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
/* 状态筛选胶囊组 */
.status-tabs {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.status-tab {
  display: flex;
  align-items: center;
  gap: 7px;
  height: 34px;
  padding: 0 16px;
  border: 1px solid #e3eaf5;
  border-radius: 999px;
  font-size: 13px;
  color: #5b6b80;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s ease;
}

.status-tab:hover {
  border-color: #bcd0fb;
  color: #2c6bf2;
}

.status-tab.active {
  border-color: transparent;
  color: #fff;
  background: linear-gradient(135deg, #2c6bf2 0%, #17bfd4 100%);
  box-shadow: 0 8px 18px rgba(44, 107, 242, 0.24);
}

/* 胶囊内的小圆点，用状态色区分 */
.tab-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.dot-pending {
  background: #f5a524;
}

.dot-in_progress {
  background: #2c6bf2;
}

.dot-finished {
  background: #0fb583;
}

.dot-rejected {
  background: #ef4b4b;
}

.status-tab.active .tab-dot {
  background: rgba(255, 255, 255, 0.9);
}

.total-hint {
  margin-left: auto;
  font-size: 12px;
  color: #a9b4c6;
}

/* 表格单元样式 */
.order-no {
  font-family: 'SF Mono', Consolas, monospace;
  font-size: 12px;
  font-weight: 600;
  color: #1b2b45;
}

.loc-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
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

.muted {
  color: #a9b4c6;
}

/* 多选分类小标签 */
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

.urgent-tag {
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  color: #b03636;
  background: #fdeeee;
}

/* 预估费用：橙色强调 */
.est-price {
  font-size: 13px;
  font-weight: 700;
  color: #ff6b00;
}

/* 状态胶囊 */
.status-pill {
  display: inline-block;
  padding: 3px 11px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
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

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* 评价弹窗内工单信息条 */
.dlg-order {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  margin-bottom: 16px;
  border-radius: 12px;
  background: #f8fafd;
}
</style>
