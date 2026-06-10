import request from './request'
import type { ApiResponse, PageParams, PageResult } from './request'

export interface OutboundItem {
  id: number
  outboundId: number
  skuId: number
  skuCode: string
  skuName: string
  quantity: number
  pickedQty: number
  locationId: number | null
  locationCode: string | null
  scanned: number
}

export interface OutboundOrder {
  id: number
  outboundNo: string
  orderId: number
  orderNo: string
  warehouseId: number
  warehouseName: string
  status: string
  pickerId: number | null
  pickerName: string | null
  remark: string
  items?: OutboundItem[]
  createdAt: string
}

// 出库单列表
export function getOutboundList(params: PageParams & { outboundNo?: string; orderNo?: string; status?: string; warehouseId?: number }) {
  return request.get<any, ApiResponse<PageResult<OutboundOrder>>>('/outbound/list', { params })
}

// 出库单详情
export function getOutboundDetail(id: number) {
  return request.get<any, ApiResponse<OutboundOrder>>(`/outbound/${id}`)
}

// 创建出库单
export function createOutbound(data: { orderId: number; remark?: string }) {
  return request.post<any, ApiResponse<number>>('/outbound/create', data)
}

// 扫码核对
export function scanOutbound(data: { outboundId: number; scanCode: string; locationId: number }) {
  return request.post<any, ApiResponse<void>>('/outbound/scan', data)
}

// 确认出库
export function confirmOutbound(outboundId: number) {
  return request.post<any, ApiResponse<void>>('/outbound/confirm', { outboundId })
}
