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
        <el-form-item label="平台单号">
          <el-input v-model="searchParams.platformOrderNo" placeholder="平台订单号" clearable style="width: 160px" @keyup.enter="handleSearch" />
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
        <el-table-column prop="platformOrderNo" label="平台单号" width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.platformOrderNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="warehouseName" label="仓库" width="140" />
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="(RETURN_STATUS_MAP[row.status]?.color as any) || 'info'" size="small">
              {{ RETURN_STATUS_MAP[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="退货原因" min-width="100" show-overflow-tooltip />
        <el-table-column prop="trackingNo" label="客户快递单号" width="140">
          <template #default="{ row }">{{ row.trackingNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="100" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link icon="View" @click="viewDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'PENDING_CHECK'" type="warning" link icon="Stamp" @click="openCheckDialog(row)">
              质检
            </el-button>
            <el-button v-if="canConfirmReturn(row.status)" type="success" link icon="Check" @click="openConfirmDialog(row)">
              确认入库
            </el-button>
            <el-popconfirm
              v-if="canCancelReturn(row.status)"
              title="确定取消退货吗？取消后订单将恢复为已发货状态"
              @confirm="handleCancelReturn(row.id)"
            >
              <template #reference>
                <el-button type="danger" link icon="Close">取消退货</el-button>
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

    <el-dialog v-model="detailVisible" title="退货单详情" width="760px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="退货单号">{{ detail.returnNo }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="平台单号">{{ detail.platformOrderNo || '-' }}</el-descriptions-item>
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
        <el-table-column prop="skuName" label="SKU名称" min-width="100" />
        <el-table-column prop="sizeValue" label="码数" width="80" align="center">
          <template #default="{ row }">{{ row.sizeValue || '-' }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="退货数量" width="90" align="center" />
        <el-table-column prop="qualityStatus" label="质检结果" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.qualityStatus" :type="(QUALITY_STATUS_MAP[row.qualityStatus]?.color as any) || 'info'" size="small">
              {{ QUALITY_STATUS_MAP[row.qualityStatus]?.label }}
            </el-tag>
            <span v-else class="text-gray-400">-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="checkDialogVisible" title="退货质检" width="580px" destroy-on-close>
      <el-form ref="checkFormRef" :model="checkForm" label-width="80px">
        <el-table :data="checkForm.items" border size="small">
          <el-table-column prop="skuName" label="SKU" min-width="100" />
          <el-table-column prop="sizeValue" label="码数" width="80" align="center">
            <template #default="{ row }">{{ row.sizeValue || '-' }}</template>
          </el-table-column>
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
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="checkDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="checking" @click="handleCheck">提交质检</el-button>
      </template>
    </el-dialog>

    <!-- 确认入库弹窗 -->
    <el-dialog v-model="confirmDialogVisible" title="确认退货入库" width="520px" destroy-on-close>
      <div class="mb-4">
        <p class="text-sm text-gray-600 mb-3">退货商品将按以下规则入库：</p>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="可售商品">
            <el-tag type="success" size="small">原发货仓</el-tag>
            <span class="ml-2 text-gray-500">{{ confirmInfo.sellableWarehouse }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="次品">
            <el-tag type="warning" size="small">次品仓</el-tag>
            <span class="ml-2 text-gray-500">{{ confirmInfo.defectiveWarehouse }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="报废">
            <el-tag type="danger" size="small">报废仓</el-tag>
            <span class="ml-2 text-gray-500">{{ confirmInfo.scrapWarehouse }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="confirmDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="confirming" @click="handleConfirmReturn">确认入库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { cancelReturn, checkReturn, confirmReturn, getReturnDetail, getReturnList } from '@/api/returns'
import type { ReturnOrder } from '@/api/returns'
import { getWarehouseList } from '@/api/warehouse'
import type { Warehouse } from '@/api/warehouse'
import { useTable } from '@/composables/useTable'
import { formatDateTime } from '@/utils/format'
import { QUALITY_STATUS_MAP, RETURN_STATUS_MAP } from '@/utils/constants'
import PageHeader from '@/components/PageHeader.vue'

const route = useRoute()
const { tableData, loading, pagination, searchParams, handleSearch, handleReset, handlePageChange, handleSizeChange, fetchData } = useTable<ReturnOrder>(getReturnList)

const warehouses = ref<Warehouse[]>([])
const detailVisible = ref(false)
const detail = ref<Partial<ReturnOrder>>({})
const checkDialogVisible = ref(false)
const checking = ref(false)
const checkFormRef = ref<FormInstance>()
const checkForm = ref({
  returnId: 0,
  items: [] as { itemId: number; skuName: string; sizeValue?: string; quantity: number; qualityStatus: string }[],
})

const confirmDialogVisible = ref(false)
const confirming = ref(false)
const confirmReturnId = ref(0)
const confirmInfo = ref({
  sellableWarehouse: '',
  defectiveWarehouse: '',
  scrapWarehouse: '',
})

onMounted(async () => {
  try {
    const res = await getWarehouseList({ page: 1, size: 100 })
    warehouses.value = res.data.list || []
  } catch {
    warehouses.value = []
  }
  // 从URL参数预填搜索条件
  const queryOrderNo = route.query.orderNo
  if (queryOrderNo) {
    searchParams.orderNo = queryOrderNo
    handleSearch()
  }
})

function canConfirmReturn(status: string) {
  return status === 'SELLABLE' || status === 'DEFECTIVE' || status === 'SCRAPPED'
}

function canCancelReturn(status: string) {
  return status === 'PENDING_CHECK' || status === 'SELLABLE' || status === 'DEFECTIVE' || status === 'SCRAPPED'
}

async function handleCancelReturn(id: number) {
  try {
    await cancelReturn(id)
    ElMessage.success('退货已取消')
    fetchData()
  } catch {}
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
  checkForm.value = {
    returnId: row.id,
    items: (returnData.items || []).map((item) => ({
      itemId: item.id,
      skuName: item.skuName || item.skuCode,
      sizeValue: item.sizeValue,
      quantity: item.quantity,
      qualityStatus: item.qualityStatus || 'SELLABLE',
    })),
  }
  checkDialogVisible.value = true
}

async function handleCheck() {
  checking.value = true
  try {
    await checkReturn({
      returnId: checkForm.value.returnId,
      items: checkForm.value.items.map((item) => ({
        itemId: item.itemId,
        qualityStatus: item.qualityStatus,
      })),
    })
    ElMessage.success('质检完成')
    checkDialogVisible.value = false
    fetchData()
  } catch {} finally {
    checking.value = false
  }
}

async function openConfirmDialog(row: ReturnOrder) {
  confirmReturnId.value = row.id
  // 查找各类仓库名称
  const defective = warehouses.value.find((w) => w.warehouseType === 'DEFECTIVE')
  const scrap = warehouses.value.find((w) => w.warehouseType === 'SCRAP')
  confirmInfo.value = {
    sellableWarehouse: row.warehouseName || '原发货仓',
    defectiveWarehouse: defective ? defective.name : '⚠️ 未配置（请先在仓库管理中创建次品仓）',
    scrapWarehouse: scrap ? scrap.name : '⚠️ 未配置（请先在仓库管理中创建报废仓）',
  }
  confirmDialogVisible.value = true
}

async function handleConfirmReturn() {
  confirming.value = true
  try {
    await confirmReturn(confirmReturnId.value)
    ElMessage.success('退货入库成功')
    confirmDialogVisible.value = false
    fetchData()
  } catch {} finally {
    confirming.value = false
  }
}
</script>
