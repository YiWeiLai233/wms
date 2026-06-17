<template>
  <el-dialog v-model="visible" title="编辑发货单" width="580px" destroy-on-close>
    <el-form ref="formRef" :model="form" label-width="90px">
      <el-form-item label="发货单号">
        <el-input :model-value="form.outboundNo" disabled />
      </el-form-item>
      <el-form-item label="快递公司">
        <el-select v-model="form.expressCompanyId" placeholder="请选择快递公司" style="width: 100%" clearable>
          <el-option v-for="c in companies" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="快递单号">
        <el-input v-model="form.trackingNo" placeholder="请输入快递单号" />
      </el-form-item>
      <el-form-item label="快递费用">
        <el-input-number v-model="form.shippingFee" :min="0" :precision="2" style="width: 100%" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { getOutboundDetail, updateOutbound } from '@/api/outbound'
import type { ExpressCompany } from '@/api/express'

const props = defineProps<{
  modelValue: boolean
  outboundId: number | null
  companies: ExpressCompany[]
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
  id: 0,
  outboundNo: '',
  expressCompanyId: undefined as number | undefined,
  trackingNo: '',
  shippingFee: undefined as number | undefined,
  remark: '',
})

watch(() => props.outboundId, async (id) => {
  if (id) {
    try {
      const res = await getOutboundDetail(id)
      const outbound = res.data
      Object.assign(form, {
        id: outbound.id,
        outboundNo: outbound.outboundNo || '',
        expressCompanyId: outbound.expressCompanyId || undefined,
        trackingNo: outbound.trackingNo || '',
        shippingFee: outbound.shippingFee || undefined,
        remark: outbound.remark || '',
      })
    } catch {}
  }
})

async function handleSubmit() {
  loading.value = true
  try {
    await updateOutbound(form.id, {
      expressCompanyId: form.expressCompanyId,
      trackingNo: form.trackingNo || undefined,
      shippingFee: form.shippingFee,
      remark: form.remark,
    })
    ElMessage.success('保存成功')
    visible.value = false
    emit('success')
  } catch {} finally {
    loading.value = false
  }
}
</script>
