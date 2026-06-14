<template>
  <div class="page-container">
    <PageHeader title="订单管理">
      <template #actions>
        <el-button type="primary" icon="Upload" @click="openImportDialog">手动导入</el-button>
        <el-button type="success" icon="Document" @click="fileImportDialogVisible = true">文档导入</el-button>
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
            <el-tag :type="(ORDER_STATUS_MAP[row.orderStatus]?.color as any) || 'info'" :effect="(ORDER_STATUS_MAP[row.orderStatus]?.effect as any) || 'light'" size="small">
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
        <el-table-column label="操作" width="400" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link icon="View" @click="viewDetail(row)">详情</el-button>
            <el-button v-if="row.orderStatus === 'WAIT_OUTBOUND'" type="success" link icon="TopRight" @click="createOutboundOrder(row)">
              创建出库单
            </el-button>
            <el-popconfirm
              v-if="row.orderStatus === 'WAIT_PAY' || row.orderStatus === 'WAIT_OUTBOUND'"
              title="确定取消订单吗？将恢复已扣减的库存"
              @confirm="handleCancelOrder(row)"
            >
              <template #reference>
                <el-button type="danger" link icon="Close">取消订单</el-button>
              </template>
            </el-popconfirm>
            <el-button v-if="row.orderStatus === 'OUTBOUNDING'" type="primary" link icon="TopRight" @click="router.push({ path: '/outbound/list', query: { orderNo: row.orderNo } })">
              出库管理
            </el-button>
            <el-button v-if="row.orderStatus === 'SHIPPED'" type="warning" link icon="BottomLeft" @click="openReturnDialog(row)">
              退货
            </el-button>
            <el-button v-if="row.orderStatus === 'RETURNING'" type="warning" link icon="BottomLeft" @click="router.push({ path: '/returns/list', query: { orderNo: row.orderNo } })">
              退货管理
            </el-button>
            <el-popconfirm
              v-if="row.orderStatus === 'RETURNING'"
              title="确定取消退货吗？取消后订单将恢复为已发货状态"
              @confirm="handleCancelReturn(row)"
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

    <el-dialog v-model="detailVisible" title="订单详情" width="760px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="平台单号">{{ detail.platformOrderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detail.warehouseName || detail.warehouseId }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="(ORDER_STATUS_MAP[detail.orderStatus || '']?.color as any) || 'info'" :effect="(ORDER_STATUS_MAP[detail.orderStatus || '']?.effect as any) || 'light'" size="small">
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
        <el-table-column prop="skuName" label="SKU名称" min-width="100" />
        <el-table-column prop="sizeValue" label="码数" width="80" align="center">
          <template #default="{ row }">{{ row.sizeValue || '-' }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" align="center" />
        <el-table-column prop="totalPrice" label="小计" width="90" align="right">
          <template #default="{ row }">¥{{ row.totalPrice?.toFixed(2) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="importDialogVisible" title="导入订单" width="900px" destroy-on-close>
      <el-form ref="importFormRef" :model="importForm" :rules="importRules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="平台单号">
              <el-input v-model="importForm.platformOrderNo" placeholder="平台订单号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="仓库" prop="warehouseId">
              <el-select v-model="importForm.warehouseId" placeholder="选择仓库" style="width: 100%" @change="handleImportWarehouseChange">
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
        <div class="mb-3">
          <el-select v-model="selectedSkuGroupKey" placeholder="请先选择仓库" filterable clearable style="width: 100%" :disabled="!importForm.warehouseId">
            <el-option
              v-for="group in skuGroups"
              :key="group.key"
              :label="`${group.skuCode} - ${group.skuName}`"
              :value="group.key"
            />
          </el-select>
        </div>
        <el-table v-if="selectedSkuGroup" :data="selectedSkuGroup.skus" border size="small" class="mb-3" max-height="350">
          <el-table-column prop="sizeValue" label="码数" width="80" align="center">
            <template #default="{ row }">{{ row.sizeValue || '-' }}</template>
          </el-table-column>
          <el-table-column prop="skuCode" label="SKU编码" width="150" show-overflow-tooltip />
          <el-table-column prop="name" label="SKU名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="availableQty" label="可用库存" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getStockTagType(row.availableQty)" size="small">{{ row.availableQty ?? 0 }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center">
            <template #default="{ row }">
              <el-button type="primary" link icon="Plus" @click="addSkuToImport(row)">加入</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-table :data="importForm.items" border size="small">
          <el-table-column prop="skuCode" label="SKU编码" width="150" show-overflow-tooltip />
          <el-table-column prop="skuName" label="SKU名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="sizeValue" label="码数" width="80" align="center">
            <template #default="{ row }">{{ row.sizeValue || '-' }}</template>
          </el-table-column>
          <el-table-column label="数量" width="140" align="center">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :min="1" :max="999999" size="small" style="width: 110px" />
            </template>
          </el-table-column>
          <el-table-column label="单价" width="140" align="right">
            <template #default="{ row }">
              <el-input-number v-model="row.unitPrice" :min="0" :precision="2" size="small" style="width: 110px" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button type="danger" link icon="Delete" @click="importForm.items.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="importForm.items.length === 0" class="text-sm text-gray-400 mt-2">
          请先选择 SKU / 商品，再点击具体码数加入订单。
        </div>
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
        <el-form-item label="客户快递单号">
          <el-input v-model="returnForm.trackingNo" placeholder="客户退回的快递单号（选填）" />
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
          <el-table-column prop="skuName" label="SKU名称" min-width="100" />
          <el-table-column prop="sizeValue" label="码数" width="80" align="center">
            <template #default="{ row }">{{ row.sizeValue || '-' }}</template>
          </el-table-column>
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

    <!-- 文档导入弹窗 -->
    <el-dialog v-model="fileImportDialogVisible" title="文档导入订单" width="600px" destroy-on-close>
      <div class="mb-4">
        <p class="text-sm text-gray-600 mb-2">支持 Excel (.xlsx) 和 CSV (.csv) 文件，文件大小不超过 10MB</p>
        <p class="text-sm text-gray-600 mb-4">
          模板格式：
          <el-button type="primary" link icon="Download" @click="downloadTemplate">下载模板</el-button>
        </p>
      </div>

      <el-upload
        ref="uploadRef"
        :action="uploadUrl"
        :headers="uploadHeaders"
        :before-upload="beforeUpload"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :on-change="handleFileChange"
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls,.csv"
        drag
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 .xlsx, .xls, .csv 格式文件</div>
        </template>
      </el-upload>

      <el-form v-if="fileImportForm.warehouseId !== undefined" :model="fileImportForm" label-width="80px" class="mt-4">
        <el-form-item label="目标仓库">
          <el-select v-model="fileImportForm.warehouseId" placeholder="选择仓库" style="width: 100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="fileImportDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="fileImporting" @click="handleFileImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { getOrderDetail, getOrderList, importOrder, updateOrderStatus } from '@/api/order'
import type { Order } from '@/api/order'
import { createOutbound } from '@/api/outbound'
import { createReturn, cancelReturnByOrderId } from '@/api/returns'
import { getAllSkuList } from '@/api/product'
import type { Sku } from '@/api/product'
import { getWarehouseList } from '@/api/warehouse'
import type { Warehouse } from '@/api/warehouse'
import { useTable } from '@/composables/useTable'
import { formatDateTime } from '@/utils/format'
import { ORDER_STATUS_MAP } from '@/utils/constants'
import PageHeader from '@/components/PageHeader.vue'

const router = useRouter()

interface ReturnItemForm {
  checked: boolean
  skuId: number
  skuCode: string
  skuName: string
  sizeValue?: string
  orderedQty: number
  quantity: number
}

type SkuListItem = Sku & { productName?: string }

interface SkuGroup {
  key: string
  skuCode: string
  skuName: string
  skus: SkuListItem[]
}

interface ImportItemForm {
  skuId?: number
  skuCode: string
  skuName: string
  sizeValue?: string
  quantity: number
  unitPrice: number
}

const { tableData, loading, pagination, searchParams, handleSearch, handleReset, handlePageChange, handleSizeChange, fetchData } = useTable<Order>(getOrderList)

const warehouses = ref<Warehouse[]>([])
const skuList = ref<SkuListItem[]>([])
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
  items: [] as ImportItemForm[],
})
const selectedSkuGroupKey = ref('')

