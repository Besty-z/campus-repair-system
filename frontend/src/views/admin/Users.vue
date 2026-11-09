<template>
  <el-card>
    <template #header>
      <span>用户管理</span>
    </template>

    <!-- 筛选栏：关键词（手机号/昵称）+ 角色 -->
    <el-form inline class="filter-bar">
      <el-form-item label="关键词">
        <el-input
          v-model="query.keyword"
          placeholder="手机号 / 昵称"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="query.role" placeholder="全部角色" clearable style="width: 140px">
          <el-option
            v-for="item in ROLE_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 用户表格 -->
    <el-table v-loading="loading" :data="tableData" border stripe>
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="phone" label="手机号" width="150" />
      <el-table-column prop="nickname" label="昵称" min-width="140" />
      <el-table-column label="角色" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="dictTagType(ROLE_MAP, row.role)" size="small">
            {{ dictLabel(ROLE_MAP, row.role) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="账号状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="170" />
      <el-table-column label="操作" width="130" align="center">
        <template #default="{ row }">
          <!-- 管理员账号不允许变更角色，不显示操作按钮 -->
          <template v-if="row.role !== 'ADMIN'">
            <el-button
              v-if="row.role === 'USER'"
              type="warning"
              link
              size="small"
              @click="handleSetRole(row, 'VIP')"
            >
              设为会员
            </el-button>
            <el-button
              v-else-if="row.role === 'VIP'"
              type="info"
              link
              size="small"
              @click="handleSetRole(row, 'USER')"
            >
              取消会员
            </el-button>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <template #empty>暂无用户数据</template>
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
  </el-card>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserPage, updateUserRole } from '../../api/user'
import { ROLE_MAP, ROLE_OPTIONS, dictLabel, dictTagType } from '../../utils/dict'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)

// 查询参数：关键词 + 角色 + 分页
const query = reactive({
  keyword: '',
  role: '',
  pageNum: 1,
  pageSize: 10
})

// 加载用户分页
async function loadData() {
  loading.value = true
  try {
    const res = await getUserPage({
      keyword: query.keyword || undefined,
      role: query.role || undefined,
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
  query.keyword = ''
  query.role = ''
  handleSearch()
}

/**
 * 设置 / 取消会员（管理员角色不可通过此接口变更）
 * @param {Object} row 用户行数据
 * @param {string} role 目标角色：VIP 设为会员 / USER 取消会员
 */
async function handleSetRole(row, role) {
  const actionText = role === 'VIP' ? '设为会员' : '取消会员'
  await ElMessageBox.confirm(
    `确定将用户「${row.nickname || row.phone}」${actionText}吗？`,
    '操作确认',
    { type: 'warning' }
  )
  await updateUserRole(row.id, role)
  ElMessage.success(`${actionText}成功`)
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.filter-bar {
  margin-bottom: 8px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
