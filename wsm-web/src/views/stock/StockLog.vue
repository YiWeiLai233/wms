<template>
  <div class="page-container">
    <PageHeader title="库存流水" subtitle="库存变动记录" />

    <!-- 搜索栏 -->
    <div class="card mb-4">
      <el-form :model="searchParams" inline>
        <el-form-item label="业务类型">
          <el-select v-model="searchParams.bizType" placeholder="全部" clearable style="width: 120px">
            <el-option label="入库" value="INBOUND" />
            <el-option label="出库" value="OUTBOUND" />
            <el-option label="退货" value="RETURN" />
            <el-option label="调整" value="ADJUST" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务单号">
          <el-input v-model="searchParams.bizNo" placeholder="单号" clearable style="width: 160px" @keyup.enter="handleDateSearch" />
        </el-form-item>
        <el-form-item label="平台单号">
          <el-input v-model="searchParams.platformOrderNo" placeholder="平台订单号" clearable style="width: 160px" @keyup.enter="handleDateSearch" />
        </el-form-item>
        <el-form-item label="时间范围">
          <DateRangePicker v-model="dateRange" width="360px" @change="handleDateSearch" />
        </el-form-item>
        <el-form-item label="仓库">
          <el-select v-model="searchParams.warehouseId" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleDateSearch">搜索</el-button>
          <el-button icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="card">
      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="bizType" label="业务类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="STOCK_BIZ_TYPE_MAP[row.bizType]?.color as any" size="small">
              {{ STOCK_BIZ_TYPE_MAP[row.bizType]?.label || row.bizType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="bizNo" label="业务单号" width="160" />
        <el-table-column prop="platformOrderNo" label="平台单号" width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.platformOrderNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="skuCode" label="SKU编码" width="120" />
        <el-table-column prop="warehouseName" label="仓库" width="100" />
        <el-table-column prop="quantityBefore" label="变动前" width="80" align="center" />
        <el-table-column prop="quantityChange" label="变动数量" width="90" align="center">
          <template #default="{ row }">
            <span :class="row.quantityChange > 0 ? 'text-green-600 font-bold' : 'text-red-500 font-bold'">
              {{ row.quantityChange > 0 ? '+' : '' }}{{ row.quantityChange }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="quantityAfter" label="变动后" width="80" align="center" />
        <el-table-column prop="operatorName" label="操作人" width="90" />
        <el-table-column prop="remark" label="备注" min-width="100" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getStockLogs } from '@/api/stock'
import { getWarehouseList } from '@/api/warehouse'
import type { StockLog } from '@/api/stock'
import type { Warehouse } from '@/api/warehouse'
import { useTable } from '@/composables/useTable'
import { formatDateTime } from '@/utils/format'
import { STOCK_BIZ_TYPE_MAP } from '@/utils/constants'
import PageHeader from '@/components/PageHeader.vue'
import DateRangePicker from '@/components/DateRangePicker.vue'

const { tableData, loading, pagination, searchParams, handleSearch, handleReset, handlePageChange, handleSizeChange } = useTable<StockLog>(getStockLogs)

const warehouses = ref<Warehouse[]>([])
const dateRange = ref<string[]>([])

onMounted(async () => {
  try {
    const res = await getWarehouseList({ page: 1, size: 100 })
    warehouses.value = res.data.list || []
  } catch {}
})

function handleDateSearch() {
  searchParams.startTime = dateRange.value?.[0] || undefined
  searchParams.endTime = dateRange.value?.[1] || undefined
  handleSearch()
}

function resetSearch() {
  dateRange.value = []
  searchParams.startTime = undefined
  searchParams.endTime = undefined
  handleReset()
}
</script>
