<template>
  <div class="page-container">
    <PageHeader title="SKU 管理">
      <template #actions>
        <el-button type="primary" icon="Plus" @click="openDialog()">新增 SKU</el-button>
      </template>
    </PageHeader>

    <div class="card mb-4">
      <el-form :model="searchParams" inline>
        <el-form-item label="关键词">
          <el-input v-model="searchParams.keyword" placeholder="SKU编码/名称" clearable style="width: 200px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="仓库">
          <el-select v-model="searchParams.warehouseId" placeholder="全部仓库" clearable style="width: 140px" @change="handleSearch">
            <el-option label="全部仓库" :value="undefined" />
            <el-option v-for="w in warehouseOptions" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
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

    <div class="card">
      <el-table :data="skuMatrixRows" v-loading="loading" stripe border>
        <el-table-column prop="skuCode" label="SKU 编码" width="150" show-overflow-tooltip />
        <el-table-column prop="skuName" label="SKU 名称" min-width="150" show-overflow-tooltip />
        <el-table-column label="仓库" width="120" align="center">
          <template #default>
            <el-tag v-if="searchParams.warehouseId" type="primary" size="small">
              {{ warehouseOptions.find(w => w.id === searchParams.warehouseId)?.name }}
            </el-tag>
            <el-tag v-else type="info" size="small">全部</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-for="size in sizeColumns" :key="size" :label="size" width="72" align="center">
          <template #default="{ row }">
            <el-tag v-if="getSizeStock(row, size) !== undefined" :type="getStockTagType(getSizeStock(row, size), getSizeSku(row, size)?.lowStockThreshold, getSizeSku(row, size)?.outOfStockThreshold)" size="small">
              {{ getSizeStock(row, size) }}
            </el-tag>
            <span v-else class="text-gray-400">-</span>
          </template>
        </el-table-column>
        <el-table-column label="货架" width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ getMatrixShelf(row) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link icon="View" @click="openMatrixDetail(row)">详情</el-button>
            <el-button type="success" link icon="Plus" @click="openAddSkuForProduct(row)">加码数</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[50, 100, 200]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑 SKU' : '新增 SKU'" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
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
        <el-form-item label="商品货架">
          <el-input :model-value="selectedProductShelf" disabled placeholder="选择商品后自动填入" />
        </el-form-item>
        <el-form-item label="SKU 编码" prop="skuCode">
          <el-input v-model="form.skuCode" placeholder="如 SKU001-42" />
        </el-form-item>
        <el-form-item label="SKU 名称" prop="name">
          <el-input v-model="form.name" placeholder="如 拖鞋-黑色-42码" />
        </el-form-item>
        <el-form-item label="码数">
          <el-input v-model="form.sizeValue" placeholder="如 42" />
        </el-form-item>

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

        <template v-if="!isEdit">
          <el-divider content-position="left">初始入库</el-divider>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="仓库" prop="warehouseId">
                <el-select v-model="form.warehouseId" placeholder="选择仓库" style="width: 100%" @change="handleWarehouseChange">
                  <el-option v-for="w in warehouseOptions" :key="w.id" :label="w.name" :value="w.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="货架" prop="shelfId">
                <el-select v-model="form.shelfId" placeholder="选择货架" style="width: 100%" :disabled="!form.warehouseId">
                  <el-option v-for="s in inboundShelfOptions" :key="s.id" :label="`${s.code} - ${s.name}`" :value="s.id" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="入库数量" prop="initialQuantity">
                <el-input-number v-model="form.initialQuantity" :min="0" :max="999999" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="入库备注">
            <el-input v-model="form.inboundRemark" type="textarea" :rows="2" placeholder="初始入库说明" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="matrixDetailVisible" :title="`${matrixDetailRow?.skuName || 'SKU'} 码数库存${searchParams.warehouseId ? ' - ' + warehouseOptions.find(w => w.id === searchParams.warehouseId)?.name : ''}`" width="820px">
      <el-table :data="matrixDetailRow?.skus || []" border size="small">
        <el-table-column prop="sizeValue" label="码数" width="80" align="center" />
        <el-table-column prop="skuCode" label="SKU编码" width="150" show-overflow-tooltip />
        <el-table-column prop="name" label="SKU名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="availableQty" label="库存数量" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStockTagType(row.availableQty, row.lowStockThreshold, row.outOfStockThreshold)" size="small">{{ row.availableQty ?? 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button type="success" link icon="Box" @click="openInboundDialog(row)">入库</el-button>
            <el-button type="primary" link icon="Edit" @click="openDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除该码数 SKU 吗？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="inboundDialogVisible" title="SKU 入库" width="520px" destroy-on-close>
      <el-descriptions :column="3" border class="mb-4" size="small">
        <el-descriptions-item label="SKU编码">{{ inboundSku?.skuCode }}</el-descriptions-item>
        <el-descriptions-item label="码数">{{ inboundSku?.sizeValue || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前可用">{{ inboundSku?.availableQty ?? 0 }}</el-descriptions-item>
      </el-descriptions>
      <el-form ref="inboundFormRef" :model="inboundForm" :rules="inboundRules" label-width="80px">
        <el-form-item label="仓库" prop="warehouseId">
          <el-select v-model="inboundForm.warehouseId" placeholder="选择仓库" style="width: 100%">
            <el-option v-for="w in warehouseOptions" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入库数量" prop="quantity">
          <el-input-number v-model="inboundForm.quantity" :min="1" :max="999999" style="width: 180px" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="inboundForm.remark" type="textarea" :rows="2" placeholder="入库说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inboundDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="inbounding" @click="handleInbound">确认入库</el-button>
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
import { adjustStock } from '@/api/stock'
import { getShelfList, getWarehouseList } from '@/api/warehouse'
import type { Warehouse, WarehouseShelf } from '@/api/warehouse'
import { useTable } from '@/composables/useTable'
import PageHeader from '@/components/PageHeader.vue'

type SkuListItem = Sku & { productName?: string; shelfCode?: string; categoryName?: string }

interface SkuMatrixRow {
  productId: number
  skuCode: string
  skuName: string
  shelfCode?: string
  categoryName?: string
  firstSku: SkuListItem
  skus: SkuListItem[]
  sizeMap: Record<string, SkuListItem>
}

const { tableData, loading, pagination, searchParams, handleSearch, handleReset, handlePageChange, handleSizeChange, fetchData } = useTable<SkuListItem>(getAllSkuList)

pagination.size = 50

const productOptions = ref<Product[]>([])
const warehouseOptions = ref<Warehouse[]>([])
const shelfList = ref<WarehouseShelf[]>([])
const inboundShelfOptions = ref<WarehouseShelf[]>([])

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
  sizeValue: '',
  quantity: 0,
  availableQty: 0,
  lockedQty: 0,
  totalQty: 0,
  initialQuantity: 0,
  warehouseId: undefined,
  inboundRemark: '',
  weight: 0,
  status: 1,
})

const requiresInboundTarget = () => !isEdit.value && (form.initialQuantity ?? 0) > 0

const rules: FormRules = {
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  skuCode: [{ required: true, message: '请输入SKU编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入SKU名称', trigger: 'blur' }],
  warehouseId: [{
    validator: (_rule, value, callback) => {
      if (requiresInboundTarget() && !value) callback(new Error('请选择仓库'))
      else callback()
    },
    trigger: 'change',
  }],
  shelfId: [{
    validator: (_rule, value, callback) => {
      if (requiresInboundTarget() && !value) callback(new Error('请选择货架'))
      else callback()
    },
    trigger: 'change',
  }],
}

const selectedProductShelf = computed(() => {
  if (!form.productId) return ''
  const product = productOptions.value.find((p) => p.id === form.productId)
  if (!product) return ''
  const shelf = shelfList.value.find((s) => s.id === product.shelfId)
  return shelf ? `${shelf.code} - ${shelf.name} (${shelf.categoryName})` : product.shelfCode || ''
})

const sizeColumns = computed(() => {
  const sizes = tableData.value
    .map((sku) => normalizeSizeValue(sku.sizeValue))
    .filter(Boolean)
  return Array.from(new Set(sizes)).sort(compareSizeValue)
})

const skuMatrixRows = computed(() => {
  const rowMap = new Map<string, SkuMatrixRow>()

  tableData.value.forEach((sku) => {
    const key = String(sku.productId || getBaseSkuCode(sku))
    let row = rowMap.get(key)
    if (!row) {
      row = {
        productId: sku.productId,
        skuCode: getBaseSkuCode(sku),
        skuName: getBaseSkuName(sku),
        shelfCode: sku.shelfCode,
        categoryName: sku.categoryName,
        firstSku: sku,
        skus: [],
        sizeMap: {},
      }
      rowMap.set(key, row)
    }

    row.skus.push(sku)
    const size = normalizeSizeValue(sku.sizeValue)
    if (size) {
      row.sizeMap[size] = sku
    }
  })

  return Array.from(rowMap.values()).map((row) => ({
    ...row,
    skus: row.skus.slice().sort((a, b) => compareSizeValue(normalizeSizeValue(a.sizeValue), normalizeSizeValue(b.sizeValue))),
  }))
})

function getStockTagType(quantity?: number, lowThreshold?: number, outThreshold?: number): string {
  const value = quantity ?? 0
  const low = lowThreshold ?? 10
  const out = outThreshold ?? 0
  if (value <= out) return 'danger'
  if (value <= low) return 'warning'
  return 'success'
}

function normalizeSizeValue(sizeValue?: string | number) {
  return String(sizeValue ?? '').trim()
}

function compareSizeValue(a: string, b: string) {
  const aNumber = Number(a)
  const bNumber = Number(b)
  if (Number.isFinite(aNumber) && Number.isFinite(bNumber)) {
    return aNumber - bNumber
  }
  return a.localeCompare(b, 'zh-CN', { numeric: true })
}

function stripSizeSuffix(value: string | undefined, sizeValue: string) {
  const text = String(value || '').trim()
  if (!text || !sizeValue) return text
  const suffixes = [`-${sizeValue}`, `_${sizeValue}`, sizeValue]
  const suffix = suffixes.find((item) => text.endsWith(item))
  return suffix ? text.slice(0, -suffix.length).replace(/[-_\s]+$/, '') : text
}

function getBaseSkuCode(sku: SkuListItem) {
  const size = normalizeSizeValue(sku.sizeValue)
  return stripSizeSuffix(sku.skuCode, size) || sku.skuCode
}

function getBaseSkuName(sku: SkuListItem) {
  const size = normalizeSizeValue(sku.sizeValue)
  return sku.productName || stripSizeSuffix(sku.name, size) || sku.name
}

function getSizeStock(row: SkuMatrixRow, sizeValue: string) {
  const sku = row.sizeMap[sizeValue]
  if (!sku) return undefined
  return sku.availableQty ?? sku.quantity ?? 0
}

function getSizeSku(row: SkuMatrixRow, sizeValue: string) {
  return row.sizeMap[sizeValue]
}

function getMatrixShelf(row: SkuMatrixRow) {
  const shelfName = getShelfName(row.firstSku.shelfId, row.shelfCode)
  const shelfCode = row.shelfCode || '-'
  return shelfName && shelfName !== '-' ? `${shelfCode} - ${shelfName}` : shelfCode
}

async function handleProductChange(productId: number) {
  const product = productOptions.value.find((p) => p.id === productId)
  if (!product?.shelfId) return
  const shelf = shelfList.value.find((s) => s.id === product.shelfId)
  if (!shelf) return
  form.warehouseId = shelf.warehouseId
  form.shelfId = shelf.id
  inboundShelfOptions.value = shelfList.value.filter((s) => s.warehouseId === shelf.warehouseId)
}

function getShelfName(shelfId?: number, shelfCode?: string) {
  if (shelfId) {
    return shelfList.value.find((s) => s.id === shelfId)?.name || '-'
  }
  if (shelfCode) {
    return shelfList.value.find((s) => s.code === shelfCode)?.name || '-'
  }
  return '-'
}

onMounted(async () => {
  try {
    const res = await getProductList({ page: 1, size: 100 })
    productOptions.value = res.data.list || []
  } catch {}

  try {
    const wRes = await getWarehouseList({ page: 1, size: 100 })
    warehouseOptions.value = (wRes.data.list || []).filter((w: any) => w.warehouseType === 'NORMAL')
    for (const w of warehouseOptions.value) {
      try {
        const sRes = await getShelfList(w.id)
        shelfList.value.push(...(sRes.data || []))
      } catch {}
    }
  } catch {}
})

async function handleWarehouseChange(warehouseId: number) {
  form.shelfId = undefined
  try {
    const res = await getShelfList(warehouseId)
    inboundShelfOptions.value = res.data || []
  } catch {
    inboundShelfOptions.value = []
  }
}

function resetInboundOptions() {
  inboundShelfOptions.value = []
}

function openDialog(row?: SkuListItem) {
  isEdit.value = !!row
  resetInboundOptions()
  if (row) {
    Object.assign(form, {
      ...row,
      initialQuantity: 0,
      warehouseId: undefined,
      inboundRemark: '',
    })
  } else {
    Object.assign(form, {
      id: undefined,
      productId: undefined,
      shelfId: undefined,
      skuCode: '',
      name: '',
      sizeValue: '',
      quantity: 0,
      availableQty: 0,
      lockedQty: 0,
      totalQty: 0,
      initialQuantity: 0,
      warehouseId: undefined,
      inboundRemark: '',
      weight: 0,
      status: 1,
    })
  }
  dialogVisible.value = true
}

const matrixDetailVisible = ref(false)
const matrixDetailRow = ref<SkuMatrixRow>()

function openMatrixDetail(row: SkuMatrixRow) {
  matrixDetailRow.value = row
  matrixDetailVisible.value = true
}

function openAddSkuForProduct(row: SkuMatrixRow) {
  openDialog()
  form.productId = row.productId
  form.skuCode = row.skuCode ? `${row.skuCode}-` : ''
  form.name = row.skuName ? `${row.skuName}-` : ''
  handleProductChange(row.productId)
}

const inboundDialogVisible = ref(false)
const inbounding = ref(false)
const inboundSku = ref<SkuListItem>()
const inboundFormRef = ref<FormInstance>()
const inboundForm = reactive({
  warehouseId: undefined as number | undefined,
  quantity: 1,
  remark: '',
})

const inboundRules: FormRules = {
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入入库数量', trigger: 'change' }],
}

function openInboundDialog(row: SkuListItem) {
  inboundSku.value = row
  Object.assign(inboundForm, {
    warehouseId: undefined,
    quantity: 1,
    remark: '',
  })
  inboundDialogVisible.value = true
}

async function handleInbound() {
  const valid = await inboundFormRef.value?.validate().catch(() => false)
  if (!valid || !inboundSku.value) return

  inbounding.value = true
  try {
    await adjustStock({
      skuId: inboundSku.value.id,
      warehouseId: inboundForm.warehouseId!,
      quantity: inboundForm.quantity,
      remark: inboundForm.remark || `SKU ${inboundSku.value.skuCode} 入库`,
    })
    ElMessage.success('入库成功')
    inboundDialogVisible.value = false
    // 立即更新详情弹窗中的库存数量
    if (inboundSku.value) {
      const skuInDetail = matrixDetailRow.value?.skus.find((s) => s.id === inboundSku.value!.id)
      if (skuInDetail) {
        skuInDetail.availableQty = (skuInDetail.availableQty ?? 0) + inboundForm.quantity
      }
    }
    await fetchData()
  } catch {} finally {
    inbounding.value = false
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      const payload = {
        id: form.id,
        productId: form.productId,
        skuCode: form.skuCode,
        name: form.name,
        sizeValue: form.sizeValue,
        weight: form.weight,
        volume: form.volume,
        image: form.image,
        status: form.status,
      }
      await updateSku(payload)
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
    matrixDetailVisible.value = false
    fetchData()
  } catch {}
}
</script>
