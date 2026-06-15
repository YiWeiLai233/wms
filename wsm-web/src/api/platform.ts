import request from './request'
import type { ApiResponse, PageParams, PageResult } from './request'

export interface Platform {
  id: number
  name: string
  color?: string
  enabled: number
  remark?: string
  createdAt: string
  updatedAt: string
}

export interface PlatformCreateDTO {
  name: string
  color?: string
  enabled?: number
  remark?: string
}

export interface PlatformUpdateDTO {
  name: string
  color?: string
  enabled?: number
  remark?: string
}

// 分页查询平台列表
export function getPlatformList(params?: PageParams & { keyword?: string; enabled?: number }) {
  return request.get<any, ApiResponse<PageResult<Platform>>>('/platforms', { params })
}

// 获取平台详情
export function getPlatformDetail(id: number) {
  return request.get<any, ApiResponse<Platform>>(`/platforms/${id}`)
}

// 获取启用的平台列表（用于下拉选择）
export function getPlatformOptions() {
  return request.get<any, ApiResponse<Platform[]>>('/platforms/options')
}

// 创建平台
export function createPlatform(data: PlatformCreateDTO) {
  return request.post<any, ApiResponse<number>>('/platforms', data)
}

// 更新平台
export function updatePlatform(id: number, data: PlatformUpdateDTO) {
  return request.put<any, ApiResponse<void>>(`/platforms/${id}`, data)
}

// 删除平台
export function deletePlatform(id: number) {
  return request.delete<any, ApiResponse<void>>(`/platforms/${id}`)
}
