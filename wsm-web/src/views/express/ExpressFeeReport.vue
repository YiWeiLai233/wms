<template>
  <div class="page-container">
    <PageHeader title="快递费用统计" subtitle="按日期范围查询快递费用明细及汇总" />

    <!-- 查询条件 -->
    <div class="card mb-4">
      <el-form :model="queryForm" inline>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            :shortcuts="dateShortcuts"
            style="width: 280px"
          />
        </el-form-item>
        <el-form-item label="快递公司">
          <el-select v-model="queryForm.expressCompanyId" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="c in companyList" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 汇总卡片 -->
    <el-row :gutter="20" class="mb-4">
      <el-col :span="12">
        <div class="card text-center">
          <div class="text-sm text-gray-500 mb-1">快递总单数</div>
          <div class="text-3xl font-bold text-blue-600">{{ reportData.totalCount || 0 }}</div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="card text-center">
          <div class="text-sm text-gray-500 mb-1">快递总费用</div>
          <div class="text-3xl font-bold text-red-500">¥{{ (reportData.totalFee || 0).toFixed(2) }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 明细表格 -->
    <div class="card">
      <el-table :data="reportData.items || []" v-loading="loading" stripe border>
        <el-table-column prop="outboundNo" label="出库单号" width="160" />
        <el-table-column prop="orderNo" label="订单号" width="140" />
        <el-table-column prop="expressCompanyName" label="快递公司" width="120">
          <template #default="{ row }">{{ row.expressCompanyName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="trackingNo" label="快递单号" width="150">
          <template #default="{ row }">
            <span v-if="row.trackingNo" class="font-mono">{{ row.trackingNo }}</span>
            <span v-else class="text-gray-400">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="shippingFee" label="快递费用" width="120" align="right">
          <template #default="{ row }">
            <span class="text-red-500 font-bold">¥{{ row.shippingFee?.toFixed(2) || '0.00' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="shippedAt" label="发货时间" min-width="170">
          <template #default="{ row }">{{ row.shippedAt || '-' }}</template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && (!reportData.items || reportData.items.length === 0)" description="暂无数据，请选择日期范围查询" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getExpressFeeReport } from '@/api/report'
import type { ExpressFeeReport } from '@/api/report'
import { getCompanyList } from '@/api/express'
import type { ExpressCompany } from '@/api/express'
import PageHeader from '@/components/PageHeader.vue'

const loading = ref(false)
const companyList = ref<ExpressCompany[]>([])
const reportData = ref<ExpressFeeReport>({ totalFee: 0, totalCount: 0, items: [] })

const dateShortcuts = [
  {
    text: '本周',
    value: () => {
      const now = new Date()
      const day = now.getDay() || 7
      const start = new Date(now)
      start.setDate(now.getDate() - day + 1)
      return [start, now]
    },
  },
  {
    text: '本月',
    value: () => {
      const now = new Date()
      const start = new Date(now.getFullYear(), now.getMonth(), 1)
      return [start, now]
    },
  },
  {
    text: '本季度',
    value: () => {
      const now = new Date()
      const quarter = Math.floor(now.getMonth() / 3)
      const start = new Date(now.getFullYear(), quarter * 3, 1)
      return [start, now]
    },
  },
  {
    text: '本年',
    value: () => {
      const now = new Date()
      const start = new Date(now.getFullYear(), 0, 1)
      return [start, now]
    },
  },
  {
    text: '最近7天',
    value: () => {
      const now = new Date()
      const start = new Date()
      start.setDate(now.getDate() - 6)
      return [start, now]
    },
  },
  {
    text: '最近30天',
    value: () => {
      const now = new Date()
      const start = new Date()
      start.setDate(now.getDate() - 29)
      return [start, now]
    },
  },
]

const queryForm = reactive({
  dateRange: [] as string[],
  expressCompanyId: undefined as number | undefined,
})

async function handleQuery() {
  loading.value = true
  try {
    const res = await getExpressFeeReport({
      startTime: queryForm.dateRange?.[0] || undefined,
      endTime: queryForm.dateRange?.[1] || undefined,
      expressCompanyId: queryForm.expressCompanyId || undefined,
    })
    reportData.value = res.data || { totalFee: 0, totalCount: 0, items: [] }
  } catch {
    reportData.value = { totalFee: 0, totalCount: 0, items: [] }
  } finally {
    loading.value = false
  }
}

function handleReset() {
  queryForm.dateRange = []
  queryForm.expressCompanyId = undefined
  reportData.value = { totalFee: 0, totalCount: 0, items: [] }
}

onMounted(async () => {
  try {
    const res = await getCompanyList()
    companyList.value = res.data || []
  } catch {}
})
</script>
