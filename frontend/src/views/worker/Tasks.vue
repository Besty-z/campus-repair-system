<template>
  <div class="page worker-tasks">
    <!-- 状态筛选胶囊 -->
    <div class="status-tabs">
      <button
        class="status-tab"
        :class="{ active: query.status === '' }"
        @click="handleStatusChange('')"
      >
        全部任务
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
      <span class="total-hint">共 {{ total }} 条任务</span>
    </div>

    <!-- 我的维修任务 -->
    <section class="panel">
      <el-table v-loading="loading" :data="tableData" :row-class-name="rowClassName">
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

        <el-table-column label="描述摘要" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description }}</template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <span class="status-pill" :class="`st-${row.status.toLowerCase()}`">
              {{ dictLabel(STATUS_MAP, row.status) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="维修工" width="110">
          <template #default="{ row }">
            <span v-if="row.workerName">{{ row.workerName }}</span>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">
            <span class="muted">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <!-- 待接单：接单后才进入维修中 -->
            <el-button
              v-if="row.status === 'PENDING'"
              type="success"
              link
              size="small"
              @click="handleAccept(row)"
            >
              接单
            </el-button>
            <!-- 维修中：填写维修备注后完工 -->
            <el-button
              v-else-if="row.status === 'IN_PROGRESS'"
              type="primary"
              link
              size="small"
              @click="openFinishDialog(row)"
            >
              完成维修
            </el-button>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>

        <template #empty>
          <div class="empty-state">
            <div class="empty-icon">🛠️</div>
            暂无维修任务，去抢单大厅看看新工单吧
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

    <!-- 完成维修弹窗：填写维修备注 -->
    <el-dialog v-model="finishDialogVisible" title="完成维修" width="480px">
      <div class="dlg-order">
        <span class="order-no">{{ currentRow?.orderNo }}</span>
        <span class="status-pill" :class="`st-${(currentRow?.status || '').toLowerCase()}`">
          {{ dictLabel(STATUS_MAP, currentRow?.status) }}
        </span>
      </div>

      <el-form label-position="top">
        <el-form-item label="维修备注" required>
          <el-input
            v-model="finishForm.adminRemark"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请填写维修处理情况（如：已更换水龙头，测试无渗漏）"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="finishDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="finishSubmitting" @click="handleFinish">
          确认完工
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWorkerTasks, acceptWorkerOrder, finishWorkerOrder } from '../../api/order'
import { STATUS_MAP, STATUS_OPTIONS, dictLabel, formatLocation } from '../../utils/dict'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)

// 查询参数：状态 + 分页
const query = reactive({
  status: '',
  pageNum: 1,
  pageSize: 10
})

// 完成维修弹窗
const finishDialogVisible = ref(false)
const finishSubmitting = ref(false)
const currentRow = ref(null)
const finishForm = reactive({ adminRemark: '' })

// 紧急工单行高亮
function rowClassName({ row }) {
  return row.urgent === 1 ? 'urgent-row' : ''
}

// 加载我的任务分页
async function loadData() {
  loading.value = true
  try {
    const res = await getWorkerTasks({
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

// 分页大小变化：回到第一页
function handleSearch() {
  query.pageNum = 1
  loadData()
}

// 接单
async function handleAccept(row) {
  await ElMessageBox.confirm(`确定接下工单「${row.orderNo}」吗？`, '接单确认', { type: 'info' })
  await acceptWorkerOrder(row.id)
  ElMessage.success('接单成功，请及时上门维修')
  loadData()
}

// 打开完成维修弹窗
function openFinishDialog(row) {
  currentRow.value = row
  finishForm.adminRemark = ''
  finishDialogVisible.value = true
}

// 提交完工：状态固定为 FINISHED
async function handleFinish() {
  if (!finishForm.adminRemark.trim()) {
    ElMessage.warning('请填写维修备注')
    return
  }
  finishSubmitting.value = true
  try {
    await finishWorkerOrder(currentRow.value.id, {
      status: 'FINISHED',
      adminRemark: finishForm.adminRemark.trim()
    })
    ElMessage.success('已完成维修')
    finishDialogVisible.value = false
    loadData()
  } finally {
    finishSubmitting.value = false
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
/* ===== 状态筛选胶囊 ===== */
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

.muted {
  color: #a9b4c6;
  font-size: 12px;
}

/* ===== 状态胶囊 ===== */
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

/* 弹窗内工单信息条 */
.dlg-order {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  margin-bottom: 16px;
  border-radius: 12px;
  background: #f8fafd;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
