<template>
  <div class="page-container">
    <PageHeader title="订单管理" />

    <!-- 操作栏 -->
    <div class="action-bar">
      <div class="flex-1"></div>
      <el-button type="primary" icon="Upload" @click="openImportDialog">手动导入</el-button>
      <el-button type="info" icon="Document" @click="fileImportDialogVisible = true">文档导入</el-button>
      <el-button type="success" icon="TopRight" @click="toggleSelectMode('outbound')">
        {{ selectMode === 'outbound' ? '取消选择' : '批量出库' }}
      </el-button>
      <el-button type="warning" icon="BottomLeft" @click="toggleSelectMode('return')">
        {{ selectMode === 'return' ? '取消选择' : '批量退货' }}
      </el-button>
      <el-button type="danger" icon="Close" @click="toggleSelectMode('cancel')">
        {{ selectMode === 'cancel' ? '取消选择' : '批量取消' }}
      </el-button>
      <el-button v-if="selectMode && selectedOrders.length > 0" type="primary" @click="handleBatchAction">
        确认{{ selectMode === 'outbound' ? '出库' : selectMode === 'return' ? '退货' : '取消' }} ({{ selectedOrders.length }})
      </el-button>
    </div>

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
      <!-- 选择模式提示 -->
      <el-alert
        v-if="selectMode"
        :title="selectMode === 'outbound' ? '批量出库模式：请勾选需要出库的订单（仅待出库状态可选）' : selectMode === 'return' ? '批量退货模式：请勾选需要退货的订单（仅已发货状态可选）' : '批量取消模式：请勾选需要取消的订单（仅待付款/待出库状态可选）'"
        :type="selectMode === 'outbound' ? 'warning' : 'error'"
        show-icon
        :closable="false"
        class="mb-3"
      />

      <el-table
        ref="tableRef"
        :data="tableData"
        v-loading="loading"
        stripe
        border
        :class="selectMode === 'outbound' ? 'outbound-mode' : selectMode === 'return' ? 'return-mode' : selectMode === 'cancel' ? 'cancel-mode' : ''"
        :row-class-name="getRowClassName"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" :selectable="isSelectable" />
        <el-table-column prop="orderNo" label="订单号" width="160" />
        <el-table-column label="平台" width="120">
          <template #default="{ row }">
            <div v-if="row.platformName" class="flex items-center gap-1">
              <div v-if="row.platformColor" class="w-3 h-3 rounded" :style="{ backgroundColor: row.platformColor }"></div>
              <span>{{ row.platformName }}</span>
            </div>
            <span v-else class="text-gray-400">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="platformOrderNo" label="平台单号" width="160" show-overflow-tooltip />
        <el-table-column prop="warehouseName" label="仓库" width="120" />
        <el-table-column prop="receiverName" label="收件人" width="90" />
        <el-table-column prop="receiverPhone" label="收件电话" width="120" />
        <el-table-column prop="receiverAddress" label="收件地址" min-width="180" show-overflow-tooltip />
        <el-table-column prop="orderStatus" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              v-if="row.orderStatus === 'EXCHANGING'"
              size="small"
              :style="{ backgroundColor: '#f3e8ff', borderColor: '#d8b4fe', color: '#7c3aed' }"
            >
              {{ ORDER_STATUS_MAP[row.orderStatus]?.label || row.orderStatus }}
            </el-tag>
            <el-tag
              v-else-if="row.orderStatus === 'EXCHANGED'"
              size="small"
              :style="{ backgroundColor: '#6b21a8', borderColor: '#6b21a8', color: '#fff' }"
            >
              {{ ORDER_STATUS_MAP[row.orderStatus]?.label || row.orderStatus }}
            </el-tag>
            <el-tag
              v-else
              :type="(ORDER_STATUS_MAP[row.orderStatus]?.color as any) || 'info'"
              :effect="(ORDER_STATUS_MAP[row.orderStatus]?.effect as any) || 'light'"
              size="small"
            >
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
        <el-table-column label="操作" width="450" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link icon="View" @click="viewDetail(row)">详情</el-button>
            <el-button type="warning" link icon="Edit" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button v-if="row.orderStatus === 'WAIT_OUTBOUND'" type="success" link icon="TopRight" @click="createOutboundOrder(row)">
              出库
            </el-button>
            <el-button v-if="row.orderStatus === 'WAIT_OUTBOUND'" type="primary" link icon="Van" @click="router.push({ path: '/outbound/list', query: { orderNo: row.orderNo } })">
              发货管理
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
            <el-button v-if="row.orderStatus === 'OUTBOUNDING'" type="success" link icon="Van" @click="openQuickShipDialog(row)">
              快速发货
            </el-button>
            <el-button v-if="row.orderStatus === 'OUTBOUNDING'" type="primary" link icon="TopRight" @click="router.push({ path: '/outbound/list', query: { orderNo: row.orderNo } })">
              发货管理
            </el-button>
            <el-button v-if="row.orderStatus === 'SHIPPED' || row.orderStatus === 'EXCHANGED'" type="warning" link icon="BottomLeft" @click="openReturnDialog(row)">
              退货
            </el-button>
            <el-button v-if="row.orderStatus === 'SHIPPED' || row.orderStatus === 'EXCHANGED'" link icon="Sort" class="exchange-btn" @click="openExchangeDialog(row)">
              换货
            </el-button>
            <el-button v-if="row.orderStatus === 'EXCHANGING'" link icon="Sort" class="exchange-btn" @click="router.push({ path: '/exchange/list', query: { orderNo: row.orderNo } })">
              换货管理
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
        <el-descriptions-item label="平台">
          <div v-if="detail.platformName" class="flex items-center gap-1">
            <div v-if="detail.platformColor" class="w-3 h-3 rounded" :style="{ backgroundColor: detail.platformColor }"></div>
            <span>{{ detail.platformName }}</span>
          </div>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="平台单号">{{ detail.platformOrderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detail.warehouseName || detail.warehouseId }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag
            v-if="detail.orderStatus === 'EXCHANGING'"
            size="small"
            :style="{ backgroundColor: '#f3e8ff', borderColor: '#d8b4fe', color: '#7c3aed' }"
          >
            {{ ORDER_STATUS_MAP[detail.orderStatus || '']?.label || detail.orderStatus }}
          </el-tag>
          <el-tag
            v-else-if="detail.orderStatus === 'EXCHANGED'"
            size="small"
            :style="{ backgroundColor: '#6b21a8', borderColor: '#6b21a8', color: '#fff' }"
          >
            {{ ORDER_STATUS_MAP[detail.orderStatus || '']?.label || detail.orderStatus }}
          </el-tag>
          <el-tag
            v-else
            :type="(ORDER_STATUS_MAP[detail.orderStatus || '']?.color as any) || 'info'"
            :effect="(ORDER_STATUS_MAP[detail.orderStatus || '']?.effect as any) || 'light'"
            size="small"
          >
            {{ ORDER_STATUS_MAP[detail.orderStatus || '']?.label || detail.orderStatus }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="收件人">{{ detail.receiverName }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ detail.receiverPhone }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ detail.receiverAddress }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ detail.totalAmount?.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 换货中的订单显示换货明细 -->
      <template v-if="detail.orderStatus === 'EXCHANGING' && exchangeDetail">
        <el-divider content-position="left">换货信息</el-divider>
        <el-descriptions :column="2" border size="small" class="mb-4">
          <el-descriptions-item label="换货单号">{{ exchangeDetail.exchangeNo }}</el-descriptions-item>
          <el-descriptions-item label="换货状态">
            <el-tag :type="(EXCHANGE_STATUS_MAP[exchangeDetail.status]?.color as any) || 'info'" size="small">
              {{ EXCHANGE_STATUS_MAP[exchangeDetail.status]?.label || exchangeDetail.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="换货原因">{{ exchangeDetail.reason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="责任方">
            <el-tag :type="exchangeDetail.remark?.includes('客户自付快递费') ? 'warning' : 'success'" size="small">
              {{ exchangeDetail.remark?.includes('客户自付快递费') ? '客户原因' : '我们原因' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <h4 class="mb-2 text-sm font-semibold text-red-500">退回商品（换货前）</h4>
        <el-table :data="exchangeReturnItems" border size="small" class="mb-4">
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
          <el-table-column label="质检结果" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.qualityStatus" :type="(QUALITY_STATUS_MAP[row.qualityStatus]?.color as any) || 'info'" size="small">
                {{ QUALITY_STATUS_MAP[row.qualityStatus]?.label }}
              </el-tag>
              <span v-else class="text-gray-400">待质检</span>
            </template>
          </el-table-column>
        </el-table>

        <h4 class="mb-2 text-sm font-semibold text-green-500">换出商品（换货后）</h4>
        <el-table :data="exchangeNewItems" border size="small">
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
      </template>

      <!-- 普通订单明细 -->
      <template v-else>
        <h4 class="mt-4 mb-2 text-sm font-semibold text-gray-700">订单明细</h4>
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
            <template #default>{{ detail.warehouseName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" align="center" />
          <el-table-column prop="totalPrice" label="小计" width="90" align="right">
            <template #default="{ row }">¥{{ row.totalPrice?.toFixed(2) }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>

    <!-- 编辑订单弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑订单" width="600px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="90px">
        <el-form-item label="平台">
          <el-select v-model="editForm.platformId" placeholder="选择平台" clearable style="width: 100%">
            <el-option v-for="p in platforms" :key="p.id" :label="p.name" :value="p.id">
              <div class="flex items-center gap-2">
                <div v-if="p.color" class="w-3 h-3 rounded" :style="{ backgroundColor: p.color }"></div>
                <span>{{ p.name }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="平台单号">
          <el-input v-model="editForm.platformOrderNo" placeholder="平台订单号" />
        </el-form-item>
        <el-form-item label="收件人" prop="receiverName">
          <el-input v-model="editForm.receiverName" placeholder="收件人姓名" />
        </el-form-item>
        <el-form-item label="电话" prop="receiverPhone">
          <el-input v-model="editForm.receiverPhone" placeholder="收件人电话" />
        </el-form-item>
        <el-form-item label="地址" prop="receiverAddress">
          <el-input v-model="editForm.receiverAddress" placeholder="收件地址" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="2" placeholder="订单备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editing" @click="handleEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialogVisible" title="导入订单" width="900px" destroy-on-close>
      <el-form ref="importFormRef" :model="importForm" :rules="importRules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="平台">
              <el-select v-model="importForm.platformId" placeholder="选择平台" clearable style="width: 100%">
                <el-option v-for="p in platforms" :key="p.id" :label="p.name" :value="p.id">
                  <div class="flex items-center gap-2">
                    <div v-if="p.color" class="w-3 h-3 rounded" :style="{ backgroundColor: p.color }"></div>
                    <span>{{ p.name }}</span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="平台单号">
              <el-input v-model="importForm.platformOrderNo" placeholder="平台订单号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
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
          <el-select v-model="selectedSkuGroupKey" :placeholder="importForm.warehouseId ? '请选择SKU/商品' : '请先选择仓库'" filterable clearable style="width: 100%" :disabled="!importForm.warehouseId">
            <el-option
              v-for="group in skuGroups"
              :key="group.key"
              :label="`${group.skuCode} - ${group.skuName}`"
              :value="group.key"
            />
          </el-select>
        </div>
        <el-table v-if="selectedSkuGroup" :data="selectedSkuGroup.skus" border size="small" class="mb-3" max-height="350">
          <el-table-column label="图片" width="60" align="center">
            <template #default="{ row }">
              <ImagePreview :src="row.image || row.mainImage" />
            </template>
          </el-table-column>
          <el-table-column prop="sizeValue" label="码数" width="80" align="center">
            <template #default="{ row }">{{ row.sizeValue || '-' }}</template>
          </el-table-column>
          <el-table-column prop="skuCode" label="SKU编码" width="150" show-overflow-tooltip />
          <el-table-column prop="name" label="SKU名称" min-width="150" show-overflow-tooltip />
          <el-table-column label="可用库存" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getStockTagType(getImportAvailableQty(row), row.lowStockThreshold, row.outOfStockThreshold)" size="small">{{ getImportAvailableQty(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center">
            <template #default="{ row }">
              <el-button type="primary" link icon="Plus" @click="addSkuToImport(row)" :disabled="getImportAvailableQty(row) <= 0">加入</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-table :data="importForm.items" border size="small">
          <el-table-column label="图片" width="60" align="center">
            <template #default="{ row }">
              <ImagePreview :src="row.image" />
            </template>
          </el-table-column>
          <el-table-column prop="skuCode" label="SKU编码" width="150" show-overflow-tooltip />
          <el-table-column prop="skuName" label="SKU名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="sizeValue" label="码数" width="80" align="center">
            <template #default="{ row }">{{ row.sizeValue || '-' }}</template>
          </el-table-column>
          <el-table-column label="可用库存" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="getStockTagType((row.availableQty ?? 0) - row.quantity, row.lowStockThreshold, row.outOfStockThreshold)" size="small">{{ (row.availableQty ?? 0) - row.quantity }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="数量" width="140" align="center">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :min="1" :max="999999" size="small" style="width: 110px" />
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
          <el-select
            v-model="returnForm.reason"
            filterable
            allow-create
            default-first-option
            placeholder="请选择或输入退货原因"
            style="width: 100%"
          >
            <el-option label="七天无理由退货" value="七天无理由退货" />
            <el-option label="商品质量问题" value="商品质量问题" />
            <el-option label="商品与描述不符" value="商品与描述不符" />
            <el-option label="发错货" value="发错货" />
            <el-option label="物流问题" value="物流问题" />
            <el-option label="客户取消订单" value="客户取消订单" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="责任方">
          <el-radio-group v-model="returnForm.responsibleParty">
            <el-radio value="CUSTOMER">客户原因（客户出快递费）</el-radio>
            <el-radio value="SELLER">我们/快递原因（我们出快递费）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="退回快递号">
          <el-input v-model="returnForm.trackingNo" placeholder="客户退回的快递单号（选填）" />
        </el-form-item>

        <!-- 客户原因：显示客户自付 -->
        <el-form-item v-if="returnForm.responsibleParty === 'CUSTOMER'" label="快递费">
          <el-tag type="warning" size="large">客户自付快递费</el-tag>
        </el-form-item>
        <!-- 我们原因：通过模板计算快递费 -->
        <template v-if="returnForm.responsibleParty === 'SELLER'">
          <el-divider content-position="left">退货快递费</el-divider>
          <el-form-item label="费用模板">
            <el-select v-model="returnForm.feeTemplateId" placeholder="选择模板自动计算" clearable style="width: 100%" @change="handleReturnTemplateChange">
              <el-option v-for="t in returnTemplateList" :key="t.id" :label="t.name" :value="t.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="预估重量(kg)">
            <el-input-number v-model="returnForm.estimatedWeight" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
          <el-form-item label="快递费用">
            <el-input-number v-model="returnForm.shippingFee" :min="0" :precision="2" style="width: 100%" />
            <div class="text-xs text-gray-400 mt-1">选择模板后自动计算，也可手动修改</div>
          </el-form-item>
        </template>
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

    <!-- 换货弹窗 -->
    <el-dialog v-model="exchangeDialogVisible" title="创建换货单" width="900px" destroy-on-close>
      <el-form ref="exchangeFormRef" :model="exchangeForm" label-width="90px">
        <el-form-item label="换货原因">
          <el-select v-model="exchangeForm.reason" filterable allow-create default-first-option placeholder="请选择或输入换货原因" style="width: 100%">
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
          <el-radio-group v-model="exchangeForm.responsibleParty">
            <el-radio value="CUSTOMER">客户原因（客户出快递费）</el-radio>
            <el-radio value="SELLER">我们/快递原因（我们出快递费）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="exchangeForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>

      <!-- 原订单商品（退回商品） -->
      <h4 class="mb-2 text-sm font-semibold text-gray-700">退回商品（勾选客户要退回的商品）</h4>
      <el-table :data="exchangeForm.returnItems" border size="small" class="mb-4" max-height="250">
        <el-table-column label="退回" width="60" align="center">
          <template #default="{ row }">
            <el-checkbox v-model="row.checked" />
          </template>
        </el-table-column>
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
        <el-table-column prop="orderedQty" label="订单数量" width="80" align="center" />
        <el-table-column label="退回数量" width="120" align="center">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="1" :max="row.orderedQty" size="small" style="width: 90px" :disabled="!row.checked" />
          </template>
        </el-table-column>
      </el-table>

      <!-- 换出商品 -->
      <div class="flex items-center justify-between mb-2">
        <h4 class="text-sm font-semibold text-gray-700">换出商品</h4>
        <div class="flex items-center gap-2">
          <el-select v-model="exchangeForm.shipWarehouseId" placeholder="选择出库仓库" size="small" style="width: 160px" @change="handleExchangeWarehouseChange">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
          <el-button type="primary" size="small" icon="Plus" :disabled="!exchangeForm.shipWarehouseId" @click="exchangeSkuSelectorVisible = true">添加换出商品</el-button>
        </div>
      </div>
      <el-table :data="exchangeForm.exchangeItems" border size="small" max-height="250">
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
        <el-table-column label="可用库存" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStockTagType((row.availableQty ?? 0) - row.quantity, row.lowStockThreshold, row.outOfStockThreshold)" size="small">{{ (row.availableQty ?? 0) - row.quantity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="120" align="center">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="1" size="small" style="width: 90px" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center">
          <template #default="{ $index }">
            <el-button type="danger" link icon="Delete" @click="exchangeForm.exchangeItems.splice($index, 1)" />
          </template>
        </el-table-column>
      </el-table>

      <!-- SKU 选择器 -->
      <el-dialog v-model="exchangeSkuSelectorVisible" title="选择换出商品" width="800px" append-to-body destroy-on-close>
        <el-input v-model="exchangeSkuSearch" placeholder="搜索SKU编码或名称" clearable class="mb-3" />
        <el-table :data="filteredExchangeSkuList" border size="small" max-height="400" @row-click="addExchangeItem">
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
        <el-button @click="exchangeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="exchanging" @click="handleExchange">创建换货单</el-button>
      </template>
    </el-dialog>

    <!-- 快速发货弹窗 -->
    <el-dialog v-model="quickShipDialogVisible" title="确认发货" width="760px" destroy-on-close>
      <el-descriptions :column="2" border class="mb-4" size="small">
        <el-descriptions-item label="发货单号">{{ quickShipDetail.outboundNo }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ quickShipDetail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="平台单号">{{ quickShipDetail.platformOrderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="订单备注">{{ quickShipDetail.orderRemark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <h4 class="mb-2 text-sm font-semibold text-gray-700">订单明细</h4>
      <el-table :data="quickShipDetail.items || []" border size="small" class="mb-4" max-height="250">
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
          <el-input v-model="quickShipForm.trackingNo" placeholder="请输入快递单号" />
        </el-form-item>
        <el-form-item label="快递公司" required>
          <el-select v-model="quickShipForm.expressCompanyId" placeholder="请选择快递公司" style="width: 100%" @change="handleQuickShipCompanyChange">
            <el-option v-for="c in companyList" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="费用模板" required>
          <el-select v-model="quickShipForm.feeTemplateId" placeholder="请选择费用模板" style="width: 100%" @change="handleQuickShipTemplateChange">
            <el-option v-for="t in quickShipTemplateList" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预估重量(kg)" required>
          <el-input-number v-model="quickShipForm.estimatedWeight" :min="0.01" :precision="2" style="width: 100%" @focus="($event.target as HTMLInputElement).select()" />
        </el-form-item>
        <el-form-item label="快递费用" required>
          <el-input-number v-model="quickShipForm.shippingFee" :min="0.01" :precision="2" style="width: 100%" />
          <div class="text-xs text-gray-400 mt-1">选择模板后自动计算，也可手动修改</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quickShipDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="quickShipping" @click="handleQuickShipConfirm">确认发货</el-button>
      </template>
    </el-dialog>

    <!-- 批量退货弹窗 -->
    <el-dialog v-model="batchReturnDialogVisible" title="批量退货" width="1200px" destroy-on-close>
      <el-table :data="batchReturnItems" border size="small" max-height="500">
        <el-table-column label="平台单号" width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.platformOrderNo || row.orderNo }}</template>
        </el-table-column>
        <el-table-column label="商品明细" min-width="200">
          <template #default="{ row }">
            <div v-if="row.skuList && row.skuList.length > 0" class="sku-tags">
              <el-tag v-for="(sku, idx) in row.skuList" :key="idx" size="small" class="sku-tag">
                {{ sku.skuName }} x{{ sku.quantity }}
              </el-tag>
            </div>
            <span v-else class="text-gray-400">-</span>
          </template>
        </el-table-column>
        <el-table-column label="退货原因" width="130">
          <template #default="{ row }">
            <el-select
              v-model="row.reason"
              filterable
              allow-create
              default-first-option
              placeholder="选择原因"
              size="small"
              style="width: 100%"
            >
              <el-option label="七天无理由退货" value="七天无理由退货" />
              <el-option label="商品质量问题" value="商品质量问题" />
              <el-option label="商品与描述不符" value="商品与描述不符" />
              <el-option label="发错货" value="发错货" />
              <el-option label="物流问题" value="物流问题" />
              <el-option label="客户取消订单" value="客户取消订单" />
              <el-option label="其他" value="其他" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="客户快递单号" width="150">
          <template #default="{ row }">
            <el-input v-model="row.trackingNo" placeholder="快递单号" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="快递公司" width="120">
          <template #default="{ row }">
            <span class="text-sm">{{ getCompanyName(row.expressCompanyId) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="费用模板" width="130">
          <template #default="{ row }">
            <el-select v-model="row.feeTemplateId" placeholder="模板" size="small" style="width: 100%" @change="(val: number) => handleBatchReturnTemplateChange(row, val)">
              <el-option v-for="t in row.templateList || []" :key="t.id" :label="t.name" :value="t.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="重量(kg)" width="100">
          <template #default="{ row }">
            <el-input-number v-model="row.estimatedWeight" :min="0" :precision="2" size="small" style="width: 100%" @change="calculateReturnItemFee(row)" />
          </template>
        </el-table-column>
        <el-table-column label="快递费用" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.shippingFee" :min="0" :precision="2" size="small" style="width: 100%" />
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="batchReturnDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchReturning" @click="handleBatchReturnSubmit">确认退货 ({{ batchReturnItems.length }})</el-button>
      </template>
    </el-dialog>

    <!-- 文档导入弹窗 -->
    <el-dialog v-model="fileImportDialogVisible" title="文档导入" width="680px" destroy-on-close>
      <el-alert type="info" :closable="false" class="mb-4">
        <template #title>
          <div class="text-sm">
            <p class="font-semibold mb-1">导入说明：</p>
            <ul class="list-disc pl-4 space-y-1">
              <li>支持 Excel (.xlsx, .xls) 和 CSV (.csv) 文件，文件大小不超过 10MB</li>
              <li>同一订单号的多行会合并为一个订单（多SKU）</li>
              <li>SKU编码必须是系统中已存在的编码</li>
            </ul>
          </div>
        </template>
      </el-alert>

      <div class="mb-4 flex gap-4">
        <el-button type="primary" link icon="Download" @click="downloadTemplate">下载订单导入模板</el-button>
        <el-button type="warning" link icon="Download" @click="downloadReturnTemplate">下载批量退货模板</el-button>
      </div>

      <el-form :model="fileImportForm" label-width="80px">
        <el-form-item label="导入类型" required>
          <el-radio-group v-model="fileImportForm.importType">
            <el-radio value="order">订单导入</el-radio>
            <el-radio value="return">批量退货</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="fileImportForm.importType === 'order'" label="目标仓库" required>
          <el-select v-model="fileImportForm.warehouseId" placeholder="请选择目标仓库" style="width: 100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-upload
        ref="uploadRef"
        :action="uploadUrl"
        :headers="uploadHeaders"
        :data="uploadData"
        :before-upload="beforeUpload"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :on-change="handleFileChange"
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls,.csv"
        drag
        class="mt-2"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 .xlsx, .xls, .csv 格式文件</div>
        </template>
      </el-upload>

      <el-divider content-position="left">模板格式</el-divider>
      <el-table :data="fileImportForm.importType === 'return' ? returnTemplateFormatData : templateFormatData" border size="small" class="mb-2">
        <el-table-column prop="field" label="字段名" width="120" />
        <el-table-column prop="required" label="必填" width="60" align="center">
          <template #default="{ row }">
            <el-tag :type="row.required ? 'danger' : 'info'" size="small">{{ row.required ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="example" label="示例" min-width="150" />
        <el-table-column prop="desc" label="说明" min-width="150" />
      </el-table>

      <template #footer>
        <el-button @click="fileImportDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="fileImporting" :disabled="fileImportForm.importType === 'order' && !fileImportForm.warehouseId" @click="handleFileImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { getOrderDetail, getOrderList, importOrder, updateOrder, updateOrderStatus } from '@/api/order'
import type { Order } from '@/api/order'
import { createOutbound, createBatchOutbound } from '@/api/outbound'
import { createBatchReturn } from '@/api/returns'
import { createReturn, cancelReturnByOrderId } from '@/api/returns'
import { createExchange, getExchangeList, getExchangeDetail } from '@/api/exchange'
import { EXCHANGE_STATUS_MAP, QUALITY_STATUS_MAP } from '@/utils/constants'
import { getAllSkuList } from '@/api/product'
import type { Sku } from '@/api/product'
import { getWarehouseList } from '@/api/warehouse'
import type { Warehouse } from '@/api/warehouse'
import { getPlatformOptions } from '@/api/platform'
import type { Platform } from '@/api/platform'
import { getCompanyList } from '@/api/express'
import type { ExpressCompany } from '@/api/express'
import { useTable } from '@/composables/useTable'
import { formatDateTime } from '@/utils/format'
import { ORDER_STATUS_MAP } from '@/utils/constants'
import PageHeader from '@/components/PageHeader.vue'
import ImagePreview from '@/components/ImagePreview.vue'

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

type SkuListItem = Sku & { productName?: string; mainImage?: string }

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
  image?: string
  availableQty?: number
  lowStockThreshold?: number
  outOfStockThreshold?: number
}

const { tableData, loading, pagination, searchParams, handleSearch, handleReset, handlePageChange, handleSizeChange, fetchData } = useTable<Order>(getOrderList)

const warehouses = ref<Warehouse[]>([])
const platforms = ref<Platform[]>([])
const companyList = ref<ExpressCompany[]>([])
const skuList = ref<SkuListItem[]>([])
const detailVisible = ref(false)
const detail = ref<Partial<Order>>({})
const exchangeDetail = ref<any>(null)

// 换货明细计算属性
const exchangeReturnItems = computed(() => {
  return (exchangeDetail.value?.items || []).filter((i: any) => i.itemType === 'RETURN_ITEM')
})
const exchangeNewItems = computed(() => {
  return (exchangeDetail.value?.items || []).filter((i: any) => i.itemType === 'EXCHANGE_ITEM')
})

// 编辑订单相关
const editDialogVisible = ref(false)
const editing = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = reactive({
  id: 0,
  platformId: undefined as number | undefined,
  platformOrderNo: '',
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  remark: '',
})
const editRules: FormRules = {
  receiverName: [{ required: true, message: '请输入收件人', trigger: 'blur' }],
  receiverPhone: [{ required: true, message: '请输入电话', trigger: 'blur' }],
  receiverAddress: [{ required: true, message: '请输入地址', trigger: 'blur' }],
}

// 批量操作相关
const selectMode = ref<'outbound' | 'return' | null>(null)
const selectedOrders = ref<Order[]>([])
const tableRef = ref()

function handleSelectionChange(selection: Order[]) {
  selectedOrders.value = selection
}

function isSelectable(row: Order) {
  if (!selectMode.value) return false
  if (selectMode.value === 'outbound') return row.orderStatus === 'WAIT_OUTBOUND'
  if (selectMode.value === 'return') return row.orderStatus === 'SHIPPED' || row.orderStatus === 'EXCHANGED'
  if (selectMode.value === 'cancel') return row.orderStatus === 'WAIT_PAY' || row.orderStatus === 'WAIT_OUTBOUND'
  return false
}

function getRowClassName({ row }: { row: Order }) {
  if (!selectMode.value) return ''
  if (selectMode.value === 'outbound' && row.orderStatus === 'WAIT_OUTBOUND') return 'selectable-row'
  if (selectMode.value === 'return' && (row.orderStatus === 'SHIPPED' || row.orderStatus === 'EXCHANGED')) return 'selectable-row'
  if (selectMode.value === 'cancel' && (row.orderStatus === 'WAIT_PAY' || row.orderStatus === 'WAIT_OUTBOUND')) return 'selectable-row'
  return 'disabled-row'
}

function toggleSelectMode(mode: 'outbound' | 'return' | 'cancel') {
  if (selectMode.value === mode) {
    // 取消选择模式
    selectMode.value = null
    selectedOrders.value = []
    tableRef.value?.clearSelection()
  } else {
    // 切换选择模式，清空已选
    selectMode.value = mode
    selectedOrders.value = []
    tableRef.value?.clearSelection()
  }
}

async function handleBatchAction() {
  if (!selectMode.value || selectedOrders.value.length === 0) return

  if (selectMode.value === 'outbound') {
    await handleBatchOutbound()
  } else if (selectMode.value === 'return') {
    await handleBatchReturn()
  } else if (selectMode.value === 'cancel') {
    await handleBatchCancel()
  }
}

async function handleBatchCancel() {
  const validOrders = selectedOrders.value.filter(o =>
    o.orderStatus === 'WAIT_PAY' || o.orderStatus === 'WAIT_OUTBOUND'
  )
  if (validOrders.length === 0) {
    ElMessage.warning('请选择待付款或待出库的订单')
    return
  }

  try {
    await ElMessageBox.confirm(`确定要取消 ${validOrders.length} 个订单吗？将恢复已扣减的库存`, '批量取消订单', { type: 'warning' })
  } catch {
    return
  }

  let successCount = 0
  for (const order of validOrders) {
    try {
      await updateOrderStatus(order.id, 'CANCELLED')
      successCount++
    } catch {}
  }

  if (successCount > 0) {
    ElMessage.success(`成功取消 ${successCount} 个订单`)
    selectedOrders.value = []
    selectMode.value = null
    tableRef.value?.clearSelection()
    fetchData()
  }
}

async function handleBatchOutbound() {
  const orderIds = selectedOrders.value.map(o => o.id)
  try {
    await ElMessageBox.confirm(`确定要对 ${orderIds.length} 个订单进行出库吗？`, '批量出库', { type: 'warning' })
  } catch {
    return
  }

  try {
    const res = await createBatchOutbound({ orderIds })
    ElMessage.success(`批量出库成功，共创建 ${res.data?.length || 0} 个发货单`)
    selectedOrders.value = []
    selectMode.value = null
    fetchData()
  } catch {}
}

async function handleBatchReturn() {
  // 只能对已发货的订单进行退货
  const validOrders = selectedOrders.value.filter(o => o.orderStatus === 'SHIPPED')
  if (validOrders.length === 0) {
    ElMessage.warning('请选择已发货的订单')
    return
  }

  // 查找"退货"快递公司
  const returnCompany = companyList.value.find(c => c.name.includes('退货'))
  let defaultTemplateList: any[] = []
  let defaultTemplate: any = null

  if (returnCompany) {
    try {
      const { getTemplateListByCompany } = await import('@/api/express')
      const res = await getTemplateListByCompany(returnCompany.id)
      defaultTemplateList = res.data || []
      defaultTemplate = defaultTemplateList.find((t: any) => t.isDefault === 1) || defaultTemplateList[0]
    } catch {}
  }

  // 初始化批量退货列表，加载订单详情获取SKU信息
  const items = []
  for (const order of validOrders) {
    let skuList: any[] = []
    try {
      const orderRes = await getOrderDetail(order.id)
      skuList = (orderRes.data.items || []).map((item: any) => ({
        skuName: item.skuName,
        quantity: item.quantity,
      }))
    } catch {}

    items.push({
      orderId: order.id,
      orderNo: order.orderNo,
      platformOrderNo: order.platformOrderNo,
      reason: '',
      trackingNo: '',
      expressCompanyId: returnCompany?.id,
      feeTemplateId: defaultTemplate?.id,
      estimatedWeight: undefined as number | undefined,
      shippingFee: undefined as number | undefined,
      remark: '',
      templateList: [...defaultTemplateList],
      templateDetail: defaultTemplate || null,
      skuList,
    })
  }
  batchReturnItems.value = items
  batchReturnDialogVisible.value = true
}

function getCompanyName(companyId: number | undefined): string {
  if (!companyId) return '-'
  return companyList.value.find(c => c.id === companyId)?.name || '-'
}

async function handleReturnCompanyChange(item: any, companyId: number) {
  item.feeTemplateId = undefined
  item.templateList = []
  item.templateDetail = null
  if (companyId) {
    try {
      const { getTemplateListByCompany } = await import('@/api/express')
      const res = await getTemplateListByCompany(companyId)
      item.templateList = res.data || []
      const defaultTemplate = item.templateList.find((t: any) => t.isDefault === 1)
      if (defaultTemplate) {
        item.feeTemplateId = defaultTemplate.id
        await handleBatchReturnTemplateChange(item, defaultTemplate.id)
      }
    } catch {
      item.templateList = []
    }
  }
}

async function handleBatchReturnTemplateChange(item: any, templateId: number) {
  if (templateId) {
    try {
      const { getTemplateDetail } = await import('@/api/express')
      const res = await getTemplateDetail(templateId)
      item.templateDetail = res.data
      if (item.estimatedWeight && item.estimatedWeight > 0) {
        calculateReturnItemFee(item)
      }
    } catch {
      item.templateDetail = null
    }
  } else {
    item.templateDetail = null
  }
}

function calculateReturnItemFee(item: any) {
  if (!item.templateDetail || !item.estimatedWeight || item.estimatedWeight <= 0) return

  const template = item.templateDetail
  let fee: number | undefined

  if (template.templateType === 'FIRST_CONTINUE') {
    const firstWeight = template.firstWeight || 1
    const firstFee = template.firstFee || 0
    const additionalWeight = template.additionalWeight || 1
    const additionalFee = template.additionalFee || 0

    if (item.estimatedWeight <= firstWeight) {
      fee = firstFee
    } else {
      const extraWeight = item.estimatedWeight - firstWeight
      const extraUnits = Math.ceil(extraWeight / additionalWeight)
      fee = firstFee + extraUnits * additionalFee
    }
  } else if (template.steps && template.steps.length > 0) {
    const sortedSteps = [...template.steps].sort((a: any, b: any) => a.minWeight - b.minWeight)
    const matchedStep = sortedSteps.find((s: any) =>
      item.estimatedWeight >= s.minWeight && item.estimatedWeight < s.maxWeight
    )
    if (matchedStep) {
      fee = matchedStep.fee
    } else {
      const lastStep = sortedSteps[sortedSteps.length - 1]
      if (item.estimatedWeight >= lastStep.minWeight) {
        fee = lastStep.fee
      }
    }
  }

  if (fee !== undefined) {
    item.shippingFee = fee
  }
}

// 批量退货弹窗
const batchReturnDialogVisible = ref(false)
const batchReturning = ref(false)
const batchReturnItems = ref<any[]>([])

async function handleBatchReturnSubmit() {
  // 验证每行数据
  for (const item of batchReturnItems.value) {
    if (!item.reason) {
      ElMessage.warning(`订单 ${item.platformOrderNo || item.orderNo} 请选择退货原因`)
      return
    }
  }

  batchReturning.value = true
  let successCount = 0

  for (const item of batchReturnItems.value) {
    try {
      // 获取订单详情以获取退货明细
      const orderRes = await getOrderDetail(item.orderId)
      const order = orderRes.data
      const returnItems = (order.items || []).map((sku: any) => ({
        skuId: sku.skuId,
        quantity: sku.quantity,
      }))

      await createReturn({
        orderId: item.orderId,
        reason: item.reason,
        trackingNo: item.trackingNo || undefined,
        shippingFee: item.shippingFee,
        remark: item.remark || undefined,
        items: returnItems,
      })
      successCount++
    } catch {}
  }

  ElMessage.success(`批量退货完成，成功 ${successCount} 个`)
  batchReturnDialogVisible.value = false
  selectedOrders.value = []
  selectMode.value = null
  tableRef.value?.clearSelection()
  fetchData()
  batchReturning.value = false
}

const importDialogVisible = ref(false)
const importing = ref(false)
const importFormRef = ref<FormInstance>()
const importForm = reactive({
  platformOrderNo: '',
  platformId: undefined as number | undefined,
  warehouseId: undefined as number | undefined,
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  remark: '',
  items: [] as ImportItemForm[],
})
const selectedSkuGroupKey = ref('')

const importRules: FormRules = {
  warehouseId: [{ required: true, message: '请选择仓库', trigger: ['change', 'blur'] }],
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
  responsibleParty: 'SELLER' as 'CUSTOMER' | 'SELLER',
  shippingFee: undefined as number | undefined,
  feeTemplateId: undefined as number | undefined,
  estimatedWeight: undefined as number | undefined,
  remark: '',
  items: [] as ReturnItemForm[],
})

const returnRules: FormRules = {
  reason: [{ required: true, message: '请输入退货原因', trigger: 'blur' }],
}

const returnTemplateList = ref<any[]>([])
const returnTemplateDetail = ref<any>(null) // 缓存模板详情

// 换货相关
const exchangeDialogVisible = ref(false)
const exchanging = ref(false)
const exchangeFormRef = ref<FormInstance>()
const exchangeSkuSelectorVisible = ref(false)
const exchangeSkuSearch = ref('')
const exchangeForm = reactive({
  orderId: 0,
  orderNo: '',
  shipWarehouseId: undefined as number | undefined,
  reason: '',
  responsibleParty: 'SELLER' as 'CUSTOMER' | 'SELLER',
  remark: '',
  returnItems: [] as { skuId: number; skuCode: string; skuName: string; sizeValue?: string; quantity: number; orderedQty: number; image?: string; checked: boolean }[],
  exchangeItems: [] as { skuId: number; skuCode: string; skuName: string; sizeValue?: string; quantity: number; image?: string; availableQty?: number; lowStockThreshold?: number; outOfStockThreshold?: number }[],
})

// 快速发货相关
const quickShipDialogVisible = ref(false)
const quickShipping = ref(false)
const quickShipDetail = ref<any>({})
const quickShipTemplateList = ref<any[]>([])
const quickShipTemplateDetail = ref<any>(null)
const quickShipForm = reactive({
  outboundId: 0,
  trackingNo: '',
  expressCompanyId: undefined as number | undefined,
  feeTemplateId: undefined as number | undefined,
  estimatedWeight: 0,
  shippingFee: undefined as number | undefined,
})

// 监听重量变化，自动计算快递费
watch(
  () => returnForm.estimatedWeight,
  () => {
    if (returnForm.estimatedWeight && returnForm.estimatedWeight > 0 && returnTemplateDetail.value) {
      calculateReturnFee()
    }
  }
)

async function handleReturnTemplateChange(templateId: number | undefined) {
  if (templateId) {
    // 加载模板详情并缓存
    try {
      const { getTemplateDetail } = await import('@/api/express')
      const res = await getTemplateDetail(templateId)
      returnTemplateDetail.value = res.data
    } catch {
      returnTemplateDetail.value = null
    }
    // 如果已有重量，重新计算
    if (returnForm.estimatedWeight && returnForm.estimatedWeight > 0) {
      calculateReturnFee()
    }
  } else {
    returnTemplateDetail.value = null
  }
}

function calculateReturnFee() {
  if (!returnTemplateDetail.value || !returnForm.estimatedWeight || returnForm.estimatedWeight <= 0) return

  const template = returnTemplateDetail.value

  // 首重续重类型
  if (template.templateType === 'FIRST_CONTINUE') {
    const firstWeight = template.firstWeight || 1
    const firstFee = template.firstFee || 0
    const additionalWeight = template.additionalWeight || 1
    const additionalFee = template.additionalFee || 0

    if (returnForm.estimatedWeight <= firstWeight) {
      returnForm.shippingFee = firstFee
    } else {
      const extraWeight = returnForm.estimatedWeight - firstWeight
      const extraUnits = Math.ceil(extraWeight / additionalWeight)
      returnForm.shippingFee = firstFee + extraUnits * additionalFee
    }
  }
  // 阶梯计费类型
  else if (template.steps && template.steps.length > 0) {
    const sortedSteps = [...template.steps].sort((a: any, b: any) => a.minWeight - b.minWeight)
    const matchedStep = sortedSteps.find((s: any) =>
      returnForm.estimatedWeight >= s.minWeight && returnForm.estimatedWeight < s.maxWeight
    )
    if (matchedStep) {
      returnForm.shippingFee = matchedStep.fee
    } else {
      const lastStep = sortedSteps[sortedSteps.length - 1]
      if (returnForm.estimatedWeight >= lastStep.minWeight) {
        returnForm.shippingFee = lastStep.fee
      } else {
        returnForm.shippingFee = undefined
        ElMessage.warning('未找到匹配的费用阶梯')
      }
    }
  }
}

// 文档导入相关
const fileImportDialogVisible = ref(false)
const fileImporting = ref(false)
const uploadRef = ref()
const selectedFile = ref<File | null>(null)
const fileImportForm = reactive({
  importType: 'order' as 'order' | 'return',
  warehouseId: undefined as number | undefined,
})

const BASE_URL = 'http://localhost:8080'
const uploadUrl = computed(() =>
  fileImportForm.importType === 'return'
    ? `${BASE_URL}/api/returns/import-file`
    : `${BASE_URL}/api/orders/import-file`
)
const uploadHeaders = {
  Authorization: `Bearer ${localStorage.getItem('token') || ''}`,
}

const uploadData = computed(() => ({
  warehouseId: fileImportForm.warehouseId
}))

const templateFormatData = [
  { field: 'platformOrderNo', required: true, example: 'TB20240101001', desc: '平台订单号，相同订单号会合并' },
  { field: 'platform', required: false, example: '淘宝', desc: '平台名称，需与系统中平台名称一致' },
  { field: 'receiverName', required: true, example: '张三', desc: '收件人姓名' },
  { field: 'receiverPhone', required: true, example: '13800138000', desc: '收件人电话' },
  { field: 'receiverAddress', required: true, example: '北京市朝阳区xxx路', desc: '收件地址' },
  { field: 'remark', required: false, example: '尽快发货', desc: '订单备注' },
  { field: 'skuCode', required: true, example: 'SPU001-42', desc: 'SKU编码，必须系统中存在' },
  { field: 'quantity', required: false, example: '1', desc: '数量，默认1' },
  { field: 'unitPrice', required: false, example: '59.90', desc: '单价，默认0' },
]

const returnTemplateFormatData = [
  { field: 'platformOrderNo', required: true, example: 'TB20240101001', desc: '平台单号，必须是系统中已发货的订单' },
  { field: 'reason', required: true, example: '商品质量问题', desc: '退货原因' },
  { field: 'trackingNo', required: false, example: 'SF1234567890', desc: '客户退回的快递单号' },
  { field: 'feeTemplate', required: false, example: '退货标准', desc: '费用模板名称，用于计算快递费' },
  { field: 'estimatedWeight', required: false, example: '1.5', desc: '预估重量(kg)，用于计算快递费' },
  { field: 'shippingFee', required: false, example: '8.00', desc: '退货快递费，不填则自动计算' },
  { field: 'remark', required: false, example: '商品有破损', desc: '备注' },
]

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

// 计算导入时的动态可用库存（原始库存 - 已添加数量）
function getImportAvailableQty(sku: any): number {
  const originalQty = sku.availableQty ?? 0
  const importedItem = importForm.items.find((item: any) => item.skuId === sku.id)
  const importedQty = importedItem ? importedItem.quantity : 0
  return Math.max(0, originalQty - importedQty)
}

const filteredExchangeSkuList = computed(() => {
  if (!exchangeSkuSearch.value) return skuList.value
  const kw = exchangeSkuSearch.value.toLowerCase()
  return skuList.value.filter((s: any) =>
    s.skuCode?.toLowerCase().includes(kw) || s.name?.toLowerCase().includes(kw)
  )
})

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

function getStockTagType(quantity?: number, lowThreshold?: number, outThreshold?: number): string {
  const value = quantity ?? 0
  const low = lowThreshold ?? 10
  const out = outThreshold ?? 0
  if (value <= out) return 'danger'
  if (value <= low) return 'warning'
  return 'success'
}

function addSkuToImport(sku: SkuListItem) {
  const availableQty = getImportAvailableQty(sku)
  if (availableQty <= 0) {
    ElMessage.warning('该SKU库存不足')
    return
  }

  const existing = importForm.items.find((item) => item.skuId === sku.id)
  if (existing) {
    if (existing.quantity >= (sku.availableQty ?? 0)) {
      ElMessage.warning('已达到最大可入库数量')
      return
    }
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
    image: sku.image || sku.mainImage || '',
    availableQty: sku.availableQty,
    lowStockThreshold: sku.lowStockThreshold,
    outOfStockThreshold: sku.outOfStockThreshold,
  })
}

function downloadTemplate() {
  // 使用 xlsx 库生成 Excel 模板
  import('xlsx').then((XLSX) => {
    const headers = ['platformOrderNo', 'platform', 'receiverName', 'receiverPhone', 'receiverAddress', 'remark', 'skuCode', 'quantity', 'unitPrice']
    const exampleRow1 = ['TB20240101001', '淘宝', '张三', '13800138000', '北京市朝阳区xxx路xxx号', '尽快发货', 'SPU001-42', 1, 59.90]
    const exampleRow2 = ['TB20240101001', '淘宝', '张三', '13800138000', '北京市朝阳区xxx路xxx号', '尽快发货', 'SPU001-43', 2, 59.90]
    const exampleRow3 = ['PDD20240101002', '拼多多', '李四', '13900139000', '上海市浦东新区xxx路xxx号', '', 'SPU002-36', 1, 89.00]

    const data = [headers, exampleRow1, exampleRow2, exampleRow3]
    const ws = XLSX.utils.aoa_to_sheet(data)

    // 设置列宽
    ws['!cols'] = [
      { wch: 20 }, // platformOrderNo
      { wch: 12 }, // platform
      { wch: 10 }, // receiverName
      { wch: 15 }, // receiverPhone
      { wch: 30 }, // receiverAddress
      { wch: 15 }, // remark
      { wch: 15 }, // skuCode
      { wch: 10 }, // quantity
      { wch: 10 }, // unitPrice
    ]

    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, '订单导入模板')
    XLSX.writeFile(wb, '订单导入模板.xlsx')
  }).catch(() => {
    ElMessage.error('生成模板失败，请确保已安装 xlsx 库')
  })
}

function downloadReturnTemplate() {
  // 使用 xlsx 库生成批量退货模板
  import('xlsx').then((XLSX) => {
    const headers = ['platformOrderNo', 'reason', 'trackingNo', 'feeTemplate', 'estimatedWeight', 'shippingFee', 'remark']
    const exampleRow1 = ['TB20240101001', '商品质量问题', 'SF1234567890', '退货标准', 1.5, '', '商品有破损']
    const exampleRow2 = ['PDD20240101002', '七天无理由退货', '', '退货标准', 0.5, '', '']
    const exampleRow3 = ['JD20240101003', '发错货', 'YT9876543210', '', '', 10.00, '收到的商品与下单不符']

    const data = [headers, exampleRow1, exampleRow2, exampleRow3]
    const ws = XLSX.utils.aoa_to_sheet(data)

    // 设置列宽
    ws['!cols'] = [
      { wch: 20 }, // platformOrderNo
      { wch: 20 }, // reason
      { wch: 20 }, // trackingNo
      { wch: 15 }, // feeTemplate
      { wch: 15 }, // estimatedWeight
      { wch: 12 }, // shippingFee
      { wch: 30 }, // remark
    ]

    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, '批量退货模板')
    XLSX.writeFile(wb, '批量退货模板.xlsx')
  }).catch(() => {
    ElMessage.error('生成模板失败，请确保已安装 xlsx 库')
  })
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
    const data = response.data
    if (data && data.errors && data.errors.length > 0) {
      // 有部分失败
      showImportResult(data)
    } else {
      // 全部成功
      ElMessage.success(`导入成功，共导入 ${data || 0} 条订单`)
      fileImportDialogVisible.value = false
      selectedFile.value = null
      fetchData()
    }
  } else {
    ElMessage.error(response.message || '导入失败')
  }
  fileImporting.value = false
}

function showImportResult(data: any) {
  const { successCount, totalCount, errors } = data
  const errorList = errors.map((e: string) => `<li class="mb-1">${e}</li>`).join('')

  ElMessageBox.alert(
    `<div>
      <p class="mb-2">导入完成：成功 <strong>${successCount}</strong> / 共 ${totalCount} 个订单</p>
      ${errors.length > 0 ? `
        <p class="mb-1 text-red-500 font-semibold">失败详情：</p>
        <ul class="list-disc pl-5 text-sm text-red-600 max-h-60 overflow-y-auto">${errorList}</ul>
      ` : ''}
    </div>`,
    '导入结果',
    {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '确定',
      type: successCount > 0 ? 'warning' : 'error',
    }
  ).then(() => {
    if (successCount > 0) {
      fileImportDialogVisible.value = false
      selectedFile.value = null
      fetchData()
    }
  })
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
  if (fileImportForm.importType === 'order' && !fileImportForm.warehouseId) {
    ElMessage.warning('请选择目标仓库')
    return
  }

  fileImporting.value = true
  const formData = new FormData()
  formData.append('file', selectedFile.value)
  if (fileImportForm.warehouseId) {
    formData.append('warehouseId', String(fileImportForm.warehouseId))
  }

  try {
    const response = await fetch(uploadUrl.value, {
      method: 'POST',
      headers: uploadHeaders,
      body: formData,
    })
    const result = await response.json()
    if (result.code === 200) {
      const data = result.data
      const importType = fileImportForm.importType === 'return' ? '退货单' : '订单'
      if (data && data.errors && data.errors.length > 0) {
        // 有部分失败
        showImportResult(data)
      } else {
        // 全部成功
        ElMessage.success(`导入成功，共导入 ${data || 0} 条${importType}`)
        fileImportDialogVisible.value = false
        selectedFile.value = null
        fetchData()
      }
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
  // 加载平台列表
  try {
    const res = await getPlatformOptions()
    platforms.value = res.data || []
  } catch {
    platforms.value = []
  }
  // 加载快递公司列表
  try {
    const res = await getCompanyList()
    companyList.value = res.data || []
  } catch {
    companyList.value = []
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
    platformId: undefined,
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
  // 清空商品选择
  selectedSkuGroupKey.value = ''
  // 根据仓库重新加载SKU库存
  if (warehouseId) {
    try {
      const res = await getAllSkuList({ page: 1, size: 1000, warehouseId })
      skuList.value = res.data.list || []
    } catch {}
  } else {
    skuList.value = []
  }
}

async function viewDetail(row: Order) {
  try {
    const res = await getOrderDetail(row.id)
    detail.value = res.data
    exchangeDetail.value = null

    // 如果是换货中状态，获取换货详情
    if (row.orderStatus === 'EXCHANGING') {
      try {
        const exchangeRes = await getExchangeList({ page: 1, size: 1, orderNo: row.orderNo })
        const exchangeList = exchangeRes.data?.list || []
        if (exchangeList.length > 0) {
          const detailRes = await getExchangeDetail(exchangeList[0].id)
          exchangeDetail.value = detailRes.data
        }
      } catch {}
    }

    detailVisible.value = true
  } catch {}
}

async function openEditDialog(row: Order) {
  const res = await getOrderDetail(row.id)
  const order = res.data
  Object.assign(editForm, {
    id: row.id,
    platformId: order.platformId || undefined,
    platformOrderNo: order.platformOrderNo || '',
    receiverName: order.receiverName || '',
    receiverPhone: order.receiverPhone || '',
    receiverAddress: order.receiverAddress || '',
    remark: order.remark || '',
  })
  editDialogVisible.value = true
}

async function handleEdit() {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) return

  editing.value = true
  try {
    await updateOrder(editForm.id, {
      platformId: editForm.platformId,
      platformOrderNo: editForm.platformOrderNo || undefined,
      receiverName: editForm.receiverName,
      receiverPhone: editForm.receiverPhone,
      receiverAddress: editForm.receiverAddress,
      remark: editForm.remark,
    })
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    fetchData()
  } catch {} finally {
    editing.value = false
  }
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
    ElMessage.success('出库成功')
    fetchData()
  } catch {}
}

async function openReturnDialog(row: Order) {
  const res = await getOrderDetail(row.id)
  const order = res.data
  Object.assign(returnForm, {
    orderId: row.id,
    reason: '',
    trackingNo: '',
    responsibleParty: 'SELLER',
    shippingFee: undefined,
    feeTemplateId: undefined,
    estimatedWeight: undefined,
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

  // 加载快递费用模板（优先选择"退货"公司）
  try {
    const { getCompanyList, getTemplateListByCompany } = await import('@/api/express')
    const companyRes = await getCompanyList()
    const companies = companyRes.data || []
    // 优先查找名称包含"退货"的公司
    const returnCompany = companies.find((c: any) => c.name.includes('退货'))
    const targetCompany = returnCompany || companies[0]
    if (targetCompany) {
      const templateRes = await getTemplateListByCompany(targetCompany.id)
      returnTemplateList.value = templateRes.data || []
      // 自动选择默认模板
      const defaultTemplate = returnTemplateList.value.find((t: any) => t.isDefault === 1)
      if (defaultTemplate) {
        returnForm.feeTemplateId = defaultTemplate.id
      }
    }
  } catch {}

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
      shippingFee: returnForm.responsibleParty === 'CUSTOMER' ? 0 : returnForm.shippingFee,
      feeTemplateId: returnForm.responsibleParty === 'SELLER' ? returnForm.feeTemplateId : undefined,
      estimatedWeight: returnForm.responsibleParty === 'SELLER' ? returnForm.estimatedWeight : undefined,
      remark: returnForm.responsibleParty === 'CUSTOMER'
        ? `客户自付快递费${returnForm.remark ? '，' + returnForm.remark : ''}`
        : returnForm.remark || undefined,
      items,
    })
    ElMessage.success('退货单创建成功')
    returnDialogVisible.value = false
    fetchData()
  } catch {} finally {
    returning.value = false
  }
}

// 换货相关函数
async function openExchangeDialog(row: Order) {
  const res = await getOrderDetail(row.id)
  const order = res.data
  Object.assign(exchangeForm, {
    orderId: row.id,
    orderNo: row.orderNo,
    shipWarehouseId: undefined,
    reason: '',
    responsibleParty: 'SELLER',
    remark: '',
    returnItems: (order.items || []).map((item: any) => ({
      skuId: item.skuId,
      skuCode: item.skuCode,
      skuName: item.skuName,
      sizeValue: item.sizeValue,
      quantity: item.quantity,
      orderedQty: item.quantity,
      image: item.skuImage || item.image || '',
      checked: true,
    })),
    exchangeItems: [],
  })
  skuList.value = []
  exchangeDialogVisible.value = true
}

async function handleExchangeWarehouseChange(warehouseId: number) {
  exchangeForm.exchangeItems = []
  if (warehouseId) {
    try {
      const skuRes = await getAllSkuList({ page: 1, size: 1000, warehouseId })
      skuList.value = skuRes.data.list || []
    } catch {}
  } else {
    skuList.value = []
  }
}

function addExchangeItem(sku: any) {
  const existing = exchangeForm.exchangeItems.find(i => i.skuId === sku.id)
  if (existing) {
    existing.quantity += 1
    return
  }
  exchangeForm.exchangeItems.push({
    skuId: sku.id,
    skuCode: sku.skuCode,
    skuName: sku.name,
    sizeValue: sku.sizeValue || '',
    quantity: 1,
    image: sku.image || sku.mainImage || '',
    availableQty: sku.availableQty,
    lowStockThreshold: sku.lowStockThreshold,
    outOfStockThreshold: sku.outOfStockThreshold,
  })
  exchangeSkuSelectorVisible.value = false
}

async function handleExchange() {
  if (!exchangeForm.shipWarehouseId) {
    ElMessage.warning('请选择出库仓库')
    return
  }
  const checkedReturnItems = exchangeForm.returnItems.filter(i => i.checked)
  if (checkedReturnItems.length === 0) {
    ElMessage.warning('请至少勾选一个退回商品')
    return
  }
  if (exchangeForm.exchangeItems.length === 0) {
    ElMessage.warning('请添加换出商品')
    return
  }

  exchanging.value = true
  try {
    await createExchange({
      orderId: exchangeForm.orderId,
      warehouseId: exchangeForm.shipWarehouseId,
      reason: exchangeForm.reason || undefined,
      shippingFee: exchangeForm.responsibleParty === 'CUSTOMER' ? 0 : undefined,
      remark: exchangeForm.responsibleParty === 'CUSTOMER'
        ? `客户自付快递费${exchangeForm.remark ? '，' + exchangeForm.remark : ''}`
        : exchangeForm.remark || undefined,
      items: [
        // 勾选的退回商品
        ...exchangeForm.returnItems.filter(i => i.checked).map(i => ({
          skuId: i.skuId,
          skuCode: i.skuCode,
          skuName: i.skuName,
          sizeValue: i.sizeValue,
          quantity: i.quantity,
          itemType: 'RETURN_ITEM',
        })),
        // 换出商品
        ...exchangeForm.exchangeItems.map(i => ({
          skuId: i.skuId,
          skuCode: i.skuCode,
          skuName: i.skuName,
          sizeValue: i.sizeValue,
          quantity: i.quantity,
          itemType: 'EXCHANGE_ITEM',
        })),
      ],
    })
    ElMessage.success('换货单创建成功')
    exchangeDialogVisible.value = false
    fetchData()
  } catch {} finally {
    exchanging.value = false
  }
}

// 快速发货
async function openQuickShipDialog(row: any) {
  try {
    // 获取出库单
    const { getOutboundList } = await import('@/api/outbound')
    const outboundRes = await getOutboundList({ page: 1, size: 1, orderNo: row.orderNo })
    const outboundList = outboundRes.data?.list || []
    if (outboundList.length === 0) {
      ElMessage.warning('未找到该订单的出库单')
      return
    }
    const outbound = outboundList[0]

    // 获取出库单详情
    const { getOutboundDetail } = await import('@/api/outbound')
    const detailRes = await getOutboundDetail(outbound.id)
    quickShipDetail.value = detailRes.data

    // 重置表单
    quickShipForm.outboundId = outbound.id
    quickShipForm.trackingNo = ''
    quickShipForm.expressCompanyId = undefined
    quickShipForm.feeTemplateId = undefined
    quickShipForm.estimatedWeight = 0
    quickShipForm.shippingFee = undefined
    quickShipTemplateList.value = []
    quickShipTemplateDetail.value = null

    // 加载快递公司和模板
    if (companyList.value.length > 0) {
      const firstCompany = companyList.value[0]
      quickShipForm.expressCompanyId = firstCompany.id
      await handleQuickShipCompanyChange(firstCompany.id)
    }
  } catch {
    ElMessage.error('获取出库单信息失败')
    return
  }
  quickShipDialogVisible.value = true
}

async function handleQuickShipCompanyChange(companyId: number) {
  quickShipForm.feeTemplateId = undefined
  quickShipTemplateDetail.value = null
  quickShipTemplateList.value = []
  if (companyId) {
    try {
      const { getTemplateListByCompany } = await import('@/api/express')
      const res = await getTemplateListByCompany(companyId)
      quickShipTemplateList.value = res.data || []
      const defaultTemplate = quickShipTemplateList.value.find((t: any) => t.isDefault === 1)
      if (defaultTemplate) {
        await loadQuickShipTemplateDetail(defaultTemplate.id)
      }
    } catch {
      quickShipTemplateList.value = []
    }
  }
}

async function loadQuickShipTemplateDetail(templateId: number) {
  quickShipForm.feeTemplateId = templateId
  try {
    const { getTemplateDetail } = await import('@/api/express')
    const res = await getTemplateDetail(templateId)
    quickShipTemplateDetail.value = res.data
    if (quickShipForm.estimatedWeight > 0) {
      calculateQuickShipFee()
    }
  } catch {
    quickShipTemplateDetail.value = null
  }
}

async function handleQuickShipTemplateChange(templateId: number) {
  if (templateId) {
    await loadQuickShipTemplateDetail(templateId)
  } else {
    quickShipTemplateDetail.value = null
    quickShipForm.shippingFee = undefined
  }
}

function calculateQuickShipFee() {
  if (!quickShipTemplateDetail.value || !quickShipForm.estimatedWeight || quickShipForm.estimatedWeight <= 0) return

  const template = quickShipTemplateDetail.value
  if (template.templateType === 'FIRST_CONTINUE') {
    const firstWeight = template.firstWeight || 1
    const firstFee = template.firstFee || 0
    const additionalWeight = template.additionalWeight || 1
    const additionalFee = template.additionalFee || 0

    if (quickShipForm.estimatedWeight <= firstWeight) {
      quickShipForm.shippingFee = firstFee
    } else {
      const extraWeight = quickShipForm.estimatedWeight - firstWeight
      const extraUnits = Math.ceil(extraWeight / additionalWeight)
      quickShipForm.shippingFee = firstFee + extraUnits * additionalFee
    }
  } else if (template.steps && template.steps.length > 0) {
    const sortedSteps = [...template.steps].sort((a: any, b: any) => a.minWeight - b.minWeight)
    const matchedStep = sortedSteps.find((s: any) =>
      quickShipForm.estimatedWeight >= s.minWeight && quickShipForm.estimatedWeight < s.maxWeight
    )
    if (matchedStep) {
      quickShipForm.shippingFee = matchedStep.fee
    } else {
      const lastStep = sortedSteps[sortedSteps.length - 1]
      if (quickShipForm.estimatedWeight >= lastStep.minWeight) {
        quickShipForm.shippingFee = lastStep.fee
      } else {
        quickShipForm.shippingFee = undefined
      }
    }
  }
}

async function handleQuickShipConfirm() {
  if (!quickShipForm.trackingNo) {
    ElMessage.warning('请输入快递单号')
    return
  }
  if (!quickShipForm.expressCompanyId) {
    ElMessage.warning('请选择快递公司')
    return
  }
  if (!quickShipForm.feeTemplateId) {
    ElMessage.warning('请选择费用模板')
    return
  }
  if (!quickShipForm.estimatedWeight || quickShipForm.estimatedWeight <= 0) {
    ElMessage.warning('请输入预估重量')
    return
  }
  if (!quickShipForm.shippingFee || quickShipForm.shippingFee <= 0) {
    ElMessage.warning('请输入快递费用')
    return
  }
  try {
    await ElMessageBox.confirm('确认发货后将扣减库存，确定继续吗？', '确认发货', { type: 'warning' })
  } catch { return }

  quickShipping.value = true
  try {
    const { confirmOutbound } = await import('@/api/outbound')
    await confirmOutbound({
      outboundId: quickShipForm.outboundId,
      trackingNo: quickShipForm.trackingNo,
      expressCompanyId: quickShipForm.expressCompanyId,
      feeTemplateId: quickShipForm.feeTemplateId,
      estimatedWeight: quickShipForm.estimatedWeight,
      shippingFee: quickShipForm.shippingFee,
    })
    ElMessage.success('发货成功')
    quickShipDialogVisible.value = false
    fetchData()
  } catch {} finally {
    quickShipping.value = false
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

<style scoped>
/* 操作栏 */
.action-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  position: sticky;
  top: 0;
  z-index: 10;
}

/* 换货按钮 - 紫色 */
.exchange-btn {
  color: #7c3aed !important;
}
.exchange-btn:hover {
  color: #6d28d9 !important;
}

/* 换货中状态标签 - 紫色 */
:deep(.el-tag--info.is-dark) {
  --el-tag-bg-color: #7c3aed;
  --el-tag-border-color: #7c3aed;
  --el-tag-text-color: #fff;
}
</style>

<style>
/* 批量出库模式 - 绿色选择框 */
.outbound-mode .el-table__row:not(.disabled-row) .el-checkbox__input .el-checkbox__inner {
  border-color: #67c23a !important;
  background-color: #67c23a !important;
  box-shadow: 0 0 0 1px #67c23a;
}
.outbound-mode .el-table__row:not(.disabled-row) .el-checkbox__input .el-checkbox__inner::after {
  border-color: #fff !important;
}
.outbound-mode .el-table__row:not(.disabled-row) .el-checkbox__input.is-checked .el-checkbox__inner {
  background-color: #409eff !important;
  border-color: #409eff !important;
  box-shadow: none;
}
.outbound-mode .el-table__row:not(.disabled-row) .el-checkbox__input.is-checked .el-checkbox__inner::after {
  border-color: #fff !important;
}

/* 批量退货模式 - 橙色选择框 */
.return-mode .el-table__row:not(.disabled-row) .el-checkbox__input .el-checkbox__inner {
  border-color: #e6a23c !important;
  background-color: #e6a23c !important;
  box-shadow: 0 0 0 1px #e6a23c;
}
.return-mode .el-table__row:not(.disabled-row) .el-checkbox__input .el-checkbox__inner::after {
  border-color: #fff !important;
}
.return-mode .el-table__row:not(.disabled-row) .el-checkbox__input.is-checked .el-checkbox__inner {
  background-color: #409eff !important;
  border-color: #409eff !important;
  box-shadow: none;
}
.return-mode .el-table__row:not(.disabled-row) .el-checkbox__input.is-checked .el-checkbox__inner::after {
  border-color: #fff !important;
}

/* 批量取消模式 - 红色选择框 */
.cancel-mode .el-table__row:not(.disabled-row) .el-checkbox__input .el-checkbox__inner {
  border-color: #f56c6c !important;
  background-color: #f56c6c !important;
  box-shadow: 0 0 0 1px #f56c6c;
}
.cancel-mode .el-table__row:not(.disabled-row) .el-checkbox__input .el-checkbox__inner::after {
  border-color: #fff !important;
}
.cancel-mode .el-table__row:not(.disabled-row) .el-checkbox__input.is-checked .el-checkbox__inner {
  background-color: #409eff !important;
  border-color: #409eff !important;
  box-shadow: none;
}
.cancel-mode .el-table__row:not(.disabled-row) .el-checkbox__input.is-checked .el-checkbox__inner::after {
  border-color: #fff !important;
}

/* 不可选择的行 - 半透明 */
.disabled-row {
  opacity: 0.5;
}
.disabled-row .el-checkbox__input .el-checkbox__inner {
  background-color: #ebeef5 !important;
  border-color: #dcdfe6 !important;
}

/* 可选择的行 - 浅色背景高亮 */
.selectable-row {
  background-color: #fdf6ec !important;
}
.selectable-row:hover td {
  background-color: #faecd8 !important;
}

/* SKU标签 */
.sku-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.sku-tag {
  margin: 0;
}

/* 紫色标签（换货中） */
.el-tag--purple {
  --el-tag-bg-color: #f3e8ff;
  --el-tag-border-color: #d8b4fe;
  --el-tag-text-color: #7c3aed;
  --el-tag-hover-color: #ede9fe;
}
.el-tag--purple.is-dark {
  --el-tag-bg-color: #7c3aed;
  --el-tag-border-color: #7c3aed;
  --el-tag-text-color: #fff;
}
/* 深紫色标签（已换货） */
.el-tag--purple-dark {
  --el-tag-bg-color: #6b21a8;
  --el-tag-border-color: #6b21a8;
  --el-tag-text-color: #fff;
  --el-tag-hover-color: #7c3aed;
}
</style>
