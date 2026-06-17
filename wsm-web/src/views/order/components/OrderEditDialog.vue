<template>
  <el-dialog v-model="visible" title="编辑订单" width="600px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="平台">
        <el-select v-model="form.platformId" placeholder="选择平台" clearable style="width: 100%">
          <el-option v-for="p in platforms" :key="p.id" :label="p.name" :value="p.id">
            <div class="flex items-center gap-2">
              <div v-if="p.color" class="w-3 h-3 rounded" :style="{ backgroundColor: p.color }"></div>
              <span>{{ p.name }}</span>
            </div>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="平台单号">
        <el-input v-model="form.platformOrderNo" placeholder="平台订单号" />
      </el-form-item>
      <el-form-item label="收件人" prop="receiverName">
        <el-input v-model="form.receiverName" placeholder="收件人姓名" />
      </el-form-item>
      <el-form-item label="电话" prop="receiverPhone">
        <el-input v-model="form.receiverPhone" placeholder="收件人电话" />
      </el-form-item>
      <el-form-item label="地址" prop="receiverAddress">
        <el-input v-model="form.receiverAddress" placeholder="收件地址" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="订单备注" />
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
import type { FormInstance, FormRules } from 'element-plus'
import { getOrderDetail, updateOrder } from '@/api/order'
import type { Order } from '@/api/order'
import type { Platform } from '@/api/platform'

const props = defineProps<{
  modelValue: boolean
  orderId: number | null
  platforms: Platform[]
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
  platformId: undefined as number | undefined,
  platformOrderNo: '',
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  remark: '',
})

const rules: FormRules = {
  receiverName: [{ required: true, message: '请输入收件人', trigger: 'blur' }],
  receiverPhone: [{ required: true, message: '请输入电话', trigger: 'blur' }],
  receiverAddress: [{ required: true, message: '请输入地址', trigger: 'blur' }],
}

watch(() => props.orderId, async (id) => {
  if (id) {
    try {
      const res = await getOrderDetail(id)
      const order = res.data
      Object.assign(form, {
        id: order.id,
        platformId: order.platformId || undefined,
        platformOrderNo: order.platformOrderNo || '',
        receiverName: order.receiverName || '',
        receiverPhone: order.receiverPhone || '',
        receiverAddress: order.receiverAddress || '',
        remark: order.remark || '',
      })
    } catch {}
  }
})

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await updateOrder(form.id, {
      platformId: form.platformId,
      platformOrderNo: form.platformOrderNo || undefined,
      receiverName: form.receiverName,
      receiverPhone: form.receiverPhone,
      receiverAddress: form.receiverAddress,
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
