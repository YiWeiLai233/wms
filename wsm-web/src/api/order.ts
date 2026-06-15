import request from './request'
import type { ApiResponse, PageParams, PageResult } from './request'

export interface OrderItem {
  id: number
  orderId: number
  skuId: number
  skuCode: string
  skuName: string
  sizeValue?: string
  quantity: number
  unitPrice: number
  totalPrice: number
}

export interface Order {
  id: number
  orderNo: string
  platformOrderNo: string
  platformId?: number
  platformName?: string
  platformColor?: string
  warehouseId: number
  warehouseName: string
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  orderStatus: string
  totalAmount: number
  remark: string
  items?: OrderItem[]
  createdAt: string
}

export interface OrderImportData {
  platformOrderNo?: string
  platformId?: number
  warehouseId?: number
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  remark?: string
  items: { skuId?: number; skuCode: string; skuName: string; sizeValue?: string; quantity: number; unitPrice: number }[]
}

// 订单列表
export function getOrderList(params: PageParams & { orderNo?: string; platformOrderNo?: string; receiverName?: string; receiverPhone?: string; orderStatus?: string; warehouseId?: number }) {
  return request.get<any, ApiResponse<PageResult<Order>>>('/orders', { params })
}

// 订单详情
export function getOrderDetail(id: number) {
  return request.get<any, ApiResponse<Order>>(`/orders/${id}`)
}

// 导入订单
export function importOrder(data: OrderImportData) {
  return request.post<any, ApiResponse<number>>('/orders/import', data)
}

// 更新订单状态
export function updateOrderStatus(id: number, targetStatus: string) {
  return request.put<any, ApiResponse<void>>(`/orders/${id}/status`, { targetStatus })
}

// 订单搜索
export function searchOrders(params: PageParams & { keyword?: string; orderStatus?: string; warehouseId?: number; startTime?: string; endTime?: string }) {
  return request.get<any, ApiResponse<PageResult<Order>>>('/orders/search', { params })
}
