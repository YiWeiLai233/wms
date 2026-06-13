import request from './request'
import type { ApiResponse, PageParams, PageResult } from './request'

// ==================== 类型定义 ====================

export interface Warehouse {
  id: number
  code: string
  name: string
  address: string
  contact: string
  phone: string
  status: number
  warehouseType: string
  createdAt: string
}

export interface WarehouseArea {
  id: number
  warehouseId: number
  code: string
  name: string
  type: number
  status: number
  createdAt?: string
}

export interface WarehouseShelf {
  id: number
  warehouseId: number
  areaId: number
  code: string
  name: string
  categoryName: string
  status: number
  createdAt?: string
}

// ==================== 仓库 API ====================

export function getWarehouseList(params: PageParams & { keyword?: string; status?: number }) {
  return request.get<any, ApiResponse<PageResult<Warehouse>>>('/warehouses', { params })
}

export function createWarehouse(data: Partial<Warehouse>) {
  return request.post<any, ApiResponse<number>>('/warehouses', data)
}

export function updateWarehouse(data: Partial<Warehouse>) {
  return request.put<any, ApiResponse<void>>('/warehouses', data)
}

export function deleteWarehouse(id: number) {
  return request.delete<any, ApiResponse<void>>(`/warehouses/${id}`)
}

// ==================== 库区 API ====================

export function getAreaList(warehouseId: number) {
  return request.get<any, ApiResponse<WarehouseArea[]>>(`/warehouse-areas/warehouse/${warehouseId}`)
}

export function createArea(data: Partial<WarehouseArea>) {
  return request.post<any, ApiResponse<number>>('/warehouse-areas', data)
}

export function updateArea(data: Partial<WarehouseArea>) {
  return request.put<any, ApiResponse<void>>('/warehouse-areas', data)
}

export function deleteArea(id: number) {
  return request.delete<any, ApiResponse<void>>(`/warehouse-areas/${id}`)
}

// ==================== 货架 API ====================

export function getShelfList(warehouseId: number) {
  return request.get<any, ApiResponse<WarehouseShelf[]>>(`/warehouse-shelves/warehouse/${warehouseId}`)
}

export function createShelf(data: Partial<WarehouseShelf>) {
  return request.post<any, ApiResponse<number>>('/warehouse-shelves', data)
}

export function updateShelf(data: Partial<WarehouseShelf>) {
  return request.put<any, ApiResponse<void>>('/warehouse-shelves', data)
}

export function deleteShelf(id: number) {
  return request.delete<any, ApiResponse<void>>(`/warehouse-shelves/${id}`)
}
