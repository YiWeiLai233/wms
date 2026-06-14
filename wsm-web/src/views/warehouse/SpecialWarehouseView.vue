<template>
  <div class="page-container">
    <PageHeader title="特殊仓库管理" subtitle="次品仓 / 报废仓库存管理" />

    <div class="card mb-4">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="次品仓" name="DEFECTIVE" />
        <el-tab-pane label="报废仓" name="SCRAP" />
      </el-tabs>

      <el-form :model="searchParams" inline class="mt-3">
        <el-form-item label="SKU编码">
          <el-input v-model="searchParams.skuCode" placeholder="SKU编码" clearable style="width: 150px" @keyup.enter="fetchData" />
        </el-form-item>
        <el-form-item label="SKU名称">
          <el-input v-model="searchParams.skuName" placeholder="SKU名称" clearable style="width: 150px" @keyup.enter="fetchData" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="fetchData">搜索</el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <el-table :data="stockList" v-loading="loading" stripe border>
        <el-table-column prop="skuCode" label="SKU编码" width="130" />
        <el-table-column prop="skuName" label="SKU名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="productName" label="商品名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="warehouseName" label="所在仓库" width="120" />
        <el-table-column prop="quantity" label="可用数量" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.quantity > 0 ? 'warning' : 'info'" size="small">{{ row.quantity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>

        <!-- 次品仓操作 -->
        <el-table-column v-if="activeTab === 'DEFECTIVE'" label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="success" link icon="Check" :disabled="row.quantity <= 0" @click="openSellableDialog(row)">确认可售</el-button>
            <el-button type="warning" link icon="Right" :disabled="row.quantity <= 0" @click="openScrapDialog(row)">转报废仓</el-button>
          </template>
        </el-table-column>

        <!-- 报废仓操作 -->
        <el-table-column v-if="activeTab === 'SCRAP'" label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" link icon="Delete" :disabled="row.quantity <= 0" @click="openDisposeDialog(row)">确认报废</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && stockList.length === 0" :description="activeTab === 'DEFECTIVE' ? '次品仓暂无库存' : '报废仓暂无库存'" />
    </div>

    <!-- 确认可售弹窗 -->
    <el-dialog v-model="sellableDialogVisible" title="确认可售 - 转移到普通仓" width="450px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="商品">
          <span>{{ sellableTarget.skuName }} ({{ sellableTarget.skuCode }})</span>
        </el-form-item>
        <el-form-item label="可转数量">
          <span class="font-bold text-orange-500">{{ sellableTarget.maxQty }}</span>
        </el-form-item>
        <el-form-item label="转移数量" required>
          <el-input-number v-model="sellableTarget.transferQty" :min="1" :max="sellableTarget.maxQty" style="width: 100%" />
        </el-form-item>
        <el-form-item label="目标仓库" required>
          <el-select v-model="sellableTarget.warehouseId" placeholder="选择普通仓" style="width: 100%">
            <el-option v-for="w in normalWarehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sellableDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSellable">确认转移</el-button>
      </template>
    </el-dialog>

    <!-- 转报废仓弹窗 -->
    <el-dialog v-model="scrapDialogVisible" title="转入报废仓" width="420px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="商品">
          <span>{{ scrapTarget.skuName }} ({{ scrapTarget.skuCode }})</span>
        </el-form-item>
        <el-form-item label="可转数量">
          <span class="font-bold text-orange-500">{{ scrapTarget.maxQty }}</span>
        </el-form-item>
        <el-form-item label="转移数量" required>
          <el-input-number v-model="scrapTarget.transferQty" :min="1" :max="scrapTarget.maxQty" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scrapDialogVisible = false">取消</el-button>
        <el-button type="warning" :loading="submitting" @click="handleScrap">确认转入报废仓</el-button>
      </template>
    </el-dialog>

    <!-- 确认报废弹窗 -->
    <el-dialog v-model="disposeDialogVisible" title="确认报废处置" width="420px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="商品">
          <span>{{ disposeTarget.skuName }} ({{ disposeTarget.skuCode }})</span>
        </el-form-item>
        <el-form-item label="可报废数量">
          <span class="font-bold text-red-500">{{ disposeTarget.maxQty }}</span>
        </el-form-item>
        <el-form-item label="报废数量" required>
          <el-input-number v-model="disposeTarget.disposeQty" :min="1" :max="disposeTarget.maxQty" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="disposeDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="handleDispose">确认报废（不可撤销）</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSpecialStock, confirmSellable, confirmDispose, confirmScrap } from '@/api/stock'
