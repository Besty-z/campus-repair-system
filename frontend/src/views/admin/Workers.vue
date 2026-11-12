<template>
  <div class="page workers-page">
    <section class="panel">
      <div class="panel-hd">
        <div class="panel-title">维修工管理</div>
        <el-button type="primary" @click="openDialog(null)">+ 新增维修工</el-button>
      </div>

      <!-- 筛选栏：姓名 / 手机号关键词 -->
      <el-form inline class="filter-bar">
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="姓名 / 手机号"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
        <span class="total-hint">共 {{ total }} 位维修工</span>
      </el-form>

      <!-- 维修工表格 -->
      <el-table v-loading="loading" :data="tableData">
        <el-table-column label="维修工" width="150">
          <template #default="{ row }">
            <div class="worker-cell">
              <span class="worker-avatar">{{ (row.name || '维').slice(0, 1) }}</span>
              <strong class="worker-name">{{ row.name }}</strong>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="phone" label="手机号" width="140">
          <template #default="{ row }">
            <span class="phone">{{ row.phone }}</span>
          </template>
        </el-table-column>

        <el-table-column label="简介" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span :class="{ muted: !row.intro }">{{ row.intro || '暂无简介' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="擅长分类" min-width="180">
          <template #default="{ row }">
            <div class="cat-tags">
              <span
                v-for="(name, index) in row.categoryNames || []"
                :key="index"
                class="cat-tag"
              >
                {{ name }}
              </span>
              <span v-if="!(row.categoryNames || []).length" class="muted">未设置</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="completedCount" label="完成数" width="90" align="center">
          <template #default="{ row }">{{ row.completedCount ?? 0 }}</template>
        </el-table-column>

        <el-table-column label="平均评分" width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.avgRating == null" class="muted">暂无</span>
            <span v-else class="rating">★ {{ Number(row.avgRating).toFixed(1) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <span class="status-pill" :class="row.status === 1 ? 'st-finished' : 'st-rejected'">
              {{ row.status === 1 ? '在职' : '停用' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDialog(row)">编辑</el-button>
            <el-button
              :type="row.status === 1 ? 'danger' : 'success'"
              link
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <div class="empty-state">
            <div class="empty-icon">🧰</div>
            暂无维修工数据，点击右上角新增
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

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑维修工' : '新增维修工'" width="480px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="form.phone"
            :disabled="isEdit"
            placeholder="请输入 11 位手机号（同时作为登录账号）"
            maxlength="11"
          />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入维修工姓名" maxlength="30" />
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input
            v-model="form.intro"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="如：熟悉水电与空调维修，5 年校园维修经验"
          />
        </el-form-item>
        <el-form-item label="擅长分类">
          <el-select
            v-model="form.categoryIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="请选择该维修工擅长处理的分类"
            style="width: 100%"
          >
            <el-option
              v-for="item in categoryOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { nextTick, reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWorkerPage, createWorker, updateWorker } from '../../api/worker'
import { listCategories } from '../../api/category'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)

// 查询参数：关键词 + 分页
const query = reactive({
  keyword: '',
  pageNum: 1,
  pageSize: 10
})

// 擅长分类选项
const categoryOptions = ref([])

// 新增 / 编辑弹窗状态
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({
  phone: '',
  name: '',
  intro: '',
  categoryIds: [],
  status: 1
})

const formRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入维修工姓名', trigger: 'blur' }]
}

// 加载维修工分页
async function loadData() {
  loading.value = true
  try {
    const res = await getWorkerPage({
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
  query.keyword = ''
  handleSearch()
}

/**
 * 打开新增 / 编辑弹窗
 * @param {Object|null} row 编辑时传入行数据，新增传 null
 */
async function openDialog(row) {
  isEdit.value = !!row
  editingId.value = row?.id ?? null
  form.phone = row?.phone ?? ''
  form.name = row?.name ?? ''
  form.intro = row?.intro ?? ''
  form.categoryIds = Array.isArray(row?.categoryIds) ? [...row.categoryIds] : []
  form.status = row?.status ?? 1
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

// 提交新增 / 编辑
async function handleSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      // 编辑：契约要求携带姓名、简介、擅长分类与状态
      await updateWorker(editingId.value, {
        name: form.name,
        intro: form.intro,
        categoryIds: form.categoryIds,
        status: form.status
      })
      ElMessage.success('维修工资料已更新')
    } else {
      // 新增：手机号 + 姓名 + 简介 + 擅长分类
      await createWorker({
        phone: form.phone,
        name: form.name,
        intro: form.intro,
        categoryIds: form.categoryIds
      })
      ElMessage.success('维修工新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

/**
 * 启用 / 停用维修工
 * @param {Object} row 维修工行数据
 */
async function handleToggleStatus(row) {
  const nextStatus = row.status === 1 ? 0 : 1
  const actionText = nextStatus === 1 ? '启用' : '停用'
  await ElMessageBox.confirm(
    `确定${actionText}维修工「${row.name}」吗？${nextStatus === 0 ? '停用后将无法登录与接单。' : ''}`,
    `${actionText}确认`,
    { type: 'warning' }
  )
  await updateWorker(row.id, {
    name: row.name,
    intro: row.intro,
    categoryIds: row.categoryIds || [],
    status: nextStatus
  })
  ElMessage.success(`${actionText}成功`)
  loadData()
}

// 初始化：加载分类选项与维修工列表
onMounted(async () => {
  loadData()
  const res = await listCategories()
  categoryOptions.value = res.data || []
})
</script>

<style scoped>
.total-hint {
  margin-left: auto;
  font-size: 12px;
  color: #a9b4c6;
}

/* 维修工信息单元 */
.worker-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.worker-avatar {
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
  background: linear-gradient(135deg, #3fd0a2, #0fb583);
}

.worker-name {
  font-size: 13px;
  font-weight: 600;
  color: #1b2b45;
}

.phone {
  font-family: 'SF Mono', Consolas, monospace;
  font-size: 12px;
  color: #5b6b80;
  letter-spacing: 0.3px;
}

.rating {
  font-size: 12px;
  font-weight: 600;
  color: #a76a05;
}

/* 分类标签 */
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
  font-size: 12px;
  color: #a9b4c6;
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

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
