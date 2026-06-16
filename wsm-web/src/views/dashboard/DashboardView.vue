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
      <el-col :span="24">
        <div class="card">
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
    return { series: [] }
  }

  // 收集所有日期
  const allDates = new Set<string>()
  platformSales.forEach((ps) => {
    ps.skuSales?.forEach((s) => allDates.add(s.date))
  })
  const dates = Array.from(allDates).sort()

  // 收集所有SKU名称
  const allSkuNames = new Set<string>()
  platformSales.forEach((ps) => {
    ps.skuSales?.forEach((s) => allSkuNames.add(s.skuName))
  })
  const skuNames = Array.from(allSkuNames)

  // 平台颜色
  const platformColors: Record<string, string> = {}
  const defaultColors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899']
  platformSales.forEach((ps, idx) => {
    platformColors[ps.platformName] = ps.platformColor || defaultColors[idx % defaultColors.length]
  })

  // SKU透明度配置（用于区分同一平台不同SKU）
  const skuOpacity = [1, 0.75, 0.5, 0.35, 0.25]

  // 构建系列：每个平台-SKU组合一个系列
  const series: any[] = []
  const legendData: string[] = []

  platformSales.forEach((ps) => {
    const baseColor = platformColors[ps.platformName]

    skuNames.forEach((skuName, sIdx) => {
      const seriesName = `${ps.platformName} - ${skuName}`
      legendData.push(seriesName)

      // 计算该平台该SKU每天的销量
      const data = dates.map((date) => {
        const skuData = ps.skuSales?.find((s) => s.skuName === skuName && s.date === date)
        return skuData?.quantity || 0
      })

      series.push({
        name: seriesName,
        type: 'bar',
        stack: ps.platformName,
        data,
        itemStyle: {
          color: baseColor,
          opacity: skuOpacity[sIdx % skuOpacity.length],
        },
        emphasis: {
          itemStyle: {
            opacity: 1,
            shadowBlur: 10,
            shadowColor: 'rgba(0,0,0,0.3)',
          },
        },
      })
    })
  })

  // 添加总计折线图
  const totalData = dates.map((date) => {
    let total = 0
    platformSales.forEach((ps) => {
      const daySales = ps.skuSales?.filter((s) => s.date === date) || []
      daySales.forEach((s) => total += s.quantity)
    })
    return total
  })

  series.push({
    name: '总计',
    type: 'line',
    data: totalData,
    smooth: true,
    symbol: 'circle',
    symbolSize: 10,
    lineStyle: {
      width: 3,
      color: '#f97316',
    },
    itemStyle: {
      color: '#f97316',
      borderColor: '#fff',
      borderWidth: 3,
    },
    label: {
      show: true,
      position: 'top',
      fontSize: 12,
      fontWeight: 'bold',
      color: '#f97316',
    },
  })

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
        shadowStyle: { color: 'rgba(0,0,0,0.05)' },
      },
      formatter: (params: any) => {
        let result = `<div style="font-weight:bold;margin-bottom:8px">${params[0]?.axisValue || ''}</div>`
        const platformTotals = new Map<string, { total: number; color: string; items: { name: string; value: number }[] }>()

        params.forEach((p: any) => {
          if (p.seriesType === 'bar' && p.value > 0) {
            const parts = p.seriesName.split(' - ')
            const platformName = parts[0]
            const skuName = parts.slice(1).join(' - ')

            if (!platformTotals.has(platformName)) {
              platformTotals.set(platformName, { total: 0, color: p.color, items: [] })
            }
            const platform = platformTotals.get(platformName)!
            platform.total += p.value
            platform.items.push({ name: skuName, value: p.value })
          }
        })

        // 按平台显示
        platformTotals.forEach((platform, platformName) => {
          result += `<div style="margin-bottom:8px">
            <div style="display:flex;align-items:center;gap:6px;font-weight:bold">
              <span style="display:inline-block;width:14px;height:14px;border-radius:3px;background:${platform.color}"></span>
              <span>${platformName} (${platform.total})</span>
            </div>`
          platform.items.forEach((item) => {
            result += `<div style="padding-left:20px;font-size:12px;color:#6b7280">
              ${item.name}: ${item.value}
            </div>`
          })
          result += '</div>'
        })

        // 显示总计
        const grandTotal = Array.from(platformTotals.values()).reduce((sum, p) => sum + p.total, 0)
        if (grandTotal > 0) {
          result += `<div style="margin-top:8px;padding-top:8px;border-top:1px solid #e5e7eb;font-weight:bold;color:#f97316">
            总计: ${grandTotal}
          </div>`
        }

        return result
      },
    },
    legend: {
      data: [...legendData, '总计'],
      bottom: 0,
      textStyle: { color: '#6b7280', fontSize: 10 },
      type: 'scroll',
      pageTextStyle: { color: '#6b7280' },
      pageIconColor: '#6b7280',
      pageIconInactiveColor: '#d1d5db',
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
    return { series: [] }
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
      formatter: (params: any) => {
        const platform = platformNames[params.data[0]] || ''
        const sku = skuNames[params.data[1]] || ''
        const value = params.data[2]
        return `<div style="font-weight:bold">${platform}</div>
                <div>${sku}: <strong>${value}</strong> 件</div>`
      },
    },
    grid: {
      left: 120,
      right: 40,
      top: 20,
      bottom: 60,
    },
    xAxis: {
      type: 'category',
      data: platformNames,
      splitArea: { show: true },
      axisLabel: {
        color: '#374151',
        fontSize: 12,
        fontWeight: 'bold',
      },
    },
    yAxis: {
      type: 'category',
      data: skuNames,
      splitArea: { show: true },
      axisLabel: {
        color: '#374151',
        fontSize: 11,
        width: 100,
        overflow: 'truncate',
      },
    },
    visualMap: {
      min: 0,
      max: maxVal || 10,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: 0,
      inRange: {
        color: ['#fef2f2', '#fecaca', '#f87171', '#ef4444', '#b91c1c'],
      },
      textStyle: { color: '#6b7280' },
    },
    series: [
      {
        name: '销量',
        type: 'heatmap',
        data: heatmapData,
        label: {
          show: true,
          fontSize: 12,
          fontWeight: 'bold',
          color: '#374151',
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(0, 0, 0, 0.5)',
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
