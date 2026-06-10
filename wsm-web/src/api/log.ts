import request from './request'
import type { ApiResponse, PageParams, PageResult } from './request'

export interface OperationLog {
  id: number
  userId: number
  username: string
  operation: string
  method: string
  params: string
  ip: string
  status: number
  errorMsg: string
  duration: number
  createdAt: string
}

// 操作日志列表
export function getOperationLogs(params: PageParams & { userId?: number; operation?: string; status?: number }) {
  return request.get<any, ApiResponse<PageResult<OperationLog>>>('/operation-logs', { params })
}
