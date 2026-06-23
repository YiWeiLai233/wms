<template>
  <div class="page-container">
    <PageHeader title="库存查询" subtitle="实时库存数据">
      <template #actions>
        <el-button type="primary" icon="Plus" @click="openInboundDialog">入库</el-button>
      </template>
    </PageHeader>

    <div class="card mb-4">
      <el-form :model="searchParams" inline>
        <el-form-item label="商品名称">
          <el-input v-model="searchParams.productName" placeholder="商品名称" clearable style="width: 150px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="SKU编码">
          <el-input v-model="searchParams.skuCode" placeholder="SKU编码" clearable style="width: 130px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="仓库">
          <el-select v-model="searchParams.warehouseId" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="库存状态">
          <el-select v-model="searchParams.stockType" placeholder="全部" clearable style="width: 120px">
            <el-option label="正常" value="normal" />
            <el-option label="低库存" value="low" />
            <el-option label="缺货" value="out" />
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
        <el-table-column prop="productName" label="商品名称" min-width="100" show-overflow-tooltip />
        <el-table-column prop="skuCode" label="SKU编码" width="130" />
        <el-table-column prop="skuName" label="SKU名称" min-width="100" show-overflow-tooltip />
        <el-table-column prop="warehouseName" label="仓库" width="120" />
        <el-table-column prop="availableQty" label="可用库存" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStockAlertTagType(row.stockAlertStatus)" size="small">
              {{ row.availableQty ?? row.quantity }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="stockAlertStatusName" label="库存状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStockAlertTagType(row.stockAlertStatus)" size="small" effect="dark">
              {{ row.stockAlertStatusName || '库存正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="物理库存" width="90" align="center">
          <template #default="{ row }">
            <span>{{ row.quantity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="lockedQty" label="锁定" width="80" align="center">
          <template #default="{ row }">
            <span class="text-orange-500">{{ row.lockedQty }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
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

    <el-dialog v-model="inboundDialogVisible" title="库存入库" width="520px" destroy-on-close>
      <el-form ref="inboundFormRef" :model="inboundForm" :rules="inboundRules" label-width="80px">
        <el-form-item label="商品" prop="productId">
          <el-select
            v-model="inboundForm.productId"
            placeholder="搜索商品"
            filterable
            remote
            :remote-method="searchProducts"
            :loading="productLoading"
            style="width: 100%"
            @change="handleInboundProductChange"
          >
            <el-option v-for="p in productOptions" :key="p.id" :label="`${p.spuCode} - ${p.name}`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="SKU" prop="skuId">
          <el-select v-model="inboundForm.skuId" placeholder="选择SKU" filterable style="width: 100%" :disabled="!inboundForm.productId">
            <el-option v-for="sku in skuOptions" :key="sku.id" :label="`${sku.skuCode} - ${sku.name}`" :value="sku.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库" prop="warehouseId">
          <el-select v-model="inboundForm.warehouseId" placeholder="选择仓库" style="width: 100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="inboundForm.quantity" :min="1" :max="999999" style="width: 180px" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="inboundForm.remark" type="textarea" :rows="2" placeholder="入库说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inboundDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="inboundSubmitting" @click="handleInboundSubmit">确认入库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { adjustStock, queryStock } from '@/api/stock'
import type { StockItem } from '@/api/stock'
import { getProductList, getSkuList } from '@/api/product'
import type { Product, Sku } from '@/api/product'
import { getWarehouseList } from '@/api/warehouse'
import type { Warehouse } from '@/api/warehouse'
import { useTable } from '@/composables/useTable'
import { formatDateTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'

const route = useRoute()
const { tableData, loading, pagination, searchParams, handleSearch, handleReset, handlePageChange, handleSizeChange, fetchData } = useTable<StockItem>(queryStock)

const warehouses = ref<Warehouse[]>([])
const productOptions = ref<Product[]>([])
const skuOptions = ref<Sku[]>([])
const productLoading = ref(false)

// 库存预警标签类型
function getStockAlertTagType(status: string): string {
  if (status === 'OUT_OF_STOCK') return 'danger'
  if (status === 'LOW_STOCK') return 'warning'
  return 'success'
}

const inboundDialogVisible = ref(false)
const inboundSubmitting = ref(false)
const inboundFormRef = ref<FormInstance>()
const inboundForm = reactive({
  productId: undefined as number | undefined,
  skuId: undefined as number | undefined,
  warehouseId: undefined as number | undefined,
  quantity: 1,
  remark: '',
})

const inboundRules: FormRules = {
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  skuId: [{ required: true, message: '请选择SKU', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'change' }],
}

onMounted(async () => {
  await Promise.all([loadWarehouses(), searchProducts('')])
  // 从 URL 参数预填仓库筛选
  const queryWarehouseId = route.query.warehouseId
  if (queryWarehouseId) {
    searchParams.warehouseId = Number(queryWarehouseId)
    handleSearch()
  }
})

async function loadWarehouses() {
  try {
    const res = await getWarehouseList({ page: 1, size: 100 })
    warehouses.value = res.data.list || []
  } catch {
    warehouses.value = []
  }
}

async function searchProducts(keyword: string) {
  productLoading.value = true
  try {
    const res = await getProductList({ page: 1, size: 50, keyword: keyword || undefined })
    productOptions.value = res.data.list || []
  } catch {
    productOptions.value = []
  } finally {
    productLoading.value = false
  }
}

function resetInboundForm() {
  Object.assign(inboundForm, {
    productId: undefined,
    skuId: undefined,
    warehouseId: undefined,
    quantity: 1,
    remark: '',
  })
  skuOptions.value = []
}

function openInboundDialog() {
  resetInboundForm()
  inboundDialogVisible.value = true
}

async function handleInboundProductChange(productId: number) {
  inboundForm.skuId = undefined
  try {
    const res = await getSkuList(productId)
    skuOptions.value = res.data || []
  } catch {
    skuOptions.value = []
  }
}

async function handleInboundSubmit() {
  const valid = await inboundFormRef.value?.validate().catch(() => false)
  if (!valid || !inboundForm.skuId || !inboundForm.warehouseId) return

  inboundSubmitting.value = true
  try {
    await adjustStock({
      skuId: inboundForm.skuId,
      warehouseId: inboundForm.warehouseId,
      quantity: inboundForm.quantity,
      remark: inboundForm.remark || '入库',
    })
    ElMessage.success('入库成功')
    inboundDialogVisible.value = false
    fetchData()
  } catch {} finally {
    inboundSubmitting.value = false
  }
}
</script>