import type { StockItem } from '@/api/stock'
import { getWarehouseList } from '@/api/warehouse'
import type { Warehouse } from '@/api/warehouse'
import { formatDateTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'

const activeTab = ref('DEFECTIVE')
const loading = ref(false)
const stockList = ref<StockItem[]>([])
const normalWarehouses = ref<Warehouse[]>([])

const searchParams = reactive({
  skuCode: '',
  skuName: '',
})

// 确认可售
const sellableDialogVisible = ref(false)
const sellableTarget = reactive({
  stockId: 0,
  skuCode: '',
  skuName: '',
  maxQty: 0,
  transferQty: 1,
  warehouseId: undefined as number | undefined,
})

// 转报废仓
const scrapDialogVisible = ref(false)
const scrapTarget = reactive({
  stockId: 0,
  skuCode: '',
  skuName: '',
  maxQty: 0,
  transferQty: 1,
})

// 确认报废
const disposeDialogVisible = ref(false)
const disposeTarget = reactive({
  stockId: 0,
  skuCode: '',
  skuName: '',
  maxQty: 0,
  disposeQty: 1,
})

const submitting = ref(false)

async function fetchData() {
  loading.value = true
  try {
    const res = await getSpecialStock(activeTab.value, {
      skuCode: searchParams.skuCode || undefined,
      skuName: searchParams.skuName || undefined,
    })
    stockList.value = res.data || []
  } catch {
    stockList.value = []
  } finally {
    loading.value = false
  }
}

function handleReset() {
  searchParams.skuCode = ''
  searchParams.skuName = ''
  fetchData()
}

function handleTabChange() {
  stockList.value = []
  fetchData()
}

// 确认可售
function openSellableDialog(row: StockItem) {
  sellableTarget.stockId = row.id
  sellableTarget.skuCode = row.skuCode
  sellableTarget.skuName = row.skuName
  sellableTarget.maxQty = row.quantity
  sellableTarget.transferQty = row.quantity
  sellableTarget.warehouseId = undefined
  sellableDialogVisible.value = true
}

async function handleSellable() {
  if (!sellableTarget.warehouseId) {
    ElMessage.warning('请选择目标仓库')
    return
  }
  submitting.value = true
  try {
    await confirmSellable(sellableTarget.stockId, sellableTarget.warehouseId, sellableTarget.transferQty)
    ElMessage.success('已转移到普通仓')
    sellableDialogVisible.value = false
    fetchData()
  } catch {} finally {
    submitting.value = false
  }
}

// 转报废仓
function openScrapDialog(row: StockItem) {
  scrapTarget.stockId = row.id
  scrapTarget.skuCode = row.skuCode
  scrapTarget.skuName = row.skuName
  scrapTarget.maxQty = row.quantity
  scrapTarget.transferQty = row.quantity
  scrapDialogVisible.value = true
}

async function handleScrap() {
  submitting.value = true
  try {
    await confirmScrap(scrapTarget.stockId, scrapTarget.transferQty)
    ElMessage.success('已转入报废仓')
    scrapDialogVisible.value = false
    fetchData()
  } catch {} finally {
    submitting.value = false
  }
}

// 确认报废
function openDisposeDialog(row: StockItem) {
  disposeTarget.stockId = row.id
  disposeTarget.skuCode = row.skuCode
  disposeTarget.skuName = row.skuName
  disposeTarget.maxQty = row.quantity
  disposeTarget.disposeQty = row.quantity
  disposeDialogVisible.value = true
}

async function handleDispose() {
  submitting.value = true
  try {
    await confirmDispose(disposeTarget.stockId, disposeTarget.disposeQty)
    ElMessage.success('已报废处置')
    disposeDialogVisible.value = false
    fetchData()
  } catch {} finally {
    submitting.value = false
  }
}

onMounted(async () => {
  fetchData()
  try {
    const res = await getWarehouseList({ page: 1, size: 100 })
    normalWarehouses.value = (res.data.list || []).filter((w: any) => w.warehouseType === 'NORMAL')
  } catch {}
})
</script>
