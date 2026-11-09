<template>
  <div v-loading="loading">
    <!-- 统计卡片：总数 + 各状态数量 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col v-for="card in cards" :key="card.label" :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区：分类饼图 + 月度柱状图 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card>
          <template #header>按故障分类统计</template>
          <div ref="pieRef" class="chart" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>按月统计</template>
          <div ref="barRef" class="chart" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getMyStats } from '../../api/order'

const loading = ref(false)
const stats = ref(null)

// 图表 DOM 引用与实例
const pieRef = ref()
const barRef = ref()
let pieChart = null
let barChart = null

// 统计卡片配置（颜色与状态字典保持一致）
const cards = computed(() => [
  { label: '全部工单', value: stats.value?.total ?? 0, color: '#303133' },
  { label: '待接单', value: stats.value?.pending ?? 0, color: '#e6a23c' },
  { label: '维修中', value: stats.value?.inProgress ?? 0, color: '#409eff' },
  { label: '已完成', value: stats.value?.finished ?? 0, color: '#67c23a' },
  { label: '已驳回', value: stats.value?.rejected ?? 0, color: '#f56c6c' }
])

// 加载我的报修统计（仅会员可用接口）
async function loadStats() {
  loading.value = true
  try {
    const res = await getMyStats()
    stats.value = res.data
    // 等待卡片渲染完成后再初始化图表
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

// 渲染 ECharts 图表
function renderCharts() {
  // 饼图：按故障分类
  pieChart = pieChart || echarts.init(pieRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 单（{d}%）' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['40%', '65%'],
        label: { formatter: '{b}\n{c} 单' },
        data: (stats.value?.byCategory || []).map((item) => ({
          name: item.categoryName,
          value: item.count
        }))
      }
    ]
  })

  // 柱状图：按月统计
  barChart = barChart || echarts.init(barRef.value)
  barChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: {
      type: 'category',
      data: (stats.value?.byMonth || []).map((item) => item.month)
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        type: 'bar',
        barWidth: '45%',
        itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
        data: (stats.value?.byMonth || []).map((item) => item.count)
      }
    ]
  })
}

// 窗口尺寸变化时图表自适应
function handleResize() {
  pieChart?.resize()
  barChart?.resize()
}

onMounted(() => {
  loadStats()
  window.addEventListener('resize', handleResize)
})

// 组件销毁时释放图表实例，避免内存泄漏
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose()
  barChart?.dispose()
})
</script>

<style scoped>
.stat-cards {
  margin-bottom: 16px;
}

.stat-card {
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
}

.stat-label {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}

.chart {
  height: 320px;
}
</style>
