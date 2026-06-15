import request from './request'
import type { ApiResponse, PageParams, PageResult } from './request'

export interface StockItem {
  id: number
  skuId: number
  skuCode: string
  skuName: string
  productId: number
  productName: string
  warehouseId: number
  warehouseName: string
  quantity: number
  lockedQty: number
  totalQuantity: number
  stockAlertStatus: string
  stockAlertStatusName: string
  lowStockThreshold: number
  outOfStockThreshold: number
  createdAt: string
  updatedAt: string
}

export interface StockLog {
  id: number
  bizType: string
  bizNo: string
  platformOrderNo?: string
  skuId: number
  skuCode: string
  warehouseId: number
  warehouseName: string
  quantityBefore: number
  quantityChange: number
  quantityAfter: number
  operatorId: number
  operatorName: string
  remark: string
  createdAt: string
}

export interface StockCheck {
  id: number
  checkNo: string
  warehouseId: number
  warehouseName: string
  status: string
  remark: string
  items?: StockCheckItem[]
  createdAt: string
}

export interface StockCheckItem {
  id: number
  checkId: number
  skuId: number
  skuCode: string
  skuName: string
  systemQty: number
  actualQty: number | null
  diffQty: number | null
}

// 库存查询
export function queryStock(params: PageParams & {
  skuId?: number
  skuCode?: string
  skuName?: string
  productName?: string
  warehouseId?: number
  stockType?: string
}) {
  return request.get<any, ApiResponse<PageResult<StockItem>>>('/stocks/query', { params })
}

// 库存调整
export function adjustStock(data: { skuId: number; warehouseId: number; quantity: number; remark?: string }) {
  return request.post<any, ApiResponse<void>>('/stocks/adjust', data)
}

// 库存流水
export function getStockLogs(params: PageParams & {
  bizType?: string
  bizNo?: string
  platformOrderNo?: string
  skuId?: number
  skuCode?: string
  warehouseId?: number
  startTime?: string
  endTime?: string
}) {
  return request.get<any, ApiResponse<PageResult<StockLog>>>('/stock-logs', { params })
}

// 盘点单列表
export function getStockCheckList(params: PageParams & { warehouseId?: number; status?: string }) {
  return request.get<any, ApiResponse<PageResult<StockCheck>>>('/stock-checks', { params })
}

// 创建盘点单
export function createStockCheck(data: { warehouseId: number; remark?: string }) {
  return request.post<any, ApiResponse<number>>('/stock-checks', data)
}

// 盘点单详情
export function getStockCheckDetail(id: number) {
  return request.get<any, ApiResponse<StockCheck>>(`/stock-checks/${id}`)
}

// 提交盘点结果
export function submitStockCheck(data: { checkId: number; items: { itemId: number; actualQty: number }[] }) {
  return request.post<any, ApiResponse<void>>('/stock-checks/submit', data)
}

// 查询特殊仓库库存（次品仓/报废仓）
export function getSpecialStock(type: string, params?: { skuCode?: string; skuName?: string }) {
  return request.get<any, ApiResponse<StockItem[]>>('/stocks/special', { params: { type, ...params } })
}

// 确认可售（次品仓→普通仓）
export function confirmSellable(stockId: number, targetWarehouseId: number, quantity: number) {
  return request.post<any, ApiResponse<void>>(`/stocks/confirm-sellable?stockId=${stockId}&targetWarehouseId=${targetWarehouseId}&quantity=${quantity}`)
}

// 确认报废处置（报废仓→移除）
export function confirmDispose(stockId: number, quantity: number) {
  return request.post<any, ApiResponse<void>>(`/stocks/confirm-dispose?stockId=${stockId}&quantity=${quantity}`)
}

// 确认转入报废仓（次品仓→报废仓）
export function confirmScrap(stockId: number, quantity: number) {
  return request.post<any, ApiResponse<void>>(`/stocks/confirm-scrap?stockId=${stockId}&quantity=${quantity}`)
}
