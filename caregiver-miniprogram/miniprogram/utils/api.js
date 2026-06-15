// utils/api.js
const { get, post, put, del } = require('./request')

/**
 * 护工认证相关接口
 */
const auth = {
  // 护工登录
  login: (data) => post('/caregiver/login', data, { needAuth: false }),
  
  // 护工注册（仅用户名、密码、手机号）
  register: (data) => post('/caregiver/register', data, { needAuth: false }),
  
  // 修改密码
  changePassword: (data) => put('/caregiver/password', data)
}

/**
 * 护工信息相关接口
 */
const caregiver = {
  // 获取个人信息
  getInfo: () => get('/caregiver/info'),
  
  // 更新个人信息
  updateInfo: (data) => put('/caregiver/info', data),
  
  // 提交入驻申请（补全姓名、头像、性别等基本信息并提交审核材料）
  settle: (data) => post('/caregiver/settle', data),
  
  // 更新位置（经度、纬度）
  updateLocation: (longitude, latitude) => post('/caregiver/location', null, {
    params: { longitude, latitude }
  }),
  
  // 切换工作状态（1接单中 3休息中）
  updateWorkState: (workState) => post('/caregiver/work-state', null, {
    params: { workState }
  }),
  
  // 获取护工详情（公开接口，聚合信息）
  getDetail: (caregiverId) => get(`/caregiver/public/${caregiverId}/detail`, {}, { needAuth: false }),
  
  // 获取护工基础信息（公开接口）
  getBasicInfo: (caregiverId) => get(`/caregiver/public/${caregiverId}`, {}, { needAuth: false }),
  
  // 获取护工技能列表（公开接口）
  getCaregiverSkills: (caregiverId) => get(`/caregiver/public/${caregiverId}/skills`, {}, { needAuth: false }),
  
  // 获取护工服务包列表（公开接口）
  getCaregiverPackages: (caregiverId) => get(`/caregiver/public/${caregiverId}/packages`, {}, { needAuth: false })
}

/**
 * 技能相关接口（公开 + 护工端）
 */
const skill = {
  // ========== 技能字典接口（需要登录） ==========
  
  // 获取所有技能字典（不分页，可选按skillType筛选）
  getList: (skillType) => get('/skill/list', skillType ? { skillType } : {}),
  
  // 分页查询技能（支持按分类、名称检索）
  getPage: (params) => get('/skill/page', params),
  
  // 搜索技能（关键词全文检索）
  search: (keyword) => get('/skill/search', { keyword }),
  
  // 查询所有技能（附带我的申请状态，用于护工端技能列表区分已申请/未申请）
  // 返回: SkillDictWithStatusVO[] — 包含 applied、auditStatus 字段
  getAllWithStatus: (skillType) => get('/caregiver/skill/all', skillType ? { skillType } : {}),
  
  // ========== 护工端接口 ==========
  
  // 获取我的技能列表
  getMySkills: () => get('/caregiver/skill/list'),
  
  // 新增技能（skillId: 技能字典ID, certImage: 可选）
  add: (data) => post('/caregiver/skill', data),
  
  // 删除技能（skillId: 技能字典ID）
  delete: (skillId) => del(`/caregiver/skill/${skillId}`)
}

/**
 * 服务包相关接口（公开 + 护工端）
 */
const servicePackage = {
  // ========== 服务包接口（需要登录） ==========
  
  // 分页查询上架服务包（用户/护工通用）
  getPage: (params) => get('/package/page', params),
  
  // 搜索服务包（关键词检索）
  search: (params) => get('/package/search', params),
  
  // 获取热门搜索关键词
  getHotKeywords: (limit = 10) => get('/package/hot-keywords', { limit }),
  
  // 获取服务包详情
  getDetail: (id) => get(`/package/${id}`),
  
  // 分页查询上架服务包（附带我的开通状态，用于护工端服务包列表区分已开通/未开通）
  // 返回: PageResult<ServicePackageWithStatusVO> — 包含 opened 字段
  getAvailableWithStatus: (params) => get('/caregiver/package/available', params),
  
  // ========== 护工端接口 ==========
  
  // 查询我开通的服务包（基本信息+准入时间）
  getMyPackages: () => get('/caregiver/package/my'),
  
  // 开通服务包（添加服务包准入）
  open: (packageId) => post('/caregiver/package', { packageId }),
  
  // 取消服务包（取消准入）
  cancel: (packageId) => del(`/caregiver/package/${packageId}`)
}

