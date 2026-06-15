// 订单详情：对接 GET /order/user/{orderId}，取消/确认完成调用后端
var constant = require('../../../utils/constant')
var api = require('../../../utils/api.js')
var router = require('../../../utils/router.js')
var billingMethod = constant.billingMethod

// 需要自动轮询刷新的订单状态（进行中的状态）
var POLLING_STATUS = [2, 3, 4, 5] // 待接单、待上门、服务中、待确认
var POLLING_INTERVAL = 10 * 1000   // 轮询间隔10秒

var statusMap = {
  1: { text: '待支付', icon: 'https://zlisten-1323337406.cos.ap-beijing.myqcloud.com/images/%E7%AD%89%E5%BE%85%E6%94%AF%E4%BB%98.png', description: '请尽快完成支付' },
  2: { text: '待接单', icon: 'https://zlisten-1323337406.cos.ap-beijing.myqcloud.com/images/%E5%BE%85%E6%8E%A5%E5%8D%95.png', description: '' },
  3: { text: '待上门', icon: 'https://zlisten-1323337406.cos.ap-beijing.myqcloud.com/images/%E5%BE%85%E4%B8%8A%E9%97%A8.png', description: '护工即将上门服务' },
  4: { text: '服务中', icon: 'https://zlisten-1323337406.cos.ap-beijing.myqcloud.com/images/%E6%9C%8D%E5%8A%A1%E4%B8%AD.png', description: '护工正在为您服务' },
  5: { text: '待确认', icon: 'https://zlisten-1323337406.cos.ap-beijing.myqcloud.com/images/%E7%AD%89%E5%BE%85%E7%A1%AE%E8%AE%A4.png', description: '请确认服务完成情况' },
  6: { text: '已完成', icon: 'https://zlisten-1323337406.cos.ap-beijing.myqcloud.com/images/%E5%B7%B2%E5%AE%8C%E6%88%90.png', description: '服务已完成' },
  7: { text: '已取消', icon: 'https://zlisten-1323337406.cos.ap-beijing.myqcloud.com/images/%E5%B7%B2%E5%8F%96%E6%B6%88.png', description: '订单已取消' },
  8: { text: '已关闭', icon: 'https://zlisten-1323337406.cos.ap-beijing.myqcloud.com/images/%E5%B7%B2%E5%85%B3%E9%97%AD%20%281%29.png', description: '订单已关闭' }
}

function getStatusDescription(status, orderTypeNum) {
  if (status === 2) {
    return orderTypeNum === 1 ? '正在为您匹配合适的护工' : '请等待该护工接单'
  }
  return (statusMap[status] && statusMap[status].description) || ''
}

function formatTime(str) {
  if (!str) return '-'
  return String(str).substring(0, 16).replace('T', ' ')
}

function billingUnit(billingMethodVal) {
  return billingMethodVal === 1 ? '月' : billingMethodVal === 2 ? '天' : billingMethodVal === 3 ? '小时' : '次'
}

