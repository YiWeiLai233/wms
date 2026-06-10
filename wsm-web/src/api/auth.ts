import request from './request'
import type { ApiResponse } from './request'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  userId: number
  username: string
  roles: string[]
}

export interface UserProfile {
  id: number
  username: string
  realName: string
  phone: string
  roles: { id: number; roleCode: string; roleName: string }[]
}

export function login(data: LoginParams) {
  return request.post<any, ApiResponse<LoginResult>>('/auth/login', data)
}

export function getProfile() {
  return request.get<any, ApiResponse<UserProfile>>('/auth/profile')
}
