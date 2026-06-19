import request from './request'
import type { ApiResponse, PageParams, PageResult } from './request'

export interface OperationLog {
  id: number
  userId: number
  userName: string
  module: string
  action: string
  targetType: string
  targetId: number
  detail: string
  ip: string
  createdAt: string
}

// 操作日志列表
export function getOperationLogs(params: PageParams & { userId?: number; module?: string; action?: string }) {
  return request.get<any, ApiResponse<PageResult<OperationLog>>>('/operation-logs', { params })
}
