import request from './request'
import type { ApiResponse, PageResult } from './request'

export interface User {
  id: number
  username: string
  realName: string
  phone: string
  email: string
  status: number
  roles: string[]
  roleIds?: number[]
  createdAt: string
}

export interface Role {
  id: number
  roleCode: string
  roleName: string
  description: string
}

export interface UserForm {
  id?: number
  username?: string
  password?: string
  realName?: string
  phone?: string
  email?: string
  status?: number
  roleIds?: number[]
}

// 用户列表
export function getUserList(params: { keyword?: string; pageNum?: number; pageSize?: number }) {
  return request.get<any, ApiResponse<PageResult<User>>>('/users', { params })
}

// 用户详情
export function getUserDetail(id: number) {
  return request.get<any, ApiResponse<User>>(`/users/${id}`)
}

// 新增用户
export function createUser(data: UserForm) {
  return request.post<any, ApiResponse<number>>('/users', data)
}

// 修改用户
export function updateUser(data: UserForm) {
  return request.put<any, ApiResponse<void>>('/users', data)
}

// 删除用户
export function deleteUser(id: number) {
  return request.delete<any, ApiResponse<void>>(`/users/${id}`)
}

// 重置密码
export function resetPassword(id: number, newPassword: string) {
  return request.put<any, ApiResponse<void>>(`/users/${id}/reset-password`, null, {
    params: { newPassword },
  })
}

// 角色列表
export function getRoleList() {
  return request.get<any, ApiResponse<Role[]>>('/roles')
}
