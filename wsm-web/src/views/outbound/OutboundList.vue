<template>
  <div class="page-container">
    <PageHeader title="发货管理">
      <template #actions>
        <el-button type="primary" icon="Plus" @click="openDialog()">新增发货单</el-button>
      </template>
    </PageHeader>

    <div class="card mb-4">
      <el-form :model="searchParams" inline>
        <el-form-item label="发货单号">
          <el-input v-model="searchParams.outboundNo" placeholder="发货单号" clearable style="width: 160px" @keyup.enter="handleSearch" />
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
        <el-table-column prop="outboundNo" label="发货单号" min-width="160" />
        <el-table-column prop="orderNo" label="订单号" min-width="140" />
        <el-table-column prop="platformOrderNo" label="平台单号" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.platformOrderNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="warehouseName" label="仓库" min-width="100" />
        <el-table-column prop="expressCompanyName" label="快递公司" min-width="100">
          <template #default="{ row }">{{ row.expressCompanyName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="trackingNo" label="快递单号" min-width="140">
          <template #default="{ row }">{{ row.trackingNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="shippingFee" label="快递费" min-width="90" align="right">
          <template #default="{ row }">{{ row.shippingFee ? `¥${row.shippingFee.toFixed(2)}` : '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getOutboundStatusType(row)" size="small">
              {{ getOutboundStatusLabel(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="300" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link icon="View" @click="viewDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 'SHIPPED'"
              type="warning"
              link
              icon="Edit"
              @click="openEditDialog(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'WAIT_PICKING' || row.status === 'PICKING'"
              type="success"
              link
              icon="Check"
              @click="openConfirmDialog(row)"
            >
              发货
            </el-button>
            <el-popconfirm
              v-if="row.status === 'WAIT_PICKING' || row.status === 'PICKING'"
              title="确定取消该发货单吗？取消后订单将标记为出库失败"
              @confirm="handleCancel(row.id)"
            >
              <template #reference>
                <el-button type="danger" link icon="Close">取消</el-button>
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

    <!-- 新增发货单弹窗 -->
    <el-dialog v-model="dialogVisible" title="新增发货单" width="520px" destroy-on-close>
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
    <el-dialog v-model="detailVisible" title="发货单详情" width="760px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="发货单号">{{ detail.outboundNo }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="平台单号">{{ detail.platformOrderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detail.warehouseName || detail.warehouseId }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="(OUTBOUND_STATUS_MAP[detail.status]?.color as any) || 'info'" size="small">
            {{ OUTBOUND_STATUS_MAP[detail.status]?.label || detail.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="快递公司">{{ detail.expressCompanyName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="快递单号">{{ detail.trackingNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="快递费用">{{ detail.shippingFee ? `¥${detail.shippingFee.toFixed(2)}` : '-' }}</el-descriptions-item>
        <el-descriptions-item label="发货备注">{{ detail.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="订单备注">{{ detail.orderRemark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <h4 class="mt-4 mb-2 text-sm font-semibold text-gray-700">发货明细</h4>
      <el-table :data="detail.items || []" border size="small">
        <el-table-column label="图片" width="60" align="center">
          <template #default="{ row }">
            <ImagePreview :src="row.skuImage" />
          </template>
        </el-table-column>
        <el-table-column prop="skuCode" label="SKU编码" width="130" />
        <el-table-column prop="skuName" label="SKU名称" min-width="100" />
        <el-table-column prop="sizeValue" label="码数" width="80" align="center">
          <template #default="{ row }">{{ row.sizeValue || '-' }}</template>
        </el-table-column>
        <el-table-column label="仓库" width="100" align="center">
          <template #default="{ row }">{{ row.warehouseName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="应出" width="80" align="center" />
      </el-table>
    </el-dialog>

    <!-- 编辑发货单弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑发货单" width="580px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" label-width="90px">
        <el-form-item label="发货单号">
          <el-input :model-value="editForm.outboundNo" disabled />
        </el-form-item>
        <el-form-item label="快递公司">
          <el-select v-model="editForm.expressCompanyId" placeholder="请选择快递公司" style="width: 100%" clearable>
            <el-option v-for="c in companyList" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="快递单号">
          <el-input v-model="editForm.trackingNo" placeholder="请输入快递单号" />
        </el-form-item>
        <el-form-item label="快递费用">
          <el-input-number v-model="editForm.shippingFee" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="2" placeholder="备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editing" @click="handleEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 扫码弹窗 -->
    <el-dialog v-model="scanDialogVisible" title="扫码拣货" width="780px" destroy-on-close>
      <el-descriptions :column="3" border class="mb-4">
        <el-descriptions-item label="发货单号">{{ scanDetail.outboundNo }}</el-descriptions-item>
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

    <!-- 确认发货弹窗 -->
    <el-dialog v-model="confirmDialogVisible" title="确认发货" width="760px" destroy-on-close>
      <el-descriptions :column="2" border class="mb-4" size="small">
        <el-descriptions-item label="发货单号">{{ confirmDetail.outboundNo }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ confirmDetail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="平台单号">{{ confirmDetail.platformOrderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="订单备注">{{ confirmDetail.orderRemark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <h4 class="mb-2 text-sm font-semibold text-gray-700">订单明细</h4>
      <el-table :data="confirmDetail.items || []" border size="small" class="mb-4" max-height="250">
        <el-table-column label="图片" width="60" align="center">
          <template #default="{ row }">
            <ImagePreview :src="row.skuImage" />
          </template>
        </el-table-column>
        <el-table-column prop="skuCode" label="SKU编码" width="130" />
        <el-table-column prop="skuName" label="SKU名称" min-width="100" />
        <el-table-column prop="sizeValue" label="码数" width="80" align="center">
          <template #default="{ row }">{{ row.sizeValue || '-' }}</template>
        </el-table-column>
        <el-table-column label="仓库" width="100" align="center">
          <template #default="{ row }">{{ row.warehouseName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" align="center" />
      </el-table>

      <el-divider content-position="left">快递信息</el-divider>
      <el-form label-width="100px">
        <el-form-item label="快递单号" required>
          <el-input v-model="confirmForm.trackingNo" placeholder="请输入快递单号" />
        </el-form-item>
        <el-form-item label="快递公司" required>
          <el-select v-model="confirmForm.expressCompanyId" placeholder="请选择快递公司" style="width: 100%" @change="handleCompanyChange">
            <el-option v-for="c in companyList" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="费用模板" required>
          <el-select v-model="confirmForm.feeTemplateId" placeholder="请选择费用模板" style="width: 100%" @change="handleTemplateChange">
            <el-option v-for="t in templateList" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预估重量(kg)" required>
          <el-input-number v-model="confirmForm.estimatedWeight" :min="0.01" :precision="2" style="width: 100%" @focus="($event.target as HTMLInputElement).select()" />
        </el-form-item>
        <el-form-item label="快递费用" required>
          <el-input-number v-model="confirmForm.shippingFee" :min="0.01" :precision="2" style="width: 100%" />
          <div class="text-xs text-gray-400 mt-1">选择模板后自动计算，也可手动修改</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="confirmDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="confirming" @click="handleConfirm">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { cancelOutbound, confirmOutbound, createOutbound, getOutboundDetail, getOutboundList, scanOutbound, updateOutbound } from '@/api/outbound'
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
import ImagePreview from '@/components/ImagePreview.vue'

const route = useRoute()
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

// 编辑弹窗
const editDialogVisible = ref(false)
const editing = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = reactive({
  id: 0,
  outboundNo: '',
  expressCompanyId: undefined as number | undefined,
  trackingNo: '',
  shippingFee: undefined as number | undefined,
  remark: '',
})

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
  // 从URL参数预填搜索条件
  const queryOrderNo = route.query.orderNo
  if (queryOrderNo) {
    searchParams.orderNo = queryOrderNo
    handleSearch()
  }
  // 加载快递公司列表
  try {
    const res = await getCompanyList()
    companyList.value = res.data || []
  } catch {}
})

function getOutboundStatusType(row: any) {
  if (row.status === 'WAIT_PICKING' && row.orderRemark?.includes('刷单')) return 'warning'
  if (row.status === 'WAIT_PICKING' && row.orderRemark?.includes('换货')) return 'primary'
  return OUTBOUND_STATUS_MAP[row.status]?.color || 'info'
}

function getOutboundStatusLabel(row: any) {
  if (row.status === 'WAIT_PICKING' && row.orderRemark?.includes('刷单')) return '刷单'
  if (row.status === 'WAIT_PICKING' && row.orderRemark?.includes('换货')) return '换货'
  return OUTBOUND_STATUS_MAP[row.status]?.label || row.status
}

async function openDialog() {
  form.orderId = undefined
  form.remark = ''
  // 加载待发货的订单
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
    ElMessage.success('发货单创建成功')
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

async function openEditDialog(row: OutboundOrder) {
  const res = await getOutboundDetail(row.id)
  const outbound = res.data
  Object.assign(editForm, {
    id: row.id,
    outboundNo: outbound.outboundNo || '',
    expressCompanyId: outbound.expressCompanyId || undefined,
    trackingNo: outbound.trackingNo || '',
    shippingFee: outbound.shippingFee || undefined,
    remark: outbound.remark || '',
  })
  editDialogVisible.value = true
}

async function handleEdit() {
  editing.value = true
  try {
    await updateOutbound(editForm.id, {
      expressCompanyId: editForm.expressCompanyId,
      trackingNo: editForm.trackingNo || undefined,
      shippingFee: editForm.shippingFee,
      remark: editForm.remark,
    })
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    fetchData()
  } catch {} finally {
    editing.value = false
  }
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

// 确认发货弹窗
const confirmDialogVisible = ref(false)
const confirming = ref(false)
const confirmDetail = ref<OutboundOrder | Record<string, never>>({})
const confirmForm = reactive({
  outboundId: 0,
  trackingNo: '',
  expressCompanyId: undefined as number | undefined,
  feeTemplateId: undefined as number | undefined,
  estimatedWeight: undefined as number | undefined,
  shippingFee: undefined as number | undefined,
})

const confirmTemplateDetail = ref<any>(null) // 缓存模板详情

// 重量变化时自动计算快递费
watch(
  () => confirmForm.estimatedWeight,
  () => {
    if (confirmForm.estimatedWeight && confirmForm.estimatedWeight > 0 && confirmTemplateDetail.value) {
      calculateFeeByTemplate()
    }
  }
)

async function openConfirmDialog(row: OutboundOrder) {
  confirmForm.outboundId = row.id
  confirmForm.trackingNo = ''
  confirmForm.expressCompanyId = undefined
  confirmForm.feeTemplateId = undefined
  confirmForm.estimatedWeight = 0
  confirmForm.shippingFee = undefined
  templateList.value = []

  // 加载发货单详情（包含订单明细）
  try {
    const res = await getOutboundDetail(row.id)
    confirmDetail.value = res.data
  } catch {
    confirmDetail.value = {}
  }

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
  confirmTemplateDetail.value = null
  templateList.value = []
  if (companyId) {
    try {
      const res = await getTemplateListByCompany(companyId)
      templateList.value = res.data || []
      // 自动选择默认模板
      const defaultTemplate = templateList.value.find(t => t.isDefault === 1)
      if (defaultTemplate) {
        await loadTemplateDetail(defaultTemplate.id)
      }
    } catch {
      templateList.value = []
    }
  }
}

async function loadTemplateDetail(templateId: number) {
  confirmForm.feeTemplateId = templateId
  try {
    const res = await getTemplateDetail(templateId)
    confirmTemplateDetail.value = res.data
    // 如果有重量，自动计算费用
    if (confirmForm.estimatedWeight && confirmForm.estimatedWeight > 0) {
      calculateFeeByTemplate()
    }
  } catch {
    confirmTemplateDetail.value = null
  }
}

async function handleTemplateChange(templateId: number) {
  if (templateId) {
    await loadTemplateDetail(templateId)
  } else {
    confirmTemplateDetail.value = null
    confirmForm.shippingFee = undefined
  }
}

function calculateFeeByTemplate() {
  if (!confirmTemplateDetail.value || !confirmForm.estimatedWeight || confirmForm.estimatedWeight <= 0) return

  const template = confirmTemplateDetail.value

  // 首重续重类型
  if (template.templateType === 'FIRST_CONTINUE') {
    const firstWeight = template.firstWeight || 1
    const firstFee = template.firstFee || 0
    const additionalWeight = template.additionalWeight || 1
    const additionalFee = template.additionalFee || 0

    if (confirmForm.estimatedWeight <= firstWeight) {
      confirmForm.shippingFee = firstFee
    } else {
      const extraWeight = confirmForm.estimatedWeight - firstWeight
      const extraUnits = Math.ceil(extraWeight / additionalWeight)
      confirmForm.shippingFee = firstFee + extraUnits * additionalFee
    }
  }
  // 阶梯计费类型
  else if (template.steps && template.steps.length > 0) {
    const sortedSteps = [...template.steps].sort((a, b) => a.minWeight - b.minWeight)
    const matchedStep = sortedSteps.find(s =>
      confirmForm.estimatedWeight >= s.minWeight && confirmForm.estimatedWeight < s.maxWeight
    )

    if (matchedStep) {
      confirmForm.shippingFee = matchedStep.fee
    } else {
      const lastStep = sortedSteps[sortedSteps.length - 1]
      if (confirmForm.estimatedWeight >= lastStep.minWeight) {
        confirmForm.shippingFee = lastStep.fee
      } else {
        confirmForm.shippingFee = undefined
        ElMessage.warning('未找到匹配的费用阶梯')
      }
    }
  }
}

async function handleCancel(id: number) {
  try {
    await cancelOutbound(id)
    ElMessage.success('发货单已取消')
    fetchData()
  } catch {}
}

async function handleConfirm() {
  if (!confirmForm.trackingNo) {
    ElMessage.warning('请输入快递单号')
    return
  }
  if (!confirmForm.expressCompanyId) {
    ElMessage.warning('请选择快递公司')
    return
  }
  if (!confirmForm.feeTemplateId) {
    ElMessage.warning('请选择费用模板')
    return
  }
  if (!confirmForm.estimatedWeight || confirmForm.estimatedWeight <= 0) {
    ElMessage.warning('请输入预估重量')
    return
  }
  if (!confirmForm.shippingFee || confirmForm.shippingFee <= 0) {
    ElMessage.warning('请输入快递费用')
    return
  }
  try {
    await ElMessageBox.confirm('确认发货后将扣减库存，确定继续吗？', '确认发货', { type: 'warning' })
  } catch {
    return
  }

  confirming.value = true
  try {
    await confirmOutbound({
      outboundId: confirmForm.outboundId,
      trackingNo: confirmForm.trackingNo,
      expressCompanyId: confirmForm.expressCompanyId,
      feeTemplateId: confirmForm.feeTemplateId,
      estimatedWeight: confirmForm.estimatedWeight,
      shippingFee: confirmForm.shippingFee,
    })
    ElMessage.success('发货确认成功')
    confirmDialogVisible.value = false
    fetchData()
  } catch {} finally {
    confirming.value = false
  }
}
</script>
