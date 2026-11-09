<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>故障分类管理</span>
        <el-button type="primary" @click="openDialog(null)">新增分类</el-button>
      </div>
    </template>

    <!-- 按地点类型分组筛选 -->
    <el-form inline class="filter-bar">
      <el-form-item label="地点类型">
        <el-radio-group v-model="filterLocationType" @change="handleSearch">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="DORM">宿舍楼内</el-radio-button>
          <el-radio-button value="PUBLIC">校园公共区域</el-radio-button>
        </el-radio-group>
      </el-form-item>
    </el-form>

    <!-- 分类表格 -->
    <el-table v-loading="loading" :data="pagedData" border stripe>
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="name" label="分类名称" min-width="160" />
      <el-table-column label="归属地点类型" width="140" align="center">
        <template #default="{ row }">
          <el-tag :type="dictTagType(LOCATION_TYPE_MAP, row.locationType)" size="small">
            {{ dictLabel(LOCATION_TYPE_MAP, row.locationType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序号" width="90" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" align="center">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="openDialog(row)">编辑</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>暂无分类数据</template>
    </el-table>

    <!-- 分页（前端分页，数据量小） -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="filteredData.length"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
      />
    </div>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新增分类'" width="440px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="如：网络故障" maxlength="50" />
        </el-form-item>
        <el-form-item label="地点类型" prop="locationType">
          <el-radio-group v-model="form.locationType">
            <el-radio value="DORM">宿舍楼内</el-radio>
            <el-radio value="PUBLIC">校园公共区域</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序号" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
          <span class="sort-tip">越小越靠前</span>
        </el-form-item>
        <!-- 编辑时才可调整启停状态 -->
        <el-form-item v-if="isEdit" label="状态">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="停用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确 定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listAllCategories,
  createCategory,
  updateCategory,
  deleteCategory
} from '../../api/category'
import { LOCATION_TYPE_MAP, dictLabel, dictTagType } from '../../utils/dict'

const loading = ref(false)
const allData = ref([]) // 全量分类（含停用）

// 地点类型筛选与前端分页
const filterLocationType = ref('')
const pageNum = ref(1)
const pageSize = ref(10)

// 新增 / 编辑弹窗状态
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({ name: '', locationType: 'DORM', sort: 0, status: 1 })

const formRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  locationType: [{ required: true, message: '请选择地点类型', trigger: 'change' }]
}

// 按地点类型筛选后的数据
const filteredData = computed(() => {
  if (!filterLocationType.value) return allData.value
  return allData.value.filter((item) => item.locationType === filterLocationType.value)
})

// 当前页数据（分类数据量小，采用前端分页即可）
const pagedData = computed(() => {
  const start = (pageNum.value - 1) * pageSize.value
  return filteredData.value.slice(start, start + pageSize.value)
})

// 加载全量分类（含停用，仅管理员接口）
async function loadData() {
  loading.value = true
  try {
    const res = await listAllCategories()
    allData.value = res.data || []
  } finally {
    loading.value = false
  }
}

// 切换地点类型筛选时回到第一页
function handleSearch() {
  pageNum.value = 1
}

/**
 * 打开新增 / 编辑弹窗
 * @param {Object|null} row 编辑时传入行数据，新增传 null
 */
function openDialog(row) {
  isEdit.value = !!row
  editingId.value = row?.id ?? null
  form.name = row?.name ?? ''
  form.locationType = row?.locationType ?? 'DORM'
  form.sort = row?.sort ?? 0
  form.status = row?.status ?? 1
  dialogVisible.value = true
}

// 提交新增 / 编辑
async function handleSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      // 编辑：携带状态字段（契约要求）
      await updateCategory(editingId.value, {
        name: form.name,
        locationType: form.locationType,
        sort: form.sort,
        status: form.status
      })
      ElMessage.success('分类修改成功')
    } else {
      // 新增：契约请求体不含状态字段
      await createCategory({
        name: form.name,
        locationType: form.locationType,
        sort: form.sort
      })
      ElMessage.success('分类新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

/**
 * 删除分类：弹确认框；删除后提示后端返回的 message
 * （若分类下已有工单，后端执行逻辑删除而非物理删除，message 会说明实际行为）
 * @param {Object} row 分类行数据
 */
async function handleDelete(row) {
  await ElMessageBox.confirm(
    `确定删除分类「${row.name}」吗？若分类下已有工单将改为停用。`,
    '删除确认',
    { type: 'warning' }
  )
  const res = await deleteCategory(row.id)
  ElMessage.success(res.message || '删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.filter-bar {
  margin-bottom: 8px;
}

.sort-tip {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
