/**
 * 请求封装，后续对接 backend 时使用
 * 文档见 backend/care-link/docs/API接口文档.md
 * 注意：getApp() 必须在请求发起时再调用，否则模块加载时 App 未就绪会报错
 */
function request(options) {
  var app = getApp()
  var baseUrl = (app && app.globalData && app.globalData.baseUrl) ? app.globalData.baseUrl : ''
  var header = {
    'Content-Type': 'application/json'
  }
  if (options.header) {
    for (var k in options.header) header[k] = options.header[k]
  }
  // 登录后所有请求都在请求头带上 token（与接口文档约定一致）
  var token = (app && app.globalData && app.globalData.token) || wx.getStorageSync('token')
  if (token) header['Authorization'] = 'Bearer ' + token
  var url = (options.url || '').trim()
  var method = options.method || 'GET'
  var data = options.data || {}
  return new Promise(function (resolve, reject) {
    wx.request({
      url: baseUrl + url,
      method: method,
      data: data,
      header: header,
      success: function (res) {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          var body = res.data
          if (body && body.code === 200) {
            resolve(body.data)
          } else {
            if (!options.silent) wx.showToast({ title: (body && body.message) || '请求失败', icon: 'none' })
            reject(body || new Error('请求失败'))
          }
        } else {
          // 仅当本次请求带了 token（needAuth）且返回 401 时清 token 并跳转登录，避免未带 token 的接口 401 误踢出
          if (res.statusCode === 401 && options.needAuth && app && app.globalData) {
            wx.removeStorageSync('token')
            app.globalData.token = null
            try { require('./router.js').reLaunch({ url: '/pages/login/login' }) } catch (e) { wx.reLaunch({ url: '/pages/login/login' }) }
          }
          reject(res)
        }
      },
      fail: function (err) {
        wx.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      }
    })
  })
}

module.exports = request