/** 将后端 OrderDetailVO 转为页面详情结构；地址字段为 address、doorNumber、detailAddress（快照） */
function mapOrderDetail(o) {
  if (!o) return {}
  var status = o.status
  var info = statusMap[status] || statusMap[1]
  var bm = o.billingMethod
  var full = (o.detailAddress && o.detailAddress.trim()) ? o.detailAddress : ((o.address || '') + (o.doorNumber ? ' ' + o.doorNumber : '')).trim() || ''
  return {
    id: o.id,
    orderNo: o.orderNo,
    orderType: o.orderType,
    orderTypeText: o.orderType === 1 ? '系统匹配' : '定向预约',
    status: status,
    statusText: info.text,
    contactName: o.contactName || '',
    contactPhone: o.contactPhone || '',
    clientName: o.clientName || '',
    clientGender: o.clientGender,
    clientGenderText: o.clientGender === 1 ? '男' : '女',
    clientAge: o.clientAge,
    clientHeight: o.clientHeight,
    clientWeight: o.clientWeight,
    intellectStatus: o.intellectStatus || '',
    selfCareAbility: o.selfCareAbility || '',
    medicalHistory: o.medicalHistory || '',
    remarks: o.remarks || '',
    address: o.address || '',
    doorNumber: o.doorNumber || '',
    detailAddress: o.detailAddress || '',
    fullAddress: full,
    packageName: o.packageName || '',
    billingMethod: bm,
    billingText: billingMethod[bm] || '',
    billingUnit: billingUnit(bm),
    unitPrice: o.unitPrice != null ? o.unitPrice : (o.buyQuantity ? Math.round((o.totalAmount || 0) / o.buyQuantity) : o.totalAmount),
    buyQuantity: o.buyQuantity,
    totalAmount: o.totalAmount,
    expectStartTime: o.expectStartTime || '',
    expectStartTimeFmt: formatTime(o.expectStartTime),
    createTime: o.createTime || '',
    createTimeFmt: formatTime(o.createTime),
    realStartTime: o.realStartTime || null,
    realStartTimeFmt: formatTime(o.realStartTime),
    finishTime: o.finishTime || null,
    finishTimeFmt: formatTime(o.finishTime),
    cancelTime: o.cancelTime || null,
    cancelTimeFmt: formatTime(o.cancelTime),
    cancelReason: o.cancelReason || null,
    caregiverId: o.caregiverId || null,
    caregiverName: o.caregiverName || '',
    caregiverPhone: o.caregiverPhone || '',
    caregiverAvatar: o.caregiverAvatar || '',
    reqGender: o.reqGender,
    reqGenderText: o.reqGender === 1 ? '男' : o.reqGender === 2 ? '女' : '不限',
    reqWorkYears: o.reqWorkYears,
    reqWorkYearsText: o.reqWorkYears != null ? o.reqWorkYears + '年' : '不限',
    reqNativePlace: o.reqNativePlace || null,
    specialRemark: o.specialRemark || null,
    matchingRadius: o.matchingRadius,
    matchingRadiusText: o.matchingRadius != null ? o.matchingRadius + 'km' : ''
  }
}

