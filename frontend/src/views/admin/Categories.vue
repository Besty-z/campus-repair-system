<template>
  <div class="page categories-page">
    <!-- 顶部统计：分类总数 / 启用 / 停用 -->
    <div class="stat-grid">
      <div class="stat-tile is-brand">
        <div class="stat-tile-label">分类总数</div>
        <div class="stat-tile-value">{{ allData.length }}</div>
        <div class="stat-tile-icon"><el-icon><Menu /></el-icon></div>
      </div>
      <div class="stat-tile is-mint">
        <div class="stat-tile-label">启用</div>
        <div class="stat-tile-value">{{ enabledCount }}</div>
        <div class="stat-tile-icon"><el-icon><CircleCheck /></el-icon></div>
      </div>
      <div class="stat-tile is-amber">
        <div class="stat-tile-label">停用</div>
        <div class="stat-tile-value">{{ disabledCount }}</div>
        <div class="stat-tile-icon"><el-icon><CircleClose /></el-icon></div>
      </div>
    </div>

    <section class="panel">
      <div class="panel-hd">
        <div class="panel-title">故障分类管理</div>
        <el-button type="primary" @click="openDialog(null)">+ 新增分类</el-button>
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
        <el-table-column label="服务价格" width="120" align="center">
          <template #default="{ row }">
            <span class="price-text">¥{{ formatPrice(row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="服务图片" width="100" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.imageUrl"
              class="cat-thumb"
              :src="row.imageUrl"
              :preview-src-list="[row.imageUrl]"
              preview-teleported
              fit="cover"
            />
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="90" align="center" />
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
          :total="allData.length"
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
        <el-form-item label="服务价格（元）" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="10" />
          <span class="sort-tip">首页展示「¥xx 起」，并用于工单费用预估</span>
        </el-form-item>
        <el-form-item label="服务图片 URL" prop="imageUrl">
          <el-input
            v-model="form.imageUrl"
            placeholder="可空，如：https://example.com/service.png"
            maxlength="500"
          />
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
import { Menu, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import {
  listAllCategories,
  createCategory,
  updateCategory,
  deleteCategory
} from '../../api/category'

const loading = ref(false)
const allData = ref([]) // 全量分类（含停用）

// 前端分页
const pageNum = ref(1)
const pageSize = ref(10)

// 新增 / 编辑弹窗状态
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({ name: '', sort: 0, status: 1, price: 0, imageUrl: '' })

const formRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  price: [
    { required: true, message: '请输入服务价格', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value == null || Number(value) < 0) {
          callback(new Error('服务价格不能小于 0'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}

// 概览统计：分类总数 / 启用 / 停用
const enabledCount = computed(() => allData.value.filter((item) => item.status === 1).length)
const disabledCount = computed(() => allData.value.filter((item) => item.status !== 1).length)

// 当前页数据（分类数据量小，采用前端分页即可）
const pagedData = computed(() => {
  const start = (pageNum.value - 1) * pageSize.value
  return allData.value.slice(start, start + pageSize.value)
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

/**
 * 打开新增 / 编辑弹窗
 * @param {Object|null} row 编辑时传入行数据，新增传 null
 */
function openDialog(row) {
  isEdit.value = !!row
  editingId.value = row?.id ?? null
  form.name = row?.name ?? ''
  form.sort = row?.sort ?? 0
  form.status = row?.status ?? 1
  form.price = row?.price ?? 0
  form.imageUrl = row?.imageUrl ?? ''
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
        sort: form.sort,
        status: form.status,
        price: Number(form.price),
        imageUrl: (form.imageUrl || '').trim()
      })
      ElMessage.success('分类修改成功')
    } else {
      // 新增：契约请求体不含状态字段
      await createCategory({
        name: form.name,
        sort: form.sort,
        price: Number(form.price),
        imageUrl: (form.imageUrl || '').trim()
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

onMounted(loadData)
</script>

<style scoped>
/* 分类名 */
.cat-name {
  font-size: 13px;
  font-weight: 600;
  color: #1b2b45;
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

/* 服务价格：橙色强调 */
.price-text {
  font-size: 13px;
  font-weight: 700;
  color: #ff6b00;
}

/* 服务图片缩略图 */
.cat-thumb {
  width: 52px;
  height: 36px;
  border-radius: 8px;
  border: 1px solid #eef2f9;
  cursor: pointer;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
