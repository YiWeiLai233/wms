<template>
  <div class="page-container">
    <PageHeader title="换货管理" class="order-sticky-header" :class="{ 'title-collapsed': titleCollapsed }">
      <template #actions>
        <el-button type="primary" icon="Plus" @click="openCreateDialog">创建换货单</el-button>
      </template>
    </PageHeader>

    <div class="card mb-4">
      <el-form :model="searchParams" inline>
        <el-form-item label="换货单号">
          <el-input v-model="searchParams.exchangeNo" placeholder="换货单号" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="searchParams.orderNo" placeholder="订单号" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="平台单号">
          <el-input v-model="searchParams.platformOrderNo" placeholder="平台订单号" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchParams.status" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="(v, k) in EXCHANGE_STATUS_MAP" :key="k" :label="v.label" :value="k" />
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
        <el-table-column prop="exchangeNo" label="换货单号" width="170" />
        <el-table-column prop="platformOrderNo" label="平台单号" width="160">
          <template #default="{ row }">{{ row.platformOrderNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="orderNo" label="订单号" width="160" />
        <el-table-column prop="warehouseName" label="仓库" width="140" />
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="(EXCHANGE_STATUS_MAP[row.status]?.color as any) || 'info'" size="small">
              {{ EXCHANGE_STATUS_MAP[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="换货原因" min-width="120" />
        <el-table-column label="责任方" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.remark?.includes('客户自付快递费') ? 'warning' : 'success'" size="small">
              {{ row.remark?.includes('客户自付快递费') ? '客户' : '我们' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="returnTrackingNo" label="退回快递号" width="140">
          <template #default="{ row }">{{ row.returnTrackingNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="expressCompanyName" label="快递公司" width="120">
          <template #default="{ row }">{{ row.expressCompanyName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="shippingFee" label="快递费" width="100" align="right">
          <template #default="{ row }">
            <el-tag v-if="row.remark?.includes('客户自付快递费')" type="warning" size="small">客户自付</el-tag>
            <span v-else>{{ row.shippingFee ? `¥${row.shippingFee.toFixed(2)}` : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link icon="View" @click="viewDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'PENDING_RETURN' || row.status === 'RETURNED'" type="warning" link icon="Stamp" @click="openCheckDialog(row)">
              收货质检
            </el-button>
            <el-button v-if="row.status === 'CHECKED'" type="primary" link icon="TopRight" @click="handleShip(row)">
              发货
            </el-button>
            <el-popconfirm
              v-if="canCancel(row.status)"
              title="确定取消换货单吗？"
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

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="换货单详情" width="800px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="换货单号">{{ detail.exchangeNo }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="平台单号">{{ detail.platformOrderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detail.warehouseName || detail.warehouseId }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="(EXCHANGE_STATUS_MAP[detail.status]?.color as any) || 'info'" size="small">
            {{ EXCHANGE_STATUS_MAP[detail.status]?.label || detail.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="换货原因">{{ detail.reason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="责任方">
          <el-tag :type="detail.remark?.includes('客户自付快递费') ? 'warning' : 'success'" size="small">
            {{ detail.remark?.includes('客户自付快递费') ? '客户原因' : '我们原因' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="退回快递单号">{{ detail.returnTrackingNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="快递公司">{{ detail.expressCompanyName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="快递费">
          <template v-if="detail.remark?.includes('客户自付快递费')">
            <el-tag type="warning" size="small">客户自付</el-tag>
          </template>
          <template v-else>{{ detail.shippingFee ? `¥${detail.shippingFee.toFixed(2)}` : '-' }}</template>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 退回商品 -->
      <h4 class="mt-4 mb-2 text-sm font-semibold text-gray-700">退回商品</h4>
      <el-table :data="returnItems" border size="small">
        <el-table-column prop="skuCode" label="SKU编码" width="130" />
        <el-table-column prop="skuName" label="SKU名称" min-width="100" />
        <el-table-column prop="sizeValue" label="码数" width="80" align="center">
          <template #default="{ row }">{{ row.sizeValue || '-' }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" align="center" />
        <el-table-column prop="qualityStatus" label="质检结果" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.qualityStatus" :type="(QUALITY_STATUS_MAP[row.qualityStatus]?.color as any) || 'info'" size="small">
              {{ QUALITY_STATUS_MAP[row.qualityStatus]?.label }}
            </el-tag>
            <span v-else class="text-gray-400">待质检</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 换出商品 -->
      <h4 class="mt-4 mb-2 text-sm font-semibold text-gray-700">换出商品</h4>
      <el-table :data="exchangeItems" border size="small">
        <el-table-column prop="skuCode" label="SKU编码" width="130" />
        <el-table-column prop="skuName" label="SKU名称" min-width="100" />
        <el-table-column prop="sizeValue" label="码数" width="80" align="center">
          <template #default="{ row }">{{ row.sizeValue || '-' }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" align="center" />
      </el-table>
    </el-dialog>

    <!-- 质检弹窗 -->
    <el-dialog v-model="checkDialogVisible" title="收货质检" width="700px" destroy-on-close>
      <p class="mb-3 text-sm text-gray-500">请为退回商品选择质检结果</p>
      <el-table :data="checkItems" border size="small">
        <el-table-column label="图片" width="60" align="center">
          <template #default="{ row }">
            <ImagePreview :src="row.image" />
          </template>
        </el-table-column>
        <el-table-column prop="skuCode" label="SKU编码" width="130" />
        <el-table-column prop="skuName" label="SKU名称" min-width="100" />
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
      <template #footer>
        <el-button @click="checkDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="checking" @click="handleCheck">提交质检</el-button>
      </template>
    </el-dialog>

    <!-- 发货弹窗 -->
    <el-dialog v-model="shipDialogVisible" title="换货发货" width="700px" destroy-on-close>
      <el-descriptions :column="2" border size="small" class="mb-4">
        <el-descriptions-item label="换货单号">{{ shipData.exchangeNo }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ shipData.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="平台单号">{{ shipData.platformOrderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ shipData.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="责任方">
          <el-tag :type="isSellerResponsible ? 'success' : 'warning'" size="small">
            {{ isSellerResponsible ? '我们原因' : '客户原因' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <h4 class="mb-2 text-sm font-semibold text-gray-700">换出商品</h4>
      <el-table :data="shipExchangeItems" border size="small" class="mb-4" max-height="200">
        <el-table-column label="图片" width="60" align="center">
          <template #default="{ row }">
            <ImagePreview :src="row.image" />
          </template>
        </el-table-column>
        <el-table-column prop="skuCode" label="SKU编码" width="130" />
        <el-table-column prop="skuName" label="SKU名称" min-width="100" />
        <el-table-column prop="sizeValue" label="码数" width="80" align="center">
          <template #default="{ row }">{{ row.sizeValue || '-' }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" align="center" />
      </el-table>

      <el-form :model="shipForm" label-width="90px">
        <el-form-item label="快递单号">
          <el-input v-model="shipForm.trackingNo" placeholder="快递单号" />
        </el-form-item>
        <!-- 我们原因：可填退回运费 -->
        <el-form-item v-if="isSellerResponsible" label="退回运费">
          <el-input-number v-model="shipForm.returnShippingFee" :min="0" :precision="2" style="width: 100%" />
          <div class="text-xs text-gray-400 mt-1">客户退回商品的运费（我们承担）</div>
        </el-form-item>
        <el-divider content-position="left">换货快递费（模板计算）</el-divider>
        <el-form-item label="快递公司">
          <el-select v-model="shipForm.expressCompanyId" placeholder="选择快递公司" clearable style="width: 100%">
            <el-option v-for="c in companyList" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="费用模板">
          <el-select v-model="shipForm.feeTemplateId" placeholder="选择模板自动计算" clearable style="width: 100%" @change="handleShipTemplateChange">
            <el-option v-for="t in shipTemplateList" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预估重量(kg)">
          <el-input-number v-model="shipForm.estimatedWeight" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="换货快递费">
          <el-input-number v-model="shipForm.shippingFee" :min="0" :precision="2" style="width: 100%" />
          <div class="text-xs text-gray-400 mt-1">选择模板后自动计算，也可手动修改</div>
        </el-form-item>
        <el-form-item v-if="isSellerResponsible" label="总费用">
          <div class="text-lg font-bold text-red-500">¥{{ totalShippingFee.toFixed(2) }}</div>
          <div class="text-xs text-gray-400">换货快递费 + 退回运费</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipping" @click="handleShipSubmit">确认发货</el-button>
      </template>
    </el-dialog>

    <!-- 创建换货单弹窗 -->
    <el-dialog v-model="createDialogVisible" title="创建换货单" width="900px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="订单号" prop="orderNo">
          <el-input v-model="createForm.orderNo" placeholder="输入订单号后回车查询" @keyup.enter="loadOrderForExchange">
            <template #append>
              <el-button icon="Search" @click="loadOrderForExchange" />
            </template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="exchangeOrder" label="订单信息">
          <div class="text-sm text-gray-600">
            平台单号：{{ exchangeOrder.platformOrderNo || '-' }}，收件人：{{ exchangeOrder.receiverName }}，状态：{{ ORDER_STATUS_MAP[exchangeOrder.orderStatus]?.label }}
          </div>
        </el-form-item>
        <el-form-item label="仓库" prop="warehouseId">
          <el-select v-model="createForm.warehouseId" placeholder="选择发货仓库" style="width: 100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="换货原因">
          <el-select v-model="createForm.reason" filterable allow-create default-first-option placeholder="请选择或输入换货原因" style="width: 100%">
            <el-option label="尺码不合适" value="尺码不合适" />
            <el-option label="商品质量问题" value="商品质量问题" />
            <el-option label="商品与描述不符" value="商品与描述不符" />
            <el-option label="发错货" value="发错货" />
            <el-option label="快递丢失/损坏" value="快递丢失/损坏" />
            <el-option label="客户要求换货" value="客户要求换货" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="责任方">
          <el-radio-group v-model="createForm.responsibleParty" @change="handleCreateResponsibleChange">
            <el-radio value="CUSTOMER">客户原因（客户出快递费）</el-radio>
            <el-radio value="SELLER">我们/快递原因（我们出快递费）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="退回快递号">
          <el-input v-model="createForm.returnTrackingNo" placeholder="客户退回的快递单号（选填）" />
        </el-form-item>
        <el-form-item label="快递公司">
          <el-select v-model="createForm.expressCompanyId" placeholder="选择快递公司（选填）" clearable style="width: 100%">
            <el-option v-for="c in companyList" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <!-- 客户原因：显示客户自付 -->
        <el-form-item v-if="createForm.responsibleParty === 'CUSTOMER'" label="快递费">
          <el-tag type="warning" size="large">客户自付快递费</el-tag>
        </el-form-item>
        <!-- 我们原因：通过模板计算快递费 -->
        <template v-if="createForm.responsibleParty === 'SELLER'">
          <el-form-item label="费用模板">
            <el-select v-model="createForm.feeTemplateId" placeholder="选择模板自动计算" clearable style="width: 100%" @change="handleCreateTemplateChange">
              <el-option v-for="t in createTemplateList" :key="t.id" :label="t.name" :value="t.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="预估重量(kg)">
            <el-input-number v-model="createForm.estimatedWeight" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
          <el-form-item label="快递费用">
            <el-input-number v-model="createForm.shippingFee" :min="0" :precision="2" style="width: 100%" />
            <div class="text-xs text-gray-400 mt-1">选择模板后自动计算，也可手动修改</div>
          </el-form-item>
        </template>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" placeholder="备注（选填）" />
        </el-form-item>
      </el-form>

      <div v-if="exchangeOrder">
        <h4 class="mb-2 text-sm font-semibold text-gray-700">退回商品（原订单商品）</h4>
        <el-table :data="exchangeOrder.items || []" border size="small" class="mb-4">
          <el-table-column prop="skuCode" label="SKU编码" width="130" />
          <el-table-column prop="skuName" label="SKU名称" min-width="100" />
          <el-table-column prop="sizeValue" label="码数" width="80" align="center">
            <template #default="{ row }">{{ row.sizeValue || '-' }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" align="center" />
        </el-table>

        <div class="flex items-center justify-between mb-2">
          <h4 class="text-sm font-semibold text-gray-700">换出商品</h4>
          <div class="flex items-center gap-2">
            <el-select v-model="createForm.shipWarehouseId" placeholder="选择出库仓库" size="small" style="width: 160px" @change="handleCreateWarehouseChange">
              <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
            </el-select>
            <el-button type="primary" size="small" icon="Plus" :disabled="!createForm.shipWarehouseId" @click="openSkuSelector">添加换出商品</el-button>
          </div>
        </div>
        <el-table :data="createForm.items" border size="small">
          <el-table-column label="图片" width="60" align="center">
            <template #default="{ row }">
              <ImagePreview :src="row.image" />
            </template>
          </el-table-column>
          <el-table-column prop="skuCode" label="SKU编码" width="130" />
          <el-table-column prop="skuName" label="SKU名称" min-width="100" />
          <el-table-column prop="sizeValue" label="码数" width="80" align="center">
            <template #default="{ row }">{{ row.sizeValue || '-' }}</template>
          </el-table-column>
          <el-table-column label="数量" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :min="1" size="small" style="width: 90px" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button type="danger" link icon="Delete" @click="createForm.items.splice($index, 1)" />
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- SKU 选择器 -->
      <el-dialog v-model="skuSelectorVisible" title="选择换出商品" width="800px" append-to-body destroy-on-close>
        <el-input v-model="skuSearchKeyword" placeholder="搜索SKU编码或名称" clearable class="mb-3" />
        <el-table :data="filteredSkuList" border size="small" max-height="400" @row-click="addExchangeItem">
          <el-table-column label="图片" width="60" align="center">
            <template #default="{ row }">
              <ImagePreview :src="row.image || row.mainImage" />
            </template>
          </el-table-column>
          <el-table-column prop="skuCode" label="SKU编码" width="140" />
          <el-table-column prop="name" label="SKU名称" min-width="120" />
          <el-table-column prop="sizeValue" label="码数" width="80" align="center">
            <template #default="{ row }">{{ row.sizeValue || '-' }}</template>
          </el-table-column>
          <el-table-column prop="availableQty" label="可用库存" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getStockTagType(row.availableQty, row.lowStockThreshold, row.outOfStockThreshold)" size="small">{{ row.availableQty ?? 0 }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-dialog>

      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建换货单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getExchangeList, getExchangeDetail, createExchange, checkExchange, shipExchange, cancelExchange } from '@/api/exchange'
import type { ExchangeOrder } from '@/api/exchange'
import { getOrderDetail } from '@/api/order'
import { getAllSkuList } from '@/api/product'
import { getCompanyList } from '@/api/express'
import { getWarehouseList } from '@/api/warehouse'
import type { Warehouse } from '@/api/warehouse'
import { useTable } from '@/composables/useTable'
import { formatDateTime } from '@/utils/format'
import { EXCHANGE_STATUS_MAP, QUALITY_STATUS_MAP, ORDER_STATUS_MAP } from '@/utils/constants'
import PageHeader from '@/components/PageHeader.vue'
import ImagePreview from '@/components/ImagePreview.vue'


const route = useRoute()
const { tableData, loading, pagination, searchParams, handleSearch, handleReset, handlePageChange, handleSizeChange, fetchData } = useTable<ExchangeOrder>(getExchangeList)

const warehouses = ref<Warehouse[]>([])
const companyList = ref<any[]>([])
const detailVisible = ref(false)
const detail = ref<Partial<ExchangeOrder>>({})
const headerRef = ref()
const titleCollapsed = ref(false)

// 质检相关
const checkDialogVisible = ref(false)
const checking = ref(false)
const checkExchangeId = ref(0)
const checkItems = ref<{ itemId: number; skuCode: string; skuName: string; sizeValue?: string; quantity: number; qualityStatus: string }[]>([])

// 发货相关
const shipDialogVisible = ref(false)
const shipping = ref(false)
const shipData = ref<any>({})
const shipExchangeItems = ref<any[]>([])
const shipForm = reactive({
  expressCompanyId: undefined as number | undefined,
  trackingNo: '',
  feeTemplateId: undefined as number | undefined,
  estimatedWeight: undefined as number | undefined,
  shippingFee: undefined as number | undefined,
  returnShippingFee: undefined as number | undefined,
})
const shipTemplateList = ref<any[]>([])
const shipTemplateDetail = ref<any>(null)

// 监听重量变化，自动计算快递费
watch(
  () => shipForm.estimatedWeight,
  () => {
    if (shipForm.estimatedWeight && shipForm.estimatedWeight > 0 && shipTemplateDetail.value) {
      calculateShipFee()
    }
  }
)

// 创建相关
const createDialogVisible = ref(false)
const creating = ref(false)
const createFormRef = ref<FormInstance>()
const exchangeOrder = ref<any>(null)
const skuSelectorVisible = ref(false)
const skuSearchKeyword = ref('')
const skuList = ref<any[]>([])

const createForm = ref({
  orderNo: '',
  warehouseId: undefined as number | undefined,
  shipWarehouseId: undefined as number | undefined,
  reason: '',
  responsibleParty: 'SELLER' as 'CUSTOMER' | 'SELLER',
  returnTrackingNo: '',
  expressCompanyId: undefined as number | undefined,
  feeTemplateId: undefined as number | undefined,
  estimatedWeight: undefined as number | undefined,
  shippingFee: undefined as number | undefined,
  remark: '',
  items: [] as { skuId: number; skuCode: string; skuName: string; sizeValue?: string; quantity: number; image?: string }[],
})
const createTemplateList = ref<any[]>([])
const createTemplateDetail = ref<any>(null)

// 监听创建表单重量变化，自动计算快递费
watch(
  () => createForm.value.estimatedWeight,
  () => {
    if (createForm.value.estimatedWeight && createForm.value.estimatedWeight > 0 && createTemplateDetail.value) {
      calculateCreateFee()
    }
  }
)

const createRules: FormRules = {
  orderNo: [{ required: true, message: '请输入订单号', trigger: 'blur' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
}

const returnItems = computed(() => (detail.value.items || []).filter((i: any) => i.itemType === 'RETURN_ITEM'))
const exchangeItems = computed(() => (detail.value.items || []).filter((i: any) => i.itemType === 'EXCHANGE_ITEM'))

const filteredSkuList = computed(() => {
  if (!skuSearchKeyword.value) return skuList.value
  const kw = skuSearchKeyword.value.toLowerCase()
  return skuList.value.filter((s: any) =>
    s.skuCode?.toLowerCase().includes(kw) || s.name?.toLowerCase().includes(kw)
  )
})

// 责任方判断
const isSellerResponsible = computed(() => {
  return shipData.value.remark?.includes('客户自付快递费') ? false : true
})

// 总运费 = 退回运费 + 换货快递费
const totalShippingFee = computed(() => {
  const returnFee = shipForm.returnShippingFee || 0
  const exchangeFee = shipForm.shippingFee || 0
  return returnFee + exchangeFee
})

onMounted(async () => {
  try {
    const res = await getWarehouseList({ page: 1, size: 100 })
    warehouses.value = (res.data.list || []).filter((w: any) => w.warehouseType === 'NORMAL')
  } catch {
    warehouses.value = []
  }

  // 加载快递公司列表
  try {
    const res = await getCompanyList()
    companyList.value = res.data || []
  } catch {
    companyList.value = []
  }

  // 从URL参数预填搜索条件
  const queryOrderNo = route.query.orderNo
  if (queryOrderNo) {
    searchParams.orderNo = queryOrderNo as string
    handleSearch()
  }

  // 监听滚动
  const scrollContainer = document.querySelector('.layout-content')
  const headerEl = headerRef.value?.$el
  if (scrollContainer && headerEl) {
    const stickyTop = headerEl.offsetTop
    const onScroll = () => {
      titleCollapsed.value = scrollContainer.scrollTop >= stickyTop
    }
    scrollContainer.addEventListener('scroll', onScroll, { passive: true })
    onBeforeUnmount(() => {
      scrollContainer.removeEventListener('scroll', onScroll)
    })
  }
})

function canCancel(status: string) {
  return status === 'PENDING_RETURN' || status === 'RETURNED' || status === 'CHECKED'
}

async function viewDetail(row: ExchangeOrder) {
  try {
    const res = await getExchangeDetail(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch {}
}

// 质检
async function openCheckDialog(row: ExchangeOrder) {
  checkExchangeId.value = row.id
  try {
    const res = await getExchangeDetail(row.id)
    const data = res.data
    checkItems.value = (data.items || [])
      .filter((i: any) => i.itemType === 'RETURN_ITEM')
      .map((i: any) => ({
        itemId: i.id,
        skuCode: i.skuCode,
        skuName: i.skuName || i.skuCode,
        sizeValue: i.sizeValue,
        image: i.image || '',
        quantity: i.quantity,
        qualityStatus: i.qualityStatus || 'SELLABLE',
      }))
  } catch { return }
  checkDialogVisible.value = true
}

async function handleCheck() {
  checking.value = true
  try {
    await checkExchange({
      exchangeId: checkExchangeId.value,
      items: checkItems.value.map(i => ({ itemId: i.itemId, qualityStatus: i.qualityStatus })),
    })
    ElMessage.success('质检完成')
    checkDialogVisible.value = false
    fetchData()
  } catch {} finally {
    checking.value = false
  }
}

// 发货模板计算
async function loadShipTemplates() {
  try {
    const res = await getCompanyList()
    const companies = res.data || []
    const returnCompany = companies.find((c: any) => c.name.includes('换货'))
    const targetCompany = returnCompany || companies[0]
    if (targetCompany) {
      const { getTemplateListByCompany } = await import('@/api/express')
      const templateRes = await getTemplateListByCompany(targetCompany.id)
      shipTemplateList.value = templateRes.data || []
      const defaultTemplate = shipTemplateList.value.find((t: any) => t.isDefault === 1)
      if (defaultTemplate) {
        shipForm.feeTemplateId = defaultTemplate.id
        handleShipTemplateChange(defaultTemplate.id)
      }
    }
  } catch {}
}

async function handleShipTemplateChange(templateId: number | undefined) {
  if (templateId) {
    try {
      const { getTemplateDetail } = await import('@/api/express')
      const res = await getTemplateDetail(templateId)
      shipTemplateDetail.value = res.data
    } catch {
      shipTemplateDetail.value = null
    }
    if (shipForm.estimatedWeight && shipForm.estimatedWeight > 0) {
      calculateShipFee()
    }
  } else {
    shipTemplateDetail.value = null
  }
}

function calculateShipFee() {
  if (!shipTemplateDetail.value || !shipForm.estimatedWeight || shipForm.estimatedWeight <= 0) return

  const template = shipTemplateDetail.value

  if (template.templateType === 'FIRST_CONTINUE') {
    const firstWeight = template.firstWeight || 1
    const firstFee = template.firstFee || 0
    const additionalWeight = template.additionalWeight || 1
    const additionalFee = template.additionalFee || 0

    if (shipForm.estimatedWeight <= firstWeight) {
      shipForm.shippingFee = firstFee
    } else {
      const extraWeight = shipForm.estimatedWeight - firstWeight
      const extraUnits = Math.ceil(extraWeight / additionalWeight)
      shipForm.shippingFee = firstFee + extraUnits * additionalFee
    }
  } else if (template.steps && template.steps.length > 0) {
    const sortedSteps = [...template.steps].sort((a: any, b: any) => a.minWeight - b.minWeight)
    const matchedStep = sortedSteps.find((s: any) =>
      shipForm.estimatedWeight! >= s.minWeight && shipForm.estimatedWeight! < s.maxWeight
    )
    if (matchedStep) {
      shipForm.shippingFee = matchedStep.fee
    } else {
      const lastStep = sortedSteps[sortedSteps.length - 1]
      if (shipForm.estimatedWeight! >= lastStep.minWeight) {
        shipForm.shippingFee = lastStep.fee
      } else {
        shipForm.shippingFee = undefined
      }
    }
  }
}

// 发货
async function handleShip(row: ExchangeOrder) {
  try {
    const res = await getExchangeDetail(row.id)
    const data = res.data
    shipData.value = data
    shipExchangeItems.value = (data.items || []).filter((i: any) => i.itemType === 'EXCHANGE_ITEM')
    shipForm.expressCompanyId = data.expressCompanyId || undefined
    shipForm.trackingNo = ''
    shipForm.feeTemplateId = undefined
    shipForm.estimatedWeight = undefined
    shipForm.shippingFee = data.shippingFee || undefined
    shipForm.returnShippingFee = undefined
    shipTemplateList.value = []
    shipTemplateDetail.value = null
    // 加载模板
    loadShipTemplates()
  } catch { return }
  shipDialogVisible.value = true
}

async function handleShipSubmit() {
  shipping.value = true
  try {
    // 计算总运费 = 退回运费 + 换货快递费
    const totalFee = isSellerResponsible.value
      ? (shipForm.returnShippingFee || 0) + (shipForm.shippingFee || 0)
      : shipForm.shippingFee

    await shipExchange(shipData.value.id, {
      expressCompanyId: shipForm.expressCompanyId || undefined,
      trackingNo: shipForm.trackingNo || undefined,
      shippingFee: totalFee,
    })
    ElMessage.success('发货成功，已创建出库单')
    shipDialogVisible.value = false
    fetchData()
  } catch {} finally {
    shipping.value = false
  }
}

// 取消
async function handleCancel(id: number) {
  try {
    await cancelExchange(id)
    ElMessage.success('换货单已取消')
    fetchData()
  } catch {}
}

// 创建换货单
async function openCreateDialog() {
  createForm.value = {
    orderNo: '',
    warehouseId: undefined,
    shipWarehouseId: undefined,
    reason: '',
    responsibleParty: 'SELLER',
    returnTrackingNo: '',
    expressCompanyId: undefined,
    feeTemplateId: undefined,
    estimatedWeight: undefined,
    shippingFee: undefined,
    remark: '',
    items: [],
  }
  createTemplateList.value = []
  createTemplateDetail.value = null
  exchangeOrder.value = null
  skuList.value = []
  createDialogVisible.value = true
}

async function loadOrderForExchange() {
  const orderNo = createForm.value.orderNo.trim()
  if (!orderNo) {
    ElMessage.warning('请输入订单号')
    return
  }
  try {
    // 先搜索订单
    const { getOrderList } = await import('@/api/order')
    const res = await getOrderList({ page: 1, size: 1, orderNo })
    const orders = res.data.list || []
    if (orders.length === 0) {
      ElMessage.error('未找到该订单')
      return
    }
    const order = orders[0]
    if (order.orderStatus !== 'SHIPPED') {
      ElMessage.warning('只能对已发货的订单创建换货单')
      return
    }
    // 获取订单详情
    const detailRes = await getOrderDetail(order.id)
    exchangeOrder.value = detailRes.data
    // 默认仓库
    if (!createForm.value.warehouseId && detailRes.data.warehouseId) {
      createForm.value.warehouseId = detailRes.data.warehouseId
    }
  } catch {
    ElMessage.error('查询订单失败')
  }
}

function openSkuSelector() {
  skuSearchKeyword.value = ''
  skuSelectorVisible.value = true
}

async function handleCreateWarehouseChange(warehouseId: number) {
  createForm.value.items = []
  if (warehouseId) {
    try {
      const res = await getAllSkuList({ page: 1, size: 1000, warehouseId })
      skuList.value = res.data.list || []
    } catch {}
  } else {
    skuList.value = []
  }
}

function handleCreateResponsibleChange(val: string) {
  if (val === 'CUSTOMER') {
    createForm.value.feeTemplateId = undefined
    createForm.value.estimatedWeight = undefined
    createForm.value.shippingFee = 0
    createTemplateDetail.value = null
  } else {
    createForm.value.shippingFee = undefined
    loadCreateTemplates()
  }
}

async function loadCreateTemplates() {
  try {
    const res = await getCompanyList()
    const companies = res.data || []
    const returnCompany = companies.find((c: any) => c.name.includes('换货'))
    const targetCompany = returnCompany || companies[0]
    if (targetCompany) {
      const { getTemplateListByCompany } = await import('@/api/express')
      const templateRes = await getTemplateListByCompany(targetCompany.id)
      createTemplateList.value = templateRes.data || []
      const defaultTemplate = createTemplateList.value.find((t: any) => t.isDefault === 1)
      if (defaultTemplate) {
        createForm.value.feeTemplateId = defaultTemplate.id
        handleCreateTemplateChange(defaultTemplate.id)
      }
    }
  } catch {}
}

async function handleCreateTemplateChange(templateId: number | undefined) {
  if (templateId) {
    try {
      const { getTemplateDetail } = await import('@/api/express')
      const res = await getTemplateDetail(templateId)
      createTemplateDetail.value = res.data
    } catch {
      createTemplateDetail.value = null
    }
    if (createForm.value.estimatedWeight && createForm.value.estimatedWeight > 0) {
      calculateCreateFee()
    }
  } else {
    createTemplateDetail.value = null
  }
}

function calculateCreateFee() {
  if (!createTemplateDetail.value || !createForm.value.estimatedWeight || createForm.value.estimatedWeight <= 0) return

  const template = createTemplateDetail.value

  if (template.templateType === 'FIRST_CONTINUE') {
    const firstWeight = template.firstWeight || 1
    const firstFee = template.firstFee || 0
    const additionalWeight = template.additionalWeight || 1
    const additionalFee = template.additionalFee || 0

    if (createForm.value.estimatedWeight <= firstWeight) {
      createForm.value.shippingFee = firstFee
    } else {
      const extraWeight = createForm.value.estimatedWeight - firstWeight
      const extraUnits = Math.ceil(extraWeight / additionalWeight)
      createForm.value.shippingFee = firstFee + extraUnits * additionalFee
    }
  } else if (template.steps && template.steps.length > 0) {
    const sortedSteps = [...template.steps].sort((a: any, b: any) => a.minWeight - b.minWeight)
    const matchedStep = sortedSteps.find((s: any) =>
      createForm.value.estimatedWeight! >= s.minWeight && createForm.value.estimatedWeight! < s.maxWeight
    )
    if (matchedStep) {
      createForm.value.shippingFee = matchedStep.fee
    } else {
      const lastStep = sortedSteps[sortedSteps.length - 1]
      if (createForm.value.estimatedWeight! >= lastStep.minWeight) {
        createForm.value.shippingFee = lastStep.fee
      } else {
        createForm.value.shippingFee = undefined
      }
    }
  }
}

function addExchangeItem(sku: any) {
  const existing = createForm.value.items.find(i => i.skuId === sku.id)
  if (existing) {
    existing.quantity += 1
    return
  }
  createForm.value.items.push({
    skuId: sku.id,
    skuCode: sku.skuCode,
    skuName: sku.name,
    sizeValue: sku.sizeValue || '',
    quantity: 1,
    image: sku.image || sku.mainImage || '',
  })
  skuSelectorVisible.value = false
}

function getStockTagType(quantity?: number, lowThreshold?: number, outThreshold?: number): string {
  const value = quantity ?? 0
  const low = lowThreshold ?? 10
  const out = outThreshold ?? 0
  if (value <= out) return 'danger'
  if (value <= low) return 'warning'
  return 'success'
}

async function handleCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!exchangeOrder.value) {
    ElMessage.warning('请先查询订单')
    return
  }
  if (!createForm.value.shipWarehouseId) {
    ElMessage.warning('请选择出库仓库')
    return
  }
  if (createForm.value.items.length === 0) {
    ElMessage.warning('请添加换出商品')
    return
  }

  creating.value = true
  try {
    await createExchange({
      orderId: exchangeOrder.value.id,
      warehouseId: createForm.value.shipWarehouseId,
      reason: createForm.value.reason || undefined,
      returnTrackingNo: createForm.value.returnTrackingNo || undefined,
      expressCompanyId: createForm.value.expressCompanyId || undefined,
      shippingFee: createForm.value.responsibleParty === 'CUSTOMER' ? 0 : createForm.value.shippingFee,
      remark: createForm.value.responsibleParty === 'CUSTOMER'
        ? `客户自付快递费${createForm.value.remark ? '，' + createForm.value.remark : ''}`
        : createForm.value.remark || undefined,
      items: createForm.value.items.map(i => ({
        skuId: i.skuId,
        skuCode: i.skuCode,
        skuName: i.skuName,
        sizeValue: i.sizeValue,
        quantity: i.quantity,
        itemType: 'EXCHANGE_ITEM',
      })),
    })
    ElMessage.success('换货单创建成功')
    createDialogVisible.value = false
    fetchData()
  } catch {} finally {
    creating.value = false
  }
}
</script>

<style scoped>
.order-sticky-header {
  position: sticky;
  top: 0;
  z-index: 10;
  transition: padding-bottom 0.2s;
}

.order-sticky-header.title-collapsed :deep(.page-header__left) {
  height: 0;
  overflow: hidden;
  margin: 0;
}

/* 换货状态标签 - 紫色系 */
:deep(.el-tag--info) {
  --el-tag-bg-color: #f3e8ff;
  --el-tag-border-color: #d8b4fe;
  --el-tag-text-color: #7c3aed;
}

:deep(.el-tag--danger) {
  --el-tag-bg-color: #fef2f2;
  --el-tag-border-color: #fca5a5;
  --el-tag-text-color: #ef4444;
}
</style>
