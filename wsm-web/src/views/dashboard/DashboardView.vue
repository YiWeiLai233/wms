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
        <div class="card combo-card">
          <div class="card-header">
            <div class="flex items-center gap-2">
              <el-icon class="text-blue-500" :size="18"><TrendCharts /></el-icon>
              <h3 class="text-base font-semibold text-gray-800">近 7 天各平台 SKU 销量趋势</h3>
            </div>
          </div>
          <v-chart ref="comboChartRef" class="combo-chart" :option="platformSkuComboOption" autoresize @wheel.prevent="handleComboChartWheel" />
        </div>
      </el-col>
    </el-row>

    <!-- 近7天各平台SKU销量热力图 -->
    <el-row :gutter="20" class="mt-4">
      <el-col :span="12">
        <div class="card card-border">
          <div class="card-header">
            <div class="flex items-center gap-2">
              <el-icon class="text-purple-500" :size="18"><Grid /></el-icon>
              <h3 class="text-base font-semibold text-gray-800">近 7 天各平台 SKU 销量热力图</h3>
            </div>
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
import { GridComponent, TooltipComponent, LegendComponent, VisualMapComponent, RadarComponent, GraphicComponent } from 'echarts/components'
import { WarningFilled, Warning, CircleCheck, TrendCharts, Grid } from '@element-plus/icons-vue'
import { getDashboard } from '@/api/report'
import type { DashboardData } from '@/api/report'
import { getLowStockList, getOutOfStockList } from '@/api/stockAlert'
import type { StockAlertStatus } from '@/api/stockAlert'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'

use([CanvasRenderer, LineChart, PieChart, BarChart, HeatmapChart, RadarChart, GridComponent, TooltipComponent, LegendComponent, VisualMapComponent, RadarComponent, GraphicComponent])

const comboChartRef = ref<any>(null)

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

// 处理图表区域滚轮事件，让 tooltip 滚动而不是页面滚动
const handleComboChartWheel = (e: WheelEvent) => {
  const tooltipDom = document.querySelector('.echarts-tooltip')
  if (tooltipDom) {
    const scrollContainer = tooltipDom.querySelector('[style*="overflow-y"]') as HTMLElement
    if (scrollContainer) {
      e.preventDefault()
      scrollContainer.scrollTop += e.deltaY
    }
  }
}

// 辅助函数：给颜色添加透明度（支持 hex 和 rgb 格式）
const colorWithAlpha = (color: string, alpha: number): string => {
  if (!color) return `rgba(148, 163, 184, ${alpha})`

  // 处理 hex 格式 (#3b82f6)
  if (color.startsWith('#')) {
    const hex = color.replace('#', '')
    const r = parseInt(hex.substring(0, 2), 16)
    const g = parseInt(hex.substring(2, 4), 16)
    const b = parseInt(hex.substring(4, 6), 16)
    return `rgba(${r}, ${g}, ${b}, ${alpha})`
  }

  // 处理 rgb 格式 (rgb(248, 1, 1))
  if (color.startsWith('rgb(')) {
    return color.replace('rgb(', 'rgba(').replace(')', `, ${alpha})`)
  }

  // 处理 rgba 格式，直接替换 alpha
  if (color.startsWith('rgba(')) {
    return color.replace(/,\s*[\d.]+\)$/, `, ${alpha})`)
  }

  // 其他格式返回默认
  return `rgba(148, 163, 184, ${alpha})`
}

