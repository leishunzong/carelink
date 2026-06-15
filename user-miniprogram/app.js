// 护联 Care-Link 用户端，与 backend/care-link 联调
// 后端 context-path: /api；城市与 token 一样持久化，供需要 city/cityCode 的接口使用
var router = require('./utils/router.js')

App({
  towxml: require('./towxml/index'),
  globalData: {
    userInfo: null,
    token: null,
    justLoggedIn: false,
    cityName: null,
    cityCode: null,
    baseUrl: 'http://localhost:8080/api',
    router: router
  },

  onLaunch() {
    var token = wx.getStorageSync('token')
    if (token) this.globalData.token = token
    var cityName = wx.getStorageSync('carelink_current_city')
    var cityCode = wx.getStorageSync('carelink_current_city_code')
    if (cityName) this.globalData.cityName = cityName
    if (cityCode) this.globalData.cityCode = cityCode
    // 有 token 时通过路由守卫进入首页（未登录时首包为登录页，不跳转）
    if (token) {
      router.switchTab({ url: '/pages/index/index' })
    }
  }
})
