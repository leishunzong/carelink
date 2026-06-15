import request from '@/utils/request'

// ============= 入驻审核相关 =============

// 审核材料项
export interface VerifyMaterial {
  materialType: 1 | 2 | 3 | 4  // 1-身份证正面 2-身份证反面 3-护工资格证 4-其他证明材料
  materialTypeName: string
  fileUrl: string
  sortOrder: number
}

// 入驻申请列表项
export interface SettleListItem {
  id: number  // 护工ID（后端返回的就是护工ID）
  realName: string
  phone: string
  gender: number
  avatar: string
  birthday: string
  nativePlace: string
  education: string
  ethnicity: string
  zodiac: string
  workYears: number
  cityCode: string
  cityName: string
  residentAddress: string
  longitude: number
  latitude: number
  verifyMaterials: VerifyMaterial[]  // 审核材料列表
  verifyStatus: 0 | 1 | 2  // 0待审 1通过 2拒绝
  rejectReason?: string
  createTime: string
  updateTime: string
}

// 获取入驻审核列表
export const getSettleList = (params: {
  realName?: string
  phone?: string
  current: number
  size: number
}) => {
  return request<{
    records: SettleListItem[]
    total: number
    size: number
    current: number
    pages: number
  }>({
    url: '/admin/caregiver/settle-list',
    method: 'get',
    params
  })
}

// 入驻审核
export const auditSettle = (data: {
  caregiverId: number
  passed: boolean
  rejectReason?: string
}) => {
  return request({
    url: '/admin/caregiver/settle/audit',
    method: 'post',
    data
  })
}

// ============= 技能审核相关 =============

// 技能申请列表项
export interface SkillApplyListItem {
  id: number
  caregiverId: number
  caregiverName: string
  caregiverPhone: string
  skillId: number
  skillName: string
  skillType: number
  certImage?: string
  auditStatus: 0 | 1 | 2  // 0-待审核 1-通过 2-拒绝
  rejectReason?: string
  createTime: string
  updateTime: string
}

// 获取技能审核列表
export const getSkillApplyList = (params: {
  caregiverName?: string
  caregiverPhone?: string
  skillName?: string
  current: number
  size: number
}) => {
  return request<{
    records: SkillApplyListItem[]
    total: number
    size: number
    current: number
    pages: number
  }>({
    url: '/admin/caregiver/skill-apply-list',
    method: 'get',
    params
  })
}

// 技能申请审核
export const auditSkillApply = (data: {
  caregiverSkillId: number
  passed: boolean
  rejectReason?: string
}) => {
  return request({
    url: '/admin/caregiver/skill-apply/audit',
    method: 'post',
    data
  })
}

// ============= 护工管理相关 =============

// 护工信息
export interface CaregiverInfo {
  id: number
  username: string
  phone: string
  realName: string
  avatar: string
  gender: number
  birthday: string
  nativePlace: string
  ethnicity: string
  zodiac: string
  education: string
  workYears: number
  verifyStatus: 0 | 1 | 2
  workState: 1 | 2 | 3  // 1接单中 2服务中 3休息中
  cityCode: string
  cityName: string
  residentAddress: string
  longitude: number
  latitude: number
  createTime: string
}

// 获取护工列表
export const getCaregiverList = (params: {
  realName?: string
  phone?: string
  gender?: number
  minAge?: number
  maxAge?: number
  education?: string
  workYears?: number
  cityName?: string
  workState?: number
  current: number
  size: number
}) => {
  return request<{
    records: CaregiverInfo[]
    total: number
    size: number
    current: number
    pages: number
  }>({
    url: '/admin/caregiver/page',
    method: 'get',
    params
  })
}

// ============= 订单管理相关 =============

// 订单列表项
export interface OrderListItem {
  id: number
  orderNo: string
  orderType: 1 | 2  // 1系统匹配 2定向预约
  status: 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8
  packageId: number
  packageName: string
  nickname?: string
  caregiverName?: string
  detailAddress: string
  billingMethod: 1 | 2 | 3 | 4  // 1按月 2按天 3按小时 4按次
  buyQuantity: number
  unitPrice: number
  totalAmount: number
  expectStartTime: string
  realStartTime?: string
  finishTime?: string
  createTime: string
}

// 获取订单列表
export const getOrderList = (params: {
  orderNo?: string
  orderType?: number
  status?: number
  caregiverName?: string
  contactName?: string
  cityName?: string
  current: number
  size: number
}) => {
  return request<{
    records: OrderListItem[]
    total: number
    size: number
    current: number
    pages: number
  }>({
    url: '/admin/order/page',
    method: 'get',
    params
  })
}

// ============= 评价管理相关 =============

// 评价列表项
export interface ReviewListItem {
  id: number
  orderNo: string
  caregiverId: number
  caregiverName: string
  serviceDate: string
  nickname: string
  avatar: string
  content: string
  type: 1 | 2  // 1好评 2差评
  stars: number
  isAnonymous: 0 | 1
  tags: string[]
  createTime: string
}

// 获取评价列表
export const getReviewList = (params: {
  nickname?: string
  caregiverName?: string
  orderNo?: string
  current: number
  size: number
}) => {
  return request<{
    records: ReviewListItem[]
    total: number
    size: number
    current: number
    pages: number
  }>({
    url: '/admin/review/page',
    method: 'get',
    params
  })
}

