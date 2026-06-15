<template>
  <div class="page-container">
    <PageHeader title="订单管理">
      <template #actions>
        <el-button type="primary" icon="Upload" @click="openImportDialog">手动导入</el-button>
        <el-button type="success" icon="Document" @click="fileImportDialogVisible = true">文档导入</el-button>
        <el-button type="warning" icon="TopRight" :type="selectMode === 'outbound' ? 'warning' : ''" @click="toggleSelectMode('outbound')">
          {{ selectMode === 'outbound' ? '取消选择' : '批量出库' }}
        </el-button>
        <el-button type="danger" icon="BottomLeft" :type="selectMode === 'return' ? 'danger' : ''" @click="toggleSelectMode('return')">
          {{ selectMode === 'return' ? '取消选择' : '批量退货' }}
        </el-button>
        <el-button v-if="selectMode && selectedOrders.length > 0" type="primary" @click="handleBatchAction">
          确认{{ selectMode === 'outbound' ? '出库' : '退货' }} ({{ selectedOrders.length }})
        </el-button>
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
      <!-- 选择模式提示 -->
      <el-alert
        v-if="selectMode"
        :title="selectMode === 'outbound' ? '批量出库模式：请勾选需要出库的订单（仅待出库状态可选）' : '批量退货模式：请勾选需要退货的订单（仅已发货状态可选）'"
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
        :class="selectMode === 'outbound' ? 'outbound-mode' : selectMode === 'return' ? 'return-mode' : ''"
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
        <el-table-column label="操作" width="450" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link icon="View" @click="viewDetail(row)">详情</el-button>
            <el-button type="warning" link icon="Edit" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button v-if="row.orderStatus === 'WAIT_OUTBOUND'" type="success" link icon="TopRight" @click="createOutboundOrder(row)">
              出库
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
              发货管理
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
          <el-table-column prop="availableQty" label="可用库存" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getStockTagType(row.availableQty, row.lowStockThreshold, row.outOfStockThreshold)" size="small">{{ row.availableQty ?? 0 }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center">
            <template #default="{ row }">
              <el-button type="primary" link icon="Plus" @click="addSkuToImport(row)">加入</el-button>
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
        <el-form-item label="客户快递单号">
          <el-input v-model="returnForm.trackingNo" placeholder="客户退回的快递单号（选填）" />
        </el-form-item>

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

    <!-- 批量退货弹窗 -->
    <el-dialog v-model="batchReturnDialogVisible" title="批量退货" width="500px" destroy-on-close>
      <el-form ref="batchReturnFormRef" :model="batchReturnForm" :rules="batchReturnRules" label-width="90px">
        <el-form-item label="退货订单">
          <div class="text-sm text-gray-600">已选择 {{ selectedOrders.length }} 个订单</div>
        </el-form-item>
        <el-form-item label="退货原因" prop="reason">
          <el-select
            v-model="batchReturnForm.reason"
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
        <el-form-item label="客户快递单号">
          <el-input v-model="batchReturnForm.trackingNo" placeholder="客户退回的快递单号（选填）" />
        </el-form-item>
        <el-form-item label="退货快递费">
          <el-input-number v-model="batchReturnForm.shippingFee" :min="0" :precision="2" style="width: 100%" placeholder="快递费用（选填）" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="batchReturnForm.remark" type="textarea" :rows="2" placeholder="备注信息（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchReturnDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchReturning" @click="handleBatchReturnSubmit">确认退货</el-button>
      </template>
    </el-dialog>

    <!-- 文档导入弹窗 -->
    <el-dialog v-model="fileImportDialogVisible" title="文档导入订单" width="680px" destroy-on-close>
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

      <div class="mb-4">
        <el-button type="primary" link icon="Download" @click="downloadTemplate">下载导入模板</el-button>
      </div>

      <el-form :model="fileImportForm" label-width="80px">
        <el-form-item label="目标仓库" required>
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
      <el-table :data="templateFormatData" border size="small" class="mb-2">
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
        <el-button type="primary" :loading="fileImporting" :disabled="!fileImportForm.warehouseId" @click="handleFileImport">开始导入</el-button>
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
import { getAllSkuList } from '@/api/product'
import type { Sku } from '@/api/product'
import { getWarehouseList } from '@/api/warehouse'
import type { Warehouse } from '@/api/warehouse'
import { getPlatformOptions } from '@/api/platform'
import type { Platform } from '@/api/platform'
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
}

