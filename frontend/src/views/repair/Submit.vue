<template>
  <div class="page submit-page">
    <div class="submit-grid">
      <!-- ================= 左侧：报修表单 ================= -->
      <section class="panel form-panel">
        <div class="panel-hd">
          <div class="panel-title">填写报修信息</div>
          <span class="role-hint" :class="{ vip: isVip }">
            {{ isVip ? '会员通道 · 最多 5 张图' : '普通用户 · 最多 1 张图' }}
          </span>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="repair-form">
          <!-- 地点类型：卡片式选择，直观区分宿舍 / 公共区域 -->
          <el-form-item label="报修地点类型" prop="locationType">
            <el-radio-group v-model="form.locationType" class="type-cards">
              <el-radio value="DORM" class="type-card">
                <span class="type-icon dorm">🏠</span>
                <span class="type-text">
                  <strong>宿舍楼内</strong>
                  <em>填写楼号与房间号</em>
                </span>
              </el-radio>
              <el-radio value="PUBLIC" class="type-card">
                <span class="type-icon public">🌳</span>
                <span class="type-text">
                  <strong>校园公共区域</strong>
                  <em>填写具体位置描述</em>
                </span>
              </el-radio>
            </el-radio-group>
          </el-form-item>

          <!-- 宿舍楼内：楼号 + 房间号（同一行更紧凑） -->
          <div v-if="form.locationType === 'DORM'" class="field-row">
            <el-form-item label="宿舍楼号" prop="buildingNo">
              <el-input v-model="form.buildingNo" placeholder="如：5号楼" maxlength="50" />
            </el-form-item>
            <el-form-item label="房间号" prop="roomNo">
              <el-input v-model="form.roomNo" placeholder="如：302" maxlength="50" />
            </el-form-item>
          </div>

          <!-- 校园公共区域：具体位置 -->
          <el-form-item v-else label="具体位置" prop="publicLocation">
            <el-input
              v-model="form.publicLocation"
              placeholder="如：图书馆东侧主干道井盖、食堂门口"
              maxlength="200"
            />
          </el-form-item>

          <!-- 故障分类：按维修工种多选，末尾提供「其他（自行填写）」 -->
          <el-form-item label="故障分类" prop="categoryIds">
            <div class="category-picker">
              <button
                v-for="item in categoryOptions"
                :key="item.id"
                type="button"
                class="category-chip"
                :class="{ active: form.categoryIds.includes(item.id) }"
                @click="toggleCategory(item.id)"
              >
                <span class="chip-name">{{ item.name }}</span>
                <span class="chip-price">¥{{ formatPrice(item.price) }}</span>
              </button>
              <button
                type="button"
                class="category-chip is-other"
                :class="{ active: isCustomCategory }"
                @click="toggleOtherCategory"
              >
                其他（自行填写）
              </button>
            </div>
            <p class="category-tip">可多选，至少选择 1 个分类</p>
          </el-form-item>

          <!-- 选择「其他」时必填自定义故障类型（≤50字） -->
          <el-form-item v-if="isCustomCategory" label="故障类型" prop="customCategory">
            <el-input
              v-model="form.customCategory"
              placeholder="请填写故障类型"
              maxlength="50"
              show-word-limit
            />
          </el-form-item>

          <!-- 费用预估：按已选服务分类单价求和，会员自动 8 折 -->
          <div class="fee-panel">
            <div class="fee-head">
              <span class="fee-title">费用预估</span>
              <span class="fee-mode">{{ isCustomCategory ? '面议' : '按服务项计算' }}</span>
            </div>

            <template v-if="isCustomCategory">
              <p class="fee-negotiable">「其他」故障类型需师傅现场评估，费用面议。</p>
            </template>
            <template v-else-if="selectedCategories.length">
              <div v-for="item in selectedCategories" :key="item.id" class="fee-line">
                <span class="fee-name">{{ item.name }}</span>
                <span class="fee-unit">¥{{ formatPrice(item.price) }}</span>
              </div>
              <div class="fee-line is-total">
                <span>合计</span>
                <span>¥{{ formatPrice(feeTotal) }}</span>
              </div>
              <template v-if="isVip">
                <div class="fee-line is-discount">
                  <span>会员 8 折</span>
                  <span>-¥{{ formatPrice(feeDiscount) }}</span>
                </div>
                <div class="fee-sum">
                  <span>预估费用</span>
                  <strong>¥{{ formatPrice(feePayable) }}</strong>
                </div>
              </template>
              <p v-else class="fee-upgrade">
                <span class="fee-crown">💎</span>
                开通会员可享 8 折，预计可省 ¥{{ formatPrice(feeDiscount) }}
              </p>
            </template>
            <p v-else class="fee-empty">请选择故障分类后查看预估费用</p>
          </div>

          <!-- 故障描述 -->
          <el-form-item label="故障描述" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              placeholder="请描述故障情况，如发生时间、影响范围，方便维修人员快速定位"
            />
          </el-form-item>

          <!-- 图片上传：普通用户 1 张，会员 5 张；达到上限隐藏上传按钮 -->
          <el-form-item :label="`故障照片（${imageUrls.length}/${maxImageCount}）`">
            <div class="upload-wrap">
              <el-upload
                :class="{ 'hide-upload': imageUrls.length >= maxImageCount }"
                action="/api/file/upload"
                list-type="picture-card"
                :headers="uploadHeaders"
                :limit="maxImageCount"
                :file-list="fileList"
                :on-success="handleUploadSuccess"
                :on-remove="handleRemove"
                :on-exceed="handleExceed"
                :before-upload="beforeUpload"
                accept=".jpg,.jpeg,.png,.gif"
              >
                <div class="upload-inner">
                  <el-icon><Plus /></el-icon>
                  <span>上传照片</span>
                </div>
              </el-upload>
              <p class="upload-tip">
                支持 jpg / png / gif，单张不超过 5MB，最多 {{ maxImageCount }} 张
              </p>
            </div>
          </el-form-item>

          <!-- 会员专属能力：预约上门时间 + 紧急标记 -->
          <template v-if="isVip">
            <div class="vip-box">
              <div class="vip-box-title">
                <span class="vip-badge">VIP</span>
                会员专属服务
              </div>
              <el-form-item label="预约上门时间">
                <el-date-picker
                  v-model="form.appointmentTime"
                  type="datetime"
                  placeholder="请选择预约上门时间（可选）"
                  format="YYYY-MM-DD HH:mm:ss"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  :disabled-date="disabledPastDate"
                  style="width: 100%"
                />
              </el-form-item>
              <el-form-item label="紧急报修">
                <div class="urgent-row">
                  <el-switch v-model="form.urgent" :active-value="1" :inactive-value="0" />
                  <span class="urgent-tip">开启后工单将在管理员端优先展示</span>
                </div>
              </el-form-item>
            </div>
          </template>

          <!-- 普通用户提示：引导开通会员 -->
          <el-form-item v-else label=" ">
            <div class="upgrade-tip">
              <span class="upgrade-icon">💎</span>
              <div>
                <strong>升级会员解锁更多能力</strong>
                <p>最多 5 张故障图 · 自选上门时间 · 紧急标记 · 历史报修统计</p>
              </div>
            </div>
          </el-form-item>

          <div class="form-actions">
            <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
              提交报修
            </el-button>
            <el-button size="large" @click="handleReset">重置</el-button>
          </div>
        </el-form>
      </section>

      <!-- ================= 右侧：说明信息 ================= -->
      <aside class="side-col">
        <div class="panel tip-panel">
          <div class="panel-hd">
            <div class="panel-title">报修小贴士</div>
          </div>
          <ul class="tips">
            <li>
              <span class="tip-index">1</span>
              <div><strong>位置写清楚</strong><p>宿舍注明楼号房间号，公共区域描述附近参照物。</p></div>
            </li>
            <li>
              <span class="tip-index">2</span>
              <div><strong>照片拍清楚</strong><p>建议拍全景 + 特写，便于管理员判断故障类型。</p></div>
            </li>
            <li>
              <span class="tip-index">3</span>
              <div><strong>进度随时查</strong><p>提交后可在「我的工单」查看待接单、维修中、已完成状态。</p></div>
            </li>
          </ul>
        </div>

        <div class="panel status-legend">
          <div class="panel-hd">
            <div class="panel-title">工单状态说明</div>
          </div>
          <div class="legend-item"><i class="dot st-pending" />待接单：等待维修管理员受理</div>
          <div class="legend-item"><i class="dot st-progress" />维修中：管理员已接单并处理</div>
          <div class="legend-item"><i class="dot st-finished" />已完成：维修完成并归档</div>
          <div class="legend-item"><i class="dot st-rejected" />已驳回：不符合报修条件</div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { listCategories } from '../../api/category'
