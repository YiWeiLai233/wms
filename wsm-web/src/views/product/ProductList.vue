<template>
  <div class="page-container">
    <PageHeader title="商品管理">
      <template #actions>
        <el-button type="primary" icon="Plus" @click="openDialog()">新增商品</el-button>
      </template>
    </PageHeader>

    <!-- 搜索栏 -->
    <div class="card mb-4">
      <el-form :model="searchParams" inline>
        <el-form-item label="关键词">
          <el-input v-model="searchParams.keyword" placeholder="名称/编码" clearable style="width: 200px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="货架">
          <el-select v-model="searchParams.shelfId" placeholder="全部货架" clearable style="width: 150px">
            <el-option v-for="s in shelfList" :key="s.id" :label="`${s.code} - ${s.name}`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchParams.status" placeholder="全部" clearable style="width: 100px">
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
        <el-table-column prop="spuCode" label="SPU编码" width="120" />
        <el-table-column prop="name" label="商品名称" min-width="180" />
        <el-table-column label="货架" width="140">
          <template #default="{ row }">{{ row.shelfCode }} - {{ getShelfName(row.shelfId) }}</template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="100" />
        <el-table-column prop="price" label="参考售价" width="100" align="right">
          <template #default="{ row }">¥{{ row.price?.toFixed(2) || '0.00' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link icon="View" @click="viewDetail(row)">详情</el-button>
            <el-button type="primary" link icon="Edit" @click="openDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除该商品吗？" @confirm="handleDelete(row.id)">
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
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑商品' : '新增商品'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="SPU编码" prop="spuCode">
              <el-input v-model="form.spuCode" placeholder="如 SPU001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="form.name" placeholder="商品名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="货架号" prop="shelfId">
              <el-select v-model="form.shelfId" placeholder="选择货架" style="width: 100%" @change="handleShelfChange">
                <el-option v-for="s in shelfList" :key="s.id" :label="`${s.code} - ${s.name} (${s.categoryName})`" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类">
              <el-input :model-value="selectedShelf?.categoryName || ''" disabled placeholder="选择货架后自动填充" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="参考售价" prop="price">
              <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="商品描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="商品详情" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="SPU编码">{{ detail.spuCode }}</el-descriptions-item>
        <el-descriptions-item label="商品名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="货架号">{{ detail.shelfCode }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ detail.categoryName }}</el-descriptions-item>
        <el-descriptions-item label="参考售价">¥{{ detail.price?.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 1 ? 'success' : 'danger'" size="small">
            {{ detail.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
      </el-descriptions>

      <h4 class="mt-4 mb-2 text-sm font-semibold text-gray-700">SKU 列表</h4>
      <el-table :data="detail.skuList || []" border size="small">
        <el-table-column prop="skuCode" label="SKU编码" width="120" />
        <el-table-column prop="name" label="SKU名称" min-width="150" />
        <el-table-column prop="quantity" label="数量" width="80" align="center" />
        <el-table-column prop="costPrice" label="成本价" width="90" align="right">
          <template #default="{ row }">¥{{ row.costPrice?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="salePrice" label="售价" width="90" align="right">
          <template #default="{ row }">¥{{ row.salePrice?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="weight" label="重量(kg)" width="90" align="center" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getProductList, getProductDetail, createProduct, updateProduct, deleteProduct } from '@/api/product'
import type { Product } from '@/api/product'
import { getWarehouseList, getShelfList } from '@/api/warehouse'
import type { Warehouse, WarehouseShelf } from '@/api/warehouse'
import { useTable } from '@/composables/useTable'
import { formatDateTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'

const { tableData, loading, pagination, searchParams, handleSearch, handleReset, handlePageChange, handleSizeChange, fetchData } = useTable<Product>(getProductList)

// 货架列表
const shelfList = ref<WarehouseShelf[]>([])
const selectedShelf = computed(() => shelfList.value.find((s) => s.id === form.shelfId))

function getShelfName(shelfId: number) {
  return shelfList.value.find((s) => s.id === shelfId)?.name || '-'
}

const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const detail = ref<any>({})

const form = reactive<Partial<Product>>({
  id: undefined,
  spuCode: '',
  name: '',
  shelfId: undefined,
  shelfCode: '',
  categoryName: '',
  price: 0,
  description: '',
  status: 1,
})

const rules: FormRules = {
  spuCode: [{ required: true, message: '请输入SPU编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  shelfId: [{ required: true, message: '请选择货架', trigger: 'change' }],
}

onMounted(async () => {
  // 加载所有仓库的货架
  try {
    const wRes = await getWarehouseList({ page: 1, size: 100 })
    const warehouses = wRes.data.list || []
    for (const w of warehouses) {
      try {
        const sRes = await getShelfList(w.id)
        shelfList.value.push(...(sRes.data || []))
      } catch {}
    }
  } catch {}
})

function handleShelfChange(shelfId: number) {
  const shelf = shelfList.value.find((s) => s.id === shelfId)
  if (shelf) {
    form.shelfCode = shelf.code
    form.categoryName = shelf.categoryName
  }
}

function openDialog(row?: Product) {
  isEdit.value = !!row
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, {
      id: undefined,
      spuCode: '',
      name: '',
      shelfId: undefined,
      shelfCode: '',
      categoryName: '',
      price: 0,
      description: '',
      status: 1,
    })
  }
  dialogVisible.value = true
}

async function viewDetail(row: Product) {
  try {
    const res = await getProductDetail(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch {}
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateProduct(form)
      ElMessage.success('修改成功')
    } else {
      await createProduct(form)
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
    await deleteProduct(id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {}
}
</script>
