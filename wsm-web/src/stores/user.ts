import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, getProfile } from '@/api/auth'
import type { LoginParams, UserProfile } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<UserProfile | null>(null)
  const roles = ref<string[]>([])

  async function login(params: LoginParams) {
    const res = await loginApi(params)
    token.value = res.data.token
    roles.value = res.data.roles
    localStorage.setItem('token', res.data.token)
    return res.data
  }

  async function fetchProfile() {
    const res = await getProfile()
    userInfo.value = res.data
    roles.value = res.data.roles.map((r) => r.roleCode)
    return res.data
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    roles.value = []
    localStorage.removeItem('token')
  }

  return { token, userInfo, roles, login, fetchProfile, logout }
})
