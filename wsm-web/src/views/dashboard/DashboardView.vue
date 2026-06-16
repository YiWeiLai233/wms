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
        <div class="card card-border">
          <div class="card-header">
            <div class="flex items-center gap-2">
              <h3 class="text-base font-semibold text-gray-800">缺货预警</h3>
              <el-badge :value="outOfStockList.length" :max="99" type="danger" />
            </div>
          </div>
          <v-chart class="alert-chart" :option="outOfStockChartOption" autoresize />
        </div>
      </el-col>
      <el-col :span="12">
        <div class="card card-border">
          <div class="card-header">
            <div class="flex items-center gap-2">
              <h3 class="text-base font-semibold text-gray-800">低库存预警</h3>
              <el-badge :value="lowStockList.length" :max="99" type="warning" />
            </div>
          </div>
          <v-chart class="alert-chart" :option="lowStockChartOption" autoresize />
        </div>
      </el-col>
    </el-row>

    <!-- 第二行：订单趋势 + 本月出货TOP10 -->
    <el-row :gutter="20" class="mt-4">
      <el-col :span="12">
        <div class="card card-border">
          <div class="card-header">
            <h3 class="text-base font-semibold text-gray-800">近 7 天订单趋势</h3>
          </div>
          <v-chart class="chart" :option="trendOption" autoresize />
        </div>
      </el-col>
      <el-col :span="12">
        <div class="card card-border">
          <div class="card-header">
            <h3 class="text-base font-semibold text-gray-800">本月出货量 TOP 10 SKU</h3>
          </div>
          <v-chart class="bar-chart" :option="barOption" autoresize />
        </div>
      </el-col>
    </el-row>

    <!-- 近7天各平台SKU销量趋势 -->
    <el-row :gutter="20" class="mt-4">
      <el-col :span="24">
        <div class="card">
          <div class="card-header">
            <h3 class="text-base font-semibold text-gray-800">近 7 天各平台 SKU 销量趋势</h3>
          </div>
          <v-chart class="combo-chart" :option="platformSkuComboOption" autoresize />
        </div>
      </el-col>
    </el-row>

    <!-- 近7天各平台SKU销量热力图 -->
    <el-row :gutter="20" class="mt-4">
      <el-col :span="12">
        <div class="card card-border">
          <div class="card-header">
            <h3 class="text-base font-semibold text-gray-800">近 7 天各平台 SKU 销量热力图</h3>
          </div>
          <v-chart class="heatmap-chart" :option="platformSkuHeatmapOption" autoresize />
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
import { LineChart, PieChart, BarChart, HeatmapChart, RadarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, VisualMapComponent, RadarComponent } from 'echarts/components'
import { WarningFilled, Warning, CircleCheck } from '@element-plus/icons-vue'
import { getDashboard } from '@/api/report'
import type { DashboardData } from '@/api/report'
import { getLowStockList, getOutOfStockList } from '@/api/stockAlert'
import type { StockAlertStatus } from '@/api/stockAlert'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'

use([CanvasRenderer, LineChart, PieChart, BarChart, HeatmapChart, RadarChart, GridComponent, TooltipComponent, LegendComponent, VisualMapComponent, RadarComponent])

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
        type: 'scroll',
        pageTextStyle: { color: '#6b7280' },
        pageIconColor: '#6b7280',
        pageIconInactiveColor: '#d1d5db',
      },
      grid: { left: 40, right: 20, top: 20, bottom: 60 },
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
        smooth: 0.5,
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

const barOption = computed(() => {
  const skus = [...(dashboard.value.topSkus || [])].reverse()

  // 收集所有平台信息
  const platformMap = new Map<number, { name: string; color: string }>()
  skus.forEach((sku) => {
    if (sku.platformQuantities) {
      sku.platformQuantities.forEach((pq) => {
        if (!platformMap.has(pq.platformId)) {
          platformMap.set(pq.platformId, { name: pq.platformName, color: pq.platformColor })
        }
      })
    }
  })
  const platforms = Array.from(platformMap.entries()).sort((a, b) => a[0] - b[0])

  // 如果没有平台数据，使用旧的单柱子模式
  if (platforms.length === 0) {
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
            color: '#3b82f6',
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
  }

  // 按平台分组显示柱子
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        let result = `<div style="font-weight:bold;margin-bottom:5px">${params[0].axisValue}</div>`
        let total = 0
        params.forEach((param: any) => {
          if (param.value > 0) {
            result += `<div style="display:flex;align-items:center;gap:5px">
              <span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${param.color}"></span>
              <span>${param.seriesName}: ${param.value}</span>
            </div>`
            total += param.value
          }
        })
        result += `<div style="margin-top:5px;font-weight:bold">总计: ${total}</div>`
        return result
      },
    },
    legend: {
      data: platforms.map(([, p]) => p.name),
      bottom: 0,
      textStyle: { color: '#6b7280', fontSize: 12 },
      type: 'scroll',
      pageTextStyle: { color: '#6b7280' },
      pageIconColor: '#6b7280',
      pageIconInactiveColor: '#d1d5db',
    },
    grid: { left: 140, right: 50, top: 10, bottom: 60 },
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
    series: platforms.map(([platformId, platform]) => ({
      name: platform.name,
      type: 'bar',
      stack: 'total',
      data: skus.map((sku) => {
        const pq = sku.platformQuantities?.find((p) => p.platformId === platformId)
        return pq?.quantity || 0
      }),
      barWidth: '55%',
      itemStyle: {
        color: platform.color,
        borderRadius: platformId === platforms[platforms.length - 1][0] ? [0, 4, 4, 0] : 0,
      },
    })),
  }
})

