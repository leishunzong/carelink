var api = require('../../utils/api.js')
var router = require('../../utils/router.js')

Page({
  data: {
    orderId: '',
    orderNo: '',
    amount: '0',
    selectedMethod: 'wechat',
    methods: [
      { id: 'wechat', name: '微信支付', icon: 'https://zlisten-1323337406.cos.ap-beijing.myqcloud.com/images/%E5%BE%AE%E4%BF%A1%E6%94%AF%E4%BB%98.png', description: '推荐使用微信支付' },
      { id: 'alipay', name: '支付宝', icon: 'https://zlisten-1323337406.cos.ap-beijing.myqcloud.com/images/%E6%94%AF%E4%BB%98%E5%AE%9D.png', description: '支付宝快捷支付' },
      { id: 'bankcard', name: '银行卡支付', icon: 'https://zlisten-1323337406.cos.ap-beijing.myqcloud.com/images/%E9%93%B6%E8%81%94.png', description: '储蓄卡/信用卡' }
    ],
    paying: false,
    showSuccess: false
  },

  onLoad(options) {
    const orderId = options.orderId || ''
    const orderNo = options.orderNo || orderId || ('ORD' + Date.now())
    const amount = options.amount || '0'
    
    console.log('[支付页面] 页面加载，订单信息:', { orderId, orderNo, amount })
    
    // 检查登录状态
    var app = getApp()
    var token = app.globalData.token || wx.getStorageSync('token')
    if (!token) {
      wx.showModal({
        title: '未登录',
        content: '请先登录后再进行支付',
        showCancel: false,
        success: function () {
          router.reLaunch({ url: '/pages/login/login' })
        }
      })
      return
    }
    
    this.setData({ orderId, orderNo, amount })
  },

  onMethod(e) {
    this.setData({ selectedMethod: e.currentTarget.dataset.id })
  },

  onPay() {
    var orderId = this.data.orderId
    if (!orderId) {
      wx.showToast({ title: '订单ID缺失', icon: 'none' })
      return
    }
    
    var that = this
    var app = getApp()
    var token = app.globalData.token || wx.getStorageSync('token')
    
    console.log('[支付页面] 开始支付订单:', orderId)
    console.log('[支付页面] 后端地址:', app.globalData.baseUrl)
    console.log('[支付页面] 当前token:', token ? '已设置' : '未设置')
    
    // 检查 token 是否存在
    if (!token) {
      wx.showModal({
        title: '登录已过期',
        content: '请重新登录后再进行支付',
        showCancel: false,
        success: function () {
          router.reLaunch({ url: '/pages/login/login' })
        }
      })
      return
    }
    
    this.setData({ paying: true })
    
    // 先验证 token 有效性
    api.getUserInfo().then(function (userInfo) {
      console.log('[支付页面] token 验证成功，用户信息:', userInfo)
      // token 有效，继续支付
      return api.payOrder(orderId)
    }).then(function (res) {
      console.log('[支付页面] 支付成功，后端响应:', res)
      that.setData({ paying: false, showSuccess: true })
      wx.showToast({ title: '支付成功', icon: 'success' })
    }).catch(function (err) {
      console.error('[支付页面] 支付失败，错误详情:', err)
      that.setData({ paying: false })
      
      // 检查是否是认证问题
      if (err && (err.code === 401 || (err.message && err.message.indexOf('Token') >= 0))) {
        console.log('[支付页面] Token 认证失败，可能是开发环境问题')
        
        // 开发环境兜底：模拟支付成功（实际项目中应该重新登录）
        wx.showModal({
          title: '开发环境提示',
          content: 'Token认证失败，是否模拟支付成功？（生产环境请重新登录）',
          confirmText: '模拟支付',
          cancelText: '重新登录',
          success: function (res) {
            if (res.confirm) {
              // 模拟支付成功
              console.log('[支付页面] 开发环境模拟支付成功')
              that.setData({ showSuccess: true })
              wx.showToast({ title: '支付成功（模拟）', icon: 'success' })
            } else {
              // 清除过期 token 并重新登录
              wx.removeStorageSync('token')
              app.globalData.token = null
              router.reLaunch({ url: '/pages/login/login' })
            }
          }
        })
      } else if (err && (err.errMsg || '').indexOf('request:fail') >= 0) {
        wx.showModal({
          title: '网络错误',
          content: '无法连接到服务器，请检查网络连接',
          showCancel: false
        })
      } else {
        wx.showToast({ title: (err && err.message) || '支付失败', icon: 'none' })
      }
    })
  },

  hideSuccess() {},
  goOrderDetail() {
    this.setData({ showSuccess: false })
    router.redirectTo({ url: '/pages/order/detail/detail?id=' + this.data.orderId })
  }
})
