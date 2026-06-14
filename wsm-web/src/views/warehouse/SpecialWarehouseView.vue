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
            <el-popconfirm
              title="确认可售？将转移到普通仓"
              @confirm="openSellableDialog(row)"
            >
              <template #reference>
                <el-button type="success" link icon="Check" :disabled="row.quantity <= 0">确认可售</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm
              title="确认转入报废仓？"
              @confirm="handleScrap(row.id)"
            >
              <template #reference>
                <el-button type="warning" link icon="Right" :disabled="row.quantity <= 0">转报废仓</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>

        <!-- 报废仓操作 -->
        <el-table-column v-if="activeTab === 'SCRAP'" label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-popconfirm
              title="确认报废处置？此操作不可撤销"
              @confirm="handleDispose(row.id)"
            >
              <template #reference>
                <el-button type="danger" link icon="Delete" :disabled="row.quantity <= 0">确认报废</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && stockList.length === 0" :description="activeTab === 'DEFECTIVE' ? '次品仓暂无库存' : '报废仓暂无库存'" />
    </div>

    <!-- 选择目标仓库弹窗 -->
    <el-dialog v-model="sellableDialogVisible" title="确认可售 - 选择目标仓库" width="420px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="商品">
          <span>{{ sellableTarget.skuName }} ({{ sellableTarget.skuCode }})</span>
        </el-form-item>
        <el-form-item label="数量">
          <span class="font-bold">{{ sellableTarget.quantity }}</span>
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

const sellableDialogVisible = ref(false)
const submitting = ref(false)
const sellableTarget = reactive({
  stockId: 0,
  skuCode: '',
  skuName: '',
  quantity: 0,
  warehouseId: undefined as number | undefined,
})

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

function openSellableDialog(row: StockItem) {
  sellableTarget.stockId = row.id
  sellableTarget.skuCode = row.skuCode
  sellableTarget.skuName = row.skuName
  sellableTarget.quantity = row.quantity
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
    await confirmSellable(sellableTarget.stockId, sellableTarget.warehouseId)
    ElMessage.success('已转移到普通仓')
    sellableDialogVisible.value = false
    fetchData()
  } catch {} finally {
    submitting.value = false
  }
}

async function handleScrap(stockId: number) {
  try {
    await confirmScrap(stockId)
    ElMessage.success('已转入报废仓')
    fetchData()
  } catch {}
}

async function handleDispose(stockId: number) {
  try {
    await confirmDispose(stockId)
    ElMessage.success('已报废处置')
    fetchData()
  } catch {}
}

onMounted(async () => {
  fetchData()
  try {
    const res = await getWarehouseList({ page: 1, size: 100 })
    normalWarehouses.value = (res.data.list || []).filter((w: any) => w.warehouseType === 'NORMAL')
  } catch {}
})
</script>