import { createOrder } from '../../api/order'
import { useUserStore } from '../../store/user'

const route = useRoute()
const userStore = useUserStore()

// 是否会员（会员可上传 5 张图、自选预约时间、标记紧急）
const isVip = computed(() => userStore.role === 'VIP')
// 图片数量上限：普通用户 1 张，会员 5 张
const maxImageCount = computed(() => (isVip.value ? 5 : 1))

const formRef = ref()
const submitting = ref(false)

// 是否选中了「其他（自行填写）」：与普通分类互斥
const otherSelected = ref(false)

// 表单数据（字段与后端接口契约保持一致）
const form = reactive({
  locationType: 'DORM', // 地点类型：DORM 宿舍楼内 / PUBLIC 校园公共区域
  buildingNo: '', // 宿舍楼号（DORM 必填）
  roomNo: '', // 房间号（DORM 必填）
  publicLocation: '', // 公共区域具体位置（PUBLIC 必填）
  categoryIds: [], // 故障分类 ID 数组（可多选；选择「其他」时为空数组）
  customCategory: '', // 自定义故障类型（选择「其他」时填写）
  description: '', // 故障描述
  appointmentTime: null, // 预约上门时间（仅会员）
  urgent: 0 // 是否紧急：1 紧急 / 0 普通（仅会员可标记）
})

