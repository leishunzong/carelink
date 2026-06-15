// 我的页：头像/昵称、功能菜单、退出登录（用户信息从后端 GET /user/info 获取）
var app = getApp()
var api = require('../../utils/api.js')
var router = require('../../utils/router.js')

Page({
  data: {
    token: null,
    userInfo: {}
  },

  onShow() {
    if (app.globalData.justLoggedIn) {
      app.globalData.justLoggedIn = false
    }
    var token = wx.getStorageSync('token') || app.globalData.token
    if (!token) {
      router.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.setData({ token: token })
    if (token) {
      this.fetchUserInfo()
    }
  },

  /** 从后端获取用户信息并同步到 storage/globalData，再更新页面 */
  fetchUserInfo() {
    var that = this
    api.getUserInfo()
      .then(function (data) {
        api.syncUserInfoToLocal(data)
        that.setData({
          userInfo: {
            id: data.id,
            username: data.username,
            nickname: data.nickname || '用户',
            avatar: data.avatar || '',
            phone: data.phone || '',
            cityCode: data.cityCode,
            cityName: data.cityName
          }
        })
      })
      .catch(function () {
        var profile = wx.getStorageSync(api.USER_PROFILE_KEY) || {}
        that.setData({
          userInfo: {
            nickname: profile.nickname || '用户',
            phone: profile.phone || '',
            avatar: profile.avatar || ''
          }
        })
      })
  },

  goLogin() {
    router.navigateTo({ url: '/pages/login/login' })
  },

  goProfile() {
    router.navigateTo({ url: '/pages/profile/edit/edit' })
  },

  goOrders() {
    router.navigateTo({ url: '/pages/order/list/list' })
  },

  goAddress() {
    router.navigateTo({ url: '/pages/address/list/list' })
  },

  goSubject() {
    router.navigateTo({ url: '/pages/client/list/list' })
  },

  goReviews() {
    router.navigateTo({ url: '/pages/review/list/list' })
  },

  goSettle() {
    wx.showToast({ title: '护工端请使用护工端小程序', icon: 'none' })
  },

  logout() {
    var that = this
    wx.showModal({
      title: '提示',
      content: '确定退出登录吗？',
      success: function (res) {
        if (res.confirm) {
          wx.removeStorageSync('token')
          app.globalData.token = null
          app.globalData.userInfo = null
          wx.removeStorageSync(api.USER_PROFILE_KEY)
          wx.showToast({ title: '已退出', icon: 'none' })
          that.setData({ token: null, userInfo: {} })
          router.reLaunch({ url: '/pages/login/login' })
        }
      }
    })
  }
})
