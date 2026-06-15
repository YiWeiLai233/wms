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
        <el-table-column label="图片" width="60" align="center">
          <template #default="{ row }">
            <ImagePreview :src="row.mainImage" />
          </template>
        </el-table-column>
        <el-table-column prop="spuCode" label="SPU编码" width="120" />
        <el-table-column prop="name" label="商品名称" width="150" show-overflow-tooltip />
        <el-table-column label="仓库" width="100">
          <template #default="{ row }">{{ row.warehouseName || '-' }}</template>
        </el-table-column>
        <el-table-column label="货架" width="140">
          <template #default="{ row }">
            <div>{{ row.shelfCode }} - {{ row.shelfName || '-' }}</div>
            <div class="text-xs text-gray-400">¥{{ row.price?.toFixed(2) || '0.00' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="80" />
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
        <el-table-column label="操作" width="240" fixed="right">
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
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑商品' : '新增商品'" width="980px" destroy-on-close>
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
            <el-form-item label="所属仓库" prop="warehouseId">
              <el-select v-model="form.warehouseId" placeholder="选择仓库" style="width: 100%" @change="handleWarehouseChange">
                <el-option v-for="w in warehouseOptions" :key="w.id" :label="w.name" :value="w.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="货架号" prop="shelfId">
              <el-select v-model="form.shelfId" placeholder="选择货架" style="width: 100%" @change="handleShelfChange">
                <el-option v-for="s in filteredShelfList" :key="s.id" :label="`${s.code} - ${s.name} (${s.categoryName})`" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="分类">
              <el-input :model-value="selectedShelf?.categoryName || ''" disabled placeholder="选择货架后自动填充" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="参考售价" prop="price">
              <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="商品主图">
          <ImageUpload v-model="form.mainImage" placeholder="上传商品图片" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="商品描述" />
        </el-form-item>
        <el-form-item label="预警模板">
          <el-select v-model="form.alertTemplateId" placeholder="选择预警模板（可选）" clearable style="width: 100%">
            <el-option
              v-for="t in alertTemplateOptions"
              :key="t.id"
              :label="`${t.name} (低库存:${t.lowStockThreshold} 缺货:${t.outOfStockThreshold})`"
              :value="t.id"
            />
          </el-select>
          <div class="text-xs text-gray-400 mt-1">选择模板后，新建的SKU将自动应用该模板的预警阈值</div>
        </el-form-item>

        <template v-if="!isEdit">
          <el-divider content-position="left">码数 SKU</el-divider>
          <div class="flex gap-2 mb-3 items-center">
            <span class="text-sm text-gray-500">码数范围</span>
            <el-input v-model="sizeRangeText" placeholder="如 30-40" clearable style="width: 160px" @keyup.enter="generateSizeSkus" />
            <el-button icon="Grid" @click="generateSizeSkus">生成码数</el-button>
            <el-button icon="Plus" @click="addSizeSku()">添加码数</el-button>
          </div>
          <el-table :data="form.skuList || []" border size="small" max-height="300">
            <el-table-column label="SKU编码" width="150" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.skuCode || buildSkuCode(row.sizeValue) || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="SKU名称" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.name || buildSkuName(row.sizeValue) || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="码数" width="100">
              <template #default="{ row }">
                <el-input v-model="row.sizeValue" placeholder="42" @change="syncSkuRow(row)" />
              </template>
            </el-table-column>
            <el-table-column label="数量" width="120">
              <template #default="{ row }">
                <el-input-number v-model="row.quantity" :min="0" :max="999999" controls-position="right" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ $index }">
                <el-button type="danger" link icon="Delete" @click="removeSizeSku($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
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
        <el-descriptions-item label="仓库">{{ detail.warehouseName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="货架号">{{ detail.shelfCode }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ detail.categoryName }}</el-descriptions-item>
        <el-descriptions-item label="参考售价">¥{{ detail.price?.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 1 ? 'success' : 'danger'" size="small">
            {{ detail.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预警模板" :span="2">
          <el-tag v-if="detail.alertTemplateName" type="success" size="small">
            {{ detail.alertTemplateName }}
          </el-tag>
          <span v-else class="text-gray-400">未设置</span>
        </el-descriptions-item>
      </el-descriptions>

      <h4 class="mt-4 mb-2 text-sm font-semibold text-gray-700">SKU 列表</h4>
      <el-table :data="detail.skuList || []" border size="small">
        <el-table-column prop="skuCode" label="SKU编码" width="120" />
        <el-table-column prop="name" label="SKU名称" min-width="100" />
        <el-table-column prop="sizeValue" label="码数" width="80" align="center" />
        <el-table-column prop="availableQty" label="可用" width="80" align="center" />
        <el-table-column prop="lockedQty" label="锁定" width="80" align="center" />
        <el-table-column prop="totalQty" label="总库存" width="80" align="center" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getProductList, getProductDetail, createProduct, updateProduct, deleteProduct } from '@/api/product'
import type { Product, ProductSizeSku } from '@/api/product'
import { getWarehouseList, getShelfList } from '@/api/warehouse'
import type { Warehouse, WarehouseShelf } from '@/api/warehouse'
import { getStockAlertTemplateOptions } from '@/api/stockAlertTemplate'
import type { StockAlertTemplate } from '@/api/stockAlertTemplate'
import { useTable } from '@/composables/useTable'
import { formatDateTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import ImagePreview from '@/components/ImagePreview.vue'
import ImageUpload from '@/components/ImageUpload.vue'

const { tableData, loading, pagination, searchParams, handleSearch, handleReset, handlePageChange, handleSizeChange, fetchData } = useTable<Product>(getProductList)

// 仓库列表
const warehouseOptions = ref<Warehouse[]>([])

// 货架列表
const shelfList = ref<WarehouseShelf[]>([])
const selectedShelf = computed(() => shelfList.value.find((s) => s.id === form.shelfId))

// 货架列表（已根据仓库过滤）
const filteredShelfList = computed(() => shelfList.value)

// 预警模板列表
const alertTemplateOptions = ref<StockAlertTemplate[]>([])
const selectedTemplate = computed(() => alertTemplateOptions.value.find((t) => t.id === form.alertTemplateId))

function getShelfName(shelfId: number) {
  return shelfList.value.find((s) => s.id === shelfId)?.name || '-'
}

const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const detail = ref<any>({})

const form = reactive<Partial<Product> & { skuList: ProductSizeSku[] }>({
  id: undefined,
  spuCode: '',
  name: '',
  warehouseId: undefined,
  shelfId: undefined,
  shelfCode: '',
  categoryName: '',
  price: 0,
  description: '',
  status: 1,
  alertTemplateId: undefined,
  skuList: [],
})

const sizeRangeText = ref('30-40')

const rules: FormRules = {
  spuCode: [{ required: true, message: '请输入SPU编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  shelfId: [{ required: true, message: '请选择货架', trigger: 'change' }],
}

onMounted(async () => {
  // 加载仓库列表
  try {
    const wRes = await getWarehouseList({ page: 1, size: 100 })
    warehouseOptions.value = (wRes.data.list || []).filter((w: any) => w.warehouseType === 'NORMAL')
  } catch {}

  // 加载预警模板选项
  try {
    const tRes = await getStockAlertTemplateOptions()
    alertTemplateOptions.value = tRes.data || []
  } catch {}
})

async function handleWarehouseChange() {
  // 切换仓库时清空货架选择
  form.shelfId = undefined
  form.shelfCode = ''
  form.categoryName = ''
  // 加载该仓库的货架列表
  if (form.warehouseId) {
    try {
      const res = await getShelfList(form.warehouseId)
      shelfList.value = res.data || []
    } catch {
      shelfList.value = []
    }
  } else {
    shelfList.value = []
  }
}

function handleShelfChange(shelfId: number) {
  const shelf = shelfList.value.find((s) => s.id === shelfId)
  if (shelf) {
    form.shelfCode = shelf.code
    form.categoryName = shelf.categoryName
  }
}

async function openDialog(row?: Product) {
  isEdit.value = !!row
  if (row) {
    Object.assign(form, { ...row, skuList: [] })
    // 编辑时加载该仓库的货架列表
    if (row.warehouseId) {
      try {
        const res = await getShelfList(row.warehouseId)
        shelfList.value = res.data || []
      } catch {
        shelfList.value = []
      }
    }
  } else {
    Object.assign(form, {
      id: undefined,
      spuCode: '',
      name: '',
      warehouseId: undefined,
      shelfId: undefined,
      shelfCode: '',
      categoryName: '',
      price: 0,
      description: '',
      status: 1,
      alertTemplateId: undefined,
      skuList: [],
    })
    generateSizeSkus()
  }
  dialogVisible.value = true
}

function normalizeSizeValue(sizeValue?: string | number) {
  return String(sizeValue ?? '').trim()
}

function buildSkuCode(sizeValue?: string | number) {
  const spuCode = String(form.spuCode || '').trim()
  const size = normalizeSizeValue(sizeValue)
  return spuCode && size ? `${spuCode}-${size}` : ''
}

function buildSkuName(sizeValue?: string | number) {
  const productName = String(form.name || '').trim()
  const size = normalizeSizeValue(sizeValue)
  return productName && size ? `${productName}-${size}` : ''
}

function syncSkuRow(row: ProductSizeSku) {
  row.sizeValue = normalizeSizeValue(row.sizeValue)
  row.skuCode = buildSkuCode(row.sizeValue)
  row.name = buildSkuName(row.sizeValue)
}

function syncSkuRows() {
  ;(form.skuList || []).forEach(syncSkuRow)
}

function createSizeSku(sizeValue = ''): ProductSizeSku {
  const sku: ProductSizeSku = {
    sizeValue,
    quantity: 0,
  }
  syncSkuRow(sku)
  return sku
}

function parseSizeRange(value: string) {
  const text = value.trim()
  const rangeMatch = text.match(/^(\d+)\s*[-~～]\s*(\d+)$/)
  if (rangeMatch) {
    const start = Number(rangeMatch[1])
    const end = Number(rangeMatch[2])
    const min = Math.min(start, end)
    const max = Math.max(start, end)
    return Array.from({ length: max - min + 1 }, (_, index) => String(min + index))
  }

  return text
    .split(/[,，\s]+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function generateSizeSkus() {
  const sizeValues = parseSizeRange(sizeRangeText.value)
  if (!sizeValues.length) {
    ElMessage.warning('请输入码数范围，如 30-40')
    return
  }
  form.skuList = sizeValues.map((sizeValue) => createSizeSku(sizeValue))
}

function addSizeSku(sizeValue = '') {
  form.skuList.push(createSizeSku(sizeValue))
}

function removeSizeSku(index: number) {
  form.skuList.splice(index, 1)
}

watch(() => [form.spuCode, form.name], () => syncSkuRows())

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
      syncSkuRows()
      const skuList = (form.skuList || []).filter((sku) => String(sku.sizeValue || '').trim())
      await createProduct({ ...form, skuList })
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
