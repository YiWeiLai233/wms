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
export function incrementalBackup(backupPath?: string) {
  return request.post<any, ApiResponse<BackupRecord>>('/backup/incremental', backupPath ? { backupPath } : {})
}

// 备份记录列表
export function getBackupList() {
  return request.get<any, ApiResponse<BackupRecord[]>>('/backup/list')
}

// 下载备份文件
export function downloadBackupUrl(fileName: string) {
  return `/api/backup/download/${encodeURIComponent(fileName)}`
}

// 删除备份记录
export function deleteBackup(id: number) {
  return request.delete<any, ApiResponse<void>>(`/backup/${id}`)
}