Page({
  data: {
    id: null,
    detail: {},
    statusInfo: {},
    statusDesc: '',
    showBottomBar: true,
    showCancelModal: false,
    showCompleteModal: false,
    isReviewed: false // 是否已评价
  },

  // 轮询定时器ID（非 data 属性，避免 setData 开销）
  _pollingTimer: null,

  onLoad(options) {
    var id = options.id
    if (!id) {
      wx.showToast({ title: '参数错误', icon: 'none' })
      return
    }
    this.setData({ id: id })
    this.loadDetail()
    this.checkReviewStatus(id)
  },

  onShow() {
    // 从支付页面返回或其他页面进入时，重新加载订单详情以获取最新状态
    if (this.data.id) {
      console.log('[订单详情] 重新加载订单状态')
      this.loadDetail()
      this.checkReviewStatus(this.data.id)
    }
  },

  onHide() {
    this.stopPolling()
  },

  onUnload() {
    this.stopPolling()
  },

  loadDetail() {
    var id = this.data.id
    var that = this
    console.log('[订单详情] 加载订单详情:', id)
    
    api.getOrderDetail(id).then(function (res) {
      var raw = res.data || res
      console.log('[订单详情] 订单详情响应:', raw)
      var detail = mapOrderDetail(raw)
      var statusInfo = statusMap[detail.status] || statusMap[1]
      var statusDesc = getStatusDescription(detail.status, detail.orderType)
      var showBottomBar = [1, 2, 3, 4, 5, 6].indexOf(detail.status) >= 0
      that.setData({
        detail: detail,
        statusInfo: statusInfo,
        statusDesc: statusDesc,
        showBottomBar: showBottomBar
      })
      // 进行中的订单状态自动轮询刷新，终态则停止
      if (POLLING_STATUS.indexOf(detail.status) >= 0) {
        that.startPolling()
      } else {
        that.stopPolling()
      }
    }).catch(function (err) {
      console.error('[订单详情] 加载失败:', err)
      
      if (err && (err.code === 500 && err.message && err.message.indexOf('无权') >= 0)) {
        wx.showModal({
          title: '权限错误',
          content: '您无权查看该订单，可能是订单不存在或不属于您',
          showCancel: false,
          success: function () {
            wx.navigateBack()
          }
        })
      } else {
        wx.showToast({ title: '加载失败', icon: 'none' })
      }
    })
  },

  // 检查订单是否已评价
  checkReviewStatus(orderId) {
    var that = this
    console.log('[订单详情] 检查订单评价状态:', orderId)
    
    api.getReviewMyList({ page: 1, size: 100 }).then(function (res) {
      var reviews = res.records || res.data || res || []
      console.log('[订单详情] 我的评价列表:', reviews.length, '条')
      
      // 检查是否有对应订单的评价（优先用订单ID，兜底用订单号）
      var hasReviewed = reviews.some(function (review) {
        return (review.orderId && review.orderId == orderId) || 
               (review.orderNo && review.orderNo === that.data.detail.orderNo)
      })
      
      console.log('[订单详情] 订单', orderId, '评价状态:', hasReviewed ? '已评价' : '未评价')
      that.setData({ isReviewed: hasReviewed })
    }).catch(function (err) {
      console.log('[订单详情] 检查评价状态失败:', err)
      // 失败时默认未评价，允许用户尝试评价
      that.setData({ isReviewed: false })
    })
  },

  pay() {
    var d = this.data.detail
    router.navigateTo({ url: '/pages/payment/payment?orderId=' + d.id + '&orderNo=' + (d.orderNo || d.id) + '&amount=' + (d.totalAmount || 0) })
  },

  cancel() {
    this.setData({ showCancelModal: true })
  },

  hideCancelModal() {
    this.setData({ showCancelModal: false })
  },

  confirmCancel() {
    var id = this.data.id
    var that = this
    this.setData({ showCancelModal: false })
    api.cancelOrder(id).then(function () {
      wx.showToast({ title: '已取消', icon: 'success' })
      setTimeout(function () { wx.navigateBack() }, 500)
    }).catch(function () {
      wx.showToast({ title: '取消失败', icon: 'none' })
    })
  },

  /** 启动订单状态轮询（仅进行中的订单） */
  startPolling() {
    if (this._pollingTimer) return // 已在轮询中，不重复启动
    var that = this
    this._pollingTimer = setInterval(function () {
      console.log('[订单详情] 定时轮询刷新')
      that.loadDetail()
    }, POLLING_INTERVAL)
    console.log('[订单详情] 轮询已启动，间隔', POLLING_INTERVAL / 1000, '秒')
  },

  /** 停止订单状态轮询 */
  stopPolling() {
    if (this._pollingTimer) {
      clearInterval(this._pollingTimer)
      this._pollingTimer = null
      console.log('[订单详情] 轮询已停止')
    }
  },

  contactService() {
    wx.showToast({ title: '客服电话：400-123-4567', icon: 'none' })
  },

  contactCaregiver() {
    var d = this.data.detail
    if (d.caregiverPhone) {
      wx.makePhoneCall({ phoneNumber: d.caregiverPhone })
    } else {
      wx.showToast({ title: '暂无护工电话', icon: 'none' })
    }
  },

  complete() {
    this.setData({ showCompleteModal: true })
  },

  hideCompleteModal() {
    this.setData({ showCompleteModal: false })
  },

  confirmCompleteOnly() {
    var id = this.data.id
    var that = this
    this.setData({ showCompleteModal: false })
    api.completeOrder(id).then(function () {
      wx.showToast({ title: '已确认完成', icon: 'success' })
      that.loadDetail()
    }).catch(function () {
      wx.showToast({ title: '操作失败', icon: 'none' })
    })
  },

  goCaregiver(e) {
    var id = e.currentTarget.dataset.id
    if (id) router.navigateTo({ url: '/pages/caregiver/detail/detail?id=' + id })
  },

  goReview() {
    this.setData({ showCompleteModal: false })
    var d = this.data.detail
    var q = 'orderId=' + d.id + '&orderNo=' + encodeURIComponent(d.orderNo || '') + '&packageName=' + encodeURIComponent(d.packageName || '') + '&caregiverName=' + encodeURIComponent(d.caregiverName || '护工')
    router.navigateTo({ url: '/pages/review/write/write?' + q })
  }
})
