import request from './request'
import type { ApiResponse } from './request'

export interface DashboardData {
  todayOrderCount: number
  pendingOutboundCount: number
  todayOutboundCount: number
  todayReturnCount: number
  stockAlertCount: number
  platformTrends?: { platformId: number; platformName: string; platformColor: string; data: { date: string; count: number }[] }[]
  orderTrend: { date: string; count: number }[]
  orderStatusDistribution: { status: string; statusName: string; count: number }[]
  topSkus: { skuId: number; skuCode: string; skuName: string; totalQuantity: number; platformQuantities?: { platformId: number; platformName: string; platformColor: string; quantity: number }[] }[]
  platformSkuSales?: { platformId: number; platformName: string; platformColor: string; skuSales: { date: string; skuName: string; quantity: number }[] }[]
}

export interface StockReport {
  totalSkuCount: number
  totalQuantity: number
  totalLockedQty: number
  warehouseStocks: { warehouseId: number; warehouseName: string; quantity: number; lockedQty: number }[]
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

// 快递费用统计
export function getExpressFeeReport(params: { orderNo?: string; platformOrderNo?: string; startTime?: string; endTime?: string; expressCompanyId?: number }) {
  return request.get<any, ApiResponse<ExpressFeeReport>>('/reports/express-fee', { params })
}

export interface ExpressFeeReport {
  totalFee: number
  totalCount: number
  outboundFee: number
  outboundCount: number
  returnFee: number
  returnCount: number
  exchangeFee: number
  exchangeCount: number
  items: ExpressFeeItem[]
}

export interface ExpressFeeItem {
  id: number
  bizNo: string
  orderNo: string
  platformOrderNo: string
  bizType: string
  bizTypeName: string
  expressCompanyId: number
  expressCompanyName: string
  trackingNo: string
  shippingFee: number
  createdAt: string
}
