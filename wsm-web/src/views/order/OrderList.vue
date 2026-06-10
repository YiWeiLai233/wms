<template>
  <div class="page-container">
    <PageHeader title="订单管理">
      <template #actions>
        <el-button type="primary" icon="Upload" @click="importDialogVisible = true">导入订单</el-button>
      </template>
    </PageHeader>

    <div class="card mb-4">
      <el-form :model="searchParams" inline>
        <el-form-item label="订单号">
          <el-input v-model="searchParams.orderNo" placeholder="订单号" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="平台单号">
          <el-input v-model="searchParams.platformOrderNo" placeholder="平台订单号" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="收件人">
          <el-input v-model="searchParams.receiverName" placeholder="收件人" clearable style="width: 120px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchParams.orderStatus" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="(v, k) in ORDER_STATUS_MAP" :key="k" :label="v.label" :value="k" />
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
        <el-table-column prop="orderNo" label="订单号" width="160" />
        <el-table-column prop="platformOrderNo" label="平台单号" width="160" show-overflow-tooltip />
        <el-table-column prop="warehouseName" label="仓库" width="120" />
        <el-table-column prop="receiverName" label="收件人" width="90" />
        <el-table-column prop="receiverPhone" label="收件电话" width="120" />
        <el-table-column prop="receiverAddress" label="收件地址" min-width="180" show-overflow-tooltip />
        <el-table-column prop="orderStatus" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="(ORDER_STATUS_MAP[row.orderStatus]?.color as any) || 'info'" size="small">
              {{ ORDER_STATUS_MAP[row.orderStatus]?.label || row.orderStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="金额" width="100" align="right">
          <template #default="{ row }">¥{{ row.totalAmount?.toFixed(2) || '0.00' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link icon="View" @click="viewDetail(row)">详情</el-button>
            <el-button v-if="row.orderStatus === 'WAIT_OUTBOUND'" type="success" link icon="TopRight" @click="createOutboundOrder(row)">
              创建出库单
            </el-button>
            <el-button v-if="row.orderStatus === 'SHIPPED'" type="warning" link icon="BottomLeft" @click="openReturnDialog(row)">
              退货
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

    <el-dialog v-model="detailVisible" title="订单详情" width="760px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="平台单号">{{ detail.platformOrderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detail.warehouseName || detail.warehouseId }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="(ORDER_STATUS_MAP[detail.orderStatus || '']?.color as any) || 'info'" size="small">
            {{ ORDER_STATUS_MAP[detail.orderStatus || '']?.label || detail.orderStatus }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="收件人">{{ detail.receiverName }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ detail.receiverPhone }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ detail.receiverAddress }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ detail.totalAmount?.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <h4 class="mt-4 mb-2 text-sm font-semibold text-gray-700">订单明细</h4>
      <el-table :data="detail.items || []" border size="small">
        <el-table-column prop="skuCode" label="SKU编码" width="130" />
        <el-table-column prop="skuName" label="SKU名称" min-width="160" />
        <el-table-column prop="quantity" label="数量" width="80" align="center" />
        <el-table-column prop="unitPrice" label="单价" width="90" align="right">
          <template #default="{ row }">¥{{ row.unitPrice?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="totalPrice" label="小计" width="90" align="right">
          <template #default="{ row }">¥{{ row.totalPrice?.toFixed(2) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="importDialogVisible" title="导入订单" width="680px" destroy-on-close>
      <el-form ref="importFormRef" :model="importForm" :rules="importRules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="平台单号">
              <el-input v-model="importForm.platformOrderNo" placeholder="平台订单号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="仓库">
              <el-select v-model="importForm.warehouseId" placeholder="选择仓库" style="width: 100%">
                <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="收件人" prop="receiverName">
          <el-input v-model="importForm.receiverName" placeholder="收件人姓名" />
        </el-form-item>
        <el-form-item label="电话" prop="receiverPhone">
          <el-input v-model="importForm.receiverPhone" placeholder="收件人电话" />
        </el-form-item>
        <el-form-item label="地址" prop="receiverAddress">
          <el-input v-model="importForm.receiverAddress" placeholder="收件地址" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="importForm.remark" type="textarea" :rows="2" />
        </el-form-item>

        <el-divider content-position="left">订单明细</el-divider>
        <div v-for="(item, index) in importForm.items" :key="index" class="flex gap-2 mb-2 items-center">
          <el-input v-model="item.skuCode" placeholder="SKU编码" style="width: 130px" />
          <el-input v-model="item.skuName" placeholder="SKU名称" style="width: 170px" />
          <el-input-number v-model="item.quantity" :min="1" placeholder="数量" style="width: 110px" />
          <el-input-number v-model="item.unitPrice" :min="0" :precision="2" placeholder="单价" style="width: 120px" />
          <el-button type="danger" icon="Delete" circle size="small" @click="importForm.items.splice(index, 1)" />
        </div>
        <el-button type="primary" link icon="Plus" @click="importForm.items.push({ skuCode: '', skuName: '', quantity: 1, unitPrice: 0 })">
          添加明细
        </el-button>
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="handleImport">确定导入</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="returnDialogVisible" title="创建退货单" width="760px" destroy-on-close>
      <el-form ref="returnFormRef" :model="returnForm" :rules="returnRules" label-width="90px">
        <el-form-item label="退货原因" prop="reason">
          <el-input v-model="returnForm.reason" placeholder="请输入退货原因" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="returnForm.remark" type="textarea" :rows="2" />
        </el-form-item>

        <el-divider content-position="left">退货明细</el-divider>
        <el-table :data="returnForm.items" border size="small">
          <el-table-column label="退货" width="70" align="center">
            <template #default="{ row }">
              <el-checkbox v-model="row.checked" />
            </template>
          </el-table-column>
          <el-table-column prop="skuCode" label="SKU编码" width="130" />
          <el-table-column prop="skuName" label="SKU名称" min-width="160" />
          <el-table-column prop="orderedQty" label="订单数量" width="90" align="center" />
          <el-table-column label="退货数量" width="150" align="center">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :min="1" :max="row.orderedQty" size="small" style="width: 120px" />
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="returnDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="returning" @click="handleReturn">创建退货单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getOrderDetail, getOrderList, importOrder } from '@/api/order'
import type { Order } from '@/api/order'
import { createOutbound } from '@/api/outbound'
import { createReturn } from '@/api/returns'
import { getWarehouseList } from '@/api/warehouse'
import type { Warehouse } from '@/api/warehouse'
import { useTable } from '@/composables/useTable'
import { formatDateTime } from '@/utils/format'
import { ORDER_STATUS_MAP } from '@/utils/constants'
import PageHeader from '@/components/PageHeader.vue'

interface ReturnItemForm {
  checked: boolean
  skuId: number
  skuCode: string
  skuName: string
  orderedQty: number
  quantity: number
}

const { tableData, loading, pagination, searchParams, handleSearch, handleReset, handlePageChange, handleSizeChange, fetchData } = useTable<Order>(getOrderList)

const warehouses = ref<Warehouse[]>([])
const detailVisible = ref(false)
const detail = ref<Partial<Order>>({})

const importDialogVisible = ref(false)
const importing = ref(false)
const importFormRef = ref<FormInstance>()
const importForm = reactive({
  platformOrderNo: '',
  warehouseId: undefined as number | undefined,
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  remark: '',
  items: [{ skuCode: '', skuName: '', quantity: 1, unitPrice: 0 }],
})

const importRules: FormRules = {
  receiverName: [{ required: true, message: '请输入收件人', trigger: 'blur' }],
  receiverPhone: [{ required: true, message: '请输入电话', trigger: 'blur' }],
  receiverAddress: [{ required: true, message: '请输入地址', trigger: 'blur' }],
}

const returnDialogVisible = ref(false)
const returning = ref(false)
const returnFormRef = ref<FormInstance>()
const returnForm = reactive({
  orderId: 0,
  reason: '',
  remark: '',
  items: [] as ReturnItemForm[],
})

const returnRules: FormRules = {
  reason: [{ required: true, message: '请输入退货原因', trigger: 'blur' }],
}

onMounted(async () => {
  try {
    const res = await getWarehouseList({ page: 1, size: 100 })
    warehouses.value = res.data.list || []
  } catch {
    warehouses.value = []
  }
})

async function viewDetail(row: Order) {
  try {
    const res = await getOrderDetail(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch {}
}

async function createOutboundOrder(row: Order) {
  try {
    await createOutbound({ orderId: row.id })
    ElMessage.success('出库单创建成功')
    fetchData()
  } catch {}
}

async function openReturnDialog(row: Order) {
  const res = await getOrderDetail(row.id)
  const order = res.data
  Object.assign(returnForm, {
    orderId: row.id,
    reason: '',
    remark: '',
    items: (order.items || []).map((item) => ({
      checked: true,
      skuId: item.skuId,
      skuCode: item.skuCode,
      skuName: item.skuName,
      orderedQty: item.quantity,
      quantity: item.quantity,
    })),
  })
  returnDialogVisible.value = true
}

async function handleReturn() {
  const valid = await returnFormRef.value?.validate().catch(() => false)
  if (!valid) return

  const items = returnForm.items
    .filter((item) => item.checked)
    .map((item) => ({ skuId: item.skuId, quantity: item.quantity }))

  if (items.length === 0) {
    ElMessage.warning('请至少选择一条退货明细')
    return
  }

  returning.value = true
  try {
    await createReturn({
      orderId: returnForm.orderId,
      reason: returnForm.reason,
      remark: returnForm.remark,
      items,
    })
    ElMessage.success('退货单创建成功')
    returnDialogVisible.value = false
    fetchData()
  } catch {} finally {
    returning.value = false
  }
}

async function handleImport() {
  const valid = await importFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (importForm.items.length === 0) {
    ElMessage.warning('请添加至少一条明细')
    return
  }

  importing.value = true
  try {
    await importOrder(importForm)
    ElMessage.success('导入成功')
    importDialogVisible.value = false
    fetchData()
  } catch {} finally {
    importing.value = false
  }
}
</script>