const platformSkuComboOption = computed(() => {
  const platformSales = dashboard.value.platformSkuSales || []
  if (platformSales.length === 0) {
    return {
      graphic: {
        type: 'text',
        left: 'center',
        top: 'center',
        style: { text: '暂无销量数据', fontSize: 14, fill: '#999' }
      }
    }
  }

  // 收集所有日期
  const allDates = new Set<string>()
  platformSales.forEach((ps) => {
    ps.skuSales?.forEach((s) => allDates.add(s.date))
  })
  const dates = Array.from(allDates).sort()

  // 平台颜色配置
  const defaultColors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899']
  const platformColors: Record<string, string> = {}
  platformSales.forEach((ps, idx) => {
    platformColors[ps.platformName] = ps.platformColor || defaultColors[idx % defaultColors.length]
  })

  // 构建系列：每个平台一个柱子 + 一条曲线
  const series: any[] = []
  const legendData: string[] = []

  platformSales.forEach((ps) => {
    const color = platformColors[ps.platformName]

    // 计算该平台每天的总销量
    const dailyTotals = dates.map((date) => {
      const daySales = ps.skuSales?.filter((s) => s.date === date) || []
      return daySales.reduce((sum, s) => sum + s.quantity, 0)
    })

    // 柱状图系列
    legendData.push(ps.platformName)
    series.push({
      name: ps.platformName,
      type: 'bar',
      data: dailyTotals,
      barWidth: '45%',
      barGap: '10%',
      itemStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: color },
            { offset: 1, color: color + '40' },
          ],
        },
        borderRadius: [6, 6, 0, 0],
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowColor: 'rgba(0,0,0,0.2)',
        },
      },
    })

    // 曲线系列（趋势线）
    series.push({
      name: ps.platformName + '趋势',
      type: 'line',
      data: dailyTotals,
      smooth: 0.4,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: {
        width: 3,
        color: color,
        type: 'solid',
      },
      itemStyle: {
        color: color,
        borderColor: '#fff',
        borderWidth: 2,
      },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: color + '30' },
            { offset: 1, color: color + '05' },
          ],
        },
      },
    })
  })

  // 添加总计曲线
  const totalData = dates.map((date) => {
    let total = 0
    platformSales.forEach((ps) => {
      const daySales = ps.skuSales?.filter((s) => s.date === date) || []
      daySales.forEach((s) => total += s.quantity)
    })
    return total
  })

  legendData.push('总计')
  series.push({
    name: '总计',
    type: 'line',
    data: totalData,
    smooth: 0.5,
    symbol: 'diamond',
    symbolSize: 10,
    lineStyle: {
      width: 3,
      color: '#f97316',
      type: 'dashed',
    },
    itemStyle: {
      color: '#f97316',
      borderColor: '#fff',
      borderWidth: 3,
    },
    label: {
      show: true,
      position: 'top',
      fontSize: 13,
      fontWeight: 'bold',
      color: '#f97316',
      formatter: '{c}',
    },
  })

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        crossStyle: { color: '#999' },
      },
      formatter: (params: any) => {
        let result = `<div style="font-weight:bold;margin-bottom:8px;font-size:13px">${params[0]?.axisValue || ''}</div>`
        let grandTotal = 0

        // 按平台分组显示
        const platformMap = new Map<string, { bar: number; line: number; color: string }>()
        params.forEach((p: any) => {
          const name = p.seriesName as string
          const isTrend = name.endsWith('趋势')
          const platformName = isTrend ? name.replace('趋势', '') : name

          if (!platformMap.has(platformName)) {
            platformMap.set(platformName, { bar: 0, line: 0, color: p.color })
          }
          const entry = platformMap.get(platformName)!
          if (p.seriesType === 'bar') {
            entry.bar = p.value
          } else if (isTrend) {
            entry.line = p.value
          }
        })

        platformMap.forEach((entry, name) => {
          if (name === '总计') return
          if (entry.bar > 0 || entry.line > 0) {
            grandTotal += entry.bar
            result += `<div style="display:flex;align-items:center;gap:8px;margin-bottom:4px">
              <span style="display:inline-block;width:12px;height:12px;border-radius:3px;background:${entry.color}"></span>
              <span style="font-weight:500">${name}</span>
              <span style="margin-left:auto;font-weight:bold">${entry.bar}</span>
            </div>`
          }
        })

        // 总计
        const totalEntry = params.find((p: any) => p.seriesName === '总计')
        if (totalEntry && totalEntry.value > 0) {
          result += `<div style="margin-top:8px;padding-top:8px;border-top:1px solid #e5e7eb;display:flex;justify-content:space-between;font-weight:bold;color:#f97316">
            <span>总计</span><span>${totalEntry.value}</span>
          </div>`
        }

        return result
      },
    },
    legend: {
      data: legendData,
      bottom: 0,
      textStyle: { color: '#6b7280', fontSize: 11 },
      type: 'scroll',
      pageTextStyle: { color: '#6b7280' },
      pageIconColor: '#6b7280',
      pageIconInactiveColor: '#d1d5db',
      itemGap: 20,
    },
    grid: {
      left: 60,
      right: 40,
      top: 40,
      bottom: 70,
    },
    xAxis: {
      type: 'category',
      data: dates.map((d) => d.slice(5)),
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisLabel: { color: '#6b7280', fontSize: 12 },
    },
    yAxis: {
      type: 'value',
      name: '销量',
      axisLine: { show: false },
      axisLabel: { color: '#6b7280' },
      splitLine: { lineStyle: { color: '#f3f4f6' } },
    },
    series,
  }
})

