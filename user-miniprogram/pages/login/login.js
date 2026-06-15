var app = getApp()
var api = require('../../utils/api.js')
var router = require('../../utils/router.js')

Page({
  data: {
    account: '',
    password: '',
    passwordVisible: false,
    loading: false
  },

  onShow() {
    var token = app.globalData.token || wx.getStorageSync('token')
    if (token) {
      router.switchTab({ url: '/pages/index/index' })
    }
  },

  onAccountInput(e) {
    this.setData({ account: e.detail.value })
  },
  onPasswordInput(e) {
    this.setData({ password: e.detail.value })
  },

  onTogglePassword() {
    this.setData({ passwordVisible: !this.data.passwordVisible })
  },

  onSubmit() {
    var that = this
    var account = (this.data.account || '').trim()
    var password = this.data.password
    if (!account || !password) {
      wx.showToast({ title: '请输入账号和密码', icon: 'none' })
      return
    }
    that.setData({ loading: true })
    var timeout = new Promise(function (_, reject) {
      setTimeout(function () { reject(new Error('timeout')) }, 15000)
    })
    Promise.race([api.login(account, password), timeout])
      .then(function (data) {
        var token = data && data.token
        if (token) {
          wx.setStorageSync('token', token)
          app.globalData.token = token
          app.globalData.justLoggedIn = true
          that.setData({ loading: false })
          api.getUserInfo().then(function (user) {
            api.syncUserInfoToLocal(user)
          }).catch(function () {})
          setTimeout(function () {
            router.switchTab({ url: '/pages/index/index' })
          }, 50)
        } else {
          that.setData({ loading: false })
          wx.showToast({ title: '登录失败', icon: 'none' })
        }
      })
      .catch(function (err) {
        that.setData({ loading: false })
        if (err && err.message === 'timeout') {
          wx.showToast({ title: '请求超时', icon: 'none' })
        }
        // 其它错误（网络、业务失败）request 里已 showToast，这里只负责关 loading
      })
  },

  goRegister() {
    require('../../utils/router.js').navigateTo({ url: '/pages/register/register' })
  }
})
