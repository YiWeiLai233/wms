<template>
  <div class="page-container">
    <PageHeader title="预警模板管理">
      <template #actions>
        <el-button type="primary" icon="Plus" @click="openDialog()">新增模板</el-button>
      </template>
    </PageHeader>

    <div class="card mb-4">
      <el-form :model="searchParams" inline>
        <el-form-item label="模板名称">
          <el-input v-model="searchParams.keyword" placeholder="模板名称" clearable style="width: 200px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchParams.enabled" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="name" label="模板名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="lowStockThreshold" label="低库存阈值" width="120" align="center" />
        <el-table-column prop="outOfStockThreshold" label="缺货阈值" width="120" align="center" />
        <el-table-column prop="productCount" label="关联商品数" width="120" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.productCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">
              {{ row.enabled === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link icon="Edit" @click="openDialog(row)">编辑</el-button>
            <el-button :type="row.enabled === 1 ? 'warning' : 'success'" link @click="handleToggleEnabled(row)">
              {{ row.enabled === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-popconfirm
              title="确定删除该模板吗？"
              @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button type="danger" link icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑模板' : '新增模板'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="低库存阈值" prop="lowStockThreshold">
          <el-input-number v-model="form.lowStockThreshold" :min="0" :max="999999" style="width: 100%" />
          <div class="text-xs text-gray-400 mt-1">库存低于此值显示黄色预警</div>
        </el-form-item>
        <el-form-item label="缺货阈值" prop="outOfStockThreshold">
          <el-input-number v-model="form.outOfStockThreshold" :min="0" :max="999999" style="width: 100%" />
          <div class="text-xs text-gray-400 mt-1">库存低于此值显示红色预警</div>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="备注说明（选填）" />
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
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getStockAlertTemplateList,
  createStockAlertTemplate,
  updateStockAlertTemplate,
  deleteStockAlertTemplate,
} from '@/api/stockAlertTemplate'
import type { StockAlertTemplate } from '@/api/stockAlertTemplate'
import { useTable } from '@/composables/useTable'
import { formatDateTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'

const { tableData, loading, pagination, searchParams, handleSearch, handleReset, handlePageChange, handleSizeChange, fetchData } = useTable<StockAlertTemplate>(getStockAlertTemplateList)

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  id: undefined as number | undefined,
  name: '',
  lowStockThreshold: 10,
  outOfStockThreshold: 0,
  enabled: 1,
  remark: '',
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  lowStockThreshold: [{ required: true, message: '请输入低库存阈值', trigger: 'blur' }],
  outOfStockThreshold: [
    { required: true, message: '请输入缺货阈值', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value > form.lowStockThreshold) {
          callback(new Error('缺货阈值不能大于低库存阈值'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

function openDialog(row?: StockAlertTemplate) {
  isEdit.value = !!row
  if (row) {
    Object.assign(form, {
      id: row.id,
      name: row.name,
      lowStockThreshold: row.lowStockThreshold,
      outOfStockThreshold: row.outOfStockThreshold,
      enabled: row.enabled,
      remark: row.remark || '',
    })
  } else {
    Object.assign(form, {
      id: undefined,
      name: '',
      lowStockThreshold: 10,
      outOfStockThreshold: 0,
      enabled: 1,
      remark: '',
    })
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value && form.id) {
      await updateStockAlertTemplate(form.id, {
        name: form.name,
        lowStockThreshold: form.lowStockThreshold,
        outOfStockThreshold: form.outOfStockThreshold,
        enabled: form.enabled,
        remark: form.remark,
      })
      ElMessage.success('更新成功')
    } else {
      await createStockAlertTemplate({
        name: form.name,
        lowStockThreshold: form.lowStockThreshold,
        outOfStockThreshold: form.outOfStockThreshold,
        enabled: form.enabled,
        remark: form.remark,
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch {} finally {
    submitting.value = false
  }
}

async function handleToggleEnabled(row: StockAlertTemplate) {
  try {
    await updateStockAlertTemplate(row.id, { enabled: row.enabled === 1 ? 0 : 1 })
    ElMessage.success(row.enabled === 1 ? '已禁用' : '已启用')
    fetchData()
  } catch {}
}

async function handleDelete(id: number) {
  try {
    await deleteStockAlertTemplate(id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {}
}
</script>
