import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, getProfile } from '@/api/auth'
import type { LoginParams, UserProfile } from '@/api/auth'
import { getUserPermissions } from '@/api/permission'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<UserProfile | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])

  async function login(params: LoginParams) {
    const res = await loginApi(params)
    token.value = res.data.token
    roles.value = res.data.roles
    localStorage.setItem('token', res.data.token)
    // 登录后获取权限
    await fetchPermissions()
    return res.data
  }

  async function fetchProfile() {
    const res = await getProfile()
    userInfo.value = res.data
    roles.value = res.data.roles.map((r) => r.roleCode)
    // 获取权限
    await fetchPermissions()
    return res.data
  }

  async function fetchPermissions() {
    try {
      const res = await getUserPermissions()
      permissions.value = res.data || []
    } catch {
      permissions.value = []
    }
  }

  function hasPermission(code: string): boolean {
    // 超级管理员拥有所有权限
    if (roles.value.includes('SUPER_ADMIN')) return true
    return permissions.value.includes(code)
  }

  function hasAnyPermission(codes: string[]): boolean {
    if (roles.value.includes('SUPER_ADMIN')) return true
    return codes.some((code) => permissions.value.includes(code))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    roles.value = []
    permissions.value = []
    localStorage.removeItem('token')
  }

  return {
    token, userInfo, roles, permissions,
    login, fetchProfile, fetchPermissions,
    hasPermission, hasAnyPermission, logout
  }
})
