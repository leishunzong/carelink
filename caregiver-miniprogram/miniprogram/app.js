// app.js
App({
  globalData: {
    userInfo: null,
    token: null,
    baseURL: 'http://localhost:8080/api',
    hasLogin: false,
    workState: 1, // 1-接单中 2-服务中 3-休息中
    location: null
  },

  onLaunch() {
    // 检查登录状态
    this.checkLoginStatus()
    
    // 获取系统信息
    this.getSystemInfo()
  },

  // 检查登录状态
  checkLoginStatus() {
    const token = wx.getStorageSync('token')
    const userInfo = wx.getStorageSync('userInfo')
    
    if (token && userInfo) {
      this.globalData.token = token
      this.globalData.userInfo = userInfo
      this.globalData.hasLogin = true
    }
  },

  // 获取系统信息
  getSystemInfo() {
    wx.getSystemInfo({
      success: (res) => {
        this.globalData.systemInfo = res
      }
    })
  },

  // 登录
  login(token, userInfo) {
    this.globalData.token = token
    this.globalData.userInfo = userInfo
    this.globalData.hasLogin = true
    
    wx.setStorageSync('token', token)
    wx.setStorageSync('userInfo', userInfo)
  },

  // 登出
  logout() {
    this.globalData.token = null
    this.globalData.userInfo = null
    this.globalData.hasLogin = false
    
    wx.removeStorageSync('token')
    wx.removeStorageSync('userInfo')
    
    wx.reLaunch({
      url: '/pages/login/login'
    })
  },

  // 更新用户信息
  updateUserInfo(userInfo) {
    this.globalData.userInfo = {
      ...this.globalData.userInfo,
      ...userInfo
    }
    wx.setStorageSync('userInfo', this.globalData.userInfo)
  },

  // 获取用户信息
  getUserInfo() {
    return this.globalData.userInfo
  },

  // 设置用户信息
  setUserInfo(userInfo) {
    this.globalData.userInfo = userInfo
    wx.setStorageSync('userInfo', userInfo)
  }
})
