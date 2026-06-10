<template>
  <div class="page-container">
    <PageHeader title="货架管理">
      <template #actions>
        <el-button type="primary" icon="Plus" @click="openDialog()">新增货架</el-button>
      </template>
    </PageHeader>

    <!-- 仓库选择 -->
    <div class="card mb-4">
      <el-form inline>
        <el-form-item label="选择仓库">
          <el-select v-model="selectedWarehouseId" placeholder="请选择仓库" style="width: 200px" @change="handleWarehouseChange">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="card">
      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="code" label="货架编码" width="120" />
        <el-table-column prop="name" label="货架名称" min-width="150" />
        <el-table-column prop="categoryName" label="商品分类" min-width="150" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link icon="Edit" @click="openDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除该货架吗？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!selectedWarehouseId" class="mt-8">
        <el-empty description="请先选择仓库" />
      </div>
    </div>

    <!-- 弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑货架' : '新增货架'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="所属仓库" prop="warehouseId">
          <el-select v-model="form.warehouseId" placeholder="请选择仓库" style="width: 100%" :disabled="isEdit">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" placeholder="如 A01" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="货架名称" />
        </el-form-item>
        <el-form-item label="商品分类" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="如：男拖、女拖、童拖" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getWarehouseList, getShelfList, createShelf, updateShelf, deleteShelf } from '@/api/warehouse'
import type { Warehouse, WarehouseShelf } from '@/api/warehouse'
import PageHeader from '@/components/PageHeader.vue'

const warehouses = ref<Warehouse[]>([])
const selectedWarehouseId = ref<number>()
const tableData = ref<WarehouseShelf[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<Partial<WarehouseShelf>>({
  id: undefined,
  warehouseId: undefined,
  code: '',
  name: '',
  categoryName: '',
  status: 1,
})

const rules: FormRules = {
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  code: [{ required: true, message: '请输入货架编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入货架名称', trigger: 'blur' }],
}

onMounted(async () => {
  const res = await getWarehouseList({ page: 1, size: 100 })
  warehouses.value = res.data.list || []
  if (warehouses.value.length > 0) {
    selectedWarehouseId.value = warehouses.value[0].id
    handleWarehouseChange()
  }
})

async function handleWarehouseChange() {
  if (!selectedWarehouseId.value) return
  loading.value = true
  try {
    const res = await getShelfList(selectedWarehouseId.value)
    tableData.value = res.data || []
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function openDialog(row?: WarehouseShelf) {
  isEdit.value = !!row
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, {
      id: undefined,
      warehouseId: selectedWarehouseId.value,
      code: '',
      name: '',
      categoryName: '',
      status: 1,
    })
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateShelf(form)
      ElMessage.success('修改成功')
    } else {
      await createShelf(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    handleWarehouseChange()
  } catch {} finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await deleteShelf(id)
    ElMessage.success('删除成功')
    handleWarehouseChange()
  } catch {}
}
</script>
