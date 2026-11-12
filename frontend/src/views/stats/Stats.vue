<template>
  <div v-loading="loading" class="page stats-page">
    <!-- 概览磁贴：总数 + 各状态数量 -->
    <div class="stat-grid">
      <div
        v-for="card in cards"
        :key="card.label"
        class="stat-tile"
        :class="card.tone"
      >
        <div class="stat-tile-label">{{ card.label }}</div>
        <div class="stat-tile-value">{{ card.value }}</div>
        <div class="stat-tile-icon">
          <el-icon><component :is="card.icon" /></el-icon>
        </div>
      </div>
    </div>

    <!-- 图表区：分类饼图 + 月度柱状图 -->
    <div class="chart-grid">
      <section class="panel">
        <div class="panel-hd">
          <div class="panel-title">按故障分类统计</div>
          <span class="panel-note">数据来源：我的历史报修</span>
        </div>
        <div ref="pieRef" class="chart" />
      </section>

      <section class="panel">
        <div class="panel-hd">
          <div class="panel-title">按月报修趋势</div>
          <span class="panel-note">近几个月提交量</span>
        </div>
        <div ref="barRef" class="chart" />
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Tickets, Timer, Tools, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { getMyStats } from '../../api/order'

const loading = ref(false)
const stats = ref(null)

// 图表 DOM 引用与实例
const pieRef = ref()
const barRef = ref()
let pieChart = null
let barChart = null

// 统计磁贴配置（与状态字典语义一致）
const cards = computed(() => [
  { label: '全部工单', value: stats.value?.total ?? 0, tone: 'is-brand', icon: Tickets },
  { label: '待接单', value: stats.value?.pending ?? 0, tone: 'is-amber', icon: Timer },
  { label: '维修中', value: stats.value?.inProgress ?? 0, tone: 'is-cyan', icon: Tools },
  { label: '已完成', value: stats.value?.finished ?? 0, tone: 'is-mint', icon: CircleCheck },
  { label: '已驳回', value: stats.value?.rejected ?? 0, tone: 'is-danger', icon: CircleClose }
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

// 渲染 ECharts 图表（配色与整体设计系统保持一致）
function renderCharts() {
  // 饼图：按故障分类，环形图 + 中心留白
  pieChart = pieChart || echarts.init(pieRef.value)
  pieChart.setOption({
    color: ['#2c6bf2', '#17bfd4', '#f5a524', '#0fb583', '#8b7bf7', '#ef4b4b', '#6a97f6'],
    tooltip: { trigger: 'item', formatter: '{b}: {c} 单（{d}%）' },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { color: '#7a879b', fontSize: 11 } },
    series: [
      {
        type: 'pie',
        radius: ['48%', '68%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 6 },
        label: { formatter: '{b}\n{c} 单', color: '#5b6b80', fontSize: 11, lineHeight: 16 },
        labelLine: { length: 10, length2: 8 },
        data: (stats.value?.byCategory || []).map((item) => ({
          name: item.categoryName,
          value: item.count
        }))
      }
    ]
  })

  // 柱状图：按月统计，圆角柱 + 渐变
  barChart = barChart || echarts.init(barRef.value)
  barChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: {
      type: 'category',
      data: (stats.value?.byMonth || []).map((item) => item.month),
      axisLine: { lineStyle: { color: '#e3eaf5' } },
      axisLabel: { color: '#7a879b', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#f0f4fb' } },
      axisLabel: { color: '#a9b4c6', fontSize: 11 }
    },
    series: [
      {
        type: 'bar',
        barWidth: '45%',
        itemStyle: {
          borderRadius: [6, 6, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#2c6bf2' },
            { offset: 1, color: '#17bfd4' }
          ])
        },
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
.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

/* 固定图表高度，保证两栏视觉对齐 */
.chart {
  height: 320px;
}

.panel-note {
  font-size: 11px;
  color: #a9b4c6;
}

@media (max-width: 1000px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
