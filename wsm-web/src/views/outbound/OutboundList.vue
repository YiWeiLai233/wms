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
        <el-form-item label="平台单号">
          <el-input v-model="searchParams.platformOrderNo" placeholder="平台订单号" clearable style="width: 160px" @keyup.enter="handleSearch" />
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
        <el-table-column prop="orderNo" label="订单号" width="140" />
        <el-table-column prop="platformOrderNo" label="平台单号" width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.platformOrderNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="warehouseName" label="仓库" width="120" />
        <el-table-column prop="trackingNo" label="快递单号" width="140">
          <template #default="{ row }">{{ row.trackingNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="shippingFee" label="快递费" width="90" align="right">
          <template #default="{ row }">{{ row.shippingFee ? `¥${row.shippingFee.toFixed(2)}` : '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="(OUTBOUND_STATUS_MAP[row.status]?.color as any) || 'info'" size="small">
              {{ OUTBOUND_STATUS_MAP[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link icon="View" @click="viewDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 'WAIT_PICKING' || row.status === 'PICKING'"
              type="success"
              link
              icon="Check"
              @click="openConfirmDialog(row)"
            >
              出库
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
            <el-option v-for="o in orderList" :key="o.id" :label="`${o.orderNo} - ${o.receiverName || ''}`" :value="o.id" />
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
        <el-descriptions-item label="平台单号">{{ detail.platformOrderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detail.warehouseName || detail.warehouseId }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="(OUTBOUND_STATUS_MAP[detail.status]?.color as any) || 'info'" size="small">
            {{ OUTBOUND_STATUS_MAP[detail.status]?.label || detail.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="快递单号">{{ detail.trackingNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="快递费用">{{ detail.shippingFee ? `¥${detail.shippingFee.toFixed(2)}` : '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <h4 class="mt-4 mb-2 text-sm font-semibold text-gray-700">出库明细</h4>
      <el-table :data="detail.items || []" border size="small">
        <el-table-column prop="skuCode" label="SKU编码" width="130" />
        <el-table-column prop="skuName" label="SKU名称" min-width="100" />
        <el-table-column prop="sizeValue" label="码数" width="80" align="center">
          <template #default="{ row }">{{ row.sizeValue || '-' }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="应出" width="80" align="center" />
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
        <el-form-item label="货架" prop="shelfId">
          <el-select v-model="scanForm.shelfId" placeholder="选择拣货货架" filterable style="width: 220px">
            <el-option v-for="s in shelfOptions" :key="s.id" :label="`${s.code} - ${s.name}`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Aim" :loading="scanning" @click="handleScan">提交扫码</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="scanDetail.items || []" border size="small">
        <el-table-column prop="skuCode" label="SKU编码" width="130" />
        <el-table-column prop="skuName" label="SKU名称" min-width="100" />
        <el-table-column prop="quantity" label="应出" width="80" align="center" />
        <el-table-column prop="pickedQty" label="已拣" width="80" align="center" />
        <el-table-column prop="shelfCode" label="货架" width="120">
          <template #default="{ row }">{{ row.shelfCode || '-' }}</template>
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

    <!-- 确认出库弹窗 -->
    <el-dialog v-model="confirmDialogVisible" title="确认出库" width="580px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="快递单号">
          <el-input v-model="confirmForm.trackingNo" placeholder="请输入快递单号（选填）" />
        </el-form-item>
        <el-form-item label="快递公司">
          <el-select v-model="confirmForm.expressCompanyId" placeholder="选择快递公司" clearable style="width: 100%" @change="handleCompanyChange">
            <el-option v-for="c in companyList" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="费用模板">
          <el-select v-model="confirmForm.feeTemplateId" placeholder="选择费用模板" clearable style="width: 100%">
            <el-option v-for="t in templateList" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预估重量(kg)">
          <el-input-number v-model="confirmForm.estimatedWeight" :min="0" :precision="2" style="width: 100%" @change="handleWeightChange" />
        </el-form-item>
        <el-form-item label="快递费用">
          <el-input-number v-model="confirmForm.shippingFee" :min="0" :precision="2" style="width: 100%" />
          <div class="text-xs text-gray-400 mt-1">选择模板后自动计算，也可手动修改</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="confirmDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="confirming" @click="handleConfirm">确认出库</el-button>
      </template>
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
import { getShelfList, getWarehouseList } from '@/api/warehouse'
import type { Warehouse, WarehouseShelf } from '@/api/warehouse'
import { getCompanyList, getTemplateListByCompany, getTemplateDetail } from '@/api/express'
import type { ExpressCompany, ExpressFeeTemplate } from '@/api/express'
import { useTable } from '@/composables/useTable'
import { formatDateTime } from '@/utils/format'
import { OUTBOUND_STATUS_MAP } from '@/utils/constants'
import PageHeader from '@/components/PageHeader.vue'

const { tableData, loading, pagination, searchParams, handleSearch, handleReset, handlePageChange, handleSizeChange, fetchData } = useTable<OutboundOrder>(getOutboundList)

const warehouses = ref<Warehouse[]>([])
const orderList = ref<Order[]>([])
const companyList = ref<ExpressCompany[]>([])
const templateList = ref<ExpressFeeTemplate[]>([])

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
const shelfOptions = ref<WarehouseShelf[]>([])
const scanning = ref(false)
const scanFormRef = ref<FormInstance>()
const scanForm = reactive({
  scanCode: '',
  shelfId: undefined as number | undefined,
})
const scanRules: FormRules = {
  scanCode: [{ required: true, message: '请输入条码或SKU编码', trigger: 'blur' }],
  shelfId: [{ required: true, message: '请选择货架', trigger: 'change' }],
}

onMounted(async () => {
  try {
    const res = await getWarehouseList({ page: 1, size: 100 })
    warehouses.value = res.data.list || []
  } catch {
    warehouses.value = []
  }
  // 加载快递公司列表
  try {
    const res = await getCompanyList()
    companyList.value = res.data || []
  } catch {}
})

async function openDialog() {
  form.orderId = undefined
  form.remark = ''
  // 加载待出库的订单
  try {
    const res = await getOrderList({ page: 1, size: 100, orderStatus: 'WAIT_OUTBOUND' })
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
  scanForm.shelfId = undefined
  const res = await getOutboundDetail(row.id)
  scanDetail.value = res.data
  await loadShelvesForWarehouse(row.warehouseId)
  scanDialogVisible.value = true
}

async function loadShelvesForWarehouse(warehouseId: number) {
  shelfOptions.value = []
  try {
    const res = await getShelfList(warehouseId)
    shelfOptions.value = res.data || []
  } catch {
    shelfOptions.value = []
  }
}

async function refreshScanDetail() {
  if (!('id' in scanDetail.value)) return
  const res = await getOutboundDetail(scanDetail.value.id)
  scanDetail.value = res.data
}

async function handleScan() {
  const valid = await scanFormRef.value?.validate().catch(() => false)
  if (!valid || !('id' in scanDetail.value) || !scanForm.shelfId) return

  scanning.value = true
  try {
    await scanOutbound({
      outboundId: scanDetail.value.id,
      scanCode: scanForm.scanCode.trim(),
      shelfId: scanForm.shelfId,
    })
    ElMessage.success('扫码成功')
    scanForm.scanCode = ''
    await refreshScanDetail()
    fetchData()
  } catch {} finally {
    scanning.value = false
  }
}

// 确认出库弹窗
const confirmDialogVisible = ref(false)
const confirming = ref(false)
const confirmForm = reactive({
  outboundId: 0,
  trackingNo: '',
  expressCompanyId: undefined as number | undefined,
  feeTemplateId: undefined as number | undefined,
  estimatedWeight: 0,
  shippingFee: undefined as number | undefined,
})

async function openConfirmDialog(row: OutboundOrder) {
  confirmForm.outboundId = row.id
  confirmForm.trackingNo = ''
  confirmForm.expressCompanyId = undefined
  confirmForm.feeTemplateId = undefined
  confirmForm.estimatedWeight = 0
  confirmForm.shippingFee = undefined
  templateList.value = []

  // 加载快递公司列表
  try {
    const res = await getCompanyList()
    companyList.value = res.data || []
  } catch {
    companyList.value = []
  }

  confirmDialogVisible.value = true
}

async function handleCompanyChange(companyId: number) {
  confirmForm.feeTemplateId = undefined
  templateList.value = []
  if (companyId) {
    try {
      const res = await getTemplateListByCompany(companyId)
      templateList.value = res.data || []
      // 自动选择默认模板
      const defaultTemplate = templateList.value.find(t => t.isDefault === 1)
      if (defaultTemplate) {
        confirmForm.feeTemplateId = defaultTemplate.id
        // 如果有重量，自动计算费用
        if (confirmForm.estimatedWeight > 0) {
          await calculateFeeByTemplate()
        }
      }
    } catch {
      templateList.value = []
    }
  }
}

async function handleWeightChange() {
  if (confirmForm.estimatedWeight > 0 && confirmForm.feeTemplateId) {
    await calculateFeeByTemplate()
  }
}

async function calculateFeeByTemplate() {
  if (!confirmForm.feeTemplateId || confirmForm.estimatedWeight <= 0) return

  try {
    const res = await getTemplateDetail(confirmForm.feeTemplateId)
    const template = res.data
    if (template && template.steps && template.steps.length > 0) {
      // 按minWeight排序，确保从小到大
      const sortedSteps = [...template.steps].sort((a, b) => a.minWeight - b.minWeight)

      // 精确匹配阶梯：minWeight <= 输入重量 < maxWeight
      const matchedStep = sortedSteps.find(s =>
        confirmForm.estimatedWeight >= s.minWeight && confirmForm.estimatedWeight < s.maxWeight
      )

      if (matchedStep) {
        confirmForm.shippingFee = matchedStep.fee
      } else {
        // 如果没有匹配到，可能是超出最大阶梯，使用最后一个阶梯的费用
        const lastStep = sortedSteps[sortedSteps.length - 1]
        if (confirmForm.estimatedWeight >= lastStep.minWeight) {
          confirmForm.shippingFee = lastStep.fee
        } else {
          confirmForm.shippingFee = undefined
          ElMessage.warning('未找到匹配的费用阶梯')
        }
      }
    }
  } catch {}
}

async function handleConfirm() {
  try {
    await ElMessageBox.confirm('确认出库后将扣减库存，确定继续吗？', '确认出库', { type: 'warning' })
  } catch {
    return
  }

  confirming.value = true
  try {
    await confirmOutbound({
      outboundId: confirmForm.outboundId,
      trackingNo: confirmForm.trackingNo || undefined,
      expressCompanyId: confirmForm.expressCompanyId,
      feeTemplateId: confirmForm.feeTemplateId,
      estimatedWeight: confirmForm.estimatedWeight > 0 ? confirmForm.estimatedWeight : undefined,
      shippingFee: confirmForm.shippingFee,
    })
    ElMessage.success('出库确认成功')
    confirmDialogVisible.value = false
    fetchData()
  } catch {} finally {
    confirming.value = false
  }
}
</script>
