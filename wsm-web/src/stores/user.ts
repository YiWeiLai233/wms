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
    // 清除旧状态，确保干净登录
    token.value = ''
    roles.value = []
    permissions.value = []
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localTabUserId = null

    const res = await loginApi(params)
    token.value = res.data.token
    roles.value = res.data.roles
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('userId', String(res.data.userId))
    localTabUserId = String(res.data.userId)
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
    localStorage.removeItem('userId')
    localTabUserId = null
  }

  return {
    token, userInfo, roles, permissions,
    login, fetchProfile, fetchPermissions,
    hasPermission, hasAnyPermission, logout
  }
})

// 跨标签页单账号：记录本标签页的 userId + 监听 storage 变化
let localTabUserId: string | null = localStorage.getItem('userId')

export function updateLocalTabUserId(id: string | null) {
  localTabUserId = id
}

if (typeof window !== 'undefined') {
  window.addEventListener('storage', (e) => {
    if (e.key !== 'token' && e.key !== 'userId') return

    // 当前标签页没登录，跳过
    if (!localTabUserId) return

    // userId 被清除或变了（另一个标签页退出/切换账号）
    if (e.key === 'userId' && e.newValue !== localTabUserId) {
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localTabUserId = null
      window.location.href = '/login'
      return
    }

    // token 被清除
    if (e.key === 'token' && e.newValue === null) {
      localStorage.removeItem('userId')
      localTabUserId = null
      window.location.href = '/login'
      return
    }
  })

  // 每次页面可见时同步本地记录（防止本标签页登录后未更新）
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
      localTabUserId = localStorage.getItem('userId')
    }
  })
}