/**
 * 订单相关接口（护工端）
 */
const order = {
  // 护工分页查询自己的订单列表
  // params: { status, category, current, size }
  getMyOrders: (params) => get('/order/caregiver/page', params),
  
  // 护工查询订单详情
  getDetail: (orderId) => get(`/order/caregiver/${orderId}`),
  
  // 护工抢单
  grab: (orderId) => post(`/order/caregiver/grab/${orderId}`),
  
  // 护工上门打卡/开始服务
  // data: { longitude, latitude }（距服务地址500m内可打卡）
  startService: (orderId, data) => post(`/order/caregiver/start/${orderId}`, data),
  
  // 护工结束服务
  finishService: (orderId) => post(`/order/caregiver/finish/${orderId}`)
}

/**
 * 评价相关接口（护工端）
 */
const review = {
  // 查询我的评价列表（护工端）
  // params: { page, size }
  getMyReviews: (params) => get('/review/caregiver/my-list', params),
  
  // 查询护工的评价列表（公开，用于查看其他护工详情）
  // params: { page, size }
  getCaregiverReviews: (caregiverId, params) => get(`/review/user/caregiver/${caregiverId}`, params, { needAuth: false })
}

/**
 * 统计数据相关接口（护工端）
 */
const stats = {
  // 查询我的统计信息（护工端）
  // 返回: orderCount, reviewCount, starCount, goodReviewRate, averageRating, cancelCount, tagStats
  getMyStats: () => get('/stats/caregiver/my'),
  
  // 查询我的标签统计（护工端）
  // 返回: TagCountVO[]
  getMyTagStats: () => get('/stats/caregiver/my/tags'),
  
  // 查询护工统计信息（公开，用于查看其他护工详情）
  getCaregiverStats: (caregiverId) => get(`/stats/user/caregiver/${caregiverId}`, {}, { needAuth: false }),
  
  // 查询护工标签统计（公开，用于查看其他护工详情）
  getCaregiverTagStats: (caregiverId) => get(`/stats/user/caregiver/${caregiverId}/tags`, {}, { needAuth: false })
}

/**
 * 评价标签相关接口（公开）
 */
const tag = {
  // 获取所有评价标签
  getList: () => get('/tag/list', {}, { needAuth: false }),
  
  // 按类型获取评价标签（1 好评 2 差评）
  getListByType: (type) => get(`/tag/list/${type}`, {}, { needAuth: false })
}

/**
 * 文件上传
 */
const file = {
  // 上传图片
  uploadImage: (filePath) => {
    return new Promise((resolve, reject) => {
      const app = getApp()
      wx.uploadFile({
        url: `${app.globalData.baseURL}/file/upload/image`,
        filePath,
        name: 'file',
        header: {
          'Authorization': `Bearer ${app.globalData.token}`
        },
        success: (res) => {
          const data = JSON.parse(res.data)
          if (data.code === 200) {
            resolve(data.data)
          } else {
            wx.showToast({
              title: data.message || '上传失败',
              icon: 'none'
            })
            reject(data)
          }
        },
        fail: (err) => {
          wx.showToast({
            title: '上传失败',
            icon: 'none'
          })
          reject(err)
        }
      })
    })
  }
}

/**
 * 健康检查
 */
const health = {
  // 健康检查
  check: () => get('/health', {}, { needAuth: false })
}

module.exports = {
  auth,
  caregiver,
  skill,
  servicePackage,
  order,
  review,
  stats,
  tag,
  file,
  health
}
