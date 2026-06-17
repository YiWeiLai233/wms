import request from './request'
import type { ApiResponse } from './request'

export interface Permission {
  id: number
  permissionCode: string
  permissionName: string
  parentId: number
  type: number
  sortOrder: number
  children?: Permission[]
}

export interface Role {
  id: number
  roleCode: string
  roleName: string
  description: string
}

export interface RoleForm {
  id?: number
  roleCode: string
  roleName: string
  description: string
  permissionIds: number[]
}

// 获取权限树
export function getPermissionTree() {
  return request.get<any, ApiResponse<Permission[]>>('/permissions/tree')
}

// 获取所有权限（扁平）
export function getAllPermissions() {
  return request.get<any, ApiResponse<Permission[]>>('/permissions')
}

// 获取角色列表
export function getRoleList() {
  return request.get<any, ApiResponse<Role[]>>('/roles')
}

// 获取角色详情
export function getRoleDetail(id: number) {
  return request.get<any, ApiResponse<Role>>(`/roles/${id}`)
}

// 获取角色的权限ID列表
export function getRolePermissionIds(roleId: number) {
  return request.get<any, ApiResponse<number[]>>(`/roles/${roleId}/permissions`)
}

// 创建角色
export function createRole(data: RoleForm) {
  return request.post<any, ApiResponse<number>>('/roles', data)
}

// 更新角色
export function updateRole(data: RoleForm) {
  return request.put<any, ApiResponse<void>>('/roles', data)
}

// 删除角色
export function deleteRole(id: number) {
  return request.delete<any, ApiResponse<void>>(`/roles/${id}`)
}

// 获取当前用户的权限编码列表
export function getUserPermissions() {
  return request.get<any, ApiResponse<string[]>>('/auth/permissions')
}
