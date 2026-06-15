// utils/request.js
const app = getApp()

/**
 * 封装wx.request
 */
function request(options) {
  return new Promise((resolve, reject) => {
    const { url, method = 'GET', data = {}, header = {}, needAuth = true, params = {} } = options

    // 完整URL
    let fullUrl = url.startsWith('http') ? url : `${app.globalData.baseURL}${url}`
    
    // 处理 URL 查询参数（GET 或有 params 时）
    if (method === 'GET' || Object.keys(params).length > 0) {
      const queryParams = method === 'GET' ? data : params
      const queryString = Object.keys(queryParams)
        .filter(key => queryParams[key] !== undefined && queryParams[key] !== null)
        .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(queryParams[key])}`)
        .join('&')
      
      if (queryString) {
        fullUrl += (fullUrl.includes('?') ? '&' : '?') + queryString
      }
    }

    // 请求头
    const requestHeader = {
      'Content-Type': 'application/json',
      ...header
    }

    // 添加token
    if (needAuth && app.globalData.token) {
      requestHeader['Authorization'] = `Bearer ${app.globalData.token}`
    }

    console.log(`[Request] ${method} ${fullUrl}`, method !== 'GET' ? data : '')

    wx.request({
      url: fullUrl,
      method,
      data: method === 'GET' ? undefined : data,
      header: requestHeader,
      success: (res) => {
        const { statusCode, data } = res

        // 成功响应
        if (statusCode === 200) {
          if (data.code === 200) {
            resolve(data.data)
          } else {
            // 业务错误
            wx.showToast({
              title: data.message || '请求失败',
              icon: 'none'
            })
            reject(data)
          }
        } else if (statusCode === 401) {
          // 未授权，跳转登录
          wx.showToast({
            title: '请先登录',
            icon: 'none'
          })
          app.logout()
          reject(res)
        } else {
          // 其他错误
          wx.showToast({
            title: `请求失败(${statusCode})`,
            icon: 'none'
          })
          reject(res)
        }
      },
      fail: (err) => {
        wx.showToast({
          title: '网络请求失败',
          icon: 'none'
        })
        reject(err)
      }
    })
  })
}

// GET请求
function get(url, data = {}, options = {}) {
  return request({
    url,
    method: 'GET',
    data,
    ...options
  })
}

// POST请求
function post(url, data = {}, options = {}) {
  return request({
    url,
    method: 'POST',
    data,
    ...options
  })
}

// PUT请求
function put(url, data = {}, options = {}) {
  return request({
    url,
    method: 'PUT',
    data,
    ...options
  })
}

// DELETE请求
function del(url, data = {}, options = {}) {
  return request({
    url,
    method: 'DELETE',
    data,
    ...options
  })
}

module.exports = {
  request,
  get,
  post,
  put,
  del
}
