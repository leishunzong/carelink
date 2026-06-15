const api = require('../../utils/api')
const location = require('../../utils/location')
const websocket = require('../../utils/websocket')
const app = getApp()

Page({
  data: {
    city: '北京',
    workState: 1, // 1-接单中(RECEIVING) 2-服务中(SERVING) 3-休息中(RESTING)
    location: '',
    detailedAddress: '', // 详细地址
    currentLocation: null, // 当前位置信息
    orderList: [],
    loading: false,
    refreshing: false,
    page: 1,
    pageSize: 10,
    wsSubscriptionId: null
  },

  onLoad() {
    // 检查登录状态
    if (!app.globalData.hasLogin) {
      wx.redirectTo({
        url: '/pages/login/login'
      })
      return
    }

    // 初始化工作状态
    const userInfo = app.globalData.userInfo
    this.setData({
      workState: userInfo?.workState || 1,
      city: userInfo?.city || '北京'
    })

    // 如果当前是接单中状态，连接 WebSocket
    if (this.data.workState === 1) {
      this.connectWebSocket()
    }
  },

  onUnload() {
    // 页面卸载时断开 WebSocket
    this.disconnectWebSocket()
  },

  // 连接 WebSocket 并订阅订单推送
  connectWebSocket() {
    const userInfo = app.globalData.userInfo
    if (!userInfo || !userInfo.id) {
      console.warn('[Home] 用户信息不完整，无法连接 WebSocket')
      return
    }

    const wsUrl = app.globalData.baseURL + '/ws'
    const token = app.globalData.token

    websocket.connect(wsUrl, token, () => {
      // 连接成功后订阅 /topic/order/{caregiverId}
      const destination = '/topic/order/' + userInfo.id
      const subId = websocket.subscribe(destination, (orderData) => {
        console.log('[Home] 收到推送订单:', orderData)
        this.handleOrderPush(orderData)
      })
      this.setData({ wsSubscriptionId: subId })
      console.log('[Home] 已订阅订单推送:', destination)
    })
  },

  // 断开 WebSocket
  disconnectWebSocket() {
    const { wsSubscriptionId } = this.data
    if (wsSubscriptionId) {
      websocket.unsubscribe(wsSubscriptionId)
      this.setData({ wsSubscriptionId: null })
    }
    websocket.close()
  },

  // 处理推送的订单
  handleOrderPush(orderData) {
    if (!orderData) return

    // 如果当前不是接单中状态，忽略推送
    if (this.data.workState !== 1) return

    // 格式化推送的订单数据（WebSocket 推送的 OrderPushVO 字段名是 orderId，需映射为 id）
    const formatted = {
      ...orderData,
      id: orderData.orderId || orderData.id,
      billingMethodText: this.getBillingMethodText(orderData.billingMethod),
      expectStartTimeText: this.formatDateTime(orderData.expectStartTime)
    }

    // 将推送的订单添加到列表顶部
    const orderList = [formatted, ...this.data.orderList]
    this.setData({ orderList })

    // 震动提示
    wx.vibrateShort({ type: 'heavy' })

    // 弹出提示
    wx.showToast({
      title: '收到新订单推送',
      icon: 'none',
      duration: 2000
    })
  },

  onShow() {
    // 更新 tabBar 选中状态
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().updateActive(0)
    }
    
    this.syncWorkState()
    this.loadData()
  },

  // 从后端同步最新工作状态
  async syncWorkState() {
    try {
      const info = await api.caregiver.getInfo()
      if (info && info.workState != null) {
        const oldState = this.data.workState
        const newState = info.workState

        if (oldState !== newState) {
          this.setData({ workState: newState })
          if (app.globalData.userInfo) {
            app.globalData.userInfo.workState = newState
          }

          // 状态变为接单中时连接 WebSocket，否则断开
          if (newState === 1) {
            this.connectWebSocket()
          } else if (oldState === 1) {
            this.disconnectWebSocket()
          }
        }
      }
    } catch (err) {
      console.error('同步工作状态失败:', err)
    }
  },

  async loadData() {
    await Promise.all([
      this.getCurrentLocation(),
      this.loadOrderPool()
    ])
  },

  // 获取当前位置（使用腾讯位置服务）
  async getCurrentLocation() {
    try {
      // 调用腾讯位置服务获取详细地址
      const locationInfo = await location.getCurrentLocation({
        get_poi: 0 // 不返回周边POI
      })
      
      // 显示地址信息
      const locationText = locationInfo.district 
        ? `${locationInfo.district} ${locationInfo.street}`.trim()
        : locationInfo.city || locationInfo.recommendAddress
      
      this.setData({
        location: locationText,
        detailedAddress: locationInfo.recommendAddress,
        currentLocation: locationInfo,
        city: locationInfo.city || this.data.city
      })
      
      // 保存到全局
      app.globalData.location = locationInfo
      
      console.log('当前位置:', locationInfo)
      
    } catch (err) {
      console.error('获取位置失败:', err)
      this.setData({ 
        location: '定位失败',
        detailedAddress: '无法获取位置信息'
      })
    }
  },

  // 点击城市重新定位
  async relocateCity() {
    wx.showLoading({
      title: '正在定位...',
      mask: true
    })
    
    try {
      const locationInfo = await location.getCurrentLocation({
        get_poi: 0
      })
      
      const newCity = locationInfo.city || this.data.city
      const locationText = locationInfo.district 
        ? `${locationInfo.district} ${locationInfo.street}`.trim()
        : locationInfo.city || locationInfo.recommendAddress
      
      this.setData({
        city: newCity,
        location: locationText,
        detailedAddress: locationInfo.recommendAddress,
        currentLocation: locationInfo
      })
      
      app.globalData.location = locationInfo
      
      wx.hideLoading()
      wx.showToast({
        title: `定位成功：${newCity}`,
        icon: 'none',
        duration: 2000
      })
    } catch (err) {
      console.error('重新定位失败:', err)
      wx.hideLoading()
      wx.showToast({
        title: '定位失败，请检查定位权限',
        icon: 'none'
      })
    }
  },

  // 更新位置（同时更新到服务器）
  async updateLocation() {
    wx.showToast({
      title: '正在更新位置...',
      icon: 'loading',
      duration: 1500
    })
    
    try {
      // 获取最新位置（使用腾讯位置服务）
      const locationInfo = await location.getCurrentLocation({
        get_poi: 0
      })
      
      // 更新到服务器
      await api.caregiver.updateLocation(locationInfo.longitude, locationInfo.latitude)

      // 更新界面显示
      const locationText = locationInfo.district 
        ? `${locationInfo.district} ${locationInfo.street}`.trim()
        : locationInfo.city || locationInfo.recommendAddress
      
      this.setData({
        location: locationText,
        detailedAddress: locationInfo.recommendAddress,
        currentLocation: locationInfo,
        city: locationInfo.city || this.data.city
      })
      
      // 保存到全局
      app.globalData.location = locationInfo
      
      setTimeout(() => {
        wx.showToast({
          title: '位置更新成功',
          icon: 'success'
        })
      }, 1000)
      
      console.log('位置更新成功:', locationInfo)
      
    } catch (err) {
      console.error('更新位置失败:', err)
      wx.showToast({
        title: '位置更新失败',
        icon: 'none'
      })
    }
  },

  // 切换到接单状态
  async switchToReceiving() {
    const { workState } = this.data
    
    // 如果当前是服务中，不允许切换
    if (workState === 2) {
      wx.showToast({
        title: '服务中无法切换状态，请先完成当前服务',
        icon: 'none',
        duration: 2000
      })
      return
    }
    
    // 如果已经是接单中，无需切换
    if (workState === 1) {
      return
    }

    try {
      await api.caregiver.updateWorkState(1)
      
      this.setData({ workState: 1 })
      
      // 更新全局状态
      if (app.globalData.userInfo) {
        app.globalData.userInfo.workState = 1
      }

      // 切换到接单中时，连接 WebSocket 订阅订单推送
      this.connectWebSocket()

      wx.showToast({
        title: '已切换至接单中',
        icon: 'success'
      })
      
    } catch (err) {
      console.error('切换状态失败:', err)
    }
  },

  // 切换到休息状态
  async switchToResting() {
    const { workState } = this.data
    
    // 如果当前是服务中，不允许切换
    if (workState === 2) {
      wx.showToast({
        title: '服务中无法切换状态，请先完成当前服务',
        icon: 'none',
        duration: 2000
      })
      return
    }
    
    // 如果已经是休息中，无需切换
    if (workState === 3) {
      return
    }

    try {
      await api.caregiver.updateWorkState(3)
      
      this.setData({ workState: 3 })
      
      // 更新全局状态
      if (app.globalData.userInfo) {
        app.globalData.userInfo.workState = 3
      }

      // 切换到休息中时，断开 WebSocket
      this.disconnectWebSocket()

      wx.showToast({
        title: '已切换至休息中',
        icon: 'success'
      })
      
    } catch (err) {
      console.error('切换状态失败:', err)
    }
  },

  // 加载接单池订单
  async loadOrderPool(isRefresh = false) {
    if (this.data.loading) return

    this.setData({ loading: true })

    try {
      const page = isRefresh ? 1 : this.data.page
      
      // 获取待接单的订单（状态为2）
      const res = await api.order.getMyOrders({
        current: page,
        size: this.data.pageSize,
        status: 2 // 待接单
      })

      const records = res.records || []
      // 格式化订单数据
      const newOrders = records.map(order => ({
        ...order,
        billingMethodText: this.getBillingMethodText(order.billingMethod),
        expectStartTimeText: this.formatDateTime(order.expectStartTime)
      }))
      const orderList = isRefresh ? newOrders : [...this.data.orderList, ...newOrders]

      this.setData({
        orderList: orderList,
        page: page + 1,
        loading: false,
        refreshing: false
      })

    } catch (err) {
      console.error('加载订单失败:', err)
      this.setData({
        loading: false,
        refreshing: false
      })
    }
  },

  // 下拉刷新
  async onPullDownRefresh() {
    this.setData({ 
      refreshing: true,
      page: 1,
      orderList: []
    })
    await this.loadData()
  },

  // 计费方式文本
  getBillingMethodText(billingMethod) {
    const map = { 1: '月', 2: '天', 3: '小时', 4: '次' }
    return map[billingMethod] || ''
  },

  // 格式化日期（iOS兼容），如果时分都为0则只显示日期
  formatDateTime(dateTime) {
    if (!dateTime) return '-'
    const isoDateTime = dateTime.replace(' ', 'T')
    const date = new Date(isoDateTime)
    if (isNaN(date.getTime())) return dateTime
    const y = date.getFullYear()
    const m = String(date.getMonth() + 1).padStart(2, '0')
    const d = String(date.getDate()).padStart(2, '0')
    const h = date.getHours()
    const min = date.getMinutes()
    if (h === 0 && min === 0) {
      return `${y}-${m}-${d}`
    }
    return `${y}-${m}-${d} ${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}`
  },

  // 抢单/接单
  handleGrabOrder(e) {
    const { id, type } = e.currentTarget.dataset
    const { workState } = this.data

    // 如果当前是休息中，不能接单
    if (workState === 3) {
      wx.showToast({
        title: '休息状态无法接单，请先切换到接单状态',
        icon: 'none',
        duration: 2000
      })
      return
    }

    // 如果当前已经在服务中，不能接新订单
    if (workState === 2) {
      wx.showToast({
        title: '您当前正在服务中，无法接新订单',
        icon: 'none',
        duration: 2000
      })
      return
    }

    const actionText = type === 1 ? '抢单' : '接单'

    wx.showModal({
      title: `确认${actionText}`,
      content: `确定要${actionText}吗？`,
      confirmText: `确认${actionText}`,
      success: async (res) => {
        if (res.confirm) {
          try {
            wx.showLoading({
              title: `${actionText}中...`,
              mask: true
            })

            await api.order.grab(id)

            wx.hideLoading()
            
            // 接单成功后，自动切换到服务中状态
            this.setData({ workState: 2 })
            
            if (app.globalData.userInfo) {
              app.globalData.userInfo.workState = 2
            }

            wx.showToast({
              title: `${actionText}成功！已自动切换到服务中状态`,
              icon: 'success',
              duration: 2000
            })

            // 刷新订单列表
            setTimeout(() => {
              this.setData({
                page: 1,
                orderList: []
              })
              this.loadOrderPool(true)
            }, 1500)

          } catch (err) {
            console.error(`${actionText}失败:`, err)
            wx.hideLoading()
          }
        }
      }
    })
  }
})
