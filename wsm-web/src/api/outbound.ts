import request from './request'
import type { ApiResponse, PageParams, PageResult } from './request'

export interface OutboundItem {
  id: number
  outboundId: number
  skuId: number
  skuCode: string
  skuName: string
  sizeValue?: string
  quantity: number
  pickedQty: number
  shelfId: number | null
  shelfCode: string | null
  scanned: number
}

export interface OutboundOrder {
  id: number
  outboundNo: string
  orderId: number
  orderNo: string
  platformOrderNo?: string
  warehouseId: number
  warehouseName: string
  status: string
  pickerId: number | null
  pickerName: string | null
  remark: string
  trackingNo: string | null
  expressCompanyId: number | null
  expressCompanyName: string | null
  shippingFee: number | null
  items?: OutboundItem[]
  createdAt: string
}

// 出库单列表
export function getOutboundList(params: PageParams & { outboundNo?: string; orderNo?: string; platformOrderNo?: string; status?: string; warehouseId?: number }) {
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
export function scanOutbound(data: { outboundId: number; scanCode: string; shelfId: number }) {
  return request.post<any, ApiResponse<void>>('/outbound/scan', data)
}

// 确认出库
export function confirmOutbound(data: {
  outboundId: number
  trackingNo?: string
  expressCompanyId?: number
  feeTemplateId?: number
  estimatedWeight?: number
  shippingFee?: number
}) {
  return request.post<any, ApiResponse<void>>('/outbound/confirm', data)
}
