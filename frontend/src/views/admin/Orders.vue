<template>
  <div class="page admin-orders">
    <!-- 状态胶囊筛选 + 紧急提示 -->
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
      <span class="urgent-note">
        <i class="urgent-dot" />
        紧急工单优先展示
      </span>
    </div>

    <section class="panel">
      <!-- 筛选栏：地点类型 / 故障分类 / 关键词 -->
      <el-form inline class="filter-bar">
        <el-form-item label="地点类型">
          <el-select
            v-model="query.locationType"
            placeholder="全部地点"
            clearable
            style="width: 150px"
          >
            <el-option
              v-for="item in LOCATION_TYPE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="故障分类">
          <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width: 160px">
            <el-option
              v-for="item in categoryOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="位置 / 描述 / 工单号"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
        <span class="total-hint">共 {{ total }} 条工单</span>
      </el-form>

      <!-- 工单表格：紧急工单行高亮 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        :row-class-name="rowClassName"
        class="order-table"
      >
        <el-table-column prop="orderNo" label="工单号" width="165">
          <template #default="{ row }">
            <div class="no-cell">
              <span class="order-no">{{ row.orderNo }}</span>
              <span v-if="row.urgent === 1" class="urgent-tag">紧急</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="报修人" width="140">
          <template #default="{ row }">
            <strong class="user-name">{{ row.userNickname }}</strong>
            <span class="user-phone">{{ row.userPhone }}</span>
          </template>
        </el-table-column>

        <el-table-column label="报修地点" min-width="170">
          <template #default="{ row }">
            <div class="loc-cell">
              <span class="loc-type" :class="row.locationType === 'DORM' ? 'dorm' : 'public'">
                {{ row.locationType === 'DORM' ? '宿舍' : '公共' }}
              </span>
              <span>{{ formatLocation(row) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="categoryName" label="分类" width="120" />

        <el-table-column label="描述" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description }}</template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <span class="status-pill" :class="`st-${row.status.toLowerCase()}`">
              {{ dictLabel(STATUS_MAP, row.status) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="维修工" width="120">
          <template #default="{ row }">
            <span v-if="row.workerName">{{ row.workerName }}</span>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>

        <el-table-column label="提交时间" width="150">
          <template #default="{ row }">
            <span class="muted">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="300" align="center" fixed="right">
          <template #default="{ row }">
            <!-- 接单：仅待接单状态可操作 -->
            <el-button
              v-if="row.status === 'PENDING'"
              type="success"
              link
              size="small"
              @click="handleAccept(row)"
            >
              接单
            </el-button>
            <!-- 指派维修工：仅待接单状态可操作 -->
            <el-button
              v-if="row.status === 'PENDING'"
              type="primary"
              link
              size="small"
              @click="openAssignDialog(row)"
            >
              指派维修工
            </el-button>
            <!-- 修改状态：仅流转中的工单可修改（终态不可再改） -->
            <el-button
              v-if="allowedStatuses(row.status).length > 0"
              type="warning"
              link
              size="small"
              @click="openStatusDialog(row)"
            >
              修改状态
            </el-button>
            <el-button type="primary" link size="small" @click="handleView(row)">详情</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <div class="empty-state">
            <img
              class="empty-image"
              :src="EMPTY_IMAGE"
              alt="暂无工单插图"
              loading="lazy"
            />
            暂无符合条件的工单
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

    <!-- 修改状态弹窗：选择目标状态 + 填写维修备注 -->
    <el-dialog v-model="statusDialogVisible" title="更新维修进度" width="460px">
      <div class="dlg-order">
        <span class="order-no">{{ currentRow?.orderNo }}</span>
        <span class="status-pill" :class="`st-${(currentRow?.status || '').toLowerCase()}`">
          {{ dictLabel(STATUS_MAP, currentRow?.status) }}
        </span>
      </div>

      <el-form label-position="top" class="status-form">
        <el-form-item label="目标状态" required>
          <el-select v-model="statusForm.status" placeholder="请选择目标状态" style="width: 100%">
            <el-option
              v-for="item in allowedStatuses(currentRow?.status)"
              :key="item"
              :label="dictLabel(STATUS_MAP, item)"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="维修备注">
          <el-input
            v-model="statusForm.adminRemark"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请填写维修备注（如：已更换水龙头，测试无渗漏）"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="statusDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="statusSubmitting" @click="handleUpdateStatus">
          确 定
        </el-button>
      </template>
    </el-dialog>

    <!-- 指派维修工弹窗：仅在职维修工可选 -->
    <el-dialog v-model="assignDialogVisible" title="指派维修工" width="460px">
      <div class="dlg-order">
        <span class="order-no">{{ assignRow?.orderNo }}</span>
        <span class="status-pill st-pending">待接单</span>
      </div>

      <el-form label-position="top">
        <el-form-item label="维修工" required>
          <el-select
            v-model="assignForm.workerId"
            placeholder="请选择在职维修工"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="worker in enabledWorkers"
              :key="worker.userId"
              :label="`${worker.name}（${worker.phone}）`"
              :value="worker.userId"
            />
          </el-select>
          <span v-if="enabledWorkers.length === 0" class="worker-tip">
            暂无在职维修工，请先到「维修工管理」新增或启用
          </span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="assignDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="assignSubmitting" @click="handleAssign">
          确 定
        </el-button>
      </template>
    </el-dialog>

    <!-- 工单详情弹窗（含图片预览） -->
    <DetailDialog v-model:visible="detailVisible" :order-id="currentOrderId" />
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminOrderPage, acceptOrder, updateOrderStatus, assignOrder } from '../../api/order'
import { getEnabledWorkers } from '../../api/worker'
import { listCategories } from '../../api/category'
import {
  STATUS_MAP,
  STATUS_OPTIONS,
  LOCATION_TYPE_MAP,
  LOCATION_TYPE_OPTIONS,
  dictLabel,
  formatLocation
} from '../../utils/dict'
import DetailDialog from '../repair/DetailDialog.vue'
import { EMPTY_IMAGE } from '../../utils/images'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)

