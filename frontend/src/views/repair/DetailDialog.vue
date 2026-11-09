<template>
  <!-- 工单详情弹窗：我的工单 / 管理员工单管理 共用 -->
  <el-dialog
    :model-value="visible"
    title="工单详情"
    width="640px"
    @update:model-value="handleUpdate"
    @open="loadDetail"
  >
    <div v-loading="loading">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="工单号" :span="2">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="地点类型">
          <el-tag :type="dictTagType(LOCATION_TYPE_MAP, detail.locationType)" size="small">
            {{ dictLabel(LOCATION_TYPE_MAP, detail.locationType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="地点">{{ formatLocation(detail) }}</el-descriptions-item>
        <el-descriptions-item label="故障分类">{{ detail.categoryName }}</el-descriptions-item>
        <el-descriptions-item label="是否紧急">
          <el-tag v-if="detail.urgent === 1" type="danger" size="small">紧急</el-tag>
          <span v-else>普通</span>
        </el-descriptions-item>
        <el-descriptions-item label="预约上门时间" :span="2">
          {{ detail.appointmentTime || '未预约（系统默认派单）' }}
        </el-descriptions-item>
        <el-descriptions-item label="故障描述" :span="2">{{ detail.description }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="dictTagType(STATUS_MAP, detail.status)" size="small">
            {{ dictLabel(STATUS_MAP, detail.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="接单时间">{{ detail.acceptTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ detail.finishTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="报修人" :span="2">
          {{ detail.userNickname }}（{{ detail.userPhone }}）
        </el-descriptions-item>
        <el-descriptions-item label="维修备注" :span="2">
          {{ detail.adminRemark || '暂无' }}
        </el-descriptions-item>

        <!-- 故障图片：缩略图可点击放大预览 -->
        <el-descriptions-item label="故障图片" :span="2">
          <template v-if="detail.images && detail.images.length > 0">
            <el-image
              v-for="(img, index) in detail.images"
              :key="index"
              class="order-image"
              :src="img"
              :preview-src-list="detail.images"
              :initial-index="index"
              fit="cover"
              preview-teleported
            />
          </template>
          <span v-else>未上传图片</span>
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <template #footer>
      <el-button @click="handleUpdate(false)">关 闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { getOrderDetail } from '../../api/order'
import { STATUS_MAP, LOCATION_TYPE_MAP, dictLabel, dictTagType, formatLocation } from '../../utils/dict'

// 组件入参：visible 控制弹窗显隐，orderId 为要查看的工单 ID
const props = defineProps({
  visible: { type: Boolean, default: false },
  orderId: { type: [Number, String], default: null }
})

const emit = defineEmits(['update:visible'])

const loading = ref(false)
const detail = ref(null)

// 弹窗打开时加载工单详情
async function loadDetail() {
  if (!props.orderId) return
  loading.value = true
  detail.value = null
  try {
    const res = await getOrderDetail(props.orderId)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

// 同步弹窗显隐状态给父组件（v-model:visible）
function handleUpdate(value) {
  emit('update:visible', value)
}
</script>

<style scoped>
.order-image {
  width: 100px;
  height: 100px;
  margin-right: 8px;
  border-radius: 4px;
  cursor: pointer;
}
</style>
