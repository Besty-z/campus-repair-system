<template>
  <div class="page categories-page">
    <!-- 顶部统计：两类地点各自的分类数量 -->
    <div class="stat-grid">
      <div class="stat-tile is-brand">
        <div class="stat-tile-label">分类总数</div>
        <div class="stat-tile-value">{{ allData.length }}</div>
        <div class="stat-tile-icon"><el-icon><Menu /></el-icon></div>
      </div>
      <div class="stat-tile is-cyan">
        <div class="stat-tile-label">宿舍楼内</div>
        <div class="stat-tile-value">{{ dormCount }}</div>
        <div class="stat-tile-icon"><el-icon><HomeFilled /></el-icon></div>
      </div>
      <div class="stat-tile is-mint">
        <div class="stat-tile-label">校园公共区域</div>
        <div class="stat-tile-value">{{ publicCount }}</div>
        <div class="stat-tile-icon"><el-icon><Location /></el-icon></div>
      </div>
      <div class="stat-tile is-amber">
        <div class="stat-tile-label">已停用</div>
        <div class="stat-tile-value">{{ disabledCount }}</div>
        <div class="stat-tile-icon"><el-icon><CircleClose /></el-icon></div>
      </div>
    </div>

    <section class="panel">
      <div class="panel-hd">
        <div class="panel-title">故障分类管理</div>
        <el-button type="primary" @click="openDialog(null)">+ 新增分类</el-button>
      </div>

      <!-- 按地点类型筛选：胶囊样式 -->
      <div class="status-tabs">
        <button class="status-tab" :class="{ active: filterLocationType === '' }" @click="setFilter('')">
          全部
        </button>
        <button
          class="status-tab"
          :class="{ active: filterLocationType === 'DORM' }"
          @click="setFilter('DORM')"
        >
          宿舍楼内
        </button>
        <button
          class="status-tab"
          :class="{ active: filterLocationType === 'PUBLIC' }"
          @click="setFilter('PUBLIC')"
        >
          校园公共区域
        </button>
        <span class="total-hint">共 {{ filteredData.length }} 个分类</span>
      </div>

      <!-- 分类表格 -->
      <el-table v-loading="loading" :data="pagedData">
        <el-table-column prop="id" label="ID" width="70" align="center">
          <template #default="{ row }"><span class="muted">#{{ row.id }}</span></template>
        </el-table-column>
        <el-table-column label="分类名称" min-width="180">
          <template #default="{ row }">
            <span class="cat-name">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="归属地点类型" width="150">
          <template #default="{ row }">
            <span class="loc-type" :class="row.locationType === 'DORM' ? 'dorm' : 'public'">
              {{ dictLabel(LOCATION_TYPE_MAP, row.locationType) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序号" width="90" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <span class="status-pill" :class="row.status === 1 ? 'st-finished' : 'st-rejected'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDialog(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-state">
            <div class="empty-icon">🗂️</div>
            暂无分类数据，点击右上角新增
          </div>
        </template>
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
    </section>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新增分类'" width="460px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="如：网络故障" maxlength="50" />
        </el-form-item>
        <el-form-item label="地点类型" prop="locationType">
          <el-radio-group v-model="form.locationType" class="type-cards">
            <el-radio value="DORM" border>宿舍楼内</el-radio>
            <el-radio value="PUBLIC" border>校园公共区域</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序号" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
          <span class="sort-tip">数字越小越靠前</span>
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
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Menu, HomeFilled, Location, CircleClose } from '@element-plus/icons-vue'
import {
  listAllCategories,
  createCategory,
  updateCategory,
  deleteCategory
} from '../../api/category'
import { LOCATION_TYPE_MAP, dictLabel } from '../../utils/dict'

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

// 概览统计：两类地点分类数量 + 停用数量
const dormCount = computed(() => allData.value.filter((item) => item.locationType === 'DORM').length)
const publicCount = computed(() => allData.value.filter((item) => item.locationType === 'PUBLIC').length)
const disabledCount = computed(() => allData.value.filter((item) => item.status !== 1).length)

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
function setFilter(value) {
  filterLocationType.value = value
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
/* 分类筛选胶囊 */
.status-tabs {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.status-tab {
  height: 32px;
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

.total-hint {
  margin-left: auto;
  font-size: 12px;
  color: #a9b4c6;
}

/* 分类名 */
.cat-name {
  font-size: 13px;
  font-weight: 600;
  color: #1b2b45;
}

/* 地点类型标签 */
.loc-type {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}

.loc-type.dorm {
  color: #1b56d0;
  background: #eef3ff;
}

.loc-type.public {
  color: #0a7f5e;
  background: #e7f8f2;
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
  font-family: 'SF Mono', Consolas, monospace;
  font-size: 12px;
  color: #a9b4c6;
}

.sort-tip {
  margin-left: 12px;
  font-size: 12px;
  color: #a9b4c6;
}

/* 弹窗中的地点类型按钮铺满 */
.type-cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  width: 100%;
}

.type-cards :deep(.el-radio) {
  margin: 0;
  height: 40px;
  justify-content: center;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
