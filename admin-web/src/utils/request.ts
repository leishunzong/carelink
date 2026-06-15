import axios from 'axios'
import type { AxiosInstance, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: '/api', // 所有请求会自动加上 /api 前缀
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 在发送请求之前做些什么
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    // 对请求错误做些什么
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    
    // 如果响应没有 code 字段，直接返回数据（可能是某些特殊接口）
    if (res.code === undefined) {
      return res
    }

    // 根据返回的 code 判断（200 或 0 都视为成功）
    if (res.code !== 200 && res.code !== 0) {
      // 401: 未登录或 token 过期
      if (res.code === 401 || res.code === 403) {
        ElMessage.error(res.message || '登录已过期，请重新登录')
        localStorage.removeItem('token')
        localStorage.removeItem('userType')
        router.push('/login')
        return Promise.reject(new Error(res.message || '登录已过期'))
      }
      
      // 其他业务错误
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    
    // 成功时只返回 data 字段
    return res.data
  },
  (error) => {
    console.error('响应错误:', error)
    
    // 处理HTTP状态码错误
    if (error.response) {
      switch (error.response.status) {
        case 401:
          // 401 表示未认证，需要重新登录
          ElMessage.error('登录已过期，请重新登录')
          localStorage.removeItem('token')
          localStorage.removeItem('userType')
          router.push('/login')
          break
        case 403:
          // 403 表示无权访问该资源，不应该跳转登录页
          ElMessage.error(error.response.data?.message || '您没有权限访问该资源')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器错误')
          break
        default:
          ElMessage.error(error.response.data?.message || error.message || '网络错误')
      }
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    
    return Promise.reject(error)
  }
)

export default service as Omit<AxiosInstance, 'request' | 'get' | 'delete' | 'head' | 'options' | 'post' | 'put' | 'patch'> & {
  request<T = unknown>(config: Parameters<AxiosInstance['request']>[0]): Promise<T>
  get<T = unknown>(url: string, config?: Parameters<AxiosInstance['get']>[1]): Promise<T>
  delete<T = unknown>(url: string, config?: Parameters<AxiosInstance['delete']>[1]): Promise<T>
  head<T = unknown>(url: string, config?: Parameters<AxiosInstance['head']>[1]): Promise<T>
  options<T = unknown>(url: string, config?: Parameters<AxiosInstance['options']>[1]): Promise<T>
  post<T = unknown>(url: string, data?: unknown, config?: Parameters<AxiosInstance['post']>[2]): Promise<T>
  put<T = unknown>(url: string, data?: unknown, config?: Parameters<AxiosInstance['put']>[2]): Promise<T>
  patch<T = unknown>(url: string, data?: unknown, config?: Parameters<AxiosInstance['patch']>[2]): Promise<T>
  <T = unknown>(config: Parameters<AxiosInstance['request']>[0]): Promise<T>
  <T = unknown>(url: string, config?: Parameters<AxiosInstance['request']>[0]): Promise<T>
}
