/**
 * 用户端 API 封装，与 backend/care-link 联调
 * 文档见 backend/care-link/docs/API接口文档.md
 * baseUrl 在 app.js globalData 中配置（已含 /api）
 */
var request = require('./request.js')

var USER_PROFILE_KEY = 'carelink_user_profile'

// 城市名 -> 6 位 adcode（与后端城市编码说明一致）
var cityNameToCode = {
  '北京': '110100',
  '北京市': '110100',
  '上海': '310100',
  '上海市': '310100',
  '天津': '120100',
  '重庆': '500100',
  '广州': '440100',
  '深圳': '440300',
  '杭州': '330100',
  '南京市': '320100',
  '南京': '320100',
  '成都': '510100',
  '武汉': '420100',
  '西安': '610100',
  '郑州': '410100',
  '长沙': '430100',
  '沈阳': '210100',
  '青岛': '370200',
  '苏州': '320500'
}

/** 返回当前 cityCode（9 位 156+adcode），优先 storage/globalData，否则按城市名查表 */
function getCityCode(cityName) {
  try {
    var stored = wx.getStorageSync('carelink_current_city_code')
    if (stored && String(stored).trim()) return String(stored).trim()
    var app = getApp()
    if (app.globalData.cityCode) return app.globalData.cityCode
  } catch (e) {}
  var six = cityNameToCode[cityName] || cityNameToCode[(cityName || '').replace('市', '')] || '110100'
  return '156' + six
}

// ---------- 用户相关 ----------

/** 用户登录 POST /user/login */
function login(username, password) {
  return request({
    url: '/user/login',
    method: 'POST',
    data: { username: username, password: password },
    needAuth: false
  })
}

/** 用户注册 POST /user/register */
function register(data) {
  return request({
    url: '/user/register',
    method: 'POST',
    data: data,
    needAuth: false
  })
}

/** 获取当前用户信息 GET /user/info，返回 UserInfoVO；失败时不弹 Toast（如「用户不存在」），由调用方用本地缓存兜底 */
function getUserInfo() {
  return request({
    url: '/user/info',
    method: 'GET',
    needAuth: true,
    silent: true
  })
}

/** 将后端 UserInfoVO 写入 storage 与 globalData，供「我的」页、个人资料页使用 */
function syncUserInfoToLocal(userInfo) {
  if (!userInfo) return
  try {
    var app = getApp()
    wx.setStorageSync(USER_PROFILE_KEY, userInfo)
    if (app.globalData) app.globalData.userInfo = userInfo
  } catch (e) {}
}

/** 修改当前用户信息 PUT /user/info */
function setUserInfo(data) {
  return request({
    url: '/user/info',
    method: 'PUT',
    data: data || {},
    needAuth: true
  })
}

/** 设置当前用户城市 POST /user/city；后端保存 9 位：国家码(156)+城市码(6位)；用于定位/选城市后同步到后端，失败时静默（如未登录） */
function setUserCity(cityName, cityCode) {
  var code = (cityCode || '').toString().trim()
  return request({
    url: '/user/city',
    method: 'POST',
    data: { cityName: cityName || '', cityCode: code },
    needAuth: true,
    silent: true
  })
}

// ---------- 服务包、护工等 ----------

/** 修改密码 PUT /user/password */
function setPassword(oldPassword, newPassword) {
  return request({
    url: '/user/password',
    method: 'PUT',
    data: { oldPassword: oldPassword, newPassword: newPassword },
    needAuth: true
  })
}

// ---------- 服务地址 /user/address ----------
// 请求体/响应：contactName(必填), contactPhone(必填), address(必填), doorNumber(选填), longitude, latitude, isDefault(0/1)

/** 服务地址列表 GET /user/address/list */
function getAddressList() {
  return request({ url: '/user/address/list', method: 'GET', needAuth: true })
}

/** 服务地址详情 GET /user/address/{addressId} */
function getAddress(addressId) {
  return request({ url: '/user/address/' + addressId, method: 'GET', needAuth: true })
}

/** 新增服务地址 POST /user/address，data: { contactName, contactPhone, address, doorNumber?, longitude?, latitude?, isDefault? } */
function addAddress(data) {
  return request({ url: '/user/address', method: 'POST', data: data || {}, needAuth: true })
}

/** 修改服务地址 PUT /user/address/{addressId} */
function updateAddress(addressId, data) {
  return request({ url: '/user/address/' + addressId, method: 'PUT', data: data || {}, needAuth: true })
}