const { tableData, loading, pagination, searchParams, handleSearch, handleReset, handlePageChange, handleSizeChange, fetchData } = useTable<Order>(getOrderList)

const warehouses = ref<Warehouse[]>([])
const platforms = ref<Platform[]>([])
const skuList = ref<SkuListItem[]>([])
const detailVisible = ref(false)
const detail = ref<Partial<Order>>({})

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
  if (selectMode.value === 'return') return row.orderStatus === 'SHIPPED'
  return false
}

function getRowClassName({ row }: { row: Order }) {
  if (!selectMode.value) return ''
  if (selectMode.value === 'outbound' && row.orderStatus === 'WAIT_OUTBOUND') return 'selectable-row'
  if (selectMode.value === 'return' && row.orderStatus === 'SHIPPED') return 'selectable-row'
  return 'disabled-row'
}

function toggleSelectMode(mode: 'outbound' | 'return') {
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
  } else {
    await handleBatchReturn()
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

  // 打开批量退货弹窗
  batchReturnForm.reason = ''
  batchReturnForm.remark = ''
  batchReturnDialogVisible.value = true
}

// 批量退货弹窗
const batchReturnDialogVisible = ref(false)
const batchReturning = ref(false)
const batchReturnFormRef = ref<FormInstance>()
const batchReturnForm = reactive({
  reason: '',
  trackingNo: '',
  shippingFee: undefined as number | undefined,
  remark: '',
})
const batchReturnRules: FormRules = {
  reason: [{ required: true, message: '请选择或输入退货原因', trigger: 'change' }],
}

async function handleBatchReturnSubmit() {
  const valid = await batchReturnFormRef.value?.validate().catch(() => false)
  if (!valid) return

  const orderIds = selectedOrders.value.filter(o => o.orderStatus === 'SHIPPED').map(o => o.id)

  batchReturning.value = true
  try {
    const res = await createBatchReturn({
      orderIds,
      reason: batchReturnForm.reason,
      remark: batchReturnForm.remark || undefined,
      trackingNo: batchReturnForm.trackingNo || undefined,
      shippingFee: batchReturnForm.shippingFee,
    })
    ElMessage.success(`批量退货成功，共创建 ${res.data?.length || 0} 个退货单`)
    batchReturnDialogVisible.value = false
    selectedOrders.value = []
    selectMode.value = null
    fetchData()
  } catch {} finally {
    batchReturning.value = false
  }
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
  warehouseId: undefined as number | undefined,
})

const BASE_URL = 'http://localhost:8080'
const uploadUrl = `${BASE_URL}/api/orders/import-file`
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

function getStockTagType(quantity?: number, lowThreshold?: number, outThreshold?: number): string {
  const value = quantity ?? 0
  const low = lowThreshold ?? 10
  const out = outThreshold ?? 0
  if (value <= out) return 'danger'
  if (value <= low) return 'warning'
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
    image: sku.image || sku.mainImage || '',
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
      const data = result.data
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
      shippingFee: returnForm.shippingFee,
      feeTemplateId: returnForm.feeTemplateId,
      estimatedWeight: returnForm.estimatedWeight,
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

<style>
/* 批量出库模式 - 黄色选择框 */
.outbound-mode .el-table__row:not(.disabled-row) .el-checkbox__input .el-checkbox__inner {
  border-color: #fadb14 !important;
  background-color: #fadb14 !important;
  box-shadow: 0 0 0 1px #fadb14;
}
.outbound-mode .el-table__row:not(.disabled-row) .el-checkbox__input .el-checkbox__inner::after {
  border-color: #333 !important;
}
.outbound-mode .el-table__row:not(.disabled-row) .el-checkbox__input.is-checked .el-checkbox__inner {
  background-color: #409eff !important;
  border-color: #409eff !important;
  box-shadow: none;
}
.outbound-mode .el-table__row:not(.disabled-row) .el-checkbox__input.is-checked .el-checkbox__inner::after {
  border-color: #fff !important;
}

/* 批量退货模式 - 红色选择框 */
.return-mode .el-table__row:not(.disabled-row) .el-checkbox__input .el-checkbox__inner {
  border-color: #ff4d4f !important;
  background-color: #ff4d4f !important;
  box-shadow: 0 0 0 1px #ff4d4f;
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
</style>