// 查询参数：状态 / 地点类型 / 故障分类 / 关键词 + 分页
const query = reactive({
  status: '',
  locationType: '',
  categoryId: null,
  keyword: '',
  pageNum: 1,
  pageSize: 10
})

// 故障分类选项（用于筛选，后端已按维修工种划分）
const categoryOptions = ref([])

// 修改状态弹窗
const statusDialogVisible = ref(false)
const statusSubmitting = ref(false)
const currentRow = ref(null)
const statusForm = reactive({ status: '', adminRemark: '' })

// 详情弹窗
const detailVisible = ref(false)
const currentOrderId = ref(null)

// 指派维修工弹窗
const assignDialogVisible = ref(false)
const assignSubmitting = ref(false)
const assignRow = ref(null)
const enabledWorkers = ref([])
const assignForm = reactive({ workerId: null })

/**
 * 计算某状态允许流转到的目标状态（与后端契约一致）
 * PENDING -> IN_PROGRESS / REJECTED；IN_PROGRESS -> FINISHED / REJECTED；终态不可再改
 * @param {string} status 当前状态
 * @returns {string[]} 允许的目标状态列表
 */
function allowedStatuses(status) {
  if (status === 'PENDING') return ['IN_PROGRESS', 'REJECTED']
  if (status === 'IN_PROGRESS') return ['FINISHED', 'REJECTED']
  return []
}

// 紧急工单行高亮（左侧红色竖条由 CSS 处理）
function rowClassName({ row }) {
  return row.urgent === 1 ? 'urgent-row' : ''
}

// 加载工单分页（后端按紧急优先 + 创建时间倒序）
async function loadData() {
  loading.value = true
  try {
    const res = await getAdminOrderPage({
      status: query.status || undefined,
      locationType: query.locationType || undefined,
      categoryId: query.categoryId || undefined,
      keyword: query.keyword || undefined,
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

// 查询：回到第一页
function handleSearch() {
  query.pageNum = 1
  loadData()
}

// 重置筛选条件
function handleReset() {
  query.status = ''
  query.locationType = ''
  query.categoryId = null
  query.keyword = ''
  handleSearch()
}

// 接单：PENDING -> IN_PROGRESS
async function handleAccept(row) {
  await ElMessageBox.confirm(`确定接单工单「${row.orderNo}」吗？`, '接单确认', { type: 'info' })
  await acceptOrder(row.id)
  ElMessage.success('接单成功')
  loadData()
}

// 打开修改状态弹窗
function openStatusDialog(row) {
  currentRow.value = row
  statusForm.status = ''
  statusForm.adminRemark = row.adminRemark || ''
  statusDialogVisible.value = true
}

// 提交修改状态（可附带维修备注）
async function handleUpdateStatus() {
  if (!statusForm.status) {
    ElMessage.warning('请选择目标状态')
    return
  }
  statusSubmitting.value = true
  try {
    await updateOrderStatus(currentRow.value.id, {
      status: statusForm.status,
      adminRemark: statusForm.adminRemark
    })
    ElMessage.success('状态修改成功')
    statusDialogVisible.value = false
    loadData()
  } finally {
    statusSubmitting.value = false
  }
}

// 打开指派维修工弹窗（每次打开重新拉取在职维修工）
async function openAssignDialog(row) {
  assignRow.value = row
  assignForm.workerId = null
  assignDialogVisible.value = true
  const res = await getEnabledWorkers()
  enabledWorkers.value = res.data || []
}

// 提交指派（workerId 为 WorkerVO.userId）
async function handleAssign() {
  if (!assignForm.workerId) {
    ElMessage.warning('请选择维修工')
    return
  }
  assignSubmitting.value = true
  try {
    await assignOrder(assignRow.value.id, assignForm.workerId)
    ElMessage.success('指派成功')
    assignDialogVisible.value = false
    loadData()
  } finally {
    assignSubmitting.value = false
  }
}

// 查看详情
function handleView(row) {
  currentOrderId.value = row.id
  detailVisible.value = true
}

// 时间格式化：T 换空格并去掉秒
function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

// 初始化：加载分类选项与工单列表
onMounted(async () => {
  loadData()
  const res = await listCategories()
  categoryOptions.value = res.data || []
})
</script>

<style scoped>
/* ===== 状态胶囊筛选 ===== */
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

/* 紧急提示 */
.urgent-note {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
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

/* 筛选栏后的总数 */
.total-hint {
  margin-left: auto;
  font-size: 12px;
  color: #a9b4c6;
}

/* ===== 表格单元 ===== */
.no-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.order-no {
  font-family: 'SF Mono', Consolas, monospace;
  font-size: 12px;
  font-weight: 600;
  color: #1b2b45;
}

.urgent-tag {
  padding: 1px 7px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 600;
  color: #b03636;
  background: #fdeeee;
  flex-shrink: 0;
}

.user-name {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #1b2b45;
}

.user-phone {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: #a9b4c6;
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

/* 紧急工单行：淡红底 + 左侧红条 */
:deep(.urgent-row) td:first-child {
  box-shadow: inset 3px 0 0 #ef4b4b;
}

:deep(.urgent-row) {
  background: #fffafa !important;
}

/* 修改状态弹窗 */
.dlg-order {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  margin-bottom: 16px;
  border-radius: 12px;
  background: #f8fafd;
}

.status-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

/* 指派弹窗：无在职维修工提示 */
.worker-tip {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  color: #a9b4c6;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
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
</style>