/** 删除服务地址 DELETE /user/address/{addressId} */
function deleteAddress(addressId) {
  return request({ url: '/user/address/' + addressId, method: 'DELETE', needAuth: true })
}

// ---------- 服务对象 /user/subject ----------

/** 服务对象列表 GET /user/subject/list */
function getSubjectList() {
  return request({ url: '/user/subject/list', method: 'GET', needAuth: true })
}

/** 服务对象详情 GET /user/subject/{subjectId} */
function getSubject(subjectId) {
  return request({ url: '/user/subject/' + subjectId, method: 'GET', needAuth: true })
}

/** 新增服务对象 POST /user/subject */
function addSubject(data) {
  return request({ url: '/user/subject', method: 'POST', data: data || {}, needAuth: true })
}

/** 修改服务对象 PUT /user/subject/{subjectId} */
function updateSubject(subjectId, data) {
  return request({ url: '/user/subject/' + subjectId, method: 'PUT', data: data || {}, needAuth: true })
}

/** 删除服务对象 DELETE /user/subject/{subjectId} */
function deleteSubject(subjectId) {
  return request({ url: '/user/subject/' + subjectId, method: 'DELETE', needAuth: true })
}

// ---------- 服务包 /package ----------

/** 服务包分页 GET /package/page */
function getPackagePage(params) {
  return request({
    url: '/package/page',
    method: 'GET',
    data: params || {},
    needAuth: false
  })
}

/** 服务包搜索 GET /package/search */
function searchPackage(params) {
  return request({
    url: '/package/search',
    method: 'GET',
    data: params || {},
    needAuth: false
  })
}

/** 热门搜索关键词 GET /package/hot-keywords */
function getHotKeywords(limit) {
  return request({
    url: '/package/hot-keywords',
    method: 'GET',
    data: { limit: limit || 10 },
    needAuth: false
  })
}

/** 服务包详情 GET /package/{id} */
function getPackageDetail(id) {
  return request({
    url: '/package/' + id,
    method: 'GET',
    needAuth: false
  })
}

// ---------- 护工 /caregiver ----------

/** 分页搜索护工 POST /caregiver/search */
function searchCaregivers(data) {
  return request({
    url: '/caregiver/search',
    method: 'POST',
    data: data || {},
    needAuth: false
  })
}

/** 附近护工 GET /caregiver/nearby */
function getNearbyCaregivers(cityCode, longitude, latitude, limit) {
  return request({
    url: '/caregiver/nearby',
    method: 'GET',
    data: {
      cityCode: cityCode,
      longitude: longitude,
      latitude: latitude,
      limit: limit || 20
    },
    needAuth: false
  })
}

/** 护工详情聚合 GET /caregiver/public/{caregiverId}/detail */
function getCaregiverDetail(caregiverId) {
  return request({
    url: '/caregiver/public/' + caregiverId + '/detail',
    method: 'GET',
    needAuth: false
  })
}

/** 护工基础信息 GET /caregiver/public/{caregiverId} */
function getCaregiverBasic(caregiverId) {
  return request({
    url: '/caregiver/public/' + caregiverId,
    method: 'GET',
    needAuth: false
  })
}

/** 护工技能列表 GET /caregiver/public/{caregiverId}/skills */
function getCaregiverSkills(caregiverId) {
  return request({
    url: '/caregiver/public/' + caregiverId + '/skills',
    method: 'GET',
    needAuth: false
  })
}

/** 护工服务包列表 GET /caregiver/public/{caregiverId}/packages */
function getCaregiverPackages(caregiverId) {
  return request({
    url: '/caregiver/public/' + caregiverId + '/packages',
    method: 'GET',
    needAuth: false
  })
}

// ---------- 订单 /order/user ----------
// 创建订单请求体：contactName, contactPhone, address(必填), doorNumber(选填), longitude?, latitude?, cityCode?, matchingRadius?；packageId, packageName, billingMethod, unitPrice, buyQuantity, totalAmount；clientName, clientGender?, clientAge?；expectStartTime；匹配订单另有 reqGender, reqWorkYears, reqNativePlace, specialRemark；定向订单另有 caregiverId(必填)

/** 创建匹配订单 POST /order/user/match/create */
function createMatchOrder(data) {
  return request({
    url: '/order/user/match/create',
    method: 'POST',
    data: data || {},
    needAuth: true
  })
}

