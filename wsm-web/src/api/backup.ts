import axios from 'axios'
import request from './request'
import type { ApiResponse } from './request'

export interface BackupRecord {
  id: number
  backupType: string
  fileName: string
  filePath: string
  fileSize: number
  status: string
  remark: string
  createdAt: string
}

// 全量备份
export function fullBackup(backupPath?: string) {
  return request.post<any, ApiResponse<BackupRecord>>('/backup/full', backupPath ? { backupPath } : {})
}

// 增量备份
export function incrementalBackup(baseBackupId?: number, backupPath?: string) {
  const body: any = {}
  if (baseBackupId) body.baseBackupId = baseBackupId
  if (backupPath) body.backupPath = backupPath
  return request.post<any, ApiResponse<BackupRecord>>('/backup/incremental', body)
}

// 备份记录列表
export function getBackupList() {
  return request.get<any, ApiResponse<BackupRecord[]>>('/backup/list')
}

// 下载备份文件（直接用 axios 带 token，绕过响应拦截器）
export async function downloadBackup(fileName: string) {
  const token = localStorage.getItem('token')
  const response = await axios.get(`/api/backup/download/${encodeURIComponent(fileName)}`, {
    responseType: 'blob',
    headers: {
      Authorization: token ? `Bearer ${token}` : ''
    }
  })
  return response.data
}

// 删除备份记录
export function deleteBackup(id: number) {
  return request.delete<any, ApiResponse<void>>(`/backup/${id}`)
}
