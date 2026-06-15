import request from './request'
import type { ApiResponse } from './request'

// 上传图片
export function uploadImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<any, ApiResponse<string>>('/images/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 删除图片
export function deleteImage(url: string) {
  return request.delete<any, ApiResponse<void>>('/images/delete', { params: { url } })
}
