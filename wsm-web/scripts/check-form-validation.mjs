import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const workspaceRoot = resolve(root, '..')

function source(path, base = root) {
  return readFileSync(resolve(base, path), 'utf8')
}

function requireSnippets(path, snippets, base = root) {
  const text = source(path, base)
  for (const snippet of snippets) {
    assert.ok(
      text.includes(snippet),
      `${path} 缺少表单校验片段: ${snippet}`,
    )
  }
}

requireSnippets('src/views/order/OrderList.vue', [
  ':rules="exchangeRules"',
  'prop="reason"',
  'prop="shipWarehouseId"',
  'ref="quickShipFormRef"',
  ':rules="quickShipRules"',
  'prop="trackingNo"',
  'prop="expressCompanyId"',
  'prop="feeTemplateId"',
  'prop="estimatedWeight"',
  'prop="shippingFee"',
  'ref="fileImportFormRef"',
  ':rules="fileImportRules"',
  'prop="importType"',
  'prop="warehouseId"',
  'exchangeFormRef.value?.validate()',
  'quickShipFormRef.value?.validate()',
  'fileImportFormRef.value?.validate()',
])

requireSnippets('src/views/outbound/OutboundList.vue', [
  'ref="confirmFormRef"',
  ':model="confirmForm"',
  ':rules="confirmRules"',
  'confirmFormRef.value?.validate()',
  'deleteOutbound',
  'handleDelete(row.id)',
  '确定删除该发货单吗？',
])

requireSnippets('src/api/outbound.ts', [
  'export function deleteOutbound',
  "request.delete<any, ApiResponse<void>>(`/outbound/${id}`)",
])

requireSnippets('src/views/returns/ReturnList.vue', [
  'validateInspectionItems',
  'if (!validateInspectionItems(checkForm.value.items)) return',
  'if (!validateInspectionItems(confirmItems.value)) return',
])

requireSnippets('src/views/exchange/ExchangeList.vue', [
  'ref="shipFormRef"',
  ':rules="shipRules"',
  'prop="reason"',
  'prop="warehouseId"',
  'shipFormRef.value?.validate()',
  'await handleCreateWarehouseChange(detailRes.data.warehouseId)',
  'validateExchangeInspectionItems',
  'if (!validateExchangeInspectionItems()) return',
])

requireSnippets('src/views/express/ExpressFeeTemplate.vue', [
  'validateFeeTemplatePricing',
  'prop="templateType"',
  'prop="firstWeight"',
  'prop="firstFee"',
  'prop="additionalWeight"',
  'prop="additionalFee"',
])

requireSnippets('src/views/express/ExpressFeeReport.vue', [
  'updateExpressFeeItem',
  'deleteExpressFeeItem',
  'openEditDialog(row)',
  'handleDelete(row)',
  '确定删除该快递费用记录吗？',
  'prop="expressCompanyId"',
  'prop="shippingFee"',
  'editFormRef.value?.validate()',
])

requireSnippets('src/api/report.ts', [
  'export function updateExpressFeeItem',
  'export function deleteExpressFeeItem',
])

requireSnippets('src/views/product/ProductList.vue', [
  'warehouseId: [{ required: true',
])

requireSnippets('src/views/system/UserList.vue', [
  'roleIds: [{ required: true',
])

requireSnippets('src/views/warehouse/SpecialWarehouseView.vue', [
  'ref="sellableFormRef"',
  ':rules="sellableRules"',
  'ref="scrapFormRef"',
  ':rules="scrapRules"',
  'ref="disposeFormRef"',
  ':rules="disposeRules"',
  'sellableFormRef.value?.validate()',
  'scrapFormRef.value?.validate()',
  'disposeFormRef.value?.validate()',
])

requireSnippets('src/views/system/BackupManage.vue', [
  'ref="configFormRef"',
  ':rules="configRules"',
  'prop="backupPath"',
  'prop="autoBackupTime"',
  'prop="remoteHost"',
  'prop="remotePort"',
  'prop="remoteUsername"',
  'prop="remotePassword"',
  'prop="remotePath"',
  'backupFormRef.value?.validate()',
  'configFormRef.value?.validate()',
])

for (const path of [
  'wms-backend/src/main/java/com/yiweilai/wms/exchange/dto/ExchangeCreateDTO.java',
  'wms-backend/src/main/java/com/yiweilai/wms/exchange/dto/ExchangeCheckDTO.java',
  'wms-backend/src/main/java/com/yiweilai/wms/express/dto/ExpressFeeTemplateSaveDTO.java',
  'wms-backend/src/main/java/com/yiweilai/wms/order/dto/OrderImportDTO.java',
  'wms-backend/src/main/java/com/yiweilai/wms/product/dto/ProductSaveDTO.java',
  'wms-backend/src/main/java/com/yiweilai/wms/returns/dto/ReturnCheckDTO.java',
  'wms-backend/src/main/java/com/yiweilai/wms/returns/dto/ReturnConfirmDTO.java',
  'wms-backend/src/main/java/com/yiweilai/wms/stock/dto/StockCheckSubmitDTO.java',
]) {
  requireSnippets(path, [
    'import jakarta.validation.Valid;',
    '@Valid',
  ], workspaceRoot)
}

requireSnippets('wms-backend/src/main/java/com/yiweilai/wms/user/dto/UserSaveDTO.java', [
  '@NotEmpty(message = "至少选择一个角色")',
  '@Size(min = 4, message = "密码至少4位")',
], workspaceRoot)

requireSnippets('wms-backend/src/main/java/com/yiweilai/wms/user/service/impl/UserServiceImpl.java', [
  'if (dto.getPassword() == null || dto.getPassword().isBlank())',
], workspaceRoot)

console.log('关键写操作表单均已绑定必填与业务校验。')
