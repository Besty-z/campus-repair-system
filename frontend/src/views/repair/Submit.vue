<template>
  <el-card>
    <template #header>
      <span>提交报修</span>
    </template>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="110px"
      style="max-width: 640px"
    >
      <!-- 地点类型：宿舍楼内 / 校园公共区域 -->
      <el-form-item label="地点类型" prop="locationType">
        <el-radio-group v-model="form.locationType" @change="handleLocationTypeChange">
          <el-radio value="DORM">宿舍楼内</el-radio>
          <el-radio value="PUBLIC">校园公共区域</el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 宿舍楼内：楼号 + 房间号 -->
      <template v-if="form.locationType === 'DORM'">
        <el-form-item label="宿舍楼号" prop="buildingNo">
          <el-input v-model="form.buildingNo" placeholder="如：5号楼" maxlength="50" />
        </el-form-item>
        <el-form-item label="房间号" prop="roomNo">
          <el-input v-model="form.roomNo" placeholder="如：302" maxlength="50" />
        </el-form-item>
      </template>

      <!-- 校园公共区域：具体位置 -->
      <el-form-item v-else label="具体位置" prop="publicLocation">
        <el-input
          v-model="form.publicLocation"
          placeholder="如：图书馆东侧主干道井盖"
          maxlength="200"
        />
      </el-form-item>

      <!-- 故障分类：根据地点类型联动加载 -->
      <el-form-item label="故障分类" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="请选择故障分类" style="width: 100%">
          <el-option
            v-for="item in categoryOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>

      <!-- 故障描述 -->
      <el-form-item label="故障描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="请描述故障情况，方便维修人员快速定位问题"
        />
      </el-form-item>

      <!-- 图片上传：普通用户 1 张，会员 5 张；达到上限隐藏上传按钮 -->
      <el-form-item label="故障图片">
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
          <el-icon><Plus /></el-icon>
        </el-upload>
        <div class="upload-tip">支持 jpg/png/gif，单张不超过 5MB，最多 {{ maxImageCount }} 张</div>
      </el-form-item>

      <!-- 会员专属能力：预约上门时间 + 紧急标记 -->
      <template v-if="isVip">
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
          <el-switch v-model="form.urgent" :active-value="1" :inactive-value="0" />
          <span class="urgent-tip">紧急工单将在管理员端优先展示</span>
        </el-form-item>
      </template>

      <!-- 普通用户提示：引导开通会员 -->
      <el-form-item v-else label=" ">
        <el-alert
          type="info"
          :closable="false"
          title="开通会员可上传最多5张图片、自选预约时间、紧急标记"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交报修</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { listCategories } from '../../api/category'
import { createOrder } from '../../api/order'
import { useUserStore } from '../../store/user'

const userStore = useUserStore()

// 是否会员（会员可上传 5 张图、自选预约时间、标记紧急）
const isVip = computed(() => userStore.role === 'VIP')
// 图片数量上限：普通用户 1 张，会员 5 张
const maxImageCount = computed(() => (isVip.value ? 5 : 1))

const formRef = ref()
const submitting = ref(false)

// 表单数据（字段与后端接口契约保持一致）
const form = reactive({
  locationType: 'DORM', // 地点类型：DORM 宿舍楼内 / PUBLIC 校园公共区域
  buildingNo: '', // 宿舍楼号（DORM 必填）
  roomNo: '', // 房间号（DORM 必填）
  publicLocation: '', // 公共区域具体位置（PUBLIC 必填）
  categoryId: null, // 故障分类 ID
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

// 故障分类下拉选项（随地点类型联动）
const categoryOptions = ref([])

// 表单校验规则（与后端校验保持一致）
const rules = {
  locationType: [{ required: true, message: '请选择地点类型', trigger: 'change' }],
  buildingNo: [{ required: true, message: '请输入宿舍楼号', trigger: 'blur' }],
  roomNo: [{ required: true, message: '请输入房间号', trigger: 'blur' }],
  publicLocation: [{ required: true, message: '请输入具体位置', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择故障分类', trigger: 'change' }],
  description: [{ required: true, message: '请填写故障描述', trigger: 'blur' }]
}

/**
 * 加载故障分类列表（按地点类型过滤）
 * @param {string} locationType 地点类型 DORM/PUBLIC
 */
async function loadCategories(locationType) {
  const res = await listCategories(locationType)
  categoryOptions.value = res.data || []
}

// 切换地点类型：清空已选分类并重新加载该类型下的分类
function handleLocationTypeChange() {
  form.categoryId = null
  loadCategories(form.locationType)
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
    const payload = {
      locationType: form.locationType,
      buildingNo: form.locationType === 'DORM' ? form.buildingNo : null,
      roomNo: form.locationType === 'DORM' ? form.roomNo : null,
      publicLocation: form.locationType === 'PUBLIC' ? form.publicLocation : null,
      categoryId: form.categoryId,
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
  form.appointmentTime = null
  form.urgent = 0
  imageUrls.value = []
  fileList.value = []
  loadCategories(form.locationType)
}

// 页面初始化：加载默认地点类型（宿舍楼内）的分类
onMounted(() => {
  loadCategories(form.locationType)
})
</script>

<style scoped>
.upload-tip {
  width: 100%;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.urgent-tip {
  margin-left: 12px;
  font-size: 12px;
  color: #f56c6c;
}

/* 达到上传上限时隐藏“+”上传按钮 */
.hide-upload :deep(.el-upload--picture-card) {
  display: none;
}
</style>
