<template>
  <div class="page-container">
    <PageHeader title="仪表盘" subtitle="实时业务概览" />

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="mb-6">
      <el-col :span="6">
        <StatCard label="今日订单" :value="dashboard.todayOrderCount" icon="Document" color="#3b82f6" />
      </el-col>
      <el-col :span="6">
        <StatCard label="待出库" :value="dashboard.pendingOutboundCount" icon="TopRight" color="#f59e0b" />
      </el-col>
      <el-col :span="6">
        <StatCard label="今日出库" :value="dashboard.todayOutboundCount" icon="Finished" color="#10b981" />
      </el-col>
      <el-col :span="6">
        <StatCard label="今日退货" :value="dashboard.todayReturnCount" icon="BottomLeft" color="#ef4444" />
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="20">
      <el-col :span="16">
        <div class="card">
          <div class="card-header">
            <h3 class="text-base font-semibold text-gray-800">近 7 天订单趋势</h3>
          </div>
          <v-chart class="chart" :option="trendOption" autoresize />
        </div>
      </el-col>
      <el-col :span="8">
        <div class="card">
          <div class="card-header">
            <h3 class="text-base font-semibold text-gray-800">订单状态分布</h3>
          </div>
          <v-chart class="chart" :option="pieOption" autoresize />
        </div>
      </el-col>
    </el-row>

    <!-- 本月出货TOP10 -->
    <el-row :gutter="20" class="mt-4">
      <el-col :span="24">
        <div class="card">
          <div class="card-header">
            <h3 class="text-base font-semibold text-gray-800">本月出货量 TOP 10 SKU</h3>
          </div>
          <v-chart class="bar-chart" :option="barOption" autoresize />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { getDashboard } from '@/api/report'
import type { DashboardData } from '@/api/report'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'

use([CanvasRenderer, LineChart, PieChart, BarChart, GridComponent, TooltipComponent, LegendComponent])

const dashboard = ref<DashboardData>({
  todayOrderCount: 0,
  pendingOutboundCount: 0,
  todayOutboundCount: 0,
  todayReturnCount: 0,
  stockAlertCount: 0,
  orderTrend: [],
  orderStatusDistribution: [],
  topSkus: [],
})

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 20, bottom: 30 },
  xAxis: {
    type: 'category',
    data: dashboard.value.orderTrend.map((i) => i.date.slice(5)),
    axisLine: { lineStyle: { color: '#e5e7eb' } },
    axisLabel: { color: '#6b7280' },
  },
  yAxis: {
    type: 'value',
    axisLine: { show: false },
    splitLine: { lineStyle: { color: '#f3f4f6' } },
    axisLabel: { color: '#6b7280' },
  },
  series: [
    {
      type: 'line',
      data: dashboard.value.orderTrend.map((i) => i.count),
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 3, color: '#3b82f6' },
      itemStyle: { color: '#3b82f6' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(59,130,246,0.2)' },
            { offset: 1, color: 'rgba(59,130,246,0)' },
          ],
        },
      },
    },
  ],
}))

const pieOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0, textStyle: { color: '#6b7280', fontSize: 12 } },
  series: [
    {
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      data: dashboard.value.orderStatusDistribution.map((i) => ({
        name: i.statusName,
        value: i.count,
      })),
      color: ['#94a3b8', '#f59e0b', '#3b82f6', '#10b981', '#06b6d4', '#ef4444'],
    },
  ],
}))

const barOption = computed(() => {
  const skus = [...(dashboard.value.topSkus || [])].reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 140, right: 50, top: 10, bottom: 20 },
    xAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f3f4f6' } },
      axisLabel: { color: '#6b7280' },
    },
    yAxis: {
      type: 'category',
      data: skus.map((i) => i.skuName),
      axisLabel: { color: '#374151', fontSize: 12 },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
    },
    series: [
      {
        type: 'bar',
        data: skus.map((i) => i.totalQuantity),
        barWidth: '55%',
        itemStyle: {
          borderRadius: [0, 4, 4, 0],
          color: (params: any) => {
            const reversedIndex = skus.length - 1 - params.dataIndex
            const colors = ['#ef4444', '#f59e0b', '#f59e0b', '#3b82f6', '#3b82f6', '#3b82f6', '#10b981', '#10b981', '#10b981', '#10b981']
            return colors[reversedIndex] || '#3b82f6'
          },
        },
        label: {
          show: true,
          position: 'right',
          fontSize: 12,
          fontWeight: 'bold',
          color: '#374151',
        },
      },
    ],
  }
})

onMounted(async () => {
  try {
    const res = await getDashboard()
    dashboard.value = res.data
  } catch {
    // 使用默认空数据
  }
})
</script>

<style scoped lang="scss">
.chart {
  height: 320px;
  width: 100%;
}
.bar-chart {
  height: 300px;
  width: 100%;
}
</style>
