import request from '@/utils/request'

// 登录请求参数
export interface LoginForm {
  username: string
  password: string
}

// 登录响应
export interface LoginResponse {
  token: string
  userType: 'admin' | 'user' | 'caregiver'
}

// 管理员登录
export const adminLogin = (data: LoginForm) => {
  return request<LoginResponse>({
    url: '/admin/login',
    method: 'post',
    data
  })
}

// 登出（前端清除token即可）
export const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  return Promise.resolve()
}
