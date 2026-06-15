import request from './request'
import type { ApiResponse, PageParams, PageResult } from './request'

export interface StockAlertConfig {
  id: number
  skuId: number
  skuCode: string
  skuName: string
  warehouseId: number | null
  warehouseName: string | null
  lowStockThreshold: number
  outOfStockThreshold: number
  enabled: number
  remark: string
  createdAt: string
  updatedAt: string
}

export interface StockAlertStatus {
  stockId: number
  skuId: number
  skuCode: string
  skuName: string
  warehouseId: number
  warehouseName: string
  quantity: number
  lockedQty: number
  lowStockThreshold: number
  outOfStockThreshold: number
  alertStatus: string
  alertStatusName: string
  updatedAt: string
}

export interface StockAlertStatistics {
  normalCount: number
  lowStockCount: number
  outOfStockCount: number
}

// 分页查询预警配置
export function getStockAlertConfigs(params: PageParams & {
  skuCode?: string
  skuName?: string
  warehouseId?: number
  enabled?: number
}) {
  return request.get<any, ApiResponse<PageResult<StockAlertConfig>>>('/stock-alert-configs', { params })
}

// 新增预警配置
export function createStockAlertConfig(data: {
  skuId: number
  warehouseId?: number | null
  lowStockThreshold: number
  outOfStockThreshold: number
  enabled?: number
  remark?: string
}) {
  return request.post<any, ApiResponse<number>>('/stock-alert-configs', data)
}

// 修改预警配置
export function updateStockAlertConfig(id: number, data: {
  lowStockThreshold: number
  outOfStockThreshold: number
  enabled?: number
  remark?: string
}) {
  return request.put<any, ApiResponse<void>>(`/stock-alert-configs/${id}`, data)
}

// 删除预警配置
export function deleteStockAlertConfig(id: number) {
  return request.delete<any, ApiResponse<void>>(`/stock-alert-configs/${id}`)
}

// 启用/禁用预警配置
export function updateStockAlertEnabled(id: number, enabled: number) {
  return request.put<any, ApiResponse<void>>(`/stock-alert-configs/${id}/enabled`, { enabled })
}

// 查询低库存商品
export function getLowStockList() {
  return request.get<any, ApiResponse<StockAlertStatus[]>>('/stock-alert-configs/low-stock')
}

// 查询缺货商品
export function getOutOfStockList() {
  return request.get<any, ApiResponse<StockAlertStatus[]>>('/stock-alert-configs/out-of-stock')
}

// 查询预警统计
export function getStockAlertStatistics() {
  return request.get<any, ApiResponse<StockAlertStatistics>>('/stock-alert-configs/statistics')
}
