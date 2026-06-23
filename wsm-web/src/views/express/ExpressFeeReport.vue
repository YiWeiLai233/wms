<template>
  <div class="page-container">
    <PageHeader title="快递费用统计" subtitle="按日期范围查询快递费用明细及汇总" />

    <!-- 查询条件 -->
    <div class="card mb-4">
      <el-form :model="queryForm" inline>
        <el-form-item label="订单号">
          <el-input v-model="queryForm.orderNo" placeholder="订单号" clearable style="width: 140px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="平台单号">
          <el-input v-model="queryForm.platformOrderNo" placeholder="平台单号" clearable style="width: 140px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="日期范围">
          <DateRangePicker v-model="queryForm.dateRange" width="280px" @change="handleQuery" />
        </el-form-item>
        <el-form-item label="快递公司">
          <el-select v-model="queryForm.expressCompanyId" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="c in companyList" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 汇总卡片 -->
    <el-row :gutter="20" class="mb-4">
      <el-col :span="4">
        <div class="card text-center">
          <div class="text-sm text-gray-500 mb-1">快递总单数</div>
          <div class="text-3xl font-bold text-blue-600">{{ reportData.totalCount || 0 }}</div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="card text-center">
          <div class="text-sm text-gray-500 mb-1">快递总费用</div>
          <div class="text-3xl font-bold text-red-500">¥{{ (reportData.totalFee || 0).toFixed(2) }}</div>
        </div>
      </el-col>
      <el-col :span="5">
        <div class="card text-center">
          <div class="text-sm text-gray-500 mb-1">出库快递费</div>
          <div class="text-2xl font-bold text-green-600">¥{{ (reportData.outboundFee || 0).toFixed(2) }}</div>
          <div class="text-xs text-gray-400">{{ reportData.outboundCount || 0 }} 单</div>
        </div>
      </el-col>
      <el-col :span="5">
        <div class="card text-center">
          <div class="text-sm text-gray-500 mb-1">退货快递费</div>
          <div class="text-2xl font-bold text-orange-500">¥{{ (reportData.returnFee || 0).toFixed(2) }}</div>
          <div class="text-xs text-gray-400">{{ reportData.returnCount || 0 }} 单</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="card text-center">
          <div class="text-sm text-gray-500 mb-1">换货快递费</div>
          <div class="text-2xl font-bold text-purple-500">¥{{ (reportData.exchangeFee || 0).toFixed(2) }}</div>
          <div class="text-xs text-gray-400">{{ reportData.exchangeCount || 0 }} 单</div>
        </div>
      </el-col>
    </el-row>

    <!-- 明细表格 -->
    <div class="card">
      <el-table :data="reportData.items || []" v-loading="loading" stripe border>
        <el-table-column prop="bizType" label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.bizType === 'OUTBOUND' ? 'success' : row.bizType === 'EXCHANGE' ? 'info' : 'warning'" size="small" :class="{ 'exchange-tag': row.bizType === 'EXCHANGE' }">
              {{ row.bizTypeName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="bizNo" label="单号" width="160" />
        <el-table-column prop="orderNo" label="订单号" width="140" />
        <el-table-column prop="platformOrderNo" label="平台单号" width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.platformOrderNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="expressCompanyName" label="快递公司" width="120">
          <template #default="{ row }">{{ row.expressCompanyName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="trackingNo" label="快递单号" width="150">
          <template #default="{ row }">
            <span v-if="row.trackingNo" class="font-mono">{{ row.trackingNo }}</span>
            <span v-else class="text-gray-400">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="shippingFee" label="快递费用" width="120" align="right">
          <template #default="{ row }">
            <span class="text-red-500 font-bold">¥{{ row.shippingFee?.toFixed(2) || '0.00' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" min-width="170">
          <template #default="{ row }">{{ row.createdAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <template v-if="canEdit(row)">
              <el-button type="primary" link icon="Edit" @click="openEditDialog(row)">修改</el-button>
              <el-popconfirm
                title="确定删除该快递费用记录吗？删除后将从统计中移除"
                @confirm="handleDelete(row)"
              >
                <template #reference>
                  <el-button type="danger" link icon="Delete">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
            <span v-else class="text-gray-400">-</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && (!reportData.items || reportData.items.length === 0)" description="暂无数据，请选择日期范围查询" />
    </div>

    <!-- 修改快递费用 -->
    <el-dialog v-model="editDialogVisible" title="修改快递费用" width="460px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="90px">
        <el-form-item label="单号">
          <el-input :model-value="editForm.bizNo" disabled />
        </el-form-item>
        <el-form-item label="快递公司" prop="expressCompanyId">
          <el-select v-model="editForm.expressCompanyId" placeholder="请选择快递公司" style="width: 100%">
            <el-option v-for="c in companyList" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="快递费用" prop="shippingFee">
          <el-input-number v-model="editForm.shippingFee" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editing" @click="handleEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { deleteExpressFeeItem, getExpressFeeReport, updateExpressFeeItem } from '@/api/report'
import type { ExpressFeeItem, ExpressFeeReport } from '@/api/report'
import { getCompanyList } from '@/api/express'
import type { ExpressCompany } from '@/api/express'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import DateRangePicker from '@/components/DateRangePicker.vue'

const loading = ref(false)
const companyList = ref<ExpressCompany[]>([])
const reportData = ref<ExpressFeeReport>({ totalFee: 0, totalCount: 0, items: [] })
const editDialogVisible = ref(false)
const editing = ref(false)
const editFormRef = ref<FormInstance>()

const queryForm = reactive({
  orderNo: '',
  platformOrderNo: '',
  dateRange: [] as string[],
  expressCompanyId: undefined as number | undefined,
})

const editForm = reactive({
  id: 0,
  bizType: '',
  bizNo: '',
  expressCompanyId: undefined as number | undefined,
  shippingFee: undefined as number | undefined,
})

const editRules: FormRules = {
  expressCompanyId: [{ required: true, message: '请选择快递公司', trigger: 'change' }],
  shippingFee: [{ required: true, message: '请输入快递费用', trigger: 'change' }],
}

async function handleQuery() {
  loading.value = true
  try {
    const res = await getExpressFeeReport({
      orderNo: queryForm.orderNo || undefined,
      platformOrderNo: queryForm.platformOrderNo || undefined,
      startTime: queryForm.dateRange?.[0] || undefined,
      endTime: queryForm.dateRange?.[1] || undefined,
      expressCompanyId: queryForm.expressCompanyId || undefined,
    })
    reportData.value = res.data || { totalFee: 0, totalCount: 0, items: [] }
  } catch {
    reportData.value = { totalFee: 0, totalCount: 0, items: [] }
  } finally {
    loading.value = false
  }
}

function handleReset() {
  queryForm.orderNo = ''
  queryForm.platformOrderNo = ''
  queryForm.dateRange = []
  queryForm.expressCompanyId = undefined
  reportData.value = { totalFee: 0, totalCount: 0, items: [] }
}

function canEdit(row: ExpressFeeItem) {
  return row.bizType === 'OUTBOUND' || row.bizType === 'EXCHANGE'
}

function openEditDialog(row: ExpressFeeItem) {
  Object.assign(editForm, {
    id: row.id,
    bizType: row.bizType,
    bizNo: row.bizNo,
    expressCompanyId: row.expressCompanyId || undefined,
    shippingFee: row.shippingFee ?? undefined,
  })
  editDialogVisible.value = true
}

async function handleEdit() {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid || !editForm.expressCompanyId || editForm.shippingFee === undefined) return

  editing.value = true
  try {
    await updateExpressFeeItem(editForm.bizType, editForm.id, {
      expressCompanyId: editForm.expressCompanyId,
      shippingFee: editForm.shippingFee,
    })
    ElMessage.success('快递费用已修改')
    editDialogVisible.value = false
    handleQuery()
  } catch {} finally {
    editing.value = false
  }
}

async function handleDelete(row: ExpressFeeItem) {
  try {
    await deleteExpressFeeItem(row.bizType, row.id)
    ElMessage.success('快递费用记录已删除')
    handleQuery()
  } catch {}
}

onMounted(async () => {
  try {
    const res = await getCompanyList()
    companyList.value = res.data || []
  } catch {}
  // 自动加载数据
  handleQuery()
})
</script>

<style scoped>
/* 换货标签 - 紫色 */
.exchange-tag {
  --el-tag-bg-color: #f3e8ff !important;
  --el-tag-border-color: #d8b4fe !important;
  --el-tag-text-color: #7c3aed !important;
}
</style>