const platformSkuHeatmapOption = computed(() => {
  const platformSales = dashboard.value.platformSkuSales || []
  if (platformSales.length === 0) {
    return {
      graphic: {
        type: 'text',
        left: 'center',
        top: 'center',
        style: { text: '暂无销量数据', fontSize: 14, fill: '#999' }
      }
    }
  }

  // 收集所有SKU名称
  const allSkuNames = new Set<string>()
  platformSales.forEach((ps) => {
    ps.skuSales?.forEach((s) => allSkuNames.add(s.skuName))
  })
  const skuNames = Array.from(allSkuNames)

  // 收集所有平台名称
  const platformNames = platformSales.map((ps) => ps.platformName)

  // 构建热力图数据: [平台index, SKUindex, 总销量]
  const heatmapData: [number, number, number][] = []
  let maxVal = 0

  platformSales.forEach((ps, pIdx) => {
    // 统计该平台每个SKU的总销量
    const skuTotalMap = new Map<string, number>()
    ps.skuSales?.forEach((s) => {
      const current = skuTotalMap.get(s.skuName) || 0
      skuTotalMap.set(s.skuName, current + s.quantity)
    })

    skuNames.forEach((skuName, sIdx) => {
      const value = skuTotalMap.get(skuName) || 0
      if (value > 0) {
        heatmapData.push([pIdx, sIdx, value])
        maxVal = Math.max(maxVal, value)
      }
    })
  })

  return {
    tooltip: {
      position: 'top',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      borderRadius: 8,
      padding: [12, 16],
      textStyle: { color: '#374151', fontSize: 13 },
      formatter: (params: any) => {
        const platform = platformNames[params.data[0]] || ''
        const sku = skuNames[params.data[1]] || ''
        const value = params.data[2]
        return `<div style="font-weight:600;margin-bottom:6px;color:#1f2937">${platform}</div>
                <div style="display:flex;align-items:center;gap:8px">
                  <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:#3b82f6"></span>
                  <span>${sku}</span>
                  <span style="font-weight:700;color:#3b82f6;margin-left:auto">${value} 件</span>
                </div>`
      },
    },
    grid: {
      left: 15,
      right: 15,
      top: 15,
      bottom: 60,
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: platformNames,
      position: 'top',
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: '#6b7280',
        fontSize: 12,
        fontWeight: 600,
        padding: [0, 0, 8, 0],
      },
      splitLine: { show: false },
    },
    yAxis: {
      type: 'category',
      data: skuNames,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: '#6b7280',
        fontSize: 11,
        width: 100,
        overflow: 'truncate',
        padding: [0, 8, 0, 0],
      },
      splitLine: { show: false },
    },
    visualMap: {
      min: 0,
      max: maxVal || 10,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: 0,
      itemWidth: 14,
      itemHeight: 200,
      inRange: {
        color: ['#eff6ff', '#bfdbfe', '#60a5fa', '#2563eb', '#1e40af'],
      },
      textStyle: {
        color: '#6b7280',
        fontSize: 11,
      },
    },
    series: [
      {
        name: '销量',
        type: 'heatmap',
        data: heatmapData,
        itemStyle: {
          borderColor: '#fff',
          borderWidth: 2,
          borderRadius: 4,
        },
        label: {
          show: true,
          fontSize: 12,
          fontWeight: 600,
          color: '#374151',
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 12,
            shadowColor: 'rgba(0, 0, 0, 0.3)',
            borderColor: '#1e40af',
            borderWidth: 2,
          },
          label: {
            fontSize: 14,
            fontWeight: 700,
          },
        },
      },
    ],
  }
})

