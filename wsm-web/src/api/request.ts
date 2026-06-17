import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 后端统一响应格式
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 分页响应格式
export interface PageResult<T = any> {
  total: number
  list: T[]
  pageNum: number
  pageSize: number
}

// 分页请求参数
export interface PageParams {
  page?: number
  size?: number
}

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// 解析 JWT payload（不验证签名，仅读取 userId）
function parseJwtPayload(token: string): any {
  try {
    const payload = token.split('.')[1]
    return JSON.parse(atob(payload))
  } catch {
    return null
  }
}

// 请求拦截器：注入 JWT Token + 单账号校验
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token) {
      // 单浏览器单账号校验：token 中的 userId 必须与 localStorage 一致
      const payload = parseJwtPayload(token)
      const storedUserId = localStorage.getItem('userId')
      if (payload && storedUserId && String(payload.userId) !== storedUserId) {
        // token 不属于当前用户，清除并跳转登录
        localStorage.removeItem('token')
        localStorage.removeItem('userId')
        router.push('/login')
        return Promise.reject(new Error('账号已切换，请重新登录'))
      }
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理响应
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data
    if (res.code === 200) {
      return res as any
    }
    // 401 未授权 → 跳转登录
    if (res.code === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
      return Promise.reject(new Error(res.message))
    }
    // 403 无权限
    if (res.code === 403) {
      ElMessage.error(res.message || '无权限访问')
      return Promise.reject(new Error(res.message))
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message))
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default service
