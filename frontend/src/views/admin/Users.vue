<template>
  <div class="page users-page">
    <section class="panel">
      <div class="panel-hd">
        <div class="panel-title">用户管理</div>
        <span class="panel-note">共 {{ total }} 位注册用户</span>
      </div>

      <!-- 筛选栏：关键词（手机号/昵称）+ 角色 -->
      <el-form inline class="filter-bar">
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="手机号 / 昵称"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.role" placeholder="全部角色" clearable style="width: 150px">
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
      <el-table v-loading="loading" :data="tableData">
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <span class="user-avatar" :class="`av-${row.role.toLowerCase()}`">
                {{ avatarText(row) }}
              </span>
              <div>
                <strong class="user-name">{{ row.nickname || '未设置昵称' }}</strong>
                <span class="user-phone">{{ row.phone }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <span class="role-pill" :class="`role-${row.role.toLowerCase()}`">
              {{ dictLabel(ROLE_MAP, row.role) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="账号状态" width="110" align="center">
          <template #default="{ row }">
            <span class="status-pill" :class="row.status === 1 ? 'st-finished' : 'st-rejected'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="注册时间" width="180">
          <template #default="{ row }">
            <span class="muted">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="会员操作" width="150" align="center">
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
                开通会员
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
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>

        <template #empty>
          <div class="empty-state">
            <div class="empty-icon">👤</div>
            暂无用户数据
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
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserPage, updateUserRole } from '../../api/user'
import { ROLE_MAP, ROLE_OPTIONS, dictLabel } from '../../utils/dict'

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

// 头像文字：昵称首字，无昵称时取手机号末两位
function avatarText(row) {
  if (row.nickname) return row.nickname.slice(0, 1)
  return (row.phone || '').slice(-2)
}

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
  const actionText = role === 'VIP' ? '开通会员' : '取消会员'
  await ElMessageBox.confirm(
    `确定将用户「${row.nickname || row.phone}」${actionText}吗？`,
    '操作确认',
    { type: 'warning' }
  )
  await updateUserRole(row.id, role)
  ElMessage.success(`${actionText}成功`)
  loadData()
}

// 时间格式化：T 换空格并去掉秒
function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

onMounted(loadData)
</script>

<style scoped>
.panel-note {
  font-size: 12px;
  color: #a9b4c6;
}

/* 用户信息单元 */
.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 11px;
  font-size: 13px;
  font-weight: 700;
  color: #fff;
}

.av-user {
  background: linear-gradient(135deg, #6a97f6, #2c6bf2);
}

.av-vip {
  background: linear-gradient(135deg, #f7b955, #f5a524);
}

.av-admin {
  background: linear-gradient(135deg, #f77b7b, #ef4b4b);
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
  letter-spacing: 0.3px;
}

/* 角色胶囊 */
.role-pill {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.role-user {
  color: #1b56d0;
  background: #eef3ff;
}

.role-vip {
  color: #a76a05;
  background: #fff5e3;
}

.role-admin {
  color: #b03636;
  background: #fdeeee;
}

/* 状态胶囊 */
.status-pill {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.st-finished {
  color: #0a7f5e;
  background: #e7f8f2;
}

.st-rejected {
  color: #b03636;
  background: #fdeeee;
}

.muted {
  color: #a9b4c6;
  font-size: 12px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
