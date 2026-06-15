// 定向预约页：从护工详情进入，带 caregiverId，可选 packageId；对接 POST /order/user/direct/create
var app = getApp()
var router = require('../../../utils/router.js')
var api = require('../../../utils/api.js')

var billingKeyToMethod = { month: 1, day: 2, hour: 3, times: 4 }

Page({
  data: {
    caregiverId: '',
    caregiver: {},
    packages: [],
    selectedPackage: {},
    selectedMethod: {},
    quantity: 1,
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
    remarks: '',
    agreedToTerms: false,
    totalAmount: '0',
    loading: false
  },

  onLoad(options) {
    var caregiverId = options.caregiverId || ''
    var packageId = options.packageId || ''
    if (!caregiverId) {
      wx.showToast({ title: '缺少护工信息', icon: 'none' })
      return
    }
    var today = new Date()
    var m = today.getMonth() + 1
    var d = today.getDate()
    var minDate = today.getFullYear() + '-' + (m < 10 ? '0' + m : m) + '-' + (d < 10 ? '0' + d : d)
    this.setData({ caregiverId: caregiverId, minDate: minDate })
    this.loadCaregiver()
    this.loadPackages(packageId)
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

  loadCaregiver() {
    var that = this
    var caregiverId = this.data.caregiverId
    console.log('加载护工详情:', caregiverId)
    
    api.getCaregiverDetail(caregiverId).then(function (res) {
      var data = res.data || res
      console.log('护工详情响应:', data)
      
      var basic = data.basicInfo || data
      var caregiver = {
        id: basic.id,
        realName: basic.realName || basic.name,
        name: basic.realName || basic.name,
        avatar: basic.avatar || '',
        rating: (data.stats && data.stats.averageRating) || 0,
        workYears: basic.workYears || 0,
        experience: (basic.workYears || 0) + '年经验'
      }
      
      that.setData({ caregiver: caregiver })
    }).catch(function (err) {
      console.error('加载护工详情失败，使用 mock 数据:', err)
      // 使用 mock 数据兜底
      var mock = {
        id: caregiverId,
        realName: '王明慧',
        name: '王明慧',
        avatar: '',
        rating: 4.8,
        workYears: 5,
        experience: '5年经验'
      }
      that.setData({ caregiver: mock })
    })
  },

  loadPackages(preSelectPackageId) {
    var that = this
    var caregiverId = this.data.caregiverId
    console.log('加载护工服务包:', caregiverId, '预选包ID:', preSelectPackageId)
    
    api.getCaregiverPackages(caregiverId).then(function (res) {
      var packages = res.data || res || []
      console.log('护工服务包响应:', packages)
      
      // 为每个服务包构建计费方式
      var processedPackages = packages.map(function (pkg) {
        var methods = []
        if (pkg.allowMonth && pkg.priceMonth) methods.push({ key: 'month', name: '按月', price: pkg.priceMonth, unit: '月' })
        if (pkg.allowDay && pkg.priceDay) methods.push({ key: 'day', name: '按天', price: pkg.priceDay, unit: '天' })
        if (pkg.allowHour && pkg.priceHour) methods.push({ key: 'hour', name: '按小时', price: pkg.priceHour, unit: '小时' })
        if (pkg.allowTimes && pkg.priceTimes) methods.push({ key: 'times', name: '按次', price: pkg.priceTimes, unit: '次' })
        
        return {
          id: pkg.id,
          name: pkg.name || '',
          description: pkg.description || '',
          methods: methods
        }
      })
      
      var selectedPackage = processedPackages[0] || {}
      var selectedMethod = selectedPackage.methods && selectedPackage.methods[0]
      
      // 如果有预选的服务包ID，优先选择
      if (preSelectPackageId) {
        var preSelected = processedPackages.find(function (p) { return p.id == preSelectPackageId })
        if (preSelected) {
          selectedPackage = preSelected
          selectedMethod = preSelected.methods && preSelected.methods[0]
        }
      }
      
      that.setData({
        packages: processedPackages,
        selectedPackage: selectedPackage,
        selectedMethod: selectedMethod || {}
      })
      that.calcTotal()
    }).catch(function (err) {
      console.error('加载护工服务包失败，使用 mock 数据:', err)
      // 使用 mock 数据兜底
      var mockPackages = [
        { id: 1, name: '基础陪护', description: '适用于完全自理病人，日常陪诊、输液、检查等', methods: [{ key: 'day', name: '按天', price: 240, unit: '天' }, { key: 'hour', name: '按小时', price: 35, unit: '小时' }] },
        { id: 2, name: '慢病照护', description: '生活支援，守护健康', methods: [{ key: 'month', name: '按月', price: 4880, unit: '月' }, { key: 'day', name: '按天', price: 180, unit: '天' }] }
      ]
      var selectedPackage = mockPackages[0] || {}
      var selectedMethod = selectedPackage.methods && selectedPackage.methods[0]
      
      if (preSelectPackageId) {
        var preSelected = mockPackages.find(function (p) { return p.id == preSelectPackageId })
        if (preSelected) {
          selectedPackage = preSelected
          selectedMethod = preSelected.methods && preSelected.methods[0]
        }
      }
      
      that.setData({
        packages: mockPackages,
        selectedPackage: selectedPackage,
        selectedMethod: selectedMethod || {}
      })
      that.calcTotal()
    })
  },

  selectPackage(e) {
    var pkg = e.currentTarget.dataset.pkg
    var method = (pkg.methods && pkg.methods[0]) || {}
    this.setData({ selectedPackage: pkg, selectedMethod: method })
    this.calcTotal()
  },

  selectMethod(e) {
    var method = e.currentTarget.dataset.method
    this.setData({ selectedMethod: method })
    this.calcTotal()
  },

  calcTotal() {
    var selectedMethod = this.data.selectedMethod
    var quantity = this.data.quantity
    var total = ((selectedMethod.price || 0) * quantity).toFixed(2)
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
    console.log('[定向预约下单] 加载默认服务地址')
    
    api.getAddressList().then(function (res) {
      var addresses = res.data || res || []
      console.log('[定向预约下单] 地址列表响应:', addresses)
      
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
        console.log('[定向预约下单] 已设置默认地址:', displayAddress)
      }
    }).catch(function (err) {
      console.log('[定向预约下单] 加载地址失败:', err)
    })
  },

  loadDefaultClient() {
    var that = this
    console.log('[定向预约下单] 加载默认服务对象')
    
    api.getSubjectList().then(function (res) {
      var clients = res.data || res || []
      console.log('[定向预约下单] 服务对象列表响应:', clients)
      
      // 查找默认服务对象或使用第一个
      var defaultClient = clients.find(function (client) { return client.isDefault === 1 }) || clients[0]
      if (defaultClient) {
        that.setData({
          clientName: defaultClient.name || '',
          clientId: defaultClient.id || ''
        })
        console.log('[定向预约下单] 已设置默认服务对象:', defaultClient.name)
      }
    }).catch(function (err) {
      console.log('[定向预约下单] 加载服务对象失败:', err)
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
  onRemarks(e) {
    this.setData({ remarks: e.detail.value })
  },

  toggleTerms() {
    this.setData({ agreedToTerms: !this.data.agreedToTerms })
  },

  onSubmit() {
    var d = this.data
    if (!d.caregiverId) {
      wx.showToast({ title: '缺少护工信息', icon: 'none' })
      return
    }
    if (!d.selectedPackage.id) {
      wx.showToast({ title: '请选择服务', icon: 'none' })
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
    var billingMethodVal = (method && billingKeyToMethod[method.key] != null) ? billingKeyToMethod[method.key] : 2
    var expectStartTime = (d.expectDate && d.expectTimeOnly) ? (d.expectDate + ' ' + d.expectTimeOnly + ':00') : ''
    var that = this
    
    // 构建基础订单数据，添加字符串长度限制
    var basePayload = {
      caregiverId: d.caregiverId,
      contactName: d.contactName.trim().substring(0, 50),
      contactPhone: d.contactPhone.trim().substring(0, 20),
      address: address.substring(0, 200),
      doorNumber: (d.orderDoorNumber || '').trim().substring(0, 50) || undefined,
      packageId: d.selectedPackage.id,
      packageName: ((d.selectedPackage && d.selectedPackage.name) || '').substring(0, 50),
      billingMethod: billingMethodVal,
      unitPrice: method ? method.price : 0,
      buyQuantity: Math.min(Math.max(d.quantity || 1, 1), 50),
      totalAmount: parseFloat(d.totalAmount) || 0,
      clientName: d.clientName.trim().substring(0, 50),
      specialRemark: (d.remarks || '').trim().substring(0, 200) || undefined,
      expectStartTime: expectStartTime
    }
    
    // 获取缓存的经纬度，如果没有则重新获取
    var location = api.getStoredLocation()
    if (!location) {
      console.log('[定向预约下单] 没有缓存的位置信息，重新获取')
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
    
    console.log('[定向预约下单] 使用缓存的位置信息:', location)
    this.submitOrderWithLocation(basePayload, location)
  },

  submitOrderWithLocation(basePayload, location) {
    var payload = Object.assign({}, basePayload, {
      longitude: location.longitude,
      latitude: location.latitude
    })
    
    console.log('[定向预约下单] 提交订单参数:', payload)
    this.setData({ loading: true })
    var that = this
    api.createDirectOrder(payload).then(function () {
      that.setData({ loading: false })
      wx.showToast({ title: '订单已创建', icon: 'success' })
      setTimeout(function () { router.redirectTo({ url: '/pages/order/list/list' }) }, 500)
    }).catch(function (err) {
      that.setData({ loading: false })
      console.error('[定向预约下单] 提交失败:', err)
      wx.showToast({ title: (err && err.message) || '提交失败', icon: 'none' })
    })
  }
})