const importRules: FormRules = {
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
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
  trackingNo: '',
  remark: '',
  items: [] as ReturnItemForm[],
})

const returnRules: FormRules = {
  reason: [{ required: true, message: '请输入退货原因', trigger: 'blur' }],
}

// 文档导入相关
const fileImportDialogVisible = ref(false)
const fileImporting = ref(false)
const uploadRef = ref()
const selectedFile = ref<File | null>(null)
const fileImportForm = reactive({
  warehouseId: undefined as number | undefined,
})

const BASE_URL = 'http://localhost:8080'
const uploadUrl = `${BASE_URL}/api/orders/import-file`
const uploadHeaders = {
  Authorization: `Bearer ${localStorage.getItem('token') || ''}`,
}

const skuGroups = computed(() => {
  const groupMap = new Map<string, SkuGroup>()
  skuList.value.forEach((sku) => {
    const key = String(sku.productId || getBaseSkuCode(sku))
    let group = groupMap.get(key)
    if (!group) {
      group = {
        key,
        skuCode: getBaseSkuCode(sku),
        skuName: getBaseSkuName(sku),
        skus: [],
      }
      groupMap.set(key, group)
    }
    group.skus.push(sku)
  })

  return Array.from(groupMap.values()).map((group) => ({
    ...group,
    skus: group.skus.slice().sort((a, b) => compareSizeValue(normalizeSizeValue(a.sizeValue), normalizeSizeValue(b.sizeValue))),
  }))
})

const selectedSkuGroup = computed(() => skuGroups.value.find((group) => group.key === selectedSkuGroupKey.value))

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

function getStockTagType(quantity?: number): string {
  const value = quantity ?? 0
  if (value <= 0) return 'danger'
  if (value <= 10) return 'warning'
  return 'success'
}

