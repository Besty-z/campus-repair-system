<template>
  <div class="page worker-profile">
    <!-- 维修数据磁贴 -->
    <div class="stat-grid">
      <div class="stat-tile is-mint">
        <div class="stat-tile-label">完成工单数</div>
        <div class="stat-tile-value">{{ profile.completedCount ?? 0 }}</div>
        <div class="stat-tile-icon"><el-icon><CircleCheck /></el-icon></div>
      </div>
      <div class="stat-tile is-amber">
        <div class="stat-tile-label">平均评分</div>
        <div class="stat-tile-value">{{ ratingText }}</div>
        <div class="stat-tile-icon"><el-icon><Star /></el-icon></div>
      </div>
      <div class="stat-tile is-cyan">
        <div class="stat-tile-label">评价数</div>
        <div class="stat-tile-value">{{ profile.ratingCount ?? 0 }}</div>
        <div class="stat-tile-icon"><el-icon><ChatDotRound /></el-icon></div>
      </div>
    </div>

    <div class="profile-grid">
      <!-- 资料展示 -->
      <section v-loading="loading" class="panel profile-card">
        <div class="panel-hd">
          <div class="panel-title">我的资料</div>
          <span class="status-pill" :class="profile.status === 1 ? 'st-finished' : 'st-rejected'">
            {{ profile.status === 1 ? '在职' : '停用' }}
          </span>
        </div>

        <div class="profile-head">
          <!-- 圆形头像：加载失败时回退为渐变文字头像 -->
          <img
            v-if="!avatarFailed"
            class="profile-avatar-img"
            :src="WORKER_AVATAR"
            :alt="`${profile.name || '维修工'}头像`"
            loading="lazy"
            @error="avatarFailed = true"
          />
          <span v-else class="profile-avatar">{{ avatarText }}</span>
          <div class="profile-name">
            <strong>{{ profile.name || '-' }}</strong>
            <span>{{ profile.phone || '-' }}</span>
          </div>
        </div>

        <div class="info-list">
          <div class="info-row">
            <span class="info-label">个人简介</span>
            <span class="info-value" :class="{ 'is-empty': !profile.intro }">
              {{ profile.intro || '暂无简介' }}
            </span>
          </div>
          <div class="info-row">
            <span class="info-label">擅长分类</span>
            <div class="cat-tags">
              <span v-for="name in categoryNames" :key="name" class="cat-tag">{{ name }}</span>
              <span v-if="categoryNames.length === 0" class="muted">未设置擅长分类</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 编辑资料 -->
      <section class="panel">
        <div class="panel-hd">
          <div class="panel-title">编辑资料</div>
        </div>

        <el-form label-position="top" class="profile-form">
          <el-form-item label="个人简介">
            <el-input
              v-model="form.intro"
              type="textarea"
              :rows="4"
              maxlength="200"
              show-word-limit
              placeholder="简单介绍你的维修经验与擅长方向"
            />
          </el-form-item>
          <el-form-item label="擅长分类">
            <el-select
              v-model="form.categoryIds"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择擅长处理的故障分类"
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
          <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
        </el-form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, Star, ChatDotRound } from '@element-plus/icons-vue'
import { getMyWorkerProfile, updateMyWorkerProfile } from '../../api/worker'
import { listCategories } from '../../api/category'
import { WORKER_AVATAR } from '../../utils/images'

const loading = ref(false)
const saving = ref(false)
const profile = ref({})
const categoryOptions = ref([])
// 头像图片是否加载失败，失败后回退为渐变文字头像
const avatarFailed = ref(false)

// 编辑表单：简介 + 擅长分类（分类 ID 数组）
const form = reactive({
  intro: '',
  categoryIds: []
})

// 头像文字：姓名首字
const avatarText = computed(() => (profile.value.name || '维').slice(0, 1))

// 平均评分展示：null 表示暂无评价
const ratingText = computed(() => {
  const rating = profile.value.avgRating
  return rating == null ? '暂无' : Number(rating).toFixed(1)
})

// 擅长分类名称（后端已返回 categoryNames）
const categoryNames = computed(() => profile.value.categoryNames || [])

// 加载本人资料，并同步到编辑表单
async function loadProfile() {
  loading.value = true
  try {
    const res = await getMyWorkerProfile()
    profile.value = res.data || {}
    form.intro = profile.value.intro || ''
    form.categoryIds = Array.isArray(profile.value.categoryIds)
      ? [...profile.value.categoryIds]
      : []
  } finally {
    loading.value = false
  }
}

// 保存资料
async function handleSave() {
  saving.value = true
  try {
    await updateMyWorkerProfile({
      intro: form.intro,
      categoryIds: form.categoryIds
    })
    ElMessage.success('资料已保存')
    await loadProfile()
  } finally {
    saving.value = false
  }
}

// 初始化：加载资料与分类选项
onMounted(async () => {
  await loadProfile()
  const res = await listCategories()
  categoryOptions.value = res.data || []
})
</script>

<style scoped>
.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
}

/* 资料展示 */
.profile-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 4px 0 16px;
  border-bottom: 1px dashed #eef2f9;
}

.profile-avatar {
  width: 52px;
  height: 52px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #3fd0a2, #0fb583);
  box-shadow: 0 8px 18px rgba(15, 181, 131, 0.28);
}

/* 维修工圆形头像：与渐变文字头像同尺寸，柔和阴影 */
.profile-avatar-img {
  width: 56px;
  height: 56px;
  flex-shrink: 0;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 8px 18px rgba(15, 181, 131, 0.28);
}

.profile-name strong {
  display: block;
  font-size: 16px;
  font-weight: 700;
  color: #12294a;
}

.profile-name span {
  display: block;
  margin-top: 3px;
  font-size: 12px;
  color: #a9b4c6;
  letter-spacing: 0.3px;
}

.info-list {
  padding-top: 14px;
}

.info-row {
  display: flex;
  gap: 14px;
  padding: 10px 0;
}

.info-row + .info-row {
  border-top: 1px dashed #f0f4fb;
}

.info-label {
  width: 72px;
  flex-shrink: 0;
  font-size: 12px;
  color: #a9b4c6;
}

.info-value {
  font-size: 13px;
  line-height: 1.7;
  color: #465468;
  word-break: break-all;
}

.info-value.is-empty {
  color: #a9b4c6;
}

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

@media (max-width: 1080px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