/** 创建定向预约订单 POST /order/user/direct/create */
function createDirectOrder(data) {
  return request({
    url: '/order/user/direct/create',
    method: 'POST',
    data: data || {},
    needAuth: true
  })
}

/** 支付订单 POST /order/user/pay */
function payOrder(orderId) {
  return request({
    url: '/order/user/pay',
    method: 'POST',
    data: { orderId: orderId },
    needAuth: true
  })
}

/** 取消订单 POST /order/user/cancel/{orderId} */
function cancelOrder(orderId, cancelReason) {
  return request({
    url: '/order/user/cancel/' + orderId,
    method: 'POST',
    data: cancelReason != null ? { cancelReason: cancelReason } : {},
    needAuth: true
  })
}

/** 用户订单分页 GET /order/user/page，Query: status?, category?, current, size（仅拼接有效数字，绝不传 undefined 字符串） */
function getOrderUserPage(params) {
  var p = params || {}
  var current = parseInt(p.current, 10)
  var size = parseInt(p.size, 10)
  if (isNaN(current) || current < 1) current = 1
  if (isNaN(size) || size < 1) size = 10
  var query = ['current=' + current, 'size=' + size]
  var statusNum = parseInt(p.status, 10)
  if (!isNaN(statusNum) && statusNum >= 1 && statusNum <= 8) query.push('status=' + statusNum)
  var categoryNum = parseInt(p.category, 10)
  if (!isNaN(categoryNum) && categoryNum >= 1 && categoryNum <= 6) query.push('category=' + categoryNum)
  var url = '/order/user/page?' + query.join('&')
  return request({
    url: url,
    method: 'GET',
    needAuth: true
  })
}

/** 订单详情 GET /order/user/{orderId} */
function getOrderDetail(orderId) {
  return request({
    url: '/order/user/' + orderId,
    method: 'GET',
    needAuth: true
  })
}

/** 确认完成 POST /order/user/complete/{orderId} */
function completeOrder(orderId) {
  return request({
    url: '/order/user/complete/' + orderId,
    method: 'POST',
    needAuth: true
  })
}

// ---------- 评价 /review/user ----------

/** 创建评价 POST /review/user/create */
function createReview(data) {
  return request({
    url: '/review/user/create',
    method: 'POST',
    data: data || {},
    needAuth: true
  })
}

/** 我发布的评价列表 GET /review/user/my-list */
function getReviewMyList(params) {
  return request({
    url: '/review/user/my-list',
    method: 'GET',
    data: params || {},
    needAuth: true
  })
}

/** 某护工的评价列表分页 GET /review/user/caregiver/{caregiverId} */
function getReviewCaregiverList(caregiverId, params) {
  return request({
    url: '/review/user/caregiver/' + caregiverId,
    method: 'GET',
    data: params || {},
    needAuth: false
  })
}

/** AI智能评价摘要 GET /review/user/caregiver/{caregiverId}/summary */
function getCaregiverReviewSummary(caregiverId) {
  return request({
    url: '/review/user/caregiver/' + caregiverId + '/summary',
    method: 'GET',
    needAuth: false
  })
}

// ---------- 统计 /stats/user ----------

/** 护工统计信息 GET /stats/user/caregiver/{caregiverId} */
function getCaregiverStats(caregiverId) {
  return request({
    url: '/stats/user/caregiver/' + caregiverId,
    method: 'GET',
    needAuth: false
  })
}

/** 护工评价标签统计 GET /stats/user/caregiver/{caregiverId}/tags */
function getCaregiverTags(caregiverId) {
  return request({
    url: '/stats/user/caregiver/' + caregiverId + '/tags',
    method: 'GET',
    needAuth: false
  })
}

// ---------- 评价标签 /tag、文件 /file ----------

/** 评价标签列表 GET /tag/list 或 /tag/list/{type} */
function getTagList(type) {
  var url = type != null ? '/tag/list/' + type : '/tag/list'
  return request({ url: url, method: 'GET', needAuth: false })
}

/** 上传图片 POST /file/upload/image，返回 data.url */
function uploadImage(filePath) {
  return new Promise(function (resolve, reject) {
    var app = getApp()
    var token = (app.globalData && app.globalData.token) || wx.getStorageSync('token')
    if (!token) {
      reject(new Error('未登录'))
      return
    }
    wx.uploadFile({
      url: (app.globalData.baseUrl || '') + '/file/upload/image',
      filePath: filePath,
      name: 'file',
      header: { 'Authorization': 'Bearer ' + token },
      success: function (res) {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          try {
            var body = JSON.parse(res.data)
            if (body.code === 200 && body.data && body.data.url) resolve(body.data.url)
            else reject(new Error(body.message || '上传失败'))
          } catch (e) { reject(e) }
        } else reject(new Error('上传失败'))
      },
      fail: reject
    })
  })
}

