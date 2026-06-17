<template>
  <el-dialog v-model="visible" title="发货单详情" width="760px">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="发货单号">{{ detail.outboundNo }}</el-descriptions-item>
      <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
      <el-descriptions-item label="平台单号">{{ detail.platformOrderNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="仓库">{{ detail.warehouseName || detail.warehouseId }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="(OUTBOUND_STATUS_MAP[detail.status]?.color as any) || 'info'" size="small">
          {{ OUTBOUND_STATUS_MAP[detail.status]?.label || detail.status }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="快递公司">{{ detail.expressCompanyName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="快递单号">{{ detail.trackingNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="快递费用">{{ detail.shippingFee ? `¥${detail.shippingFee.toFixed(2)}` : '-' }}</el-descriptions-item>
      <el-descriptions-item label="发货备注">{{ detail.remark || '-' }}</el-descriptions-item>
      <el-descriptions-item label="订单备注">{{ detail.orderRemark || '-' }}</el-descriptions-item>
    </el-descriptions>

    <h4 class="mt-4 mb-2 text-sm font-semibold text-gray-700">发货明细</h4>
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
        <template #default="{ row }">{{ row.warehouseName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="quantity" label="应出" width="80" align="center" />
    </el-table>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { OutboundOrder } from '@/api/outbound'
import { OUTBOUND_STATUS_MAP } from '@/utils/constants'
import ImagePreview from '@/components/ImagePreview.vue'

const props = defineProps<{
  modelValue: boolean
  detail: Partial<OutboundOrder>
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})
</script>
