<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>我的工单</span>
        <!-- 状态筛选 -->
        <el-select
          v-model="query.status"
          placeholder="按状态筛选"
          clearable
          style="width: 160px"
          @change="handleSearch"
        >
          <el-option
            v-for="item in STATUS_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </div>
    </template>

    <!-- 工单列表 -->
    <el-table v-loading="loading" :data="tableData" border stripe>
      <el-table-column prop="orderNo" label="工单号" width="160" />
      <el-table-column label="地点" min-width="150">
        <template #default="{ row }">{{ formatLocation(row) }}</template>
      </el-table-column>
      <el-table-column prop="categoryName" label="故障分类" width="120" />
      <el-table-column label="描述摘要" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.description }}</template>
      </el-table-column>
      <el-table-column label="紧急" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.urgent === 1" type="danger" size="small">紧急</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="dictTagType(STATUS_MAP, row.status)" size="small">
            {{ dictLabel(STATUS_MAP, row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleView(row)">详情</el-button>
        </template>
      </el-table-column>
      <template #empty>暂无工单，快去提交报修吧</template>
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

    <!-- 工单详情弹窗（含图片预览、维修备注） -->
    <DetailDialog v-model:visible="detailVisible" :order-id="currentOrderId" />
  </el-card>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { getMyOrders } from '../../api/order'
import { STATUS_MAP, STATUS_OPTIONS, dictLabel, dictTagType, formatLocation } from '../../utils/dict'
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

onMounted(loadData)
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