function getStoredCityName() {
  try {
    return getApp().globalData.cityName || wx.getStorageSync('carelink_current_city') || ''
  } catch (e) { return '' }
}

/** 获取缓存的经纬度信息 */
function getStoredLocation() {
  try {
    var app = getApp()
    var longitude = app.globalData.longitude || wx.getStorageSync('carelink_current_longitude')
    var latitude = app.globalData.latitude || wx.getStorageSync('carelink_current_latitude')
    if (longitude && latitude) {
      return { longitude: parseFloat(longitude), latitude: parseFloat(latitude) }
    }
    return null
  } catch (e) {
    return null
  }
}

/** 缓存经纬度信息 */
function setStoredLocation(longitude, latitude) {
  try {
    var app = getApp()
    if (app.globalData) {
      app.globalData.longitude = longitude
      app.globalData.latitude = latitude
    }
    wx.setStorageSync('carelink_current_longitude', longitude)
    wx.setStorageSync('carelink_current_latitude', latitude)
  } catch (e) {
    console.error('缓存经纬度失败:', e)
  }
}

// ---------- AI 对话流式 POST /ai/chat/stream（SSE：event token 为逐段内容，done 为结束 data=conversationId） ----------
/** ArrayBuffer 转 UTF-8 字符串 */
function arrayBufferToUtf8(buf) {
  try {
    return new TextDecoder('utf-8').decode(buf)
  } catch (e) {
    var arr = new Uint8Array(buf)
    var s = ''
    for (var i = 0; i < arr.length; i++) s += String.fromCharCode(arr[i])
    return s
  }
}

/**
 * 发起 AI 流式对话，通过回调逐段渲染
 * @param {string} message 用户输入
 * @param {string} [conversationId] 会话 ID，不传则新建
 * @param {{ onToken: function(string), onDone: function(conversationId), onError: function(Error) }} callbacks
 * @returns {WechatMiniprogram.RequestTask} 可 abort 取消
 */
/** callbacks: { onToken, onDone, onError, scene } */
function requestChatStream(message, conversationId, callbacks) {
  var app = getApp()
  var baseUrl = (app && app.globalData && app.globalData.baseUrl) ? app.globalData.baseUrl : ''
  var token = (app && app.globalData && app.globalData.token) || wx.getStorageSync('token')
  var header = { 'Content-Type': 'application/json' }
  if (token) header['Authorization'] = 'Bearer ' + token
  var url = baseUrl + '/ai/chat/stream'
  var body = { message: message || '' }
  if (conversationId) body.conversationId = conversationId
  if (callbacks && callbacks.scene) body.scene = callbacks.scene
  // 附加前端实时位置信息（经纬度 + 城市编码），后端用于搜索附近护工等场景
  if (callbacks && callbacks.longitude != null) body.longitude = callbacks.longitude
  if (callbacks && callbacks.latitude != null) body.latitude = callbacks.latitude
  if (callbacks && callbacks.cityCode) body.cityCode = callbacks.cityCode

  var buffer = ''
  function parseSSE(text) {
    buffer += text
    var events = []
    var idx
    while ((idx = buffer.indexOf('\n\n')) !== -1) {
      var block = buffer.substring(0, idx)
      buffer = buffer.substring(idx + 2)
      var eventName = ''
      var data = ''
      var lines = block.split('\n')
      for (var i = 0; i < lines.length; i++) {
        var line = lines[i]
        if (line.indexOf('event:') === 0) eventName = line.replace(/^event:\s*/, '').trim()
        else if (line.indexOf('data:') === 0) data = (data ? data + '\n' : '') + line.replace(/^data:\s*/, '').trim()
      }
      if (eventName && (eventName === 'token' || eventName === 'done')) events.push({ event: eventName, data: data })
    }
    return events
  }

  var task = wx.request({
    url: url,
    method: 'POST',
    header: header,
    data: body,
    enableChunked: true,
    success: function () {},
    fail: function (err) {
      if (callbacks && callbacks.onError) callbacks.onError(err)
    }
  })
  task.onChunkReceived && task.onChunkReceived(function (res) {
    var chunk = res.data
    if (!chunk) return
    var text = typeof chunk === 'string' ? chunk : arrayBufferToUtf8(chunk)
    var events = parseSSE(text)
    for (var j = 0; j < events.length; j++) {
      var ev = events[j]
      if (ev.event === 'token' && callbacks.onToken) callbacks.onToken(ev.data)
      if (ev.event === 'done' && callbacks.onDone) callbacks.onDone(ev.data)
    }
  })
  return task
}

