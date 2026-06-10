<template>
  <div class="page-container">
    <PageHeader title="出库管理">
      <template #actions>
        <el-button type="primary" icon="Plus" @click="openDialog()">新增出库单</el-button>
      </template>
    </PageHeader>

    <div class="card mb-4">
      <el-form :model="searchParams" inline>
        <el-form-item label="出库单号">
          <el-input v-model="searchParams.outboundNo" placeholder="出库单号" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="searchParams.orderNo" placeholder="订单号" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchParams.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="(v, k) in OUTBOUND_STATUS_MAP" :key="k" :label="v.label" :value="k" />
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
        <el-table-column prop="outboundNo" label="出库单号" width="160" />
        <el-table-column prop="orderNo" label="订单号" width="160" />
        <el-table-column prop="warehouseName" label="仓库" width="140" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="(OUTBOUND_STATUS_MAP[row.status]?.color as any) || 'info'" size="small">
              {{ OUTBOUND_STATUS_MAP[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pickerName" label="拣货人" width="100">
          <template #default="{ row }">{{ row.pickerName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link icon="View" @click="viewDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 'WAIT_PICKING' || row.status === 'PICKING'"
              type="warning"
              link
              icon="Aim"
              @click="openScanDialog(row)"
            >
              扫码
            </el-button>
            <el-button
              v-if="row.status === 'WAIT_PICKING' || row.status === 'PICKING'"
              type="success"
              link
              icon="Check"
              @click="handleConfirm(row.id)"
            >
              确认出库
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

    <!-- 新增出库单弹窗 -->
    <el-dialog v-model="dialogVisible" title="新增出库单" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="选择订单" prop="orderId">
          <el-select v-model="form.orderId" placeholder="请选择订单" filterable style="width: 100%">
            <el-option v-for="o in orderList" :key="o.id" :label="`${o.orderNo} - ${o.customerName || ''}`" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="出库单详情" width="760px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="出库单号">{{ detail.outboundNo }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detail.warehouseName || detail.warehouseId }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="(OUTBOUND_STATUS_MAP[detail.status]?.color as any) || 'info'" size="small">
            {{ OUTBOUND_STATUS_MAP[detail.status]?.label || detail.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="拣货人">{{ detail.pickerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <h4 class="mt-4 mb-2 text-sm font-semibold text-gray-700">出库明细</h4>
      <el-table :data="detail.items || []" border size="small">
        <el-table-column prop="skuCode" label="SKU编码" width="130" />
        <el-table-column prop="skuName" label="SKU名称" min-width="160" />
        <el-table-column prop="quantity" label="应出" width="80" align="center" />
        <el-table-column prop="pickedQty" label="已拣" width="80" align="center" />
        <el-table-column prop="locationCode" label="拣货库位" width="120">
          <template #default="{ row }">{{ row.locationCode || '-' }}</template>
        </el-table-column>
        <el-table-column label="进度" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.pickedQty >= row.quantity ? 'success' : 'warning'" size="small">
              {{ row.pickedQty >= row.quantity ? '已完成' : '待扫码' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 扫码弹窗 -->
    <el-dialog v-model="scanDialogVisible" title="扫码拣货" width="780px" destroy-on-close>
      <el-descriptions :column="3" border class="mb-4">
        <el-descriptions-item label="出库单号">{{ scanDetail.outboundNo }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ scanDetail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ scanDetail.warehouseName || scanDetail.warehouseId }}</el-descriptions-item>
      </el-descriptions>

      <el-form ref="scanFormRef" :model="scanForm" :rules="scanRules" inline>
        <el-form-item label="条码/SKU" prop="scanCode">
          <el-input v-model="scanForm.scanCode" placeholder="扫描或输入SKU编码/条码" clearable style="width: 220px" @keyup.enter="handleScan" />
        </el-form-item>
        <el-form-item label="库位" prop="locationId">
          <el-select v-model="scanForm.locationId" placeholder="选择拣货库位" filterable style="width: 220px">
            <el-option v-for="l in locationOptions" :key="l.id" :label="`${l.code} - ${l.name}`" :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Aim" :loading="scanning" @click="handleScan">提交扫码</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="scanDetail.items || []" border size="small">
        <el-table-column prop="skuCode" label="SKU编码" width="130" />
        <el-table-column prop="skuName" label="SKU名称" min-width="160" />
        <el-table-column prop="quantity" label="应出" width="80" align="center" />
        <el-table-column prop="pickedQty" label="已拣" width="80" align="center" />
        <el-table-column prop="locationCode" label="库位" width="120">
          <template #default="{ row }">{{ row.locationCode || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.pickedQty >= row.quantity ? 'success' : 'warning'" size="small">
              {{ row.pickedQty >= row.quantity ? '已扫满' : '未扫满' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { confirmOutbound, createOutbound, getOutboundDetail, getOutboundList, scanOutbound } from '@/api/outbound'
import type { OutboundOrder } from '@/api/outbound'
import { getOrderList } from '@/api/order'
import type { Order } from '@/api/order'
import { getAreaList, getLocationList, getShelfList, getWarehouseList } from '@/api/warehouse'
import type { Warehouse, WarehouseLocation } from '@/api/warehouse'
import { useTable } from '@/composables/useTable'
import { formatDateTime } from '@/utils/format'
import { OUTBOUND_STATUS_MAP } from '@/utils/constants'
import PageHeader from '@/components/PageHeader.vue'

const { tableData, loading, pagination, searchParams, handleSearch, handleReset, handlePageChange, handleSizeChange, fetchData } = useTable<OutboundOrder>(getOutboundList)

const warehouses = ref<Warehouse[]>([])
const orderList = ref<Order[]>([])

// 新增弹窗
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  orderId: undefined as number | undefined,
  remark: '',
})
const rules: FormRules = {
  orderId: [{ required: true, message: '请选择订单', trigger: 'change' }],
}

// 详情弹窗
const detailVisible = ref(false)
const detail = ref<OutboundOrder | Record<string, never>>({})

// 扫码弹窗
const scanDialogVisible = ref(false)
const scanDetail = ref<OutboundOrder | Record<string, never>>({})
const locationOptions = ref<WarehouseLocation[]>([])
const scanning = ref(false)
const scanFormRef = ref<FormInstance>()
const scanForm = reactive({
  scanCode: '',
  locationId: undefined as number | undefined,
})
const scanRules: FormRules = {
  scanCode: [{ required: true, message: '请输入条码或SKU编码', trigger: 'blur' }],
  locationId: [{ required: true, message: '请选择库位', trigger: 'change' }],
}

onMounted(async () => {
  try {
    const res = await getWarehouseList({ page: 1, size: 100 })
    warehouses.value = res.data.list || []
  } catch {
    warehouses.value = []
  }
})

async function openDialog() {
  form.orderId = undefined
  form.remark = ''
  // 加载待出库的订单
  try {
    const res = await getOrderList({ page: 1, size: 100, status: 'WAIT_OUTBOUND' })
    orderList.value = res.data.list || []
  } catch {
    orderList.value = []
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await createOutbound({ orderId: form.orderId!, remark: form.remark })
    ElMessage.success('出库单创建成功')
    dialogVisible.value = false
    fetchData()
  } catch {} finally {
    submitting.value = false
  }
}

async function viewDetail(row: OutboundOrder) {
  try {
    const res = await getOutboundDetail(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch {}
}

async function openScanDialog(row: OutboundOrder) {
  scanForm.scanCode = ''
  scanForm.locationId = undefined
  const res = await getOutboundDetail(row.id)
  scanDetail.value = res.data
  await loadLocationsForWarehouse(row.warehouseId)
  scanDialogVisible.value = true
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

async function refreshScanDetail() {
  if (!('id' in scanDetail.value)) return
  const res = await getOutboundDetail(scanDetail.value.id)
  scanDetail.value = res.data
}

async function handleScan() {
  const valid = await scanFormRef.value?.validate().catch(() => false)
  if (!valid || !('id' in scanDetail.value) || !scanForm.locationId) return

  scanning.value = true
  try {
    await scanOutbound({
      outboundId: scanDetail.value.id,
      scanCode: scanForm.scanCode.trim(),
      locationId: scanForm.locationId,
    })
    ElMessage.success('扫码成功')
    scanForm.scanCode = ''
    await refreshScanDetail()
    fetchData()
  } catch {} finally {
    scanning.value = false
  }
}

async function handleConfirm(id: number) {
  try {
    await ElMessageBox.confirm('确认出库后将扣减库存，确定继续吗？', '确认出库', { type: 'warning' })
  } catch {
    return
  }

  try {
    await confirmOutbound(id)
    ElMessage.success('出库确认成功')
    fetchData()
  } catch {}
}
</script>
