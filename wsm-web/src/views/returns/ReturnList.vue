<template>
  <div class="page-container">
    <PageHeader title="退货管理" />

    <div class="card mb-4">
      <el-form :model="searchParams" inline>
        <el-form-item label="退货单号">
          <el-input v-model="searchParams.returnNo" placeholder="退货单号" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="searchParams.orderNo" placeholder="订单号" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchParams.status" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="(v, k) in RETURN_STATUS_MAP" :key="k" :label="v.label" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库">
          <el-select v-model="searchParams.warehouseId" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
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
        <el-table-column prop="returnNo" label="退货单号" width="160" />
        <el-table-column prop="orderNo" label="订单号" width="160" />
        <el-table-column prop="warehouseName" label="仓库" width="140" />
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="(RETURN_STATUS_MAP[row.status]?.color as any) || 'info'" size="small">
              {{ RETURN_STATUS_MAP[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="退货原因" min-width="150" show-overflow-tooltip />
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link icon="View" @click="viewDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'PENDING_CHECK'" type="warning" link icon="Stamp" @click="openCheckDialog(row)">
              质检
            </el-button>
            <el-button v-if="canConfirmReturn(row.status)" type="success" link icon="Check" @click="handleConfirmReturn(row.id)">
              确认入库
            </el-button>
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

    <el-dialog v-model="detailVisible" title="退货单详情" width="760px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="退货单号">{{ detail.returnNo }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detail.warehouseName || detail.warehouseId }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="(RETURN_STATUS_MAP[detail.status]?.color as any) || 'info'" size="small">
            {{ RETURN_STATUS_MAP[detail.status]?.label || detail.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="退货原因">{{ detail.reason }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <h4 class="mt-4 mb-2 text-sm font-semibold text-gray-700">退货明细</h4>
      <el-table :data="detail.items || []" border size="small">
        <el-table-column prop="skuCode" label="SKU编码" width="130" />
        <el-table-column prop="skuName" label="SKU名称" min-width="160" />
        <el-table-column prop="quantity" label="退货数量" width="90" align="center" />
        <el-table-column prop="qualityStatus" label="质检结果" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.qualityStatus" :type="(QUALITY_STATUS_MAP[row.qualityStatus]?.color as any) || 'info'" size="small">
              {{ QUALITY_STATUS_MAP[row.qualityStatus]?.label }}
            </el-tag>
            <span v-else class="text-gray-400">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="locationCode" label="入库库位" width="120">
          <template #default="{ row }">{{ row.locationCode || row.locationId || '-' }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="checkDialogVisible" title="退货质检" width="780px" destroy-on-close>
      <el-form ref="checkFormRef" :model="checkForm" label-width="80px">
        <el-table :data="checkForm.items" border size="small">
          <el-table-column prop="skuName" label="SKU" min-width="180" />
          <el-table-column prop="quantity" label="数量" width="80" align="center" />
          <el-table-column label="质检结果" width="150">
            <template #default="{ row }">
              <el-select v-model="row.qualityStatus" style="width: 120px">
                <el-option label="可售" value="SELLABLE" />
                <el-option label="次品" value="DEFECTIVE" />
                <el-option label="报废" value="SCRAPPED" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="入库库位" min-width="220">
            <template #default="{ row }">
              <el-select v-model="row.locationId" placeholder="选择库位" filterable style="width: 200px">
                <el-option v-for="l in locationOptions" :key="l.id" :label="`${l.code} - ${l.name}`" :value="l.id" />
              </el-select>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="checkDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="checking" @click="handleCheck">提交质检</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { checkReturn, confirmReturn, getReturnDetail, getReturnList } from '@/api/returns'
import type { ReturnOrder } from '@/api/returns'
import { getAreaList, getLocationList, getShelfList, getWarehouseList } from '@/api/warehouse'
import type { Warehouse, WarehouseLocation } from '@/api/warehouse'
import { useTable } from '@/composables/useTable'
import { formatDateTime } from '@/utils/format'
import { QUALITY_STATUS_MAP, RETURN_STATUS_MAP } from '@/utils/constants'
import PageHeader from '@/components/PageHeader.vue'

const { tableData, loading, pagination, searchParams, handleSearch, handleReset, handlePageChange, handleSizeChange, fetchData } = useTable<ReturnOrder>(getReturnList)

const warehouses = ref<Warehouse[]>([])
const detailVisible = ref(false)
const detail = ref<Partial<ReturnOrder>>({})
const checkDialogVisible = ref(false)
const checking = ref(false)
const checkFormRef = ref<FormInstance>()
const locationOptions = ref<WarehouseLocation[]>([])
const checkForm = ref({
  returnId: 0,
  items: [] as { itemId: number; skuName: string; quantity: number; qualityStatus: string; locationId?: number }[],
})

onMounted(async () => {
  try {
    const res = await getWarehouseList({ page: 1, size: 100 })
    warehouses.value = res.data.list || []
  } catch {
    warehouses.value = []
  }
})

function canConfirmReturn(status: string) {
  return status === 'SELLABLE' || status === 'DEFECTIVE' || status === 'SCRAPPED'
}

async function viewDetail(row: ReturnOrder) {
  try {
    const res = await getReturnDetail(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch {}
}

async function openCheckDialog(row: ReturnOrder) {
  const res = await getReturnDetail(row.id)
  const returnData = res.data
  await loadLocationsForWarehouse(returnData.warehouseId)
  checkForm.value = {
    returnId: row.id,
    items: (returnData.items || []).map((item) => ({
      itemId: item.id,
      skuName: item.skuName || item.skuCode,
      quantity: item.quantity,
      qualityStatus: item.qualityStatus || 'SELLABLE',
      locationId: item.locationId,
    })),
  }
  checkDialogVisible.value = true
}

async function loadLocationsForWarehouse(warehouseId: number) {
  locationOptions.value = []
  try {
    const areaRes = await getAreaList(warehouseId)
    for (const area of areaRes.data || []) {
      const shelfRes = await getShelfList(area.id)
      for (const shelf of shelfRes.data || []) {
        const locationRes = await getLocationList(shelf.id)
        locationOptions.value.push(...(locationRes.data || []))
      }
    }
  } catch {
    locationOptions.value = []
  }
}

async function handleCheck() {
  if (checkForm.value.items.some((item) => !item.locationId)) {
    ElMessage.warning('请为每个退货商品选择入库库位')
    return
  }

  checking.value = true
  try {
    await checkReturn({
      returnId: checkForm.value.returnId,
      items: checkForm.value.items.map((item) => ({
        itemId: item.itemId,
        qualityStatus: item.qualityStatus,
        locationId: item.locationId!,
      })),
    })
    ElMessage.success('质检完成')
    checkDialogVisible.value = false
    fetchData()
  } catch {} finally {
    checking.value = false
  }
}

async function handleConfirmReturn(id: number) {
  try {
    await ElMessageBox.confirm('确认退货入库后将增加库存，确定继续吗？', '确认入库', { type: 'warning' })
  } catch {
    return
  }

  try {
    await confirmReturn(id)
    ElMessage.success('退货入库成功')
    fetchData()
  } catch {}
}
</script>