// ---------- AI 会话管理（对话记录） ----------
/** 我的会话列表 GET /ai/conversations，分页 */
function getAiConversations(params) {
  return request({
    url: '/ai/conversations',
    method: 'GET',
    data: params || { current: 1, size: 50 },
    needAuth: true
  })
}

/** 指定会话消息列表 GET /ai/conversation/{conversationId}/messages */
function getAiConversationMessages(conversationId, params) {
  return request({
    url: '/ai/conversation/' + conversationId + '/messages',
    method: 'GET',
    data: params || { current: 1, size: 50 },
    needAuth: true
  })
}

/** 删除单个会话 DELETE /ai/conversation/{conversationId} */
function deleteAiConversation(conversationId) {
  return request({
    url: '/ai/conversation/' + conversationId,
    method: 'DELETE',
    needAuth: true
  })
}

/** 清空所有会话 DELETE /ai/conversations/clear */
function clearAiConversations() {
  return request({
    url: '/ai/conversations/clear',
    method: 'DELETE',
    needAuth: true
  })
}

/** 置顶/取消置顶 PUT /ai/conversation/{conversationId}/pin，Query: pinned */
function pinAiConversation(conversationId, pinned) {
  return request({
    url: '/ai/conversation/' + conversationId + '/pin?pinned=' + (pinned ? 'true' : 'false'),
    method: 'PUT',
    needAuth: true
  })
}

/** 收藏/取消收藏 PUT /ai/conversation/{conversationId}/favorite，Query: favorite */
function favoriteAiConversation(conversationId, favorite) {
  return request({
    url: '/ai/conversation/' + conversationId + '/favorite?favorite=' + (favorite ? 'true' : 'false'),
    method: 'PUT',
    needAuth: true
  })
}

module.exports = {
  USER_PROFILE_KEY: USER_PROFILE_KEY,
  getCityCode: getCityCode,
  getStoredCityName: getStoredCityName,
  getStoredLocation: getStoredLocation,
  setStoredLocation: setStoredLocation,
  login: login,
  register: register,
  getUserInfo: getUserInfo,
  syncUserInfoToLocal: syncUserInfoToLocal,
  setUserInfo: setUserInfo,
  setUserCity: setUserCity,
  setPassword: setPassword,
  getAddressList: getAddressList,
  getAddress: getAddress,
  addAddress: addAddress,
  updateAddress: updateAddress,
  deleteAddress: deleteAddress,
  getSubjectList: getSubjectList,
  getSubject: getSubject,
  addSubject: addSubject,
  updateSubject: updateSubject,
  deleteSubject: deleteSubject,
  getPackagePage: getPackagePage,
  searchPackage: searchPackage,
  getHotKeywords: getHotKeywords,
  getPackageDetail: getPackageDetail,
  searchCaregivers: searchCaregivers,
  getNearbyCaregivers: getNearbyCaregivers,
  getCaregiverDetail: getCaregiverDetail,
  getCaregiverBasic: getCaregiverBasic,
  getCaregiverSkills: getCaregiverSkills,
  getCaregiverPackages: getCaregiverPackages,
  createMatchOrder: createMatchOrder,
  createDirectOrder: createDirectOrder,
  payOrder: payOrder,
  cancelOrder: cancelOrder,
  getOrderUserPage: getOrderUserPage,
  getOrderDetail: getOrderDetail,
  completeOrder: completeOrder,
  createReview: createReview,
  getReviewMyList: getReviewMyList,
  getReviewCaregiverList: getReviewCaregiverList,
  getCaregiverReviewSummary: getCaregiverReviewSummary,
  getCaregiverStats: getCaregiverStats,
  getCaregiverTags: getCaregiverTags,
  getTagList: getTagList,
  uploadImage: uploadImage,
  requestChatStream: requestChatStream,
  getAiConversations: getAiConversations,
  getAiConversationMessages: getAiConversationMessages,
  deleteAiConversation: deleteAiConversation,
  clearAiConversations: clearAiConversations,
  pinAiConversation: pinAiConversation,
  favoriteAiConversation: favoriteAiConversation
}
