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
      <el-col :span="12">
        <div class="card">
          <div class="card-header">
            <h3 class="text-base font-semibold text-gray-800">近 7 天订单趋势</h3>
          </div>
          <v-chart class="chart" :option="trendOption" autoresize />
        </div>
      </el-col>
      <el-col :span="6">
        <div class="card">
          <div class="card-header">
            <h3 class="text-base font-semibold text-gray-800">订单状态分布</h3>
          </div>
          <v-chart class="chart" :option="pieOption" autoresize />
        </div>
      </el-col>
      <el-col :span="6">
        <div class="card alert-card">
          <div class="card-header">
            <h3 class="text-base font-semibold text-gray-800">库存预警</h3>
            <el-tabs v-model="alertTab" class="alert-tabs">
              <el-tab-pane label="缺货" name="outOfStock" />
              <el-tab-pane label="低库存" name="lowStock" />
            </el-tabs>
          </div>
          <el-table :data="currentAlertList" size="small" height="100%" stripe>
            <el-table-column prop="skuName" label="商品" show-overflow-tooltip />
            <el-table-column label="仓库" width="70">
              <template #default="{ row }">
                {{ row.warehouseName || '所有仓库' }}
              </template>
            </el-table-column>
            <el-table-column label="库存" width="55" align="center">
              <template #default="{ row }">
                <span :class="row.alertStatus === 'OUT_OF_STOCK' ? 'text-red-500 font-bold' : 'text-yellow-500 font-bold'">
                  {{ row.quantity }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="阈值" width="55" align="center">
              <template #default="{ row }">
                {{ row.alertStatus === 'OUT_OF_STOCK' ? row.outOfStockThreshold : row.lowStockThreshold }}
              </template>
            </el-table-column>
          </el-table>
          <div v-if="currentAlertList.length === 0" class="empty-text">暂无预警</div>
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
import { getLowStockList, getOutOfStockList } from '@/api/stockAlert'
import type { StockAlertStatus } from '@/api/stockAlert'
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

const trendOption = computed(() => {
  const dates = dashboard.value.orderTrend.map((i) => i.date.slice(5))
  const platformTrends = dashboard.value.platformTrends || []

  // 如果有平台趋势数据，按平台显示曲线
  if (platformTrends.length > 0) {
    return {
      tooltip: {
        trigger: 'axis',
        formatter: (params: any) => {
          let result = `<div style="font-weight:bold;margin-bottom:5px">${params[0].axisValue}</div>`
          params.forEach((param: any) => {
            result += `<div style="display:flex;align-items:center;gap:5px">
              <span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${param.color}"></span>
              <span>${param.seriesName}: ${param.value}</span>
            </div>`
          })
          return result
        },
      },
      legend: {
        data: platformTrends.map((t) => t.platformName),
        bottom: 0,
        textStyle: { color: '#6b7280', fontSize: 12 },
      },
      grid: { left: 40, right: 20, top: 20, bottom: 40 },
      xAxis: {
        type: 'category',
        data: dates,
        axisLine: { lineStyle: { color: '#e5e7eb' } },
        axisLabel: { color: '#6b7280' },
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        splitLine: { lineStyle: { color: '#f3f4f6' } },
        axisLabel: { color: '#6b7280' },
      },
      series: platformTrends.map((trend) => ({
        name: trend.platformName,
        type: 'line',
        data: trend.data.map((i) => i.count),
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2, color: trend.platformColor },
        itemStyle: { color: trend.platformColor },
      })),
    }
  }

  // 兼容旧版：显示总计曲线
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      data: dates,
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
  }
})

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

const alertTab = ref<'outOfStock' | 'lowStock'>('outOfStock')
const outOfStockList = ref<StockAlertStatus[]>([])
const lowStockList = ref<StockAlertStatus[]>([])
const currentAlertList = computed(() =>
  alertTab.value === 'outOfStock' ? outOfStockList.value : lowStockList.value,
)

onMounted(async () => {
  try {
    const [dashRes, oosRes, lsRes] = await Promise.all([
      getDashboard(),
      getOutOfStockList(),
      getLowStockList(),
    ])
    dashboard.value = dashRes.data
    outOfStockList.value = oosRes.data || []
    lowStockList.value = lsRes.data || []
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
.alert-card {
  height: 362px;
  display: flex;
  flex-direction: column;
  :deep(.el-table) {
    flex: 1;
  }
}
.alert-tabs {
  :deep(.el-tabs__header) {
    margin: 0;
  }
  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }
  :deep(.el-tabs__item) {
    height: 28px;
    line-height: 28px;
    font-size: 12px;
    padding: 0 8px;
  }
  :deep(.el-tabs__active-bar) {
    height: 2px;
  }
}
.empty-text {
  text-align: center;
  color: #9ca3af;
  padding: 40px 0;
  font-size: 13px;
}
</style>
