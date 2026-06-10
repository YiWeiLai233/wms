<template>
  <div class="page-container">
    <PageHeader title="SKU 管理">
      <template #actions>
        <el-button type="primary" icon="Plus" @click="openDialog()">新增 SKU</el-button>
      </template>
    </PageHeader>

    <!-- 搜索栏 -->
    <div class="card mb-4">
      <el-form :model="searchParams" inline>
        <el-form-item label="关键词">
          <el-input v-model="searchParams.keyword" placeholder="SKU编码/名称" clearable style="width: 200px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="货架">
          <el-select v-model="searchParams.shelfId" placeholder="全部货架" clearable style="width: 150px">
            <el-option v-for="s in shelfList" :key="s.id" :label="`${s.code} - ${s.categoryName}`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchParams.status" placeholder="全部" clearable style="width: 120px">
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

    <!-- 表格 -->
    <div class="card">
      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="skuCode" label="SKU 编码" width="130" show-overflow-tooltip />
        <el-table-column prop="name" label="SKU 名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="productName" label="所属商品" min-width="130" show-overflow-tooltip />
        <el-table-column label="货架" width="140">
          <template #default="{ row }">{{ row.shelfCode }} - {{ getShelfName(row.shelfId, row.shelfCode) }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" align="center">
          <template #default="{ row }">
            <span :class="{ 'text-red-500 font-bold': row.quantity <= 5 }">{{ row.quantity ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="costPrice" label="成本价" width="90" align="right">
          <template #default="{ row }">¥{{ row.costPrice?.toFixed(2) || '0.00' }}</template>
        </el-table-column>
        <el-table-column prop="salePrice" label="售价" width="90" align="right">
          <template #default="{ row }">¥{{ row.salePrice?.toFixed(2) || '0.00' }}</template>
        </el-table-column>
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
            <el-popconfirm title="确定删除该 SKU 吗？" @confirm="handleDelete(row.id)">
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
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑 SKU' : '新增 SKU'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item v-if="!isEdit" label="所属商品" prop="productId">
          <el-select v-model="form.productId" placeholder="请选择商品" filterable style="width: 100%" @change="handleProductChange">
            <el-option
              v-for="p in productOptions"
              :key="p.id"
              :label="`${p.spuCode} - ${p.name}`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="货架号">
          <el-input :model-value="selectedProductShelf" disabled placeholder="选择商品后自动填充" />
        </el-form-item>
        <el-form-item label="SKU 编码" prop="skuCode">
          <el-input v-model="form.skuCode" placeholder="如 SKU001-42" />
        </el-form-item>
        <el-form-item label="SKU 名称" prop="name">
          <el-input v-model="form.name" placeholder="如 拖鞋-黑色-42码" />
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="form.quantity" :min="0" placeholder="请输入数量" style="width: 100%" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="成本价" prop="costPrice">
              <el-input-number v-model="form.costPrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="售价" prop="salePrice">
              <el-input-number v-model="form.salePrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="重量(kg)" prop="weight">
              <el-input-number v-model="form.weight" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getAllSkuList, createSku, updateSku, deleteSku, getProductList } from '@/api/product'
import type { Sku, Product } from '@/api/product'
import { getWarehouseList, getShelfList } from '@/api/warehouse'
import type { WarehouseShelf } from '@/api/warehouse'
import { useTable } from '@/composables/useTable'
import PageHeader from '@/components/PageHeader.vue'

const { tableData, loading, pagination, searchParams, handleSearch, handleReset, handlePageChange, handleSizeChange, fetchData } = useTable<Sku>(getAllSkuList)

const productOptions = ref<Product[]>([])
const shelfList = ref<WarehouseShelf[]>([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<Partial<Sku>>({
  id: undefined,
  productId: undefined,
  shelfId: undefined,
  skuCode: '',
  name: '',
  quantity: 0,
  costPrice: 0,
  salePrice: 0,
  weight: 0,
  status: 1,
})

const rules: FormRules = {
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  skuCode: [{ required: true, message: '请输入SKU编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入SKU名称', trigger: 'blur' }],
}

// 选择商品后自动填充货架
const selectedProductShelf = computed(() => {
  if (!form.productId) return ''
  const product = productOptions.value.find((p) => p.id === form.productId)
  if (!product) return ''
  const shelf = shelfList.value.find((s) => s.id === product.shelfId)
  return shelf ? `${shelf.code} - ${shelf.name} (${shelf.categoryName})` : product.shelfCode || ''
})

function handleProductChange(productId: number) {
  const product = productOptions.value.find((p) => p.id === productId)
  if (product) {
    form.shelfId = product.shelfId
  }
}

function getShelfName(shelfId: number, shelfCode: string) {
  if (shelfId) {
    return shelfList.value.find((s) => s.id === shelfId)?.name || '-'
  }
  if (shelfCode) {
    return shelfList.value.find((s) => s.code === shelfCode)?.name || '-'
  }
  return '-'
}

onMounted(async () => {
  // 加载商品列表
  try {
    const res = await getProductList({ page: 1, size: 100 })
    productOptions.value = res.data.list || []
  } catch {}
  // 加载货架列表
  try {
    const wRes = await getWarehouseList({ page: 1, size: 100 })
    for (const w of wRes.data.list || []) {
      try {
        const sRes = await getShelfList(w.id)
        shelfList.value.push(...(sRes.data || []))
      } catch {}
    }
  } catch {}
})

function openDialog(row?: Sku) {
  isEdit.value = !!row
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, {
      id: undefined,
      productId: undefined,
      shelfId: undefined,
      skuCode: '',
      name: '',
      quantity: 0,
      costPrice: 0,
      salePrice: 0,
      weight: 0,
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
      await updateSku(form)
      ElMessage.success('修改成功')
    } else {
      await createSku(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch {} finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await deleteSku(id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {}
}
</script>
