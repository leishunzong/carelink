// 通用分页参数
export interface PaginationParams {
  currentPage: number
  pageSize: number
  total: number
}

// 通用响应数据
export interface ResponseData<T = any> {
  code: number
  message: string
  data: T
}

// 登录表单
export interface LoginForm {
  username: string
  password: string
}

// 用户信息
export interface UserInfo {
  id: string
  username: string
  name: string
  avatar?: string
  role: string
}

// 护工信息
export interface NurseInfo {
  id: string
  name: string
  gender: string
  age: number
  phone: string
  city: string
  education: string
  workYears: string
  auditStatus: string
  workStatus: string
}

// 订单信息
export interface OrderInfo {
  id: string
  orderNo: string
  userName: string
  nurseName: string
  serviceType: string
  serviceTime: string
  amount: string
  status: string
  createTime: string
}

// 评价信息
export interface ReviewInfo {
  id: string
  orderNo: string
  nurseName: string
  userName: string
  rating: number
  tags: string[]
  content: string
  createTime: string
}
