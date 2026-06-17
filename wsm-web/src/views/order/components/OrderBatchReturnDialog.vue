<template>
  <el-dialog v-model="visible" title="批量退货" width="500px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="退货订单">
        <div class="text-sm text-gray-600">已选择 {{ orderCount }} 个订单</div>
      </el-form-item>
      <el-form-item label="退货原因" prop="reason">
        <el-select
          v-model="form.reason"
          filterable
          allow-create
          default-first-option
          placeholder="请选择或输入退货原因"
          style="width: 100%"
        >
          <el-option label="七天无理由退货" value="七天无理由退货" />
          <el-option label="商品质量问题" value="商品质量问题" />
          <el-option label="商品与描述不符" value="商品与描述不符" />
          <el-option label="发错货" value="发错货" />
          <el-option label="物流问题" value="物流问题" />
          <el-option label="客户取消订单" value="客户取消订单" />
          <el-option label="其他" value="其他" />
        </el-select>
      </el-form-item>
      <el-form-item label="客户快递单号">
        <el-input v-model="form.trackingNo" placeholder="客户退回的快递单号（选填）" />
      </el-form-item>
      <el-form-item label="退货快递费">
        <el-input-number v-model="form.shippingFee" :min="0" :precision="2" style="width: 100%" placeholder="快递费用（选填）" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息（选填）" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">确认退货</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createBatchReturn } from '@/api/returns'

const props = defineProps<{
  modelValue: boolean
  orderIds: number[]
  orderCount: number
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  reason: '',
  trackingNo: '',
  shippingFee: undefined as number | undefined,
  remark: '',
})

const rules: FormRules = {
  reason: [{ required: true, message: '请选择或输入退货原因', trigger: 'change' }],
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await createBatchReturn({
      orderIds: props.orderIds,
      reason: form.reason,
      remark: form.remark || undefined,
      trackingNo: form.trackingNo || undefined,
      shippingFee: form.shippingFee,
    })
    ElMessage.success(`批量退货成功，共创建 ${res.data?.length || 0} 个退货单`)
    visible.value = false
    emit('success')
  } catch {} finally {
    loading.value = false
  }
}
</script>