// 图片相关：imageUrls 保存已上传成功的相对路径；fileList 供 el-upload 回显
const imageUrls = ref([])
const fileList = ref([])

// 上传请求头：携带 JWT token
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`
}))

// 故障分类选项（后端已按维修工种划分，无需按地点类型过滤）
const categoryOptions = ref([])

// 是否选中了「其他（自行填写）」
const isCustomCategory = computed(() => otherSelected.value)

// 已选服务分类（含单价），用于费用预估
const selectedCategories = computed(() =>
  categoryOptions.value.filter((item) => form.categoryIds.includes(item.id))
)
// 费用合计：所选分类单价之和
const feeTotal = computed(() =>
  selectedCategories.value.reduce((sum, item) => sum + Number(item.price || 0), 0)
)
// 会员折扣金额：与后端一致按 8 折计算并保留两位小数
const feeDiscount = computed(() => Math.round(feeTotal.value * 0.2 * 100) / 100)
// 会员预估费用
const feePayable = computed(() => Math.round(feeTotal.value * 0.8 * 100) / 100)

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

// 表单校验规则（与后端校验保持一致）
const rules = {
  locationType: [{ required: true, message: '请选择地点类型', trigger: 'change' }],
  buildingNo: [{ required: true, message: '请输入宿舍楼号', trigger: 'blur' }],
  roomNo: [{ required: true, message: '请输入房间号', trigger: 'blur' }],
  publicLocation: [{ required: true, message: '请输入具体位置', trigger: 'blur' }],
  categoryIds: [
    {
      validator: (rule, value, callback) => {
        // 选择「其他」时无需普通分类；否则至少选择 1 个
        if (!otherSelected.value && (!Array.isArray(value) || value.length === 0)) {
          callback(new Error('请至少选择 1 个故障分类'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  customCategory: [
    {
      validator: (rule, value, callback) => {
        if (isCustomCategory.value && !String(value || '').trim()) {
          callback(new Error('请填写故障类型'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  description: [{ required: true, message: '请填写故障描述', trigger: 'blur' }]
}

// 加载启用中的故障分类列表
async function loadCategories() {
  const res = await listCategories()
  categoryOptions.value = res.data || []
  applyPreselectedCategory()
}

// 从首页服务卡片带来的 categoryId 自动选中对应分类
function applyPreselectedCategory() {
  const raw = route.query.categoryId
  const id = Number(Array.isArray(raw) ? raw[0] : raw)
  if (!id) return
  if (!categoryOptions.value.some((item) => item.id === id)) return
  otherSelected.value = false
  form.customCategory = ''
  form.categoryIds = [id]
}

/**
 * 切换普通分类选中状态（多选）；选择普通分类时取消「其他」
 * @param {number} id 分类 ID
 */
function toggleCategory(id) {
  otherSelected.value = false
  form.customCategory = ''
  const index = form.categoryIds.indexOf(id)
  if (index >= 0) {
    form.categoryIds.splice(index, 1)
  } else {
    form.categoryIds.push(id)
  }
  formRef.value?.clearValidate(['categoryIds', 'customCategory'])
}

// 切换「其他」：选中时清空已选普通分类，取消时清空自定义内容
function toggleOtherCategory() {
  otherSelected.value = !otherSelected.value
  if (otherSelected.value) {
    form.categoryIds = []
  } else {
    form.customCategory = ''
  }
  formRef.value?.clearValidate(['categoryIds', 'customCategory'])
}

/**
 * 上传前校验：格式与大小（jpg/jpeg/png/gif，≤5MB）
 * @param {Object} file 待上传文件
 * @returns {boolean} 是否允许上传
 */
function beforeUpload(file) {
  const allowTypes = ['image/jpeg', 'image/png', 'image/gif']
  if (!allowTypes.includes(file.type)) {
    ElMessage.error('仅支持 jpg/jpeg/png/gif 格式图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('单张图片不能超过 5MB')
    return false
  }
  return true
}

/**
 * 上传成功回调：后端返回相对路径，保存到 imageUrls
 * @param {Object} res 后端统一响应体 { code, message, data }
 * @param {Object} file 当前上传文件
 * @param {Array} files 当前文件列表
 */
function handleUploadSuccess(res, file, files) {
  if (res.code === 200) {
    imageUrls.value.push(res.data)
    fileList.value = files
  } else {
    // 上传接口业务失败：移除该文件并提示
    ElMessage.error(res.message || '图片上传失败')
    fileList.value = files.filter((f) => f.uid !== file.uid)
  }
}

/**
 * 移除图片回调：同步移除 imageUrls 中对应路径
 * @param {Object} file 被移除的文件
 * @param {Array} files 剩余文件列表
 */
function handleRemove(file, files) {
  fileList.value = files
  // 已上传成功的文件 response 中保存了后端返回的相对路径
  const url = file.response?.data
  if (url) {
    imageUrls.value = imageUrls.value.filter((item) => item !== url)
  }
}

// 超出数量限制回调（正常情况按钮已隐藏，这里兜底提示）
function handleExceed() {
  ElMessage.warning(`最多只能上传 ${maxImageCount.value} 张图片`)
}

/**
 * 禁用过去的日期（预约上门时间不可早于今天）
 * @param {Date} date 待判断日期
 * @returns {boolean} 是否禁用
 */
function disabledPastDate(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

// 提交报修：按契约组装请求体（普通用户强制 urgent=0、appointmentTime=null）
async function handleSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    // 选择「其他」时 categoryIds 传空数组，改传 customCategory（两者互斥）
    const payload = {
      locationType: form.locationType,
      buildingNo: form.locationType === 'DORM' ? form.buildingNo : null,
      roomNo: form.locationType === 'DORM' ? form.roomNo : null,
      publicLocation: form.locationType === 'PUBLIC' ? form.publicLocation : null,
      categoryIds: otherSelected.value ? [] : [...form.categoryIds],
      customCategory: otherSelected.value ? form.customCategory.trim() : null,
      description: form.description,
      images: imageUrls.value,
      urgent: isVip.value ? form.urgent : 0,
      appointmentTime: isVip.value ? form.appointmentTime : null
    }
    await createOrder(payload)
    ElMessage.success('报修提交成功')
    handleReset()
  } finally {
    submitting.value = false
  }
}

// 重置表单与图片
function handleReset() {
  formRef.value.resetFields()
  form.locationType = 'DORM'
  form.categoryIds = []
  form.customCategory = ''
  otherSelected.value = false
  form.appointmentTime = null
  form.urgent = 0
  imageUrls.value = []
  fileList.value = []
}

// 页面初始化：加载故障分类
onMounted(() => {
  loadCategories()
})
</script>

<style scoped>
.submit-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 16px;
  align-items: start;
}

.form-panel {
  padding: 22px 26px 26px;
}

/* 右上角数量提示 */
.role-hint {
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  color: #5b6b80;
  background: #f2f5fa;
}

.role-hint.vip {
  color: #a76a05;
  background: #fff5e3;
}

.repair-form {
  margin-top: 4px;
}

/* 地点类型卡片式单选 */
.type-cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
}

.type-card {
  position: relative;
  display: flex;
  align-items: center;
  height: auto;
  margin: 0;
  padding: 14px 16px;
  border: 1px solid #e3eaf5;
  border-radius: 14px;
  background: #fbfcff;
  transition: all 0.2s ease;
}

.type-card:hover {
  border-color: #bcd0fb;
  background: #f6f9ff;
}

/* 选中态：主色描边 + 淡蓝底 */
.type-card.is-checked {
  border-color: #2c6bf2;
  background: #f2f6ff;
  box-shadow: 0 8px 20px rgba(44, 107, 242, 0.12);
}

.type-card :deep(.el-radio__input) {
  display: none;
}

.type-card :deep(.el-radio__label) {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0;
  width: 100%;
}

.type-icon {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  font-size: 20px;
}

.type-icon.dorm {
  background: #eef3ff;
}

.type-icon.public {
  background: #e7f8f2;
}

.type-text strong {
  display: block;
  font-size: 14px;
  color: #1b2b45;
}

.type-text em {
  display: block;
  margin-top: 3px;
  font-size: 11px;
  font-style: normal;
  color: #a9b4c6;
}

/* 楼号 + 房间号同行 */
.field-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

/* 故障分类多选标签 */
.category-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  width: 100%;
}

.category-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
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

/* 标签上的服务单价：橙色强调 */
.chip-price {
  font-size: 12px;
  font-weight: 700;
  color: #ff6b00;
}

.category-chip.active .chip-price {
  color: #fff;
  opacity: 0.92;
}

.category-chip:hover {
  border-color: #bcd0fb;
  color: #2c6bf2;
}

/* 选中态：品牌渐变，与状态胶囊保持一致 */
.category-chip.active {
  border-color: transparent;
  color: #fff;
  background: linear-gradient(135deg, #2c6bf2 0%, #17bfd4 100%);
  box-shadow: 0 8px 18px rgba(44, 107, 242, 0.24);
}

/* 「其他」用虚线区分，选中后同样高亮 */
.category-chip.is-other {
  border-style: dashed;
}

.category-chip.is-other.active {
  border-style: solid;
}

.category-tip {
  width: 100%;
  margin: 8px 0 0;
  font-size: 12px;
  color: #a9b4c6;
}

/* ============ 费用预估面板 ============ */
.fee-panel {
  margin: 2px 0 18px;
  padding: 16px 18px;
  border: 1px solid #ffe3cf;
  border-radius: 16px;
  background: linear-gradient(135deg, #fff8f2 0%, #fff 70%);
}

.fee-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.fee-title {
  font-size: 13px;
  font-weight: 700;
  color: #1b2b45;
}

.fee-mode {
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 11px;
  color: #ff8a3d;
  background: #fff0e4;
}

.fee-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 5px 0;
  font-size: 13px;
  color: #5b6b80;
}

.fee-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fee-unit {
  font-weight: 600;
  color: #ff6b00;
}

.fee-line.is-total {
  margin-top: 4px;
  padding-top: 8px;
  border-top: 1px dashed #ffe0c9;
  font-weight: 600;
  color: #1b2b45;
}

.fee-line.is-total span:last-child {
  font-weight: 700;
  color: #ff6b00;
}

.fee-line.is-discount span:last-child {
  color: #ff6b00;
}

.fee-sum {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #ffe0c9;
  font-size: 13px;
  color: #5b6b80;
}

.fee-sum strong {
  font-size: 22px;
  font-weight: 800;
  color: #ff6b00;
}

.fee-upgrade {
  margin: 10px 0 0;
  padding: 8px 12px;
  border-radius: 10px;
  font-size: 12px;
  color: #a76a05;
  background: #fff5e3;
}

.fee-crown {
  margin-right: 6px;
}

.fee-empty,
.fee-negotiable {
  margin: 0;
  padding: 4px 0;
  font-size: 12px;
  color: #7a879b;
}

/* 上传区 */
.upload-wrap {
  width: 100%;
}

.upload-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #7a879b;
}

.upload-tip {
  margin: 8px 0 0;
  font-size: 12px;
  color: #a9b4c6;
  line-height: 1.5;
}

/* 会员专属区块 */
.vip-box {
  margin: 4px 0 18px;
  padding: 16px 18px 4px;
  border-radius: 14px;
  background: linear-gradient(135deg, #fffaef 0%, #fff 100%);
  border: 1px solid #ffedc7;
}

.vip-box-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 700;
  color: #a76a05;
}

.vip-badge {
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #f7b955, #f5a524);
}

.urgent-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.urgent-tip {
  font-size: 12px;
  color: #a76a05;
}

/* 非会员升级提示 */
.upgrade-tip {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
  padding: 14px 18px;
  border-radius: 14px;
  background: #f7f9fd;
  border: 1px dashed #d9e3f5;
}

.upgrade-icon {
  font-size: 20px;
}

.upgrade-tip strong {
  display: block;
  font-size: 13px;
  color: #1b2b45;
}

.upgrade-tip p {
  margin: 5px 0 0;
  font-size: 12px;
  color: #7a879b;
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
  padding-top: 20px;
  border-top: 1px solid #f0f4fb;
}

/* ============ 右侧栏 ============ */
.side-col {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.tips {
  margin: 0;
  padding: 0;
  list-style: none;
}

.tips li {
  display: flex;
  gap: 12px;
  padding: 12px 0;
}

.tips li + li {
  border-top: 1px dashed #eef2f9;
}

.tip-index {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
  color: #2c6bf2;
  background: #eef3ff;
}

.tips strong {
  display: block;
  font-size: 13px;
  color: #1b2b45;
}

.tips p {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.7;
  color: #7a879b;
}

/* 状态图例 */
.legend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 0;
  font-size: 12px;
  color: #5b6b80;
}

.legend-item + .legend-item {
  border-top: 1px dashed #f2f5fb;
}

.dot {
  width: 9px;
  height: 9px;
  flex-shrink: 0;
  border-radius: 50%;
}

.dot.st-pending {
  background: #f5a524;
}

.dot.st-progress {
  background: #2c6bf2;
}

.dot.st-finished {
  background: #0fb583;
}

.dot.st-rejected {
  background: #ef4b4b;
}

/* 达到上传上限时隐藏“+”上传按钮 */
.hide-upload :deep(.el-upload--picture-card) {
  display: none;
}

@media (max-width: 1100px) {
  .submit-grid {
    grid-template-columns: 1fr;
  }

  .type-cards {
    grid-template-columns: 1fr;
  }
}
</style>