const outOfStockList = ref<StockAlertStatus[]>([])
const lowStockList = ref<StockAlertStatus[]>([])

// 缺货横向柱状图（进度条样式）
const outOfStockChartOption = computed(() => {
  const list = outOfStockList.value
  if (list.length === 0) {
    return {
      graphic: {
        type: 'text',
        left: 'center',
        top: 'center',
        style: { text: '暂无缺货预警', fontSize: 14, fill: '#999' }
      }
    }
  }
  const sortedList = [...list].sort((a, b) => {
    const ratioA = (a.quantity || 0) / (a.outOfStockThreshold || 1)
    const ratioB = (b.quantity || 0) / (b.outOfStockThreshold || 1)
    return ratioA - ratioB
  })
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e5e7eb',
      textStyle: { color: '#333', fontSize: 13 },
      formatter: (params: any) => {
        const item = sortedList[params[0]?.dataIndex]
        if (!item) return ''
        const ratio = Math.round(((item.quantity || 0) / (item.outOfStockThreshold || 1)) * 100)
        return `<div style="font-weight:600;margin-bottom:4px">${item.skuName}</div>
                <div>当前库存: <span style="color:#ef4444;font-weight:600">${item.quantity}</span></div>
                <div>缺货阈值: ${item.outOfStockThreshold || 0}</div>
                <div>库存占比: <span style="color:${ratio < 50 ? '#ef4444' : '#f59e0b'};font-weight:600">${ratio}%</span></div>
                <div>仓库: ${item.warehouseName || '所有仓库'}</div>`
      }
    },
    grid: { left: 10, right: 60, top: 15, bottom: 20, containLabel: true },
    xAxis: {
      type: 'value',
      max: (value: any) => value.max * 1.1,
      axisLabel: { show: false },
      axisLine: { show: false },
      splitLine: { show: false }
    },
    yAxis: {
      type: 'category',
      data: sortedList.map(i => i.skuName || ''),
      axisLabel: {
        color: '#374151',
        fontSize: 11,
        width: 100,
        overflow: 'truncate'
      },
      axisLine: { show: false },
      axisTick: { show: false }
    },
    series: [
      // 背景（阈值）
      {
        name: '阈值',
        type: 'bar',
        data: sortedList.map(i => i.outOfStockThreshold || 0),
        itemStyle: {
          borderRadius: [0, 6, 6, 0],
          color: '#fee2e2'
        },
        barWidth: '60%',
        barGap: '-100%',
        z: 1
      },
      // 前景（实际库存）
      {
        name: '库存',
        type: 'bar',
        data: sortedList.map(i => i.quantity || 0),
        itemStyle: {
          borderRadius: [0, 6, 6, 0],
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 1, y2: 0,
            colorStops: [
              { offset: 0, color: '#fca5a5' },
              { offset: 1, color: '#ef4444' }
            ]
          }
        },
        barWidth: '60%',
        z: 2,
        label: {
          show: true,
          position: 'right',
          formatter: (params: any) => {
            const item = sortedList[params.dataIndex]
            return `${params.value}/${item.outOfStockThreshold || 0}`
          },
          fontSize: 11,
          fontWeight: 500,
          color: '#6b7280'
        }
      }
    ]
  }
})

