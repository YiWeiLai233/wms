import request from './request'
import type { ApiResponse, PageParams, PageResult } from './request'

export interface StockAlertTemplate {
  id: number
  name: string
  lowStockThreshold: number
  outOfStockThreshold: number
  enabled: number
  remark?: string
  productCount?: number
  createdAt: string
  updatedAt: string
}

export interface StockAlertTemplateCreateDTO {
  name: string
  lowStockThreshold: number
  outOfStockThreshold: number
  enabled?: number
  remark?: string
}

export interface StockAlertTemplateUpdateDTO {
  name?: string
  lowStockThreshold?: number
  outOfStockThreshold?: number
  enabled?: number
  remark?: string
}

// 分页查询模板列表
export function getStockAlertTemplateList(params?: PageParams & { keyword?: string; enabled?: number }) {
  return request.get<any, ApiResponse<PageResult<StockAlertTemplate>>>('/stock-alert-templates', { params })
}

// 获取模板详情
export function getStockAlertTemplateDetail(id: number) {
  return request.get<any, ApiResponse<StockAlertTemplate>>(`/stock-alert-templates/${id}`)
}

// 获取启用的模板列表（用于下拉选择）
export function getStockAlertTemplateOptions() {
  return request.get<any, ApiResponse<StockAlertTemplate[]>>('/stock-alert-templates/options')
}

// 创建模板
export function createStockAlertTemplate(data: StockAlertTemplateCreateDTO) {
  return request.post<any, ApiResponse<number>>('/stock-alert-templates', data)
}

// 更新模板
export function updateStockAlertTemplate(id: number, data: StockAlertTemplateUpdateDTO) {
  return request.put<any, ApiResponse<void>>(`/stock-alert-templates/${id}`, data)
}

// 删除模板
export function deleteStockAlertTemplate(id: number) {
  return request.delete<any, ApiResponse<void>>(`/stock-alert-templates/${id}`)
}
