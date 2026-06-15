import request from './request'
import type { ApiResponse, PageParams, PageResult } from './request'

export interface ReturnItem {
  id: number
  returnId: number
  skuId: number
  skuCode: string
  skuName: string
  sizeValue?: string
  quantity: number
  qualityStatus?: string
}

export interface ReturnOrder {
  id: number
  returnNo: string
  orderId: number
  orderNo: string
  platformOrderNo?: string
  warehouseId: number
  warehouseName: string
  status: string
  reason: string
  trackingNo?: string
  remark: string
  items?: ReturnItem[]
  createdAt: string
}

// 退货单列表
export function getReturnList(params: PageParams & { returnNo?: string; orderNo?: string; platformOrderNo?: string; status?: string; warehouseId?: number }) {
  return request.get<any, ApiResponse<PageResult<ReturnOrder>>>('/returns/list', { params })
}

// 退货单详情
export function getReturnDetail(id: number) {
  return request.get<any, ApiResponse<ReturnOrder>>(`/returns/${id}`)
}

// 创建退货单
export function createReturn(data: { orderId: number; reason: string; trackingNo?: string; remark?: string; items: { skuId: number; quantity: number }[] }) {
  return request.post<any, ApiResponse<number>>('/returns/create', data)
}

// 批量创建退货单
export function createBatchReturn(data: { orderIds: number[]; reason: string; trackingNo?: string; remark?: string }) {
  return request.post<any, ApiResponse<number[]>>('/returns/create-batch', data)
}

// 退货质检
export function checkReturn(data: { returnId: number; items: { itemId: number; qualityStatus: string }[] }) {
  return request.post<any, ApiResponse<void>>('/returns/check', data)
}

// 确认退货入库
export function confirmReturn(returnId: number, items?: { itemId: number; qualityStatus: string }[]) {
  return request.post<any, ApiResponse<void>>('/returns/confirm', { returnId, items })
}

// 取消退货单
export function cancelReturn(id: number) {
  return request.post<any, ApiResponse<void>>(`/returns/${id}/cancel`)
}

// 按订单ID取消退货单
export function cancelReturnByOrderId(orderId: number) {
  return request.post<any, ApiResponse<void>>(`/returns/cancel-by-order/${orderId}`)
}
