const api = require('../../../utils/api')
const { ORDER_STATUS, PACKAGE_TYPES } = require('../../../utils/constants')
const app = getApp()

Page({
  data: {
    orderId: null,
    order: null,
    loading: true,

    // 状态配置：使用后端数字状态码
    statusMap: {
      1: { text: '待支付', color: '#f97316', bg: '#fff7ed', icon: 'balance-o' },
      2: { text: '待接单', color: '#3b82f6', bg: '#eff6ff', icon: 'clock-o' },
      3: { text: '待上门', color: '#f97316', bg: '#fff7ed', icon: 'clock-o' },
      4: { text: '服务中', color: '#3b82f6', bg: '#eff6ff', icon: 'upgrade' },
      5: { text: '待确认', color: '#a855f7', bg: '#faf5ff', icon: 'info-o' },
      6: { text: '已完成', color: '#16a34a', bg: '#f0fdf4', icon: 'passed' },
      7: { text: '已取消', color: '#6b7280', bg: '#f9fafb', icon: 'cross' },
      8: { text: '已关闭', color: '#6b7280', bg: '#f9fafb', icon: 'close' }
    }
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ orderId: options.id })
      this.loadOrderDetail()
    }
  },

  async loadOrderDetail() {
    try {
      this.setData({ loading: true })
      const res = await api.order.getDetail(this.data.orderId)
      this.setData({ order: res, loading: false })
    } catch (err) {
      console.error('加载订单详情失败:', err)
      wx.showToast({ title: '加载失败', icon: 'none' })
      this.setData({ loading: false })
    }
  },

  onPullDownRefresh() {
    this.loadOrderDetail().finally(() => wx.stopPullDownRefresh())
  },

  // 格式化日期（iOS兼容）
  formatDateTime(dateTime) {
    if (!dateTime) return '-'
    const isoDateTime = dateTime.replace(' ', 'T')
    const date = new Date(isoDateTime)
    if (isNaN(date.getTime())) return '-'
    const y = date.getFullYear()
    const m = String(date.getMonth() + 1).padStart(2, '0')
    const d = String(date.getDate()).padStart(2, '0')
    const h = String(date.getHours()).padStart(2, '0')
    const min = String(date.getMinutes()).padStart(2, '0')
    return `${y}-${m}-${d} ${h}:${min}`
  },

  // 拨打电话
  onCallPhone(e) {
    const phone = e.currentTarget.dataset.phone
    if (phone) {
      wx.makePhoneCall({ phoneNumber: phone })
    }
  },

  // 查看地址
  onViewLocation() {
    const order = this.data.order
    if (order && order.latitude && order.longitude) {
      wx.openLocation({
        latitude: Number(order.latitude),
        longitude: Number(order.longitude),
        name: '服务地址',
        address: order.address || '',
        scale: 15
      })
    }
  },

  // 上门打卡/开始服务
  onStartService() {
    wx.showModal({
      title: '确认上门',
      content: '确认已到达服务地点，开始提供服务吗？',
      success: async (res) => {
        if (!res.confirm) return
        try {
          wx.showLoading({ title: '处理中...', mask: true })
          
          const location = await this.getCurrentLocation()
          await api.order.startService(this.data.orderId, {
            longitude: location.longitude,
            latitude: location.latitude
          })

          wx.showToast({ title: '开始服务成功', icon: 'success' })
          this.loadOrderDetail()
        } catch (err) {
          wx.showToast({ title: err.message || '操作失败', icon: 'none' })
        } finally {
          wx.hideLoading()
        }
      }
    })
  },

  // 完成服务
  onFinishService() {
    wx.showModal({
      title: '完成服务',
      content: '确认已完成全部服务内容吗？',
      success: async (res) => {
        if (!res.confirm) return
        try {
          wx.showLoading({ title: '处理中...', mask: true })
          await api.order.finishService(this.data.orderId)
          wx.showToast({ title: '服务已完成', icon: 'success' })
          this.loadOrderDetail()
        } catch (err) {
          wx.showToast({ title: err.message || '操作失败', icon: 'none' })
        } finally {
          wx.hideLoading()
        }
      }
    })
  },

  getCurrentLocation() {
    return new Promise((resolve, reject) => {
      wx.getLocation({
        type: 'gcj02',
        success: resolve,
        fail: () => {
          wx.showToast({ title: '获取位置失败', icon: 'none' })
          reject(new Error('获取位置失败'))
        }
      })
    })
  }
})
