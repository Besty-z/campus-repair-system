<template>
  <!-- 工单详情弹窗：我的工单 / 管理员工单管理 共用 -->
  <el-dialog
    :model-value="visible"
    width="680px"
    class="detail-dialog"
    @update:model-value="handleUpdate"
    @open="loadDetail"
  >
    <!-- 自定义头部：工单号 + 状态，信息一眼可见 -->
    <template #header>
      <div class="dlg-head">
        <div>
          <p class="dlg-label">工单号</p>
          <h3 class="dlg-no">{{ detail?.orderNo || '加载中...' }}</h3>
        </div>
        <div class="dlg-head-tags">
          <span v-if="detail?.urgent === 1" class="urgent-tag">紧急</span>
          <span v-if="detail" class="status-pill" :class="`st-${detail.status.toLowerCase()}`">
            {{ dictLabel(STATUS_MAP, detail.status) }}
          </span>
        </div>
      </div>
    </template>

    <div v-loading="loading" class="detail-body">
      <template v-if="detail">
        <!-- 故障图片：优先展示，缩略图可点击放大 -->
        <div class="gallery-block">
          <p class="block-title">故障照片</p>
          <div v-if="detail.images && detail.images.length > 0" class="gallery">
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
          </div>
          <div v-else class="gallery-empty">未上传故障照片</div>
        </div>

        <!-- 基础信息 -->
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">地点类型</span>
            <span class="info-value">
              <el-tag :type="dictTagType(LOCATION_TYPE_MAP, detail.locationType)" size="small">
                {{ dictLabel(LOCATION_TYPE_MAP, detail.locationType) }}
              </el-tag>
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">报修地点</span>
            <span class="info-value">{{ formatLocation(detail) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">故障分类</span>
            <span class="info-value">
              <template v-if="detailCategoryNames.length">
                <el-tag
                  v-for="(name, index) in detailCategoryNames"
                  :key="index"
                  size="small"
                  class="cat-tag"
                >
                  {{ name }}
                </el-tag>
              </template>
              <template v-else>-</template>
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">是否紧急</span>
            <span class="info-value">{{ detail.urgent === 1 ? '紧急工单（优先处理）' : '普通工单' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">预估费用</span>
            <span class="info-value est-price" :class="{ 'is-empty': detail.estimatedPrice == null }">
              {{ formatEstimatedPrice(detail.estimatedPrice) }}
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">预约上门</span>
            <span class="info-value">
              {{ detail.appointmentTime || '未预约（系统默认派单）' }}
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">报修人</span>
            <span class="info-value">{{ detail.userNickname }}（{{ detail.userPhone }}）</span>
          </div>
          <div class="info-item">
            <span class="info-label">维修工</span>
            <span class="info-value" :class="{ 'is-empty': !detail.workerName }">{{ workerText }}</span>
          </div>
        </div>

        <!-- 故障描述 -->
        <div class="text-block">
          <p class="block-title">故障描述</p>
          <p class="desc-text">{{ detail.description }}</p>
        </div>

        <!-- 维修备注 -->
        <div class="text-block">
          <p class="block-title">维修备注</p>
          <p class="desc-text" :class="{ 'is-empty': !detail.adminRemark }">
            {{ detail.adminRemark || '暂无维修备注' }}
          </p>
        </div>

        <!-- 用户评价：有评分时展示星级、分数与评价内容 -->
        <div v-if="detail.ratingScore != null" class="text-block">
          <p class="block-title">用户评价</p>
          <div class="rating-row">
            <el-rate :model-value="detail.ratingScore" disabled />
            <span class="rating-score">{{ detail.ratingScore }} 分</span>
            <span v-if="detail.ratingTime" class="rating-time">
              {{ formatTime(detail.ratingTime) }}
            </span>
          </div>
          <p class="desc-text" :class="{ 'is-empty': !detail.ratingComment }">
            {{ detail.ratingComment || '用户未填写评价内容' }}
          </p>
        </div>

        <!-- 时间线：创建 -> 接单 -> 完成 -->
        <div class="timeline">
          <div class="tl-item done">
            <span class="tl-dot" />
            <div>
              <strong>提交报修</strong>
              <em>{{ formatTime(detail.createTime) }}</em>
            </div>
          </div>
          <div class="tl-line" :class="{ done: detail.acceptTime }" />
          <div class="tl-item" :class="{ done: detail.acceptTime }">
            <span class="tl-dot" />
            <div>
              <strong>管理员接单</strong>
              <em>{{ detail.acceptTime ? formatTime(detail.acceptTime) : '等待接单' }}</em>
            </div>
          </div>
          <div class="tl-line" :class="{ done: detail.finishTime }" />
          <div class="tl-item" :class="{ done: detail.finishTime }">
            <span class="tl-dot" />
            <div>
              <strong>维修完成</strong>
              <em>{{ detail.finishTime ? formatTime(detail.finishTime) : '进行中' }}</em>
            </div>
          </div>
        </div>
      </template>
    </div>

    <template #footer>
      <el-button @click="handleUpdate(false)">关 闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
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

// 分类展示：优先使用 categoryNames（多选），否则回退 categoryName
const detailCategoryNames = computed(() => {
  if (detail.value?.categoryNames?.length) return detail.value.categoryNames
  return detail.value?.categoryName ? [detail.value.categoryName] : []
})

// 维修工展示：姓名（手机号），未指派时提示
const workerText = computed(() => {
  const name = detail.value?.workerName
  if (!name) return '待指派'
  const phone = detail.value?.workerPhone
  return phone ? `${name}（${phone}）` : name
})

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

// 时间格式化：T 换空格并去掉秒
function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

/**
 * 预估费用展示：null 表示「其他」分类面议
 * @param {number|null} value 预估费用
 * @returns {string} 展示文本
 */
function formatEstimatedPrice(value) {
  if (value == null) return '面议'
  return `¥${Number(value).toFixed(2).replace(/\.?0+$/, '') || '0'}`
}
</script>

<style scoped>
.dlg-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.dlg-label {
  margin: 0;
  font-size: 11px;
  color: #a9b4c6;
}

.dlg-no {
  margin: 4px 0 0;
  font-family: 'SF Mono', Consolas, monospace;
  font-size: 16px;
  font-weight: 700;
  color: #12294a;
}

.dlg-head-tags {
  display: flex;
  align-items: center;
  gap: 8px;
}

.urgent-tag {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  color: #b03636;
  background: #fdeeee;
}

.status-pill {
  padding: 3px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.st-pending {
  color: #a76a05;
  background: #fff5e3;
}

.st-in_progress {
  color: #1b56d0;
  background: #eef3ff;
}

.st-finished {
  color: #0a7f5e;
  background: #e7f8f2;
}

.st-rejected {
  color: #b03636;
  background: #fdeeee;
}

/* 图片区 */
.gallery-block {
  margin-bottom: 18px;
}

.block-title {
  margin: 0 0 10px;
  font-size: 12px;
  font-weight: 600;
  color: #7a879b;
}

.gallery {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.order-image {
  width: 104px;
  height: 104px;
  border-radius: 12px;
  cursor: pointer;
  border: 1px solid #eef2f9;
}

.gallery-empty {
  padding: 18px;
  border-radius: 12px;
  background: #f7f9fd;
  font-size: 12px;
  color: #a9b4c6;
  text-align: center;
}

/* 信息网格 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1px;
  background: #f0f4fb;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 18px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 12px 14px;
  background: #fff;
}

.info-label {
  font-size: 11px;
  color: #a9b4c6;
}

.info-value {
  font-size: 13px;
  color: #1b2b45;
  word-break: break-all;
}

.info-value.is-empty {
  color: #a9b4c6;
}

/* 预估费用：橙色强调，面议时恢复弱化色 */
.info-value.est-price {
  font-weight: 700;
  color: #ff6b00;
}

.info-value.est-price.is-empty {
  font-weight: 400;
  color: #a9b4c6;
}

/* 多选分类小标签 */
.cat-tag {
  margin-right: 6px;
  margin-bottom: 4px;
}

/* 文本区 */
.text-block {
  margin-bottom: 16px;
}

.desc-text {
  margin: 0;
  padding: 12px 14px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.8;
  color: #465468;
  background: #f8fafd;
}

.desc-text.is-empty {
  color: #a9b4c6;
}

/* 用户评价 */
.rating-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 2px 8px;
}

.rating-score {
  font-size: 13px;
  font-weight: 700;
  color: #a76a05;
}

.rating-time {
  margin-left: auto;
  font-size: 11px;
  color: #a9b4c6;
}

/* 时间线 */
.timeline {
  display: flex;
  align-items: flex-start;
  padding: 16px 14px;
  border-radius: 12px;
  background: #fbfcff;
  border: 1px solid #f0f4fb;
}

.tl-item {
  flex: 1;
  display: flex;
  gap: 8px;
  min-width: 0;
}

.tl-dot {
  width: 10px;
  height: 10px;
  flex-shrink: 0;
  margin-top: 4px;
  border-radius: 50%;
  background: #dbe4f2;
}

.tl-item.done .tl-dot {
  background: linear-gradient(135deg, #2c6bf2, #17bfd4);
  box-shadow: 0 0 0 4px rgba(44, 107, 242, 0.12);
}

.tl-item strong {
  display: block;
  font-size: 12px;
  color: #1b2b45;
}

.tl-item em {
  display: block;
  margin-top: 3px;
  font-size: 11px;
  font-style: normal;
  color: #a9b4c6;
}

.tl-line {
  width: 24px;
  height: 1px;
  flex-shrink: 0;
  margin-top: 9px;
  background: #e3eaf5;
}

.tl-line.done {
  background: #bcd0fb;
}

@media (max-width: 760px) {
  .info-grid {
    grid-template-columns: 1fr;
  }

  .timeline {
    flex-direction: column;
    gap: 12px;
  }

  .tl-line {
    display: none;
  }
}
</style>
