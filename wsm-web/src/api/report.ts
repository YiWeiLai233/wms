import request from './request'
import type { ApiResponse } from './request'

export interface DashboardData {
  todayOrderCount: number
  pendingOutboundCount: number
  todayOutboundCount: number
  todayReturnCount: number
  stockAlertCount: number
  orderTrend: { date: string; count: number }[]
  orderStatusDistribution: { status: string; statusName: string; count: number }[]
  topSkus: { skuId: number; skuCode: string; skuName: string; totalQuantity: number }[]
}

export interface StockReport {
  totalSkuCount: number
  totalQuantity: number
  totalLockedQty: number
  totalDefectiveQty: number
  warehouseStocks: { warehouseId: number; warehouseName: string; quantity: number; lockedQty: number; defectiveQty: number }[]
}

export interface OutboundReport {
  todayOutboundCount: number
  monthOutboundCount: number
  pendingPickingCount: number
  outboundTrend: { date: string; count: number }[]
}

// 首页仪表盘
export function getDashboard() {
  return request.get<any, ApiResponse<DashboardData>>('/reports/dashboard')
}

// 库存报表
export function getStockReport() {
  return request.get<any, ApiResponse<StockReport>>('/reports/stock')
}

// 出库报表
export function getOutboundReport() {
  return request.get<any, ApiResponse<OutboundReport>>('/reports/outbound')
}
