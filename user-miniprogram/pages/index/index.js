// 服务页（首页）：城市（进入时逆地址解析）、轮播、服务分类、附近护工、热卖服务包（与后端联调）
var api = require('../../utils/api.js')
var locationService = require('../../utils/location.js')
var router = require('../../utils/router.js')

var serviceCategories = [
  { id: 1, name: '居家陪护', icon: 'https://zlisten-1323337406.cos.ap-beijing.myqcloud.com/images/%E5%B1%85%E5%AE%B6%E6%8A%A4%E7%90%86.png', color: 'bg-1' },
  { id: 2, name: '医院陪护', icon: 'https://zlisten-1323337406.cos.ap-beijing.myqcloud.com/images/%E5%8C%BB%E9%99%A2.png', color: 'bg-2' },
  { id: 3, name: '周期护理', icon: 'https://zlisten-1323337406.cos.ap-beijing.myqcloud.com/images/%E5%91%A8%E6%9C%9F%E7%AE%A1%E7%90%86.png', color: 'bg-3' },
  { id: 4, name: '家政服务', icon: 'https://zlisten-1323337406.cos.ap-beijing.myqcloud.com/images/%E5%AE%B6%E6%94%BF%E6%9C%8D%E5%8A%A1.png', color: 'bg-4' },
  { id: 5, name: '陪诊服务', icon: 'https://zlisten-1323337406.cos.ap-beijing.myqcloud.com/images/%E5%B0%B1%E5%8C%BB%E9%99%AA%E8%AF%8A.png', color: 'bg-5' },
  { id: 6, name: '母婴护理', icon: 'https://zlisten-1323337406.cos.ap-beijing.myqcloud.com/images/%E6%AF%8D%E5%A9%B4.png', color: 'bg-6' }
]

var CITY_KEY = 'carelink_current_city'
var PAGE_SIZE = 10
var NEARBY_REFRESH_INTERVAL = 60 * 1000 // 60秒定时刷新

function mapPackageToCard(vo) {
  if (!vo) return null
  var price = 0
  var unit = '次'
  if (vo.priceDay != null && vo.priceDay > 0) {
    price = vo.priceDay
    unit = '天'
  } else if (vo.priceMonth != null && vo.priceMonth > 0) {
    price = vo.priceMonth
    unit = '月'
  } else if (vo.priceHour != null && vo.priceHour > 0) {
    price = vo.priceHour
    unit = '小时'
  } else if (vo.priceTimes != null && vo.priceTimes > 0) {
    price = vo.priceTimes
    unit = '次'
  }
  return {
    id: vo.id,
    name: vo.name || '',
    description: vo.description || '',
    sales: vo.sales || 0,
    price: price,
    unit: unit,
    image: vo.coverImage || ''
  }
}

function mapNearbyToCard(vo) {
  if (!vo) return null
  var distance = (vo.distanceKm != null) ? (vo.distanceKm < 1 ? (vo.distanceKm * 1000).toFixed(0) + 'm' : vo.distanceKm.toFixed(1) + 'km') : '-'
  
  // 好评率处理：兼容小数（0.9091）和百分比数值（90.91）
  var goodRate = '-'
  if (vo.goodReviewRate != null) {
    var rate = Number(vo.goodReviewRate)
    if (rate > 1) {
      // 如果大于 1，说明后端返回的已经是百分比数值（如 90.91）
      goodRate = Math.round(rate) + '%'
    } else {
      // 如果小于等于 1，说明是小数（如 0.9091），需要乘以 100
      goodRate = Math.round(rate * 100) + '%'
    }
  }
  
  return {
    id: vo.id,
    name: vo.realName || '',
    avatar: vo.avatar || '',
    rating: vo.averageRating != null ? Number(vo.averageRating).toFixed(1) : '0',
    distance: distance,
    goodRating: goodRate,
    completedOrders: vo.orderCount || 0,
    workYears: vo.workYears || 0
  }
}

