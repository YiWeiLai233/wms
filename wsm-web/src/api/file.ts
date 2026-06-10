import request from './request'
import type { ApiResponse } from './request'

export interface FileInfo {
  id: number
  fileName: string
  filePath: string
  fileSize: number
  fileType: string
  url: string
  createdAt: string
}

// 上传文件
export function uploadFile(file: File, bizType?: string, bizId?: number) {
  const formData = new FormData()
  formData.append('file', file)
  if (bizType) formData.append('bizType', bizType)
  if (bizId) formData.append('bizId', String(bizId))
  return request.post<any, ApiResponse<FileInfo>>('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

// 获取文件信息
export function getFileInfo(id: number) {
  return request.get<any, ApiResponse<FileInfo>>(`/files/${id}`)
}

// 删除文件
export function deleteFile(id: number) {
  return request.delete<any, ApiResponse<void>>(`/files/${id}`)
}
