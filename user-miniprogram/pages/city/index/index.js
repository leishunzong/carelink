// 选择城市：搜索（行政区划）、定位当前城市（逆地址）、保存城市名与 cityCode（156+adcode），并同步到 globalData 与后端
var app = getApp()
var locationService = require('../../../utils/location.js')
var api = require('../../../utils/api.js')

var CITY_KEY = 'carelink_current_city'
var CITY_CODE_KEY = 'carelink_current_city_code'

function saveCityAndSync(cityName, cityCode) {
  wx.setStorageSync(CITY_KEY, cityName)
  wx.setStorageSync(CITY_CODE_KEY, cityCode)
  app.globalData.cityName = cityName
  app.globalData.cityCode = cityCode
  app.globalData.cityJustSelected = true
  api.setUserCity(cityName, cityCode).catch(function () {})
}

var hotCities = ['北京', '上海', '广州', '深圳', '杭州', '成都', '重庆', '武汉', '西安', '南京', '天津', '苏州', '郑州', '长沙', '沈阳', '青岛', '宁波', '无锡', '大连', '厦门']

Page({
  data: {
    searchQuery: '',
    locatedCity: '',
    locatedCityCode: '',
    locateLoading: true,
    searchResults: [],
    searchLoading: false,
    filteredCities: hotCities,
    useSearchResults: false
  },

  onLoad() {
    this.setData({ filteredCities: hotCities, useSearchResults: false })
    this.loadLocatedCity()
  },

  /** 进入页面时或点击重试时：获取经纬度 -> 逆地址解析，得到「当前定位城市」 */
  loadLocatedCity() {
    var that = this
    this.setData({ locateLoading: true })
    console.log('[城市页-定位] 开始请求经纬度 wx.getLocation')
    wx.getLocation({
      type: 'gcj02',
      success: function (loc) {
        console.log('[城市页-定位] 获取经纬度成功', loc.latitude, loc.longitude)
        locationService.reverseGeocode(loc.latitude, loc.longitude).then(function (addr) {
          var cityName = addr.city || addr.province || ''
          var cityCode = addr.cityCode || ''
          console.log('[城市页-定位] 逆地址解析成功', cityName, 'cityCode=', cityCode)
          if (!cityName) {
            that.setData({ locatedCity: '定位失败', locateLoading: false })
            return
          }
          that.setData({
            locatedCity: cityName,
            locatedCityCode: cityCode,
            locateLoading: false
          })
          if (cityName && cityCode) saveCityAndSync(cityName, cityCode)
        }).catch(function (err) {
          console.log('[城市页-定位] 逆地址解析失败', err)
          that.setData({ locatedCity: '定位失败', locateLoading: false })
        })
      },
      fail: function (err) {
        console.log('[城市页-定位] 获取经纬度失败', err)
        that.setData({ locatedCity: '未获取定位', locateLoading: false })
      }
    })
  },

  onSearchInput(e) {
    var q = (e.detail.value || '').trim()
    this.setData({ searchQuery: q })
    if (!q) {
      this.setData({ searchResults: [], useSearchResults: false, filteredCities: hotCities })
      return
    }
    this.setData({ searchLoading: true })
    locationService.districtSearch(q).then(function (list) {
      var raw = (list || []).map(function (item) {
        return {
          id: item.id,
          fullname: (item.fullname || item.name || '').trim(),
          name: (item.name || item.fullname || '').trim()
        }
      }).filter(function (item) { return item.fullname })
      // 按 fullname 去重，避免「洛阳镇」等重复展示
      var seen = {}
      var results = []
      for (var i = 0; i < raw.length; i++) {
        var key = raw[i].fullname
        if (seen[key]) continue
        seen[key] = true
        results.push(raw[i])
      }
      console.log('[城市页-搜索] 关键词', q, '原始条数', raw.length, '去重后', results.length)
      this.setData({ searchResults: results, useSearchResults: true, searchLoading: false })
    }.bind(this)).catch(function (err) {
      console.log('[城市页-搜索] 失败', err)
      this.setData({ searchLoading: false, searchResults: [] })
    }.bind(this))
  },

  /** 点击「当前定位城市」：若定位失败/未获取则重新发起逆地址解析；否则使用该城市并返回 */
  onUseLocatedCity() {
    var cityName = this.data.locatedCity
    var cityCode = this.data.locatedCityCode || ''
    if (!cityName || cityName === '定位失败' || cityName === '未获取定位') {
      console.log('[城市页-定位] 用户点击重试，重新请求定位与逆地址解析')
      wx.showToast({ title: '正在重新定位…', icon: 'none' })
      this.loadLocatedCity()
      return
    }
    saveCityAndSync(cityName, cityCode)
    wx.showToast({ title: '已切换至 ' + cityName, icon: 'success' })
    setTimeout(function () { wx.navigateBack() }, 500)
  },

  onSelect(e) {
    var city = e.currentTarget.dataset.city
    var adcode = e.currentTarget.dataset.code || ''
    if (!city) return
    var cityCode = locationService.toCityCode(adcode)
    this.setData({ currentCity: city, currentCityCode: cityCode })
    saveCityAndSync(city, cityCode)
    wx.showToast({ title: '已切换至 ' + city, icon: 'success' })
    setTimeout(function () { wx.navigateBack() }, 500)
  },

  /** 从热门城市选择：根据名称查 cityCode（156+adcode）再保存 */
  onHotSelect(e) {
    var city = e.currentTarget.dataset.city
    var that = this
    locationService.getCityCodeByKeyword(city).then(function (res) {
      var name = (res && res.cityName) ? res.cityName : city
      var code = (res && res.cityCode) ? res.cityCode : ''
      that.setData({ currentCity: name, currentCityCode: code })
      saveCityAndSync(name, code)
      wx.showToast({ title: '已切换至 ' + name, icon: 'success' })
      setTimeout(function () { wx.navigateBack() }, 500)
    }).catch(function () {
      that.setData({ currentCity: city })
      saveCityAndSync(city, '')
      wx.showToast({ title: '已切换至 ' + city, icon: 'success' })
      setTimeout(function () { wx.navigateBack() }, 500)
    })
  }
})
