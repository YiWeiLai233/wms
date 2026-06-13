<template>
  <div class="page-container">
    <PageHeader title="快递管理" subtitle="快递查询与费用统计" />

    <!-- 查询区域 -->
    <div class="card mb-4">
      <el-form :model="queryForm" inline>
        <el-form-item label="快递单号">
          <el-input v-model="queryForm.trackingNo" placeholder="请输入快递单号" clearable style="width: 250px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="快递公司">
          <el-select v-model="queryForm.carrier" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="c in companyList" :key="c.code" :label="c.name" :value="c.code" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">查询快递</el-button>
          <el-button icon="Refresh" @click="resetQueryForm">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 快递查询结果 -->
    <div v-if="expressInfo" class="card mb-4">
      <h3 class="text-lg font-semibold mb-4">快递信息</h3>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="快递单号">
          <span class="font-mono font-bold">{{ expressInfo.trackingNo }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="快递公司">{{ expressInfo.carrierName || expressInfo.carrier }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <el-tag :type="getStatusType(expressInfo.status)">{{ expressInfo.statusDesc || getStatusLabel(expressInfo.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ expressInfo.tracks?.[0]?.time || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 物流轨迹 -->
      <h4 class="text-base font-semibold mt-4 mb-3">物流轨迹</h4>
      <el-timeline v-if="expressInfo.tracks && expressInfo.tracks.length > 0">
        <el-timeline-item
          v-for="(track, index) in expressInfo.tracks"
          :key="index"
          :timestamp="track.time"
          :type="index === 0 ? 'primary' : ''"
          :hollow="index !== 0"
          placement="top"
        >
          <p class="text-sm">{{ track.content }}</p>
          <p v-if="track.location" class="text-xs text-gray-400 mt-1">{{ track.location }}</p>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无物流轨迹" />
    </div>

    <!-- 未查询到提示 -->
    <div v-else-if="hasQueried" class="card mb-4">
      <el-empty description="未查询到快递信息">
        <template #description>
          <p>未查询到快递信息，可能原因：</p>
          <ul class="text-sm text-gray-500 mt-2">
            <li>• 快递单号输入错误</li>
            <li>• 快递公司未匹配</li>
            <li>• 快递100 API 未配置</li>
          </ul>
        </template>
      </el-empty>
    </div>

    <!-- 费用计算工具 -->
    <div class="card mb-4">
      <h3 class="text-lg font-semibold mb-4">快递费用计算</h3>
      <el-form :model="feeForm" inline label-width="80px">
        <el-form-item label="快递公司">
          <el-select v-model="feeForm.companyId" placeholder="选择公司" clearable style="width: 150px" @change="handleCompanyChange">
            <el-option v-for="c in companyList" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="费用模板">
          <el-select v-model="feeForm.templateId" placeholder="选择模板" clearable style="width: 180px">
            <el-option v-for="t in templateList" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="重量(kg)">
          <el-input-number v-model="feeForm.weight" :min="0.1" :precision="2" style="width: 130px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleCalculateFee">计算费用</el-button>
        </el-form-item>
      </el-form>

      <div v-if="feeResult" class="mt-4 p-4 bg-gray-50 rounded">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="总重量">{{ feeResult.totalWeight }} kg</el-descriptions-item>
          <el-descriptions-item label="计费方式">{{ feeResult.billingMethod }}</el-descriptions-item>
          <el-descriptions-item label="总费用">
            <span class="text-red-500 font-bold text-lg">¥{{ feeResult.totalFee?.toFixed(2) }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </div>

    <!-- 出库快递记录 -->
    <div class="card">
      <h3 class="text-lg font-semibold mb-4">出库快递记录</h3>
      <el-form :model="searchParams" inline class="mb-4">
        <el-form-item label="快递单号">
          <el-input v-model="searchParams.trackingNo" placeholder="快递单号" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchParams.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="待出库" value="WAIT_PICKING" />
            <el-option label="拣货中" value="PICKING" />
            <el-option label="已发货" value="SHIPPED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
          <el-button icon="Refresh" @click="handleResetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="outboundNo" label="出库单号" width="160" />
        <el-table-column prop="orderNo" label="订单号" width="140" />
        <el-table-column prop="trackingNo" label="快递单号" width="150">
          <template #default="{ row }">
            <span v-if="row.trackingNo" class="font-mono">{{ row.trackingNo }}</span>
            <span v-else class="text-gray-400">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="warehouseName" label="仓库" width="120" />
        <el-table-column prop="shippingFee" label="快递费" width="100" align="right">
          <template #default="{ row }">
            <span v-if="row.shippingFee" class="text-red-500 font-bold">¥{{ row.shippingFee.toFixed(2) }}</span>
            <span v-else class="text-gray-400">-</span>
          </template>
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
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.trackingNo" type="primary" link icon="Search" @click="quickQuery(row.trackingNo)">查询</el-button>
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

    <!-- 入库快递记录 -->
    <div class="card mt-4">
      <h3 class="text-lg font-semibold mb-4">入库快递记录</h3>
      <el-form :model="returnSearch" inline class="mb-4">
        <el-form-item label="快递单号">
          <el-input v-model="returnSearch.trackingNo" placeholder="客户快递单号" clearable style="width: 160px" @keyup.enter="handleReturnSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="returnSearch.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="(v, k) in RETURN_STATUS_MAP" :key="k" :label="v.label" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleReturnSearch">搜索</el-button>
          <el-button icon="Refresh" @click="handleReturnReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="returnList" v-loading="returnLoading" stripe border>
        <el-table-column prop="returnNo" label="退货单号" width="160" />
        <el-table-column prop="orderNo" label="订单号" width="140" />
        <el-table-column prop="trackingNo" label="客户快递单号" width="150">
          <template #default="{ row }">
            <span v-if="row.trackingNo" class="font-mono">{{ row.trackingNo }}</span>
            <span v-else class="text-gray-400">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="warehouseName" label="仓库" width="120" />
        <el-table-column prop="reason" label="退货原因" min-width="100" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="(RETURN_STATUS_MAP[row.status]?.color as any) || 'info'" size="small">
              {{ RETURN_STATUS_MAP[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.trackingNo" type="primary" link icon="Search" @click="quickQuery(row.trackingNo)">查询</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="returnPagination.page"
          v-model:page-size="returnPagination.size"
          :total="returnPagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="(p: number) => { returnPagination.page = p; fetchReturnList() }"
          @size-change="(s: number) => { returnPagination.size = s; returnPagination.page = 1; fetchReturnList() }"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getOutboundList } from '@/api/outbound'
import type { OutboundOrder } from '@/api/outbound'
import { getReturnList } from '@/api/returns'
import type { ReturnOrder } from '@/api/returns'
import { getCompanyList, getTemplateListByCompany, calculateFee } from '@/api/express'
import type { ExpressCompany, ExpressFeeTemplate } from '@/api/express'
import { useTable } from '@/composables/useTable'
import { formatDateTime } from '@/utils/format'
import { OUTBOUND_STATUS_MAP, RETURN_STATUS_MAP } from '@/utils/constants'
import PageHeader from '@/components/PageHeader.vue'
import request from '@/api/request'

interface ExpressTrack {
  time: string
  content: string
  location?: string
}

interface ExpressInfo {
  trackingNo: string
  carrier: string
  carrierName: string
  status: string
  statusDesc: string
  tracks: ExpressTrack[]
}

interface FeeResult {
  totalWeight: number
  totalFee: number
  billingMethod: string
}

const { tableData, loading, pagination, searchParams, handleSearch, handleReset, handlePageChange, handleSizeChange, fetchData } = useTable<OutboundOrder>(getOutboundList)

const returnSearch = reactive({ trackingNo: '', status: '' })
const returnLoading = ref(false)
const returnList = ref<ReturnOrder[]>([])
const returnPagination = reactive({ page: 1, size: 10, total: 0 })

async function fetchReturnList() {
  returnLoading.value = true
  try {
    const res = await getReturnList({
      page: returnPagination.page,
      size: returnPagination.size,
      trackingNo: returnSearch.trackingNo || undefined,
      status: returnSearch.status || undefined,
    })
    returnList.value = res.data.list || []
    returnPagination.total = res.data.total || 0
  } catch {
    returnList.value = []
  } finally {
    returnLoading.value = false
  }
}

function handleReturnSearch() {
  returnPagination.page = 1
  fetchReturnList()
}

function handleReturnReset() {
  returnSearch.trackingNo = ''
  returnSearch.status = ''
  returnPagination.page = 1
  fetchReturnList()
}

const companyList = ref<ExpressCompany[]>([])
const templateList = ref<ExpressFeeTemplate[]>([])

const queryForm = reactive({
  trackingNo: '',
  carrier: '',
})

const expressInfo = ref<ExpressInfo | null>(null)
const hasQueried = ref(false)

const feeForm = reactive({
  companyId: undefined as number | undefined,
  templateId: undefined as number | undefined,
  weight: 1,
})

const feeResult = ref<FeeResult | null>(null)

function getStatusType(status: string): string {
  const map: Record<string, string> = {
    '0': 'info',
    '1': 'warning',
    '2': '',
    '3': 'warning',
    '4': 'success',
    '5': 'danger',
  }
  return map[status] || 'info'
}

function getStatusLabel(status: string): string {
  const map: Record<string, string> = {
    '0': '待查询',
    '1': '已揽收',
    '2': '运输中',
    '3': '派送中',
    '4': '已签收',
    '5': '异常',
  }
  return map[status] || '未知'
}

function handleResetSearch() {
  searchParams.trackingNo = ''
  searchParams.status = ''
  handleReset()
}

async function handleQuery() {
  if (!queryForm.trackingNo.trim()) {
    ElMessage.warning('请输入快递单号')
    return
  }

  hasQueried.value = true

  try {
    const res = await request.get<any, any>('/express/query', {
      params: {
        trackingNo: queryForm.trackingNo,
        carrier: queryForm.carrier || undefined,
      },
    })

    if (res.code === 200 && res.data) {
      expressInfo.value = res.data
      ElMessage.success('查询成功')
    } else {
      expressInfo.value = null
      ElMessage.warning(res.message || '未查询到快递信息')
    }
  } catch {
    expressInfo.value = null
    ElMessage.error('查询失败，请检查网络或API配置')
  }
}

function resetQueryForm() {
  queryForm.trackingNo = ''
  queryForm.carrier = ''
  expressInfo.value = null
  hasQueried.value = false
}

function quickQuery(trackingNo: string) {
  queryForm.trackingNo = trackingNo
  handleQuery()
}

async function handleCompanyChange(companyId: number) {
  feeForm.templateId = undefined
  templateList.value = []
  if (companyId) {
    try {
      const res = await getTemplateListByCompany(companyId)
      templateList.value = res.data || []
      // 自动选择默认模板
      const defaultTemplate = templateList.value.find(t => t.isDefault === 1)
      if (defaultTemplate) {
        feeForm.templateId = defaultTemplate.id
      }
    } catch {
      templateList.value = []
    }
  }
}

async function handleCalculateFee() {
  if (feeForm.weight <= 0) {
    ElMessage.warning('请输入有效的重量')
    return
  }

  try {
    const res = await calculateFee({
      totalWeight: feeForm.weight,
      templateId: feeForm.templateId || undefined,
    })

    if (res.code === 200) {
      feeResult.value = res.data
    } else {
      ElMessage.error(res.message || '计算失败')
    }
  } catch {
    ElMessage.error('计算失败，请检查网络')
  }
}

onMounted(async () => {
  fetchData()
  fetchReturnList()
  // 加载快递公司列表
  try {
    const res = await getCompanyList()
    companyList.value = res.data || []
  } catch {}
})
</script>