// ============= 统计数据相关 =============

// 运营数据统计
export interface OperationStats {
  caregiverTotal: number
  orderTotal: number
  pendingSettleTotal: number
  pendingSkillTotal: number
  userTotal: number
  reviewTotal: number
  servicePackageTotal: number
  skillTotal: number
  reviewTagTotal: number
  ragDocumentTotal: number
  todayRevenue: number
  totalRevenue: number
}

// 获取运营统计数据
export const getOperationStats = () => {
  return request<OperationStats>({
    url: '/admin/stats',
    method: 'get'
  })
}

// ============= 标签管理相关 =============

// 评价标签
export interface ReviewTag {
  id: number
  name: string
  type: 1 | 2  // 1好评标签 2差评标签
  sort: number
  createTime: string
  updateTime: string
}

// 获取标签列表（不分页）
export const getTagList = (type?: 1 | 2) => {
  return request<ReviewTag[]>({
    url: type ? `/tag/list/${type}` : '/tag/list',
    method: 'get'
  })
}

// 新增标签
export const addTag = (data: {
  name: string
  type: 1 | 2
  sort?: number
}) => {
  return request({
    url: '/admin/tag',
    method: 'post',
    data
  })
}

// 修改标签
export const updateTag = (id: number, data: {
  name: string
  type: 1 | 2
  sort?: number
}) => {
  return request({
    url: `/admin/tag/${id}`,
    method: 'put',
    data
  })
}

// ============= 技能管理相关 =============

// 技能字典
export interface SkillDict {
  id: number
  skillName: string
  skillType: 1 | 2 | 3 | 4 | 5 | 6  // 见枚举
  description: string
  needAudit: 0 | 1
  createTime: string
  updateTime: string
}

// 获取技能列表（分页）
export const getSkillPage = (params: {
  skillType?: number
  skillName?: string
  current: number
  size: number
}) => {
  return request<{
    records: SkillDict[]
    total: number
    size: number
    current: number
    pages: number
  }>({
    url: '/skill/page',
    method: 'get',
    params
  })
}

// 新增技能
export const addSkill = (data: {
  skillName: string
  skillType: number
  description: string
  needAudit?: number
}) => {
  return request({
    url: '/admin/skill',
    method: 'post',
    data
  })
}

// 修改技能
export const updateSkill = (id: number, data: {
  skillName: string
  skillType: number
  description: string
  needAudit?: number
}) => {
  return request({
    url: `/admin/skill/${id}`,
    method: 'put',
    data
  })
}

// 删除技能
export const deleteSkill = (id: number) => {
  return request({
    url: `/admin/skill/${id}`,
    method: 'delete'
  })
}

// ============= 服务包管理相关 =============

// 服务包
export interface ServicePackage {
  id: number
  name: string
  category: 1 | 2 | 3 | 4 | 5 | 6
  coverImage: string
  description: string
  detail: string
  sales: number
  allowMonth: 0 | 1
  allowDay: 0 | 1
  allowHour: 0 | 1
  allowTimes: 0 | 1
  priceMonth?: number
  priceDay?: number
  priceHour?: number
  priceTimes?: number
  mandatorySkillIds?: number[]
  status: 0 | 1  // 0下架 1上架
  createTime: string
  updateTime: string
}

// 获取服务包列表（分页）
export const getPackagePage = (params: {
  category?: number
  status?: number
  current: number
  size: number
}) => {
  return request<{
    records: ServicePackage[]
    total: number
    size: number
    current: number
    pages: number
  }>({
    url: '/admin/package/page',
    method: 'get',
    params
  })
}

// 新增服务包
export const addPackage = (data: Partial<ServicePackage>) => {
  return request({
    url: '/admin/package',
    method: 'post',
    data
  })
}

// 修改服务包
export const updatePackage = (id: number, data: Partial<ServicePackage>) => {
  return request({
    url: `/admin/package/${id}`,
    method: 'put',
    data
  })
}

// 删除服务包
export const deletePackage = (id: number) => {
  return request({
    url: `/admin/package/${id}`,
    method: 'delete'
  })
}

// 上架服务包
export const onShelfPackage = (id: number) => {
  return request({
    url: `/admin/package/${id}/on-shelf`,
    method: 'put'
  })
}

// 下架服务包
export const offShelfPackage = (id: number) => {
  return request({
    url: `/admin/package/${id}/off-shelf`,
    method: 'put'
  })
}

// ============= RAG 知识库相关 =============

// 知识库文档
export interface RagDocument {
  id: number
  title: string
  content?: string
  fileName?: string
  fileSize?: number
  createTime: string
}

// 获取文档列表
export const getRagDocuments = () => {
  return request<RagDocument[]>({
    url: '/admin/rag/documents',
    method: 'get'
  })
}

// 上传文档
export const uploadRagDocument = (formData: FormData) => {
  return request({
    url: '/admin/rag/document',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 直接提交文本
export const addRagDocumentText = (params: {
  title: string
  content: string
}) => {
  return request({
    url: '/admin/rag/document/text',
    method: 'post',
    params
  })
}

// 删除文档
export const deleteRagDocument = (id: number) => {
  return request({
    url: `/admin/rag/document/${id}`,
    method: 'delete'
  })
}

// ============= 文件上传相关 =============

// 上传图片（需要登录）
export const uploadImage = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request<{ url: string }>({
    url: '/file/upload/image',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
