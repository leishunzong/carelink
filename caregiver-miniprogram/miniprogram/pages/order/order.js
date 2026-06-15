const api = require('../../utils/api')

// 订单状态映射
const ORDER_STATUS = {
  1: '待支付',
  2: '待接单',
  3: '待上门',
  4: '服务中',
  5: '待确认',
  6: '已完成',
  7: '已取消',
  8: '已关闭'
}

// 订单类型映射
const ORDER_TYPE = {
  1: '系统匹配',
  2: '定向预约'
}

// 护工可见的订单状态
const CAREGIVER_ORDER_STATUS = [3, 4, 5, 6, 7]

// 服务包类型映射
const SERVICE_PACKAGE_CATEGORIES = {
  1: '居家陪护',
  2: '医院陪护',
  3: '周期护理',
  4: '家政服务',
  5: '陪诊服务',
  6: '母婴护理'
}

// 计费方式映射
const BILLING_METHOD = {
  1: '按月',
  2: '按天',
  3: '按小时',
  4: '按次'
}

// 计费方式单位
const BILLING_UNIT = {
  1: '月',
  2: '天',
  3: '小时',
  4: '次'
}

Page({
  data: {
    selectedCategory: null, // 选中的服务包类型
    selectedStatus: null,   // 选中的订单状态
    allOrders: [],          // 所有订单
    orderList: [],          // 过滤后的订单列表
    loading: false
  },

  onLoad() {
    this.loadOrders()
  },

  onShow() {
    // 更新 tabBar 选中状态
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().updateActive(2)
    }
    
    // 每次显示页面都刷新订单
    this.loadOrders()
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.loadOrders().finally(() => {
      wx.stopPullDownRefresh()
    })
  },

  // 选择服务包类型
  selectCategory(e) {
    const value = e.currentTarget.dataset.value
    this.setData({
      selectedCategory: value === 'null' || value === null ? null : Number(value)
    }, () => {
      this.filterOrders()
    })
  },

  // 选择订单状态
  selectStatus(e) {
    const value = e.currentTarget.dataset.value
    this.setData({
      selectedStatus: value === 'null' || value === null ? null : Number(value)
    }, () => {
      this.filterOrders()
    })
  },

  // 过滤订单
  filterOrders() {
    const { allOrders, selectedCategory, selectedStatus } = this.data
    
    let filtered = allOrders.filter(order => {
      // 只显示护工可见的订单状态
      if (!CAREGIVER_ORDER_STATUS.includes(order.status)) {
        return false
      }

      // 按服务包类型筛选（使用category字段）
      if (selectedCategory !== null && order.category !== selectedCategory) {
        return false
      }

      // 按订单状态筛选
      if (selectedStatus !== null && order.status !== selectedStatus) {
        return false
      }

      return true
    })

    // 处理订单数据，添加显示所需的字段
    filtered = filtered.map(order => {
      // 拼接完整地址
      const fullAddress = order.doorNumber 
        ? `${order.address} ${order.doorNumber}` 
        : order.address
      
      // 格式化时间
      const expectStartTime = order.expectStartTime 
        ? this.formatDateTime(order.expectStartTime)
        : '-'

      return {
        ...order,
        // 文本转换
        orderTypeText: ORDER_TYPE[order.orderType] || '未知',
        statusText: ORDER_STATUS[order.status],
        categoryText: SERVICE_PACKAGE_CATEGORIES[order.category] || '其他',
        billingMethodText: BILLING_METHOD[order.billingMethod] || '',
        billingUnit: BILLING_UNIT[order.billingMethod] || '',
        // 地址处理
        fullAddress,
        // 时间格式化
        expectStartTime,
        // 单价（后端返回的可能是unitPrice，需要适配）
        unitPrice: order.unitPrice || order.price || 0,
        // 操作按钮
        buttonText: this.getOrderButtonText(order.status)
      }
    })

    this.setData({
      orderList: filtered
    })
  },

  // 格式化日期时间，如果时分都为0则只显示日期
  formatDateTime(dateTime) {
    if (!dateTime) return '-'
    
    // iOS兼容性：将空格替换为T，支持标准ISO 8601格式
    const isoDateTime = dateTime.replace(' ', 'T')
    const date = new Date(isoDateTime)
    
    // 检查日期是否有效
    if (isNaN(date.getTime())) {
      console.error('Invalid date:', dateTime)
      return '-'
    }
    
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hour = date.getHours()
    const minute = date.getMinutes()
    
    if (hour === 0 && minute === 0) {
      return `${year}-${month}-${day}`
    }
    return `${year}-${month}-${day} ${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`
  },

  // 获取订单按钮文字
  getOrderButtonText(status) {
    switch (status) {
      case 3: // 待上门
        return '上门'
      case 4: // 服务中
        return '完成'
      default:
        return ''
    }
  },

  // 加载订单列表
  async loadOrders() {
    this.setData({ loading: true })

    try {
      const res = await api.order.getMyOrders({
        current: 1,
        size: 100
      })

      this.setData({
        allOrders: res.records || []
      }, () => {
        this.filterOrders()
      })
    } catch (err) {
      console.error('加载订单失败:', err)
      // Toast 已在 request.js 中统一处理
    } finally {
      this.setData({ loading: false })
    }
  },

  // 跳转到订单详情
  goToOrderDetail(e) {
    const { id } = e.currentTarget.dataset
    wx.navigateTo({
      url: `/pages/order/order-detail/order-detail?id=${id}`
    })
  },

  // 处理订单操作
  handleOrderAction(e) {
    const { id, status } = e.currentTarget.dataset

    if (status === 3) {
      // 待上门 - 上门打卡
      this.startService(id)
    } else if (status === 4) {
      // 服务中 - 完成服务
      this.finishService(id)
    }
  },

  // 上门打卡/开始服务
  async startService(orderId) {
    wx.showModal({
      title: '确认上门',
      content: '请确认已到达服务地点。系统将验证您的位置是否在服务地址500米范围内。',
      success: async (res) => {
        if (res.confirm) {
          try {
            wx.showLoading({
              title: '定位中...',
              mask: true
            })

            // 获取当前位置
            const location = await this.getCurrentLocation()

            wx.showLoading({
              title: '打卡中...',
              mask: true
            })

            // 调用上门打卡接口
            await api.order.startService(orderId, {
              longitude: location.longitude,
              latitude: location.latitude
            })

            wx.hideLoading()
            
            wx.showToast({
              title: '上门打卡成功',
              icon: 'success'
            })
            
            // 刷新订单列表
            setTimeout(() => {
              this.loadOrders()
            }, 1500)

          } catch (err) {
            wx.hideLoading()
            console.error('上门打卡失败:', err)
            
            // 可能是距离超出范围
            if (err.message && err.message.includes('距离')) {
              wx.showModal({
                title: '打卡失败',
                content: '您当前位置距离服务地址超过500米，请到达服务地点后再打卡。',
                showCancel: false
              })
            }
          }
        }
      }
    })
  },

  // 获取当前位置
  getCurrentLocation() {
    return new Promise((resolve, reject) => {
      wx.getLocation({
        type: 'gcj02',
        success: (res) => {
          resolve({
            latitude: res.latitude,
            longitude: res.longitude
          })
        },
        fail: (err) => {
          wx.showToast({
            title: '获取位置失败',
            icon: 'none'
          })
          reject(err)
        }
      })
    })
  },

  // 完成服务
  finishService(orderId) {
    wx.showModal({
      title: '确认完成',
      content: '确认服务已完成？完成后订单将进入待确认状态。',
      success: async (res) => {
        if (res.confirm) {
          try {
            wx.showLoading({
              title: '提交中...',
              mask: true
            })

            await api.order.finishService(orderId)

            wx.hideLoading()
            
            wx.showToast({
              title: '服务已完成',
              icon: 'success'
            })
            
            // 刷新订单列表
            setTimeout(() => {
              this.loadOrders()
            }, 1500)

          } catch (err) {
            wx.hideLoading()
            console.error('完成服务失败:', err)
          }
        }
      }
    })
  }
})
