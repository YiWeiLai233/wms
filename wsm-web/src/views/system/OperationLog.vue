<template>
  <div class="page-container">
    <PageHeader title="操作日志" subtitle="系统操作审计记录" />

    <!-- 搜索栏 -->
    <div class="card mb-4">
      <el-form :model="searchParams" inline>
        <el-form-item label="模块">
          <el-select v-model="searchParams.module" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="m in modules" :key="m.value" :label="m.label" :value="m.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作">
          <el-input v-model="searchParams.action" placeholder="操作类型" clearable style="width: 140px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="card">
      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="userName" label="操作人" width="90" />
        <el-table-column prop="module" label="模块" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="MODULE_COLOR[row.module] || 'info'" size="small">
              {{ MODULE_LABEL[row.module] || row.module }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetType" label="对象" width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.targetType">{{ TARGET_LABEL[row.targetType] || row.targetType }}</span>
            <span v-else class="text-gray-400">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="targetId" label="ID" width="70" align="center">
          <template #default="{ row }">
            <span v-if="row.targetId">#{{ row.targetId }}</span>
            <span v-else class="text-gray-400">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="detail" label="操作详情" min-width="350">
          <template #default="{ row }">
            <div :class="{ 'text-red-500': row.detail?.includes('❌') }" style="white-space: normal; line-height: 1.5; word-break: break-all;">
              {{ row.detail || '-' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column prop="createdAt" label="操作时间" width="170">
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
import { getOperationLogs } from '@/api/log'
import type { OperationLog } from '@/api/log'
import { useTable } from '@/composables/useTable'
import { formatDateTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'

const modules = [
  { label: '订单', value: 'order' },
  { label: '商品', value: 'product' },
  { label: '库存', value: 'stock' },
  { label: '出库', value: 'outbound' },
  { label: '退货', value: 'return' },
  { label: '换货', value: 'exchange' },
  { label: '仓库', value: 'warehouse' },
  { label: '快递', value: 'express' },
  { label: '用户', value: 'user' },
  { label: '系统', value: 'system' },
  { label: 'AI助手', value: 'ai_assistant' },
]

const MODULE_LABEL: Record<string, string> = {
  order: '订单', product: '商品', stock: '库存', outbound: '出库',
  return: '退货', exchange: '换货', warehouse: '仓库', express: '快递',
  user: '用户', system: '系统', ai_assistant: 'AI助手',
}

const MODULE_COLOR: Record<string, string> = {
  order: 'primary', product: 'success', stock: 'warning', outbound: 'danger',
  return: 'info', exchange: '', warehouse: 'primary', express: 'success',
  user: 'warning', system: 'danger', ai_assistant: 'info',
}

const TARGET_LABEL: Record<string, string> = {
  SalesOrder: '销售订单', Product: '商品', ProductSku: 'SKU', Stock: '库存',
  OutboundOrder: '出库单', ReturnOrder: '退货单', ExchangeOrder: '换货单',
  Warehouse: '仓库', ExpressCompany: '快递公司', User: '用户', Backup: '备份',
}

const { tableData, loading, pagination, searchParams, handleSearch, handleReset, handlePageChange, handleSizeChange } = useTable<OperationLog>(getOperationLogs)
</script>
