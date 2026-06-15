// 系统匹配下单页：从服务包详情/首页进入，仅 packageId，无 caregiverId；对接 POST /order/user/match/create
var router = require('../../../utils/router.js')
var api = require('../../../utils/api.js')

var billingKeyToMethod = { month: 1, day: 2, hour: 3, times: 4 }

Page({
  data: {
    packageId: '',
    packageInfo: {},
    selectedMethod: {},
    quantity: 1,
    matchDistance: '5',
    expectTime: '',
    expectDate: '',
    expectTimeOnly: '',
    minDate: '',
    detailAddress: '',
    orderAddress: '',
    orderDoorNumber: '',
    addressId: '',
    clientName: '',
    clientId: '',
    contactName: '',
    contactPhone: '',
    reqGender: '',
    reqExperience: '',
    reqOrigin: '',
    originIndex: 0,
    genderOptions: ['不限', '男', '女'],
    experienceOptions: ['1年以下', '1-3年', '3-5年', '5-10年', '10年以上'],
    originOptions: ['不限', '本地', '江苏', '浙江', '安徽', '山东', '河南', '四川', '湖北'],
    remarks: '',
    agreedToTerms: false,
    totalAmount: '0',
    loading: false
  },

  onLoad(options) {
    var packageId = options.packageId || ''
    if (!packageId) {
      wx.showToast({ title: '请先选择服务包', icon: 'none' })
      return
    }
    var today = new Date()
    var m = today.getMonth() + 1
    var d = today.getDate()
    var minDate = today.getFullYear() + '-' + (m < 10 ? '0' + m : m) + '-' + (d < 10 ? '0' + d : d)
    this.setData({ packageId: packageId, minDate: minDate })
    this.loadPackage(packageId)
    this.loadDefaultAddress()
    this.loadDefaultClient()
  },

  onShow() {
    var addr = wx.getStorageSync('carelink_selected_address')
    if (addr) {
      var full = addr.full || (addr.address || '') + (addr.doorNumber ? ' ' + addr.doorNumber : '')
      this.setData({
        detailAddress: full,
        orderAddress: addr.address || addr.full || full,
        orderDoorNumber: addr.doorNumber || '',
        addressId: addr.id || '',
        contactName: addr.contactName || this.data.contactName || '',
        contactPhone: addr.contactPhone || this.data.contactPhone || ''
      })
      wx.removeStorageSync('carelink_selected_address')
    }
    var client = wx.getStorageSync('carelink_selected_client')
    if (client) {
      this.setData({
        clientName: client.name || '',
        clientId: client.id || ''
      })
      wx.removeStorageSync('carelink_selected_client')
    }
  },

  loadPackage(packageId) {
    var that = this
    console.log('加载服务包详情:', packageId)
    
    api.getPackageDetail(packageId).then(function (res) {
      var pkg = res.data || res
      console.log('服务包详情响应:', pkg)
      
      // 构建计费方式列表
      var methods = []
      if (pkg.allowMonth && pkg.priceMonth) methods.push({ key: 'month', name: '按月', price: pkg.priceMonth, unit: '月' })
      if (pkg.allowDay && pkg.priceDay) methods.push({ key: 'day', name: '按天', price: pkg.priceDay, unit: '天' })
      if (pkg.allowHour && pkg.priceHour) methods.push({ key: 'hour', name: '按小时', price: pkg.priceHour, unit: '小时' })
      if (pkg.allowTimes && pkg.priceTimes) methods.push({ key: 'times', name: '按次', price: pkg.priceTimes, unit: '次' })
      
      var packageInfo = {
        id: pkg.id,
        name: pkg.name || '',
        description: pkg.description || '',
        coverImage: pkg.coverImage || '',
        sales: pkg.sales || 0,
        methods: methods
      }
      
      that.setData({
        packageInfo: packageInfo,
        selectedMethod: methods[0] || {}
      })
      that.calcTotal()
    }).catch(function (err) {
      console.error('加载服务包详情失败，使用 mock 数据:', err)
      // 使用 mock 数据兜底
      var methods = [
        { key: 'day', name: '按天', price: 240, unit: '天' },
        { key: 'hour', name: '按小时', price: 35, unit: '小时' }
      ]
      var mock = {
        id: packageId,
        name: '基础陪护',
        description: '适用于完全自理病人，需要日常陪诊、输液、检查等',
        coverImage: '',
        sales: 38004,
        methods: methods
      }
      that.setData({
        packageInfo: mock,
        selectedMethod: methods[0]
      })
      that.calcTotal()
    })
  },

  selectMethod(e) {
    var method = e.currentTarget.dataset.method
    this.setData({ selectedMethod: method })
    this.calcTotal()
  },

  calcTotal() {
    var method = this.data.selectedMethod
    var q = this.data.quantity
    var total = ((method.price || 0) * q).toFixed(2)
    this.setData({ totalAmount: total })
  },

  minusQty() {
    var q = this.data.quantity
    if (q <= 1) return
    this.setData({ quantity: q - 1 })
    this.calcTotal()
  },

  plusQty() {
    var q = this.data.quantity
    if (q >= 50) return // 限制最大50
    this.setData({ quantity: q + 1 })
    this.calcTotal()
  },

  onMatchDistance(e) {
    var value = e.detail.value || '5'
    var num = parseInt(value, 10)
    if (isNaN(num) || num < 1) num = 1
    if (num > 50) num = 50
    this.setData({ matchDistance: String(num) })
  },

  onExpectDateChange(e) {
    var date = e.detail.value
    this.setData({ expectDate: date })
    this._applyExpectTime()
  },

  onExpectTimeChange(e) {
    var time = e.detail.value
    this.setData({ expectTimeOnly: time })
    this._applyExpectTime()
  },

  _applyExpectTime() {
    var date = this.data.expectDate
    var time = this.data.expectTimeOnly
    if (date && time) {
      this.setData({ expectTime: date + ' ' + time })
    } else if (date) {
      this.setData({ expectTime: date })
    } else {
      this.setData({ expectTime: '' })
    }
  },

  loadDefaultAddress() {
    var that = this
    console.log('[系统匹配下单] 加载默认服务地址')
    
    api.getAddressList().then(function (res) {
      var addresses = res.data || res || []
      console.log('[系统匹配下单] 地址列表响应:', addresses)
      
      // 查找默认地址或使用第一个地址
      var defaultAddr = addresses.find(function (addr) { return addr.isDefault === 1 }) || addresses[0]
      if (defaultAddr) {
        // 简化地址显示，只显示关键信息
        var addressParts = (defaultAddr.address || '').split(' ')
        var shortAddress = addressParts.length > 3 ? 
          addressParts.slice(-2).join(' ') : // 取后两部分，如"东华门街道 正义路"
          defaultAddr.address || ''
        var displayAddress = shortAddress + (defaultAddr.doorNumber ? ' ' + defaultAddr.doorNumber : '')
        
        that.setData({
          detailAddress: displayAddress,
          orderAddress: defaultAddr.address || '',
          orderDoorNumber: defaultAddr.doorNumber || '',
          addressId: defaultAddr.id || '',
          contactName: defaultAddr.contactName || that.data.contactName || '',
          contactPhone: defaultAddr.contactPhone || that.data.contactPhone || ''
        })
        console.log('[系统匹配下单] 已设置默认地址:', displayAddress)
      }
    }).catch(function (err) {
      console.log('[系统匹配下单] 加载地址失败:', err)
    })
  },

  loadDefaultClient() {
    var that = this
    console.log('[系统匹配下单] 加载默认服务对象')
    
    api.getSubjectList().then(function (res) {
      var clients = res.data || res || []
      console.log('[系统匹配下单] 服务对象列表响应:', clients)
      
      // 查找默认服务对象或使用第一个
      var defaultClient = clients.find(function (client) { return client.isDefault === 1 }) || clients[0]
      if (defaultClient) {
        that.setData({
          clientName: defaultClient.name || '',
          clientId: defaultClient.id || ''
        })
        console.log('[系统匹配下单] 已设置默认服务对象:', defaultClient.name)
      }
    }).catch(function (err) {
      console.log('[系统匹配下单] 加载服务对象失败:', err)
    })
  },

  chooseAddress() {
    router.navigateTo({ url: '/pages/address/list/list?from=order' })
  },

  chooseClient() {
    router.navigateTo({ url: '/pages/client/list/list?from=order' })
  },

  onContactName(e) {
    this.setData({ contactName: e.detail.value })
  },
  onContactPhone(e) {
    this.setData({ contactPhone: e.detail.value })
  },

  setReqGender(e) {
    this.setData({ reqGender: e.currentTarget.dataset.val })
  },
  setReqExperience(e) {
    this.setData({ reqExperience: e.currentTarget.dataset.val })
  },
  onOriginChange(e) {
    var i = parseInt(e.detail.value, 10)
    this.setData({ originIndex: i, reqOrigin: this.data.originOptions[i] })
  },

  onRemarks(e) {
    this.setData({ remarks: e.detail.value })
  },

  toggleTerms() {
    this.setData({ agreedToTerms: !this.data.agreedToTerms })
  },

  onSubmit() {
    var d = this.data
    if (!d.packageId) {
      wx.showToast({ title: '请先选择服务包', icon: 'none' })
      return
    }
    if (!d.contactName || !d.contactPhone) {
      wx.showToast({ title: '请填写联系人和手机号', icon: 'none' })
      return
    }
    var address = (d.orderAddress || d.detailAddress || '').trim()
    if (!address) {
      wx.showToast({ title: '请选择服务地址', icon: 'none' })
      return
    }
    if (!d.clientName) {
      wx.showToast({ title: '请选择服务对象', icon: 'none' })
      return
    }
    if (!d.expectTime) {
      wx.showToast({ title: '请选择上门时间', icon: 'none' })
      return
    }
    if (!d.agreedToTerms) {
      wx.showToast({ title: '请同意服务协议', icon: 'none' })
      return
    }
    var method = d.selectedMethod
    var billingMethodVal = billingKeyToMethod[method.key] != null ? billingKeyToMethod[method.key] : 2
    var expectStartTime = (d.expectDate && d.expectTimeOnly) ? (d.expectDate + ' ' + d.expectTimeOnly + ':00') : ''
    var cityCode = api.getCityCode(api.getStoredCityName())
    var reqGenderVal = d.reqGender === '男' ? 1 : d.reqGender === '女' ? 2 : 0
    var that = this
    
    // 构建基础订单数据，添加字符串长度限制
    var basePayload = {
      contactName: d.contactName.trim().substring(0, 50),
      contactPhone: d.contactPhone.trim().substring(0, 20),
      address: address.substring(0, 200),
      doorNumber: (d.orderDoorNumber || '').trim().substring(0, 50) || undefined,
      cityCode: cityCode,
      matchingRadius: Math.min(parseInt(d.matchDistance || '5', 10), 50), // 直接使用km，不转换为米
      packageId: d.packageId,
      packageName: ((d.packageInfo && d.packageInfo.name) || '').substring(0, 50),
      billingMethod: billingMethodVal,
      unitPrice: method.price || 0,
      buyQuantity: Math.min(Math.max(d.quantity || 1, 1), 50),
      totalAmount: parseFloat(d.totalAmount) || 0,
      clientName: d.clientName.trim().substring(0, 50),
      reqGender: reqGenderVal,
      reqWorkYears: d.reqExperience || undefined,
      reqNativePlace: (d.reqOrigin || '').substring(0, 50) || undefined,
      specialRemark: (d.remarks || '').trim().substring(0, 200) || undefined,
      expectStartTime: expectStartTime
    }
    
    // 获取缓存的经纬度，如果没有则重新获取
    var location = api.getStoredLocation()
    if (!location) {
      console.log('[系统匹配下单] 没有缓存的位置信息，重新获取')
      wx.showLoading({ title: '获取位置中...' })
      wx.getLocation({
        type: 'gcj02',
        success: function (loc) {
          wx.hideLoading()
          api.setStoredLocation(loc.longitude, loc.latitude)
          that.submitOrderWithLocation(basePayload, { longitude: loc.longitude, latitude: loc.latitude })
        },
        fail: function () {
          wx.hideLoading()
          wx.showToast({ title: '无法获取位置信息，请检查定位权限', icon: 'none' })
        }
      })
      return
    }
    
    console.log('[系统匹配下单] 使用缓存的位置信息:', location)
    this.submitOrderWithLocation(basePayload, location)
  },

  submitOrderWithLocation(basePayload, location) {
    var payload = Object.assign({}, basePayload, {
      longitude: location.longitude,
      latitude: location.latitude
    })
    
    console.log('[系统匹配下单] 提交订单参数详细:')
    console.log('- contactName:', payload.contactName, '长度:', payload.contactName.length)
    console.log('- contactPhone:', payload.contactPhone, '长度:', payload.contactPhone.length)
    console.log('- address:', payload.address, '长度:', payload.address.length)
    console.log('- doorNumber:', payload.doorNumber)
    console.log('- matchingRadius:', payload.matchingRadius, 'km')
    console.log('- buyQuantity:', payload.buyQuantity)
    console.log('- unitPrice:', payload.unitPrice)
    console.log('- totalAmount:', payload.totalAmount)
    console.log('- clientName:', payload.clientName, '长度:', payload.clientName.length)
    console.log('- packageName:', payload.packageName, '长度:', payload.packageName.length)
    this.setData({ loading: true })
    var that = this
    api.createMatchOrder(payload).then(function () {
      that.setData({ loading: false })
      wx.showToast({ title: '订单已创建', icon: 'success' })
      setTimeout(function () { router.redirectTo({ url: '/pages/order/list/list' }) }, 500)
    }).catch(function (err) {
      that.setData({ loading: false })
      console.error('[系统匹配下单] 提交失败:', err)
      wx.showToast({ title: (err && err.message) || '提交失败', icon: 'none' })
    })
  }
})
