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

export interface BackupConfig {
  id?: number
  autoBackupEnabled: boolean
  autoBackupType: string
  autoBackupTime: string
  backupPath: string
  remoteBackupEnabled: boolean
  remoteHost: string
  remotePort: number
  remoteUsername: string
  remotePassword: string
  remotePath: string
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

// 获取备份配置
export function getBackupConfig() {
  return request.get<any, ApiResponse<BackupConfig>>('/backup/config')
}

// 保存备份配置
export function saveBackupConfig(data: BackupConfig) {
  return request.post<any, ApiResponse<void>>('/backup/config', data)
}

// 测试远程连接
export function testRemoteConnection(data: BackupConfig) {
  return request.post<any, ApiResponse<boolean>>('/backup/config/test-remote', data)
}
