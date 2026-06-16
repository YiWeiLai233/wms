import request from './request'
import type { ApiResponse, PageParams, PageResult } from './request'

export interface ExchangeItem {
  id: number
  exchangeId: number
  skuId: number
  skuCode: string
  skuName: string
  sizeValue?: string
  image?: string
  quantity: number
  unitPrice?: number
  qualityStatus?: string
  itemType: string // RETURN_ITEM / EXCHANGE_ITEM
}

export interface ExchangeOrder {
  id: number
  exchangeNo: string
  orderId: number
  orderNo: string
  platformOrderNo?: string
  warehouseId: number
  warehouseName?: string
  platformName?: string
  status: string
  reason?: string
  returnTrackingNo?: string
  expressCompanyId?: number
  expressCompanyName?: string
  shippingFee?: number
  remark?: string
  items?: ExchangeItem[]
  createdAt: string
}

// 换货单列表
export function getExchangeList(params: PageParams & { exchangeNo?: string; orderNo?: string; platformOrderNo?: string; status?: string; warehouseId?: number }) {
  return request.get<any, ApiResponse<PageResult<ExchangeOrder>>>('/exchange/list', { params })
}

// 换货单详情
export function getExchangeDetail(id: number) {
  return request.get<any, ApiResponse<ExchangeOrder>>(`/exchange/${id}`)
}

// 创建换货单
export function createExchange(data: {
  orderId: number
  warehouseId: number
  reason?: string
  returnTrackingNo?: string
  expressCompanyId?: number
  shippingFee?: number
  remark?: string
  items: { skuId: number; skuCode: string; skuName: string; sizeValue?: string; quantity: number; unitPrice?: number; itemType: string }[]
}) {
  return request.post<any, ApiResponse<number>>('/exchange/create', data)
}

// 换货收货
export function receiveExchange(exchangeId: number) {
  return request.post<any, ApiResponse<void>>(`/exchange/${exchangeId}/receive`)
}

// 换货质检
export function checkExchange(data: { exchangeId: number; items: { itemId: number; qualityStatus: string }[] }) {
  return request.post<any, ApiResponse<void>>('/exchange/check', data)
}

// 换货发货
export function shipExchange(exchangeId: number, data?: { expressCompanyId?: number; trackingNo?: string; shippingFee?: number }) {
  return request.post<any, ApiResponse<void>>(`/exchange/${exchangeId}/ship`, data)
}

// 取消换货单
export function cancelExchange(id: number) {
  return request.post<any, ApiResponse<void>>(`/exchange/${id}/cancel`)
}