const trendOption = computed(() => {
  const dates = dashboard.value.orderTrend.map((i) => i.date.slice(5))
  const platformTrends = dashboard.value.platformTrends || []

  // 如果有平台趋势数据，按平台显示曲线
  if (platformTrends.length > 0) {
    return {
      tooltip: {
        trigger: 'axis',
        confine: true,
        appendToBody: true,
        extraCssText: 'pointer-events:auto;',
        formatter: (params: any) => {
          let result = `<div onwheel="event.stopPropagation()" style="font-weight:bold;margin-bottom:5px">${params[0].axisValue}</div>`
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
      confine: true,
      appendToBody: true,
      extraCssText: 'pointer-events:auto;',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        let result = `<div onwheel="event.stopPropagation()" style="font-weight:bold;margin-bottom:5px">${params[0].axisValue}</div>`
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
  platformSales.forEach((ps) => ps.skuSales?.forEach((s) => allDates.add(s.date)))
  const dates = Array.from(allDates).sort()

  // 平台颜色
  const defaultColors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16']
  const platformColors: Record<string, string> = {}
  platformSales.forEach((ps, idx) => {
    platformColors[ps.platformName] = ps.platformColor || defaultColors[idx % defaultColors.length]
  })

  // 每个平台一个柱子
  const series: any[] = []
  const legendData: string[] = []

  platformSales.forEach((ps) => {
    const color = platformColors[ps.platformName]
    legendData.push(ps.platformName)

    const dailyTotals = dates.map((date) =>
      ps.skuSales?.filter((s) => s.date === date).reduce((sum, s) => sum + s.quantity, 0) || 0
    )

    series.push({
      name: ps.platformName,
      type: 'bar',
      data: dailyTotals,
      barWidth: 'auto',
      barGap: '20%',
      barCategoryGap: '40%',
      itemStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: color },
            { offset: 1, color: colorWithAlpha(color, 0.6) },
          ],
        },
        borderRadius: [4, 4, 0, 0],
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowColor: 'rgba(0,0,0,0.2)',
        },
      },
    })
  })

  return {
    tooltip: {
      trigger: 'axis',
      confine: true,
      enterable: true,
      appendToBody: true,
      position: function (point: any) {
        // tooltip 出现在鼠标右侧偏移位置
        return { left: point[0] + 20, top: point[1] - 50 }
      },
      axisPointer: {
        type: 'shadow',
        shadowStyle: { color: 'rgba(0,0,0,0.05)' },
      },
      backgroundColor: 'rgba(255,255,255,0.98)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      borderRadius: 12,
      padding: [14, 20],
      textStyle: { color: '#374151', fontSize: 12 },
      extraCssText: 'overscroll-behavior: contain; pointer-events: auto; max-width: 450px;',
      formatter: (params: any) => {
        if (!params || params.length === 0) return ''
        const dateLabel = params[0]?.axisValue || ''
        const originalDate = dates.find(d => d.slice(5) === dateLabel) || ''

        let html = `<div style="min-width:350px;max-width:450px;max-height:500px;overflow-y:auto;padding:8px 16px;overscroll-behavior:contain;cursor:default" onmouseenter="event.stopPropagation()" onwheel="event.stopPropagation();event.preventDefault();this.scrollTop+=event.deltaY;return false">`
        html += `<div style="font-weight:600;color:#1f2937;font-size:14px;margin-bottom:12px;padding-bottom:8px;border-bottom:2px solid #e5e7eb;position:sticky;top:0;background:rgba(255,255,255,0.98);z-index:1">📅 ${dateLabel}</div>`

        params.forEach((p: any) => {
          if (p.value === 0) return
          const platformName = p.seriesName
          const platformColor = platformColors[platformName] || '#3b82f6'

          const platformData = platformSales.find(ps => ps.platformName === platformName)
          const skuSales = platformData?.skuSales?.filter(s => s.date === originalDate) || []
          const sortedSales = [...skuSales].sort((a, b) => b.quantity - a.quantity)

          html += `<div style="margin-bottom:10px">`
          html += `<div style="display:flex;align-items:center;gap:8px;margin-bottom:4px">
            <span style="display:inline-block;width:10px;height:10px;border-radius:3px;background:${platformColor}"></span>
            <span style="font-weight:600;color:#374151">${platformName}</span>
            <span style="margin-left:auto;font-weight:600;color:#1f2937">${p.value} 件</span>
          </div>`

          if (sortedSales.length > 0) {
            html += `<div style="padding-left:18px;font-size:11px;color:#6b7280">`
            sortedSales.forEach((s) => {
              html += `<div style="display:flex;justify-content:space-between;padding:2px 0">
                <span>${s.skuName}</span>
                <span style="font-weight:500;color:#374151">${s.quantity}</span>
              </div>`
            })
            html += `</div>`
          }

          html += `</div>`
        })

        html += `</div>`
        return html
      },
    },
    legend: {
      data: legendData,
      bottom: 0,
      left: 'center',
      textStyle: { color: '#6b7280', fontSize: 11 },
      type: 'scroll',
      pageTextStyle: { color: '#6b7280' },
      pageIconColor: '#6b7280',
      pageIconInactiveColor: '#d1d5db',
      itemGap: 20,
      itemWidth: 14,
      itemHeight: 10,
    },
    grid: {
      left: 60,
      right: 40,
      top: 50,
      bottom: 60,
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: dates.map((d) => d.slice(5)),
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisLabel: { color: '#6b7280', fontSize: 12 },
      boundaryGap: true,
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
      confine: true,
      appendToBody: true,
      extraCssText: 'pointer-events:auto;',
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
        return `<div onwheel="event.stopPropagation()" style="font-weight:600;margin-bottom:6px;color:#1f2937">${platform}</div>
                <div style="display:flex;align-items:center;gap:8px">
                  <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:#3b82f6"></span>
                  <span>${sku}</span>
                  <span style="font-weight:700;color:#3b82f6;margin-left:auto">${value} 件</span>
                </div>`
      },
    },
    grid: {
      left: 20,
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
        width: 150,
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
          formatter: (params: any) => {
            const ratio = maxVal > 0 ? Math.min(params.data[2] / maxVal, 1) : 0
            const stops = [
              [0xef, 0xf6, 0xff],
              [0xbf, 0xdb, 0xfe],
              [0x60, 0xa5, 0xfa],
              [0x25, 0x63, 0xeb],
              [0x1e, 0x40, 0xaf],
            ]
            const idx = ratio * (stops.length - 1)
            const lo = Math.floor(idx)
            const hi = Math.min(lo + 1, stops.length - 1)
            const t = idx - lo
            const r = Math.round(stops[lo][0] + (stops[hi][0] - stops[lo][0]) * t)
            const g = Math.round(stops[lo][1] + (stops[hi][1] - stops[lo][1]) * t)
            const b = Math.round(stops[lo][2] + (stops[hi][2] - stops[lo][2]) * t)
            const luminance = 0.299 * r + 0.587 * g + 0.114 * b
            const textColor = luminance > 150 ? '#000000' : '#ffffff'
            return `{${textColor === '#000000' ? 'bk' : 'wh'}|${params.data[2]}}`
          },
          rich: {
            bk: { color: '#000000', fontSize: 12, fontWeight: 600 },
            wh: { color: '#ffffff', fontSize: 12, fontWeight: 600 },
          },
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
      confine: true,
      appendToBody: true,
      extraCssText: 'pointer-events:auto;',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e5e7eb',
      textStyle: { color: '#333', fontSize: 13 },
      formatter: (params: any) => {
        const item = sortedList[params[0]?.dataIndex]
        if (!item) return ''
        const ratio = Math.round(((item.quantity || 0) / (item.outOfStockThreshold || 1)) * 100)
        return `<div onwheel="event.stopPropagation()" style="font-weight:600;margin-bottom:4px">${item.skuName}</div>
                <div>当前库存: <span style="color:#ef4444;font-weight:600">${item.quantity}</span></div>
                <div>缺货阈值: ${item.outOfStockThreshold || 0}</div>
                <div>库存占比: <span style="color:${ratio < 50 ? '#ef4444' : '#f59e0b'};font-weight:600">${ratio}%</span></div>
                <div>仓库: ${item.warehouseName || '所有仓库'}</div>`
      }
    },
    legend: {
      data: ['库存', '阈值'],
      bottom: 0,
      textStyle: { color: '#6b7280', fontSize: 11 },
      itemGap: 20,
    },
    grid: { left: 10, right: 60, top: 15, bottom: 40, containLabel: true },
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
        width: 150,
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
      confine: true,
      appendToBody: true,
      extraCssText: 'pointer-events:auto;',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e5e7eb',
      textStyle: { color: '#333', fontSize: 13 },
      formatter: (params: any) => {
        const item = sortedList[params[0]?.dataIndex]
        if (!item) return ''
        const ratio = Math.round(((item.quantity || 0) / (item.lowStockThreshold || 1)) * 100)
        return `<div onwheel="event.stopPropagation()" style="font-weight:600;margin-bottom:4px">${item.skuName}</div>
                <div>当前库存: <span style="color:#f59e0b;font-weight:600">${item.quantity}</span></div>
                <div>低库存阈值: ${item.lowStockThreshold || 0}</div>
                <div>库存占比: <span style="color:${ratio < 50 ? '#ef4444' : '#22c55e'};font-weight:600">${ratio}%</span></div>
                <div>仓库: ${item.warehouseName || '所有仓库'}</div>`
      }
    },
    legend: {
      data: ['库存', '阈值'],
      bottom: 0,
      textStyle: { color: '#6b7280', fontSize: 11 },
      itemGap: 20,
    },
    grid: { left: 10, right: 60, top: 15, bottom: 40, containLabel: true },
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
        width: 150,
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
.combo-card {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.combo-card .card-header {
  background: white;
  border-bottom: 1px solid #e2e8f0;
  padding: 16px 24px;
}

.combo-chart {
  height: 500px;
  width: 100%;
  padding: 16px;
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