// 低库存横向柱状图（进度条样式）
const lowStockChartOption = computed(() => {
  const list = lowStockList.value
  if (list.length === 0) {
    return {
      graphic: {
        type: 'text',
        left: 'center',
        top: 'center',
        style: { text: '暂无低库存预警', fontSize: 14, fill: '#999' }
      }
    }
  }
  const sortedList = [...list].sort((a, b) => {
    const ratioA = (a.quantity || 0) / (a.lowStockThreshold || 1)
    const ratioB = (b.quantity || 0) / (b.lowStockThreshold || 1)
    return ratioA - ratioB
  })
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e5e7eb',
      textStyle: { color: '#333', fontSize: 13 },
      formatter: (params: any) => {
        const item = sortedList[params[0]?.dataIndex]
        if (!item) return ''
        const ratio = Math.round(((item.quantity || 0) / (item.lowStockThreshold || 1)) * 100)
        return `<div style="font-weight:600;margin-bottom:4px">${item.skuName}</div>
                <div>当前库存: <span style="color:#f59e0b;font-weight:600">${item.quantity}</span></div>
                <div>低库存阈值: ${item.lowStockThreshold || 0}</div>
                <div>库存占比: <span style="color:${ratio < 50 ? '#ef4444' : '#22c55e'};font-weight:600">${ratio}%</span></div>
                <div>仓库: ${item.warehouseName || '所有仓库'}</div>`
      }
    },
    grid: { left: 10, right: 60, top: 15, bottom: 20, containLabel: true },
    xAxis: {
      type: 'value',
      max: (value: any) => value.max * 1.1,
      axisLabel: { show: false },
      axisLine: { show: false },
      splitLine: { show: false }
    },
    yAxis: {
      type: 'category',
      data: sortedList.map(i => i.skuName || ''),
      axisLabel: {
        color: '#374151',
        fontSize: 11,
        width: 100,
        overflow: 'truncate'
      },
      axisLine: { show: false },
      axisTick: { show: false }
    },
    series: [
      // 背景（阈值）
      {
        name: '阈值',
        type: 'bar',
        data: sortedList.map(i => i.lowStockThreshold || 0),
        itemStyle: {
          borderRadius: [0, 6, 6, 0],
          color: '#fef3c7'
        },
        barWidth: '60%',
        barGap: '-100%',
        z: 1
      },
      // 前景（实际库存）
      {
        name: '库存',
        type: 'bar',
        data: sortedList.map(i => i.quantity || 0),
        itemStyle: {
          borderRadius: [0, 6, 6, 0],
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 1, y2: 0,
            colorStops: [
              { offset: 0, color: '#fde68a' },
              { offset: 1, color: '#f59e0b' }
            ]
          }
        },
        barWidth: '60%',
        z: 2,
        label: {
          show: true,
          position: 'right',
          formatter: (params: any) => {
            const item = sortedList[params.dataIndex]
            return `${params.value}/${item.lowStockThreshold || 0}`
          },
          fontSize: 11,
          fontWeight: 500,
          color: '#6b7280'
        }
      }
    ]
  }
})

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
.card-border {
  background: transparent;
  border: 1px solid #e5e7eb;
  box-shadow: none;
}
.card-header {
  padding: 16px 20px 8px;
}
.chart {
  height: 320px;
  width: 100%;
}
.bar-chart {
  height: 300px;
  width: 100%;
}
.heatmap-chart {
  height: 400px;
  width: 100%;
}
.combo-chart {
  height: 450px;
  width: 100%;
}
.alert-card {
  height: 410px;
  display: flex;
  flex-direction: column;
  :deep(.el-table) {
    flex: 1;
  }
}
.alert-badge {
  :deep(.el-badge__content) {
    font-size: 10px;
    padding: 0 4px;
    height: 16px;
    line-height: 16px;
  }
}
.alert-chart {
  height: 400px;
  width: 100%;
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
.alert-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 8px;

  &::-webkit-scrollbar {
    width: 4px;
  }
  &::-webkit-scrollbar-thumb {
    background-color: #e5e7eb;
    border-radius: 2px;
  }
  &::-webkit-scrollbar-track {
    background-color: transparent;
  }
}
.alert-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 8px;
  transition: all 0.2s ease;

  &:hover {
    transform: translateX(2px);
  }

  &.alert-danger {
    background: linear-gradient(135deg, #fef2f2, #fee2e2);
    border-left: 3px solid #ef4444;

    &:hover {
      background: linear-gradient(135deg, #fee2e2, #fecaca);
    }
  }

  &.alert-warning {
    background: linear-gradient(135deg, #fffbeb, #fef3c7);
    border-left: 3px solid #f59e0b;

    &:hover {
      background: linear-gradient(135deg, #fef3c7, #fde68a);
    }
  }
}
.alert-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(255, 255, 255, 0.8);
  flex-shrink: 0;
}
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}
</style>