function addSkuToImport(sku: SkuListItem) {
  const existing = importForm.items.find((item) => item.skuId === sku.id)
  if (existing) {
    existing.quantity += 1
    return
  }
  importForm.items.push({
    skuId: sku.id,
    skuCode: sku.skuCode,
    skuName: sku.name,
    sizeValue: sku.sizeValue || '',
    quantity: 1,
    unitPrice: sku.salePrice || 0,
  })
}

function downloadTemplate() {
  // 创建 CSV 模板内容
  const template = 'platformOrderNo,receiverName,receiverPhone,receiverAddress,remark,skuCode,quantity,unitPrice\n示例单号001,张三,13800138000,北京市朝阳区xxx路xxx号,备注,SPU001-42,1,59.90'
  const blob = new Blob(['﻿' + template], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = '订单导入模板.csv'
  link.click()
  URL.revokeObjectURL(link.href)
}

function beforeUpload(file: File) {
  const isValidType = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
    file.type === 'application/vnd.ms-excel' ||
    file.type === 'text/csv' ||
    file.name.endsWith('.xlsx') ||
    file.name.endsWith('.xls') ||
    file.name.endsWith('.csv')
  const isLt10M = file.size / 1024 / 1024 < 10

  if (!isValidType) {
    ElMessage.error('只支持 Excel 或 CSV 文件!')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB!')
    return false
  }
  return true
}

function handleFileChange(file: any) {
  selectedFile.value = file.raw
}

function handleUploadSuccess(response: any) {
  if (response.code === 200) {
    ElMessage.success(`导入成功，共导入 ${response.data || 0} 条订单`)
    fileImportDialogVisible.value = false
    selectedFile.value = null
    fetchData()
  } else {
    ElMessage.error(response.message || '导入失败')
  }
  fileImporting.value = false
}

function handleUploadError() {
  ElMessage.error('上传失败，请检查网络或服务器')
  fileImporting.value = false
}

async function handleFileImport() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择要导入的文件')
    return
  }
  if (!fileImportForm.warehouseId) {
    ElMessage.warning('请选择目标仓库')
    return
  }

  fileImporting.value = true
  const formData = new FormData()
  formData.append('file', selectedFile.value)
  formData.append('warehouseId', String(fileImportForm.warehouseId))

  try {
    const response = await fetch(uploadUrl, {
      method: 'POST',
      headers: uploadHeaders,
      body: formData,
    })
    const result = await response.json()
    if (result.code === 200) {
      ElMessage.success(`导入成功，共导入 ${result.data || 0} 条订单`)
      fileImportDialogVisible.value = false
      selectedFile.value = null
      fetchData()
    } else {
      ElMessage.error(result.message || '导入失败')
    }
  } catch {
    ElMessage.error('导入失败，请检查网络或服务器')
  } finally {
    fileImporting.value = false
  }
}

onMounted(async () => {
  try {
    const res = await getWarehouseList({ page: 1, size: 100 })
    warehouses.value = (res.data.list || []).filter((w: any) => w.warehouseType === 'NORMAL')
  } catch {
    warehouses.value = []
  }
  // 加载SKU列表
  try {
    const res = await getAllSkuList({ page: 1, size: 1000 })
    skuList.value = res.data.list || []
  } catch {
    skuList.value = []
  }
})

async function openImportDialog() {
  Object.assign(importForm, {
    platformOrderNo: '',
    warehouseId: undefined,
    receiverName: '',
    receiverPhone: '',
    receiverAddress: '',
    remark: '',
    items: [],
  })
  selectedSkuGroupKey.value = ''
  // 刷新SKU列表以获取最新库存
  try {
    const skuRes = await getAllSkuList({ page: 1, size: 1000 })
    skuList.value = skuRes.data.list || []
  } catch {}
  importDialogVisible.value = true
}

async function handleImportWarehouseChange(warehouseId: number) {
  // 清空已选明细
  importForm.items = []
  selectedSkuGroupKey.value = ''
  // 根据仓库重新加载SKU库存
  if (warehouseId) {
    try {
      const res = await getAllSkuList({ page: 1, size: 1000, warehouseId })
      skuList.value = res.data.list || []
    } catch {}
  }
}

async function viewDetail(row: Order) {
  try {
    const res = await getOrderDetail(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch {}
}

async function handleCancelOrder(row: Order) {
  try {
    await updateOrderStatus(row.id, 'CANCELLED')
    ElMessage.success('订单已取消')
    fetchData()
  } catch {}
}

async function handleCancelReturn(row: Order) {
  try {
    await cancelReturnByOrderId(row.id)
    ElMessage.success('退货已取消')
    fetchData()
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
      sizeValue: item.sizeValue,
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
      trackingNo: returnForm.trackingNo || undefined,
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
    // 刷新SKU列表以更新库存数量
    try {
      const skuRes = await getAllSkuList({ page: 1, size: 1000 })
      skuList.value = skuRes.data.list || []
    } catch {}
  } catch {} finally {
    importing.value = false
  }
}
</script>
