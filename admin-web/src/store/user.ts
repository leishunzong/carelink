import { defineStore } from 'pinia'
import { ref } from 'vue'
import { adminLogin, logout as logoutApi, type LoginForm } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<any>(null)
  const token = ref<string>(localStorage.getItem('token') || '')

  // 登录
  const login = async (loginForm: LoginForm) => {
    try {
      const data = await adminLogin(loginForm)
      
      if (!data || !data.token) {
        throw new Error('登录响应数据格式错误')
      }
      
      token.value = data.token
      localStorage.setItem('token', data.token)
      localStorage.setItem('userType', data.userType)
      return data
    } catch (error) {
      return Promise.reject(error)
    }
  }

  // 登出
  const logout = async () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userType')
    await logoutApi()
  }

  // 设置 token
  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  // 设置用户信息
  const setUserInfo = (info: any) => {
    userInfo.value = info
  }

  // 清除用户信息
  const clearUserInfo = () => {
    userInfo.value = null
    token.value = ''
    localStorage.removeItem('token')
  }

  return {
    userInfo,
    token,
    login,
    logout,
    setToken,
    setUserInfo,
    clearUserInfo
  }
})
