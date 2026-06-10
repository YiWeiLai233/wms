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
  locationId: number
  locationCode: string
  quantity: number
  lockedQty: number
  defectiveQty: number
  totalQuantity: number
  createdAt: string
  updatedAt: string
}

export interface StockLog {
  id: number
  bizType: string
  bizNo: string
  skuId: number
  skuCode: string
  warehouseId: number
  warehouseName: string
  locationId: number
  locationCode: string
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
  locationCode: string
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
  locationId?: number
  locationCode?: string
  stockType?: string
}) {
  return request.get<any, ApiResponse<PageResult<StockItem>>>('/stocks/query', { params })
}

// 库存调整
export function adjustStock(data: { skuId: number; warehouseId: number; locationId: number; quantity: number; remark?: string }) {
  return request.post<any, ApiResponse<void>>('/stocks/adjust', data)
}

// 库存流水
export function getStockLogs(params: PageParams & { bizType?: string; bizNo?: string; skuId?: number; warehouseId?: number }) {
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
