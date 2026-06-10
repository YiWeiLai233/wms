<template>
  <div class="page-container">
    <PageHeader title="盘点管理">
      <template #actions>
        <el-button type="primary" icon="Plus" @click="openCreateDialog">创建盘点单</el-button>
      </template>
    </PageHeader>

    <!-- 搜索栏 -->
    <div class="card mb-4">
      <el-form :model="searchParams" inline>
        <el-form-item label="仓库">
          <el-select v-model="searchParams.warehouseId" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchParams.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="待盘点" value="PENDING" />
            <el-option label="盘点中" value="CHECKING" />
            <el-option label="已完成" value="COMPLETED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 盘点单列表 -->
    <div class="card">
      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="checkNo" label="盘点单号" width="160" />
        <el-table-column prop="warehouseName" label="盘点仓库" width="140" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="CHECK_STATUS_MAP[row.status]?.color as any" size="small">
              {{ CHECK_STATUS_MAP[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link icon="View" @click="viewDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 'PENDING' || row.status === 'CHECKING'"
              type="success" link icon="EditPen"
              @click="openSubmitDialog(row)"
            >录入结果</el-button>
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

    <!-- 创建盘点单弹窗 -->
    <el-dialog v-model="createDialogVisible" title="创建盘点单" width="480px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="80px">
        <el-form-item label="盘点仓库" prop="warehouseId">
          <el-select v-model="createForm.warehouseId" placeholder="请选择仓库" style="width: 100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="createForm.remark" type="textarea" :rows="3" placeholder="盘点备注（如：月度盘点、年终盘点）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">确定创建</el-button>
      </template>
    </el-dialog>

    <!-- 盘点单详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="盘点单详情" width="800px">
      <el-descriptions :column="3" border class="mb-4">
        <el-descriptions-item label="盘点单号">{{ detail.checkNo }}</el-descriptions-item>
        <el-descriptions-item label="盘点仓库">{{ detail.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="CHECK_STATUS_MAP[detail.status]?.color as any" size="small">
            {{ CHECK_STATUS_MAP[detail.status]?.label }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <h4 class="mb-2 text-sm font-semibold text-gray-700">盘点明细</h4>
      <el-table :data="detail.items || []" border size="small" max-height="400">
        <el-table-column prop="skuCode" label="SKU编码" width="120" />
        <el-table-column prop="skuName" label="SKU名称" min-width="160" />
        <el-table-column prop="locationCode" label="库位" width="100" />
        <el-table-column prop="systemQty" label="系统数量" width="90" align="center" />
        <el-table-column prop="actualQty" label="实盘数量" width="90" align="center">
          <template #default="{ row }">
            <span :class="{ 'font-bold': row.actualQty != null }">{{ row.actualQty ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="diffQty" label="差异数量" width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.diffQty != null" :class="row.diffQty === 0 ? 'text-green-600' : 'text-red-500 font-bold'">
              {{ row.diffQty > 0 ? '+' : '' }}{{ row.diffQty }}
            </span>
            <span v-else class="text-gray-400">-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 录入盘点结果弹窗 -->
    <el-dialog v-model="submitDialogVisible" title="录入盘点结果" width="800px" destroy-on-close>
      <div class="mb-3 text-sm text-gray-500">
        盘点单号：<span class="font-semibold text-gray-700">{{ submitDetail.checkNo }}</span>
        <span class="ml-4">仓库：<span class="font-semibold text-gray-700">{{ submitDetail.warehouseName }}</span></span>
      </div>

      <el-table :data="submitItems" border size="small" max-height="400">
        <el-table-column prop="skuCode" label="SKU编码" width="120" />
        <el-table-column prop="skuName" label="SKU名称" min-width="160" />
        <el-table-column prop="locationCode" label="库位" width="100" />
        <el-table-column prop="systemQty" label="系统数量" width="90" align="center" />
        <el-table-column label="实盘数量" width="140" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="row.actualQty"
              :min="0"
              :max="99999"
              size="small"
              controls-position="right"
              style="width: 110px"
            />
          </template>
        </el-table-column>
        <el-table-column label="差异" width="90" align="center">
          <template #default="{ row }">
            <span
              v-if="row.actualQty != null"
              :class="(row.actualQty - row.systemQty) === 0 ? 'text-green-600' : 'text-red-500 font-bold'"
            >
              {{ (row.actualQty - row.systemQty) > 0 ? '+' : '' }}{{ row.actualQty - row.systemQty }}
            </span>
            <span v-else class="text-gray-400">-</span>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="submitDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitResult">提交盘点结果</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getStockCheckList, getStockCheckDetail, createStockCheck, submitStockCheck } from '@/api/stock'
import type { StockCheck, StockCheckItem } from '@/api/stock'
import { getWarehouseList } from '@/api/warehouse'
import type { Warehouse } from '@/api/warehouse'
import { formatDateTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'

// 盘点状态映射
const CHECK_STATUS_MAP: Record<string, { label: string; color: string }> = {
  PENDING: { label: '待盘点', color: 'warning' },
  CHECKING: { label: '盘点中', color: '' },
  COMPLETED: { label: '已完成', color: 'success' },
}

const warehouses = ref<Warehouse[]>([])
const tableData = ref<StockCheck[]>([])
const loading = ref(false)
const pagination = reactive({ page: 1, size: 10, total: 0 })
const searchParams = reactive({ warehouseId: undefined as number | undefined, status: '' })

// 创建盘点单
const createDialogVisible = ref(false)
const submitting = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({ warehouseId: undefined as number | undefined, remark: '' })
const createRules: FormRules = {
  warehouseId: [{ required: true, message: '请选择盘点仓库', trigger: 'change' }],
}

// 盘点单详情
const detailDialogVisible = ref(false)
const detail = ref<StockCheck>({ id: 0, checkNo: '', warehouseId: 0, warehouseName: '', status: '', remark: '', createdAt: '' })

// 录入盘点结果
const submitDialogVisible = ref(false)
const submitDetail = ref<StockCheck>({ id: 0, checkNo: '', warehouseId: 0, warehouseName: '', status: '', remark: '', createdAt: '' })
const submitItems = ref<(StockCheckItem & { actualQty: number | null })[]>([])

onMounted(async () => {
  try {
    const res = await getWarehouseList({ page: 1, size: 100 })
    warehouses.value = res.data.list || []
  } catch {}
  fetchData()
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getStockCheckList({
      pageNum: pagination.page,
      pageSize: pagination.size,
      warehouseId: searchParams.warehouseId || undefined,
      status: searchParams.status || undefined,
    })
    tableData.value = res.data.list || []
    pagination.total = res.data.total || 0
  } catch {
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handleReset() {
  searchParams.warehouseId = undefined
  searchParams.status = ''
  pagination.page = 1
  fetchData()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchData()
}

function handleSizeChange(size: number) {
  pagination.size = size
  pagination.page = 1
  fetchData()
}

// 创建盘点单
function openCreateDialog() {
  Object.assign(createForm, { warehouseId: undefined, remark: '' })
  createDialogVisible.value = true
}

async function handleCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await createStockCheck({ warehouseId: createForm.warehouseId!, remark: createForm.remark })
    ElMessage.success('盘点单创建成功')
    createDialogVisible.value = false
    fetchData()
  } catch {} finally {
    submitting.value = false
  }
}

// 查看详情
async function viewDetail(row: StockCheck) {
  try {
    const res = await getStockCheckDetail(row.id)
    detail.value = res.data
    detailDialogVisible.value = true
  } catch {}
}

// 录入盘点结果
async function openSubmitDialog(row: StockCheck) {
  try {
    const res = await getStockCheckDetail(row.id)
    const data = res.data
    submitDetail.value = data
    submitItems.value = (data.items || []).map((item) => ({
      ...item,
      actualQty: item.actualQty ?? item.systemQty, // 默认填系统数量
    }))
    submitDialogVisible.value = true
  } catch {}
}

async function handleSubmitResult() {
  // 检查是否所有项都填了实盘数量
  const hasEmpty = submitItems.value.some((item) => item.actualQty == null)
  if (hasEmpty) {
    ElMessage.warning('请填写所有明细的实盘数量')
    return
  }

  await ElMessageBox.confirm('提交后将无法修改，确定提交盘点结果吗？', '确认提交', { type: 'warning' })

  submitting.value = true
  try {
    await submitStockCheck({
      checkId: submitDetail.value.id,
      items: submitItems.value.map((item) => ({
        itemId: item.id,
        actualQty: item.actualQty!,
      })),
    })
    ElMessage.success('盘点结果提交成功')
    submitDialogVisible.value = false
    fetchData()
  } catch {} finally {
    submitting.value = false
  }
}
</script>
