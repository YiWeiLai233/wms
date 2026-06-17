<template>
  <el-dialog v-model="visible" title="订单详情" width="760px">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
      <el-descriptions-item label="平台">
        <div v-if="detail.platformName" class="flex items-center gap-1">
          <div v-if="detail.platformColor" class="w-3 h-3 rounded" :style="{ backgroundColor: detail.platformColor }"></div>
          <span>{{ detail.platformName }}</span>
        </div>
        <span v-else>-</span>
      </el-descriptions-item>
      <el-descriptions-item label="平台单号">{{ detail.platformOrderNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="仓库">{{ detail.warehouseName || detail.warehouseId }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="(ORDER_STATUS_MAP[detail.orderStatus || '']?.color as any) || 'info'" :effect="(ORDER_STATUS_MAP[detail.orderStatus || '']?.effect as any) || 'light'" size="small">
          {{ ORDER_STATUS_MAP[detail.orderStatus || '']?.label || detail.orderStatus }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="收件人">{{ detail.receiverName }}</el-descriptions-item>
      <el-descriptions-item label="电话">{{ detail.receiverPhone }}</el-descriptions-item>
      <el-descriptions-item label="地址" :span="2">{{ detail.receiverAddress }}</el-descriptions-item>
      <el-descriptions-item label="金额">¥{{ detail.totalAmount?.toFixed(2) }}</el-descriptions-item>
      <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
    </el-descriptions>

    <h4 class="mt-4 mb-2 text-sm font-semibold text-gray-700">订单明细</h4>
    <el-table :data="detail.items || []" border size="small">
      <el-table-column label="图片" width="60" align="center">
        <template #default="{ row }">
          <ImagePreview :src="row.skuImage" />
        </template>
      </el-table-column>
      <el-table-column prop="skuCode" label="SKU编码" width="130" />
      <el-table-column prop="skuName" label="SKU名称" min-width="100" />
      <el-table-column prop="sizeValue" label="码数" width="80" align="center">
        <template #default="{ row }">{{ row.sizeValue || '-' }}</template>
      </el-table-column>
      <el-table-column label="仓库" width="100" align="center">
        <template #default>{{ detail.warehouseName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="quantity" label="数量" width="80" align="center" />
      <el-table-column prop="totalPrice" label="小计" width="90" align="right">
        <template #default="{ row }">¥{{ row.totalPrice?.toFixed(2) }}</template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Order } from '@/api/order'
import { ORDER_STATUS_MAP } from '@/utils/constants'
import ImagePreview from '@/components/ImagePreview.vue'

const props = defineProps<{
  modelValue: boolean
  detail: Partial<Order>
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})
</script>