Page({
  data: {
    statusBarHeight: 20,
    city: '定位中…',
    locating: true,
    locationFailed: false,
    bannerImages: [
      'https://zlisten-1323337406.cos.ap-beijing.myqcloud.com/images/s1%20%281%29.png',
      'https://zlisten-1323337406.cos.ap-beijing.myqcloud.com/images/s2%20%281%29.png',
      'https://zlisten-1323337406.cos.ap-beijing.myqcloud.com/images/s3%20%281%29.png'
    ],
    serviceCategories: serviceCategories,
    nearbyList: [],
    nearbyLoading: false,
    displayedHotServices: [],
    hotPage: 1,
    hotHasMore: true,
    hotLoading: false
  },

  // 上一次用于请求附近护工的经纬度
  lastLat: null,
  lastLng: null,
  // 位置监听是否已启动
  locationWatching: false,
  // 定时器 ID
  nearbyTimer: null,

  onLoad() {
    var app = getApp()
    var statusBarHeight = 20
    if (typeof wx.getWindowInfoSync === 'function') {
      try { statusBarHeight = (wx.getWindowInfoSync() || {}).statusBarHeight || 20 } catch (e) {}
    }
    this.setData({ statusBarHeight: statusBarHeight, city: '定位中…', locating: true })
    this.loadHotServices(true)
    this.loadNearby()
    this.locateCityOnEnter()
    this.startLocationWatch()
    this.startNearbyTimer()
  },

  /** 进入首页时根据经纬度逆地址解析；未出结果前显示「定位中」，失败显示「选城市」，成功显示城市名（不展示旧缓存） */
  locateCityOnEnter() {
    var that = this
    var app = getApp()
    that.setData({ city: '定位中…', locating: true, locationFailed: false })
    console.log('[服务页-定位] 开始请求经纬度 wx.getLocation')
    wx.getLocation({
      type: 'gcj02',
      success: function (loc) {
        console.log('[服务页-定位] 获取经纬度成功', loc.latitude, loc.longitude)
        locationService.reverseGeocode(loc.latitude, loc.longitude).then(function (addr) {
          var cityName = addr.city || addr.province || ''
          var cityCode = addr.cityCode || ''
          console.log('[服务页-定位] 逆地址解析成功', cityName, 'cityCode=', cityCode)
          if (!cityName) {
            wx.showToast({ title: '定位解析失败，请选择城市', icon: 'none' })
            that.setData({ city: '选城市', locating: false, locationFailed: true })
            return
          }
          wx.setStorageSync(CITY_KEY, cityName)
          wx.setStorageSync('carelink_current_city_code', cityCode)
          if (app.globalData) {
            app.globalData.cityName = cityName
            app.globalData.cityCode = cityCode
          }
          // 缓存经纬度信息，供下单时使用
          api.setStoredLocation(loc.longitude, loc.latitude)
          console.log('[服务页-定位] 经纬度已缓存:', loc.longitude, loc.latitude)
          
          that.setData({ city: cityName, locating: false, locationFailed: false })
          api.setUserCity(cityName, cityCode).catch(function () {})
        }).catch(function (err) {
          console.log('[服务页-定位] 逆地址解析失败', err)
          wx.showToast({ title: '定位解析失败，请选择城市', icon: 'none' })
          that.setData({ city: '选城市', locating: false, locationFailed: true })
        })
      },
      fail: function (err) {
        console.log('[服务页-定位] 获取经纬度失败', err)
        wx.showToast({ title: '无法获取定位，请选择城市', icon: 'none' })
        that.setData({ city: '选城市', locating: false, locationFailed: true })
      }
    })
  },

  onShow() {
    var app = getApp()
    if (app.globalData.justLoggedIn) {
      app.globalData.justLoggedIn = false
      this.setData({ city: '定位中…', locating: true, locationFailed: false })
      this.locateCityOnEnter()
      return
    }
    var token = wx.getStorageSync('token') || app.globalData.token
    if (!token) {
      router.reLaunch({ url: '/pages/login/login' })
      return
    }
    // 从城市选择页选完返回时，用新选的城市刷新
    if (!this.data.locating) {
      if (app.globalData.cityJustSelected) {
        app.globalData.cityJustSelected = false
        var city = app.globalData.cityName || wx.getStorageSync(CITY_KEY) || ''
        if (city) this.setData({ city: city, locationFailed: false })
        // 城市变更后重新加载附近护工
        this.loadNearby(true)
      } else if (!this.data.locationFailed) {
        var stored = app.globalData.cityName || wx.getStorageSync(CITY_KEY)
        if (stored) this.setData({ city: stored })
      }
    }
    // 启动位置监听和定时刷新
    this.startLocationWatch()
    this.startNearbyTimer()
  },

  onHide() {
    this.stopLocationWatch()
    this.stopNearbyTimer()
  },

  onUnload() {
    this.stopLocationWatch()
    this.stopNearbyTimer()
  },

  /** 启动实时位置监听，经纬度变化超过阈值时自动刷新附近护工 */
  startLocationWatch() {
    if (this.locationWatching) return
    var that = this
    // 位置变化回调：与上次请求的经纬度比较，超过约500米才刷新
    this._onLocationChangeFn = function (loc) {
      var lat = loc.latitude
      var lng = loc.longitude
      if (that.lastLat != null && that.lastLng != null) {
        var dLat = Math.abs(lat - that.lastLat)
        var dLng = Math.abs(lng - that.lastLng)
        // 约0.005度 ≈ 500米，变化小于此阈值则不刷新
        if (dLat < 0.005 && dLng < 0.005) return
      }
      console.log('[位置监听] 经纬度变化，刷新附近护工', { lat: lat, lng: lng, lastLat: that.lastLat, lastLng: that.lastLng })
      that.loadNearbyWithLocation(lng, lat)
    }
    wx.startLocationUpdate({
      success: function () {
        that.locationWatching = true
        wx.onLocationChange(that._onLocationChangeFn)
        console.log('[位置监听] 已启动')
      },
      fail: function (err) {
        console.warn('[位置监听] 启动失败，回退到单次定位', err)
      }
    })
  },

  /** 停止实时位置监听 */
  stopLocationWatch() {
    if (!this.locationWatching) return
    if (this._onLocationChangeFn) {
      wx.offLocationChange(this._onLocationChangeFn)
    }
    wx.stopLocationUpdate({
      success: function () {},
      fail: function () {}
    })
    this.locationWatching = false
    console.log('[位置监听] 已停止')
  },

  /** 启动附近护工定时刷新（兜底机制，确保即使位置监听失败也能定期更新） */
  startNearbyTimer() {
    this.stopNearbyTimer()
    var that = this
    this.nearbyTimer = setInterval(function () {
      var app = getApp()
      var token = wx.getStorageSync('token') || (app && app.globalData.token)
      if (token && !that.data.nearbyLoading) {
        console.log('[定时刷新] 刷新附近护工数据')
        that.loadNearby(true)
      }
    }, NEARBY_REFRESH_INTERVAL)
    console.log('[定时刷新] 已启动，间隔:', NEARBY_REFRESH_INTERVAL / 1000, '秒')
  },

  /** 停止附近护工定时刷新 */
  stopNearbyTimer() {
    if (this.nearbyTimer) {
      clearInterval(this.nearbyTimer)
      this.nearbyTimer = null
      console.log('[定时刷新] 已停止')
    }
  },

  loadHotServices(reset) {
    if (this.data.hotLoading) return
    var page = reset ? 1 : this.data.hotPage
    this.setData({ hotLoading: true })
    api.getPackagePage({ current: page, size: PAGE_SIZE }).then(function (res) {
      var records = (res && res.records) || []
      var list = (reset ? [] : this.data.displayedHotServices).concat(records.map(mapPackageToCard).filter(Boolean))
      var total = (res && res.total) || 0
      this.setData({
        displayedHotServices: list,
        hotPage: page + 1,
        hotHasMore: list.length < total,
        hotLoading: false
      })
    }.bind(this)).catch(function () {
      this.setData({ hotLoading: false })
    }.bind(this))
  },

  loadMoreHot() {
    if (this.data.hotLoading || !this.data.hotHasMore) return
    this.loadHotServices(false)
  },

  onPullDownRefresh() {
    console.log('用户下拉刷新')
    this.loadNearby(true)
    this.loadHotServices(true)
    setTimeout(function () {
      wx.stopPullDownRefresh()
    }, 1000)
  },

  /** 使用指定经纬度加载附近护工（位置监听回调使用） */
  loadNearbyWithLocation(longitude, latitude) {
    if (this.data.nearbyLoading) return
    var city = wx.getStorageSync(CITY_KEY) || '北京'
    var cityCode = api.getCityCode(city)
    this.setData({ nearbyLoading: true })
    this.lastLat = latitude
    this.lastLng = longitude
    api.setStoredLocation(longitude, latitude)
    var that = this
    api.getNearbyCaregivers(cityCode, longitude, latitude, 20).then(function (list) {
      var arr = (list || []).map(mapNearbyToCard).filter(Boolean)
      console.log('[附近护工] 位置变化刷新，结果:', arr.length, '条')
      that.setData({ nearbyList: arr, nearbyLoading: false })
    }).catch(function (err) {
      console.error('[附近护工] 接口调用失败:', err)
      var mockData = that.getMockNearbyData()
      that.setData({ nearbyList: mockData, nearbyLoading: false })
    })
  },

  /** 首次加载或下拉刷新时通过单次定位获取经纬度再查询 */
  loadNearby(forceRefresh) {
    if (this.data.nearbyLoading) return
    var city = wx.getStorageSync(CITY_KEY) || '北京'
    var cityCode = api.getCityCode(city)
    this.setData({ nearbyLoading: true })
    var that = this
    wx.getLocation({
      type: 'gcj02',
      success: function (loc) {
        that.lastLat = loc.latitude
        that.lastLng = loc.longitude
        api.setStoredLocation(loc.longitude, loc.latitude)
        console.log('[附近护工] 单次定位成功，查询:', { cityCode: cityCode, lng: loc.longitude, lat: loc.latitude })
        api.getNearbyCaregivers(cityCode, loc.longitude, loc.latitude, 20).then(function (list) {
          var arr = (list || []).map(mapNearbyToCard).filter(Boolean)
          console.log('[附近护工] 查询结果:', arr.length, '条')
          that.setData({ nearbyList: arr, nearbyLoading: false })
        }).catch(function (err) {
          console.error('[附近护工] 接口调用失败:', err)
          var mockData = that.getMockNearbyData()
          that.setData({ nearbyList: mockData, nearbyLoading: false })
        })
      },
      fail: function (err) {
        console.error('[附近护工] 获取位置失败:', err)
        var mockData = that.getMockNearbyData()
        that.setData({ nearbyList: mockData, nearbyLoading: false })
      }
    })
  },

  // Mock 数据兜底
  getMockNearbyData() {
    var mockData = [
      { id: 1, realName: '王明慧', avatar: '', workYears: 5, distanceKm: 0.8, orderCount: 328, goodReviewRate: 0.98, averageRating: 4.8 },
      { id: 2, realName: '李秀芳', avatar: '', workYears: 8, distanceKm: 1.2, orderCount: 562, goodReviewRate: 0.99, averageRating: 4.9 },
      { id: 3, realName: '张翠珍', avatar: '', workYears: 6, distanceKm: 1.5, orderCount: 426, goodReviewRate: 0.97, averageRating: 4.7 },
      { id: 4, realName: '赵大姐', avatar: '', workYears: 4, distanceKm: 1.8, orderCount: 218, goodReviewRate: 0.95, averageRating: 4.6 },
      { id: 5, realName: '孙阿姨', avatar: '', workYears: 7, distanceKm: 2.1, orderCount: 467, goodReviewRate: 0.98, averageRating: 4.8 },
      { id: 6, realName: '刘护工', avatar: '', workYears: 3, distanceKm: 2.5, orderCount: 156, goodReviewRate: 0.94, averageRating: 4.5 }
    ]
    return mockData.map(mapNearbyToCard).filter(Boolean)
  },

  goCitySelector() {
    router.navigateTo({ url: '/pages/city/index/index' })
  },

  goSearch() {
    router.navigateTo({ url: '/pages/search/search' })
  },

  goServiceList(e) {
    var id = e.currentTarget.dataset.id || 0
    router.navigateTo({ url: '/pages/package/list/list?categoryId=' + id })
  },


  goCaregiverDetail(e) {
    var id = e.currentTarget.dataset.id
    router.navigateTo({ url: '/pages/caregiver/detail/detail?id=' + id })
  },

  goServiceDetail(e) {
    var id = e.currentTarget.dataset.id
    router.navigateTo({ url: '/pages/package/detail/detail?id=' + id })
  }
})
