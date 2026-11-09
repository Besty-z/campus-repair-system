<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>工单管理</span>
        <span class="urgent-note">紧急工单优先展示</span>
      </div>
    </template>

    <!-- 筛选栏：状态 / 地点类型 / 故障分类 / 关键词 -->
    <el-form inline class="filter-bar">
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
          <el-option
            v-for="item in STATUS_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="地点类型">
        <el-select
          v-model="query.locationType"
          placeholder="全部地点"
          clearable
          style="width: 150px"
          @change="query.categoryId = null"
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
            v-for="item in filteredCategories"
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
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工单表格：紧急工单行红色底纹高亮 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      border
      stripe
      :row-class-name="rowClassName"
    >
      <el-table-column prop="orderNo" label="工单号" width="150" />
      <el-table-column label="报修人" width="130">
        <template #default="{ row }">{{ row.userNickname }}<br />{{ row.userPhone }}</template>
      </el-table-column>
      <el-table-column label="地点类型" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="dictTagType(LOCATION_TYPE_MAP, row.locationType)" size="small">
            {{ dictLabel(LOCATION_TYPE_MAP, row.locationType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="地点" min-width="130">
        <template #default="{ row }">{{ formatLocation(row) }}</template>
      </el-table-column>
      <el-table-column prop="categoryName" label="分类" width="110" />
      <el-table-column label="描述" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">{{ row.description }}</template>
      </el-table-column>
      <el-table-column label="紧急" width="70" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.urgent === 1" type="danger" size="small">紧急</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="dictTagType(STATUS_MAP, row.status)" size="small">
            {{ dictLabel(STATUS_MAP, row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="200" align="center" fixed="right">
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
      <template #empty>暂无工单数据</template>
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

    <!-- 修改状态弹窗：选择目标状态 + 填写维修备注 -->
    <el-dialog v-model="statusDialogVisible" title="修改工单状态" width="440px">
      <el-form label-width="90px">
        <el-form-item label="当前状态">
          <el-tag :type="dictTagType(STATUS_MAP, currentRow?.status)" size="small">
            {{ dictLabel(STATUS_MAP, currentRow?.status) }}
          </el-tag>
        </el-form-item>
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

    <!-- 工单详情弹窗（含图片预览） -->
    <DetailDialog v-model:visible="detailVisible" :order-id="currentOrderId" />
  </el-card>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminOrderPage, acceptOrder, updateOrderStatus } from '../../api/order'
import { listCategories } from '../../api/category'
import {
  STATUS_MAP,
  STATUS_OPTIONS,
  LOCATION_TYPE_MAP,
  LOCATION_TYPE_OPTIONS,
  dictLabel,
  dictTagType,
  formatLocation
} from '../../utils/dict'
import DetailDialog from '../repair/DetailDialog.vue'

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

// 故障分类选项（用于筛选，按所选地点类型过滤）
const categoryOptions = ref([])
const filteredCategories = computed(() => {
  if (!query.locationType) return categoryOptions.value
  return categoryOptions.value.filter((item) => item.locationType === query.locationType)
})

// 修改状态弹窗
const statusDialogVisible = ref(false)
const statusSubmitting = ref(false)
const currentRow = ref(null)
const statusForm = reactive({ status: '', adminRemark: '' })

// 详情弹窗
const detailVisible = ref(false)
const currentOrderId = ref(null)

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

// 紧急工单行红色底纹高亮
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

// 查看详情
function handleView(row) {
  currentOrderId.value = row.id
  detailVisible.value = true
}

// 初始化：加载分类选项与工单列表
onMounted(async () => {
  loadData()
  const res = await listCategories()
  categoryOptions.value = res.data || []
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.urgent-note {
  font-size: 12px;
  color: #f56c6c;
}

.filter-bar {
  margin-bottom: 8px;
}

/* 紧急工单行红色底纹 */
:deep(.urgent-row) {
  background-color: #fef0f0 !important;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
