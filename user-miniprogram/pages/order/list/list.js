// 订单列表：对接 GET /order/user/page，取消/确认完成调用后端
var constant = require('../../../utils/constant')
var api = require('../../../utils/api.js')
var router = require('../../../utils/router.js')
var orderStatus = constant.orderStatus
var billingMethod = constant.billingMethod

var categoryList = [
  { id: 0, name: '全部' },
  { id: 1, name: '居家陪护' },
  { id: 2, name: '医院陪护' },
  { id: 3, name: '周期护理' },
  { id: 4, name: '家政服务' },
  { id: 5, name: '陪诊服务' },
  { id: 6, name: '母婴护理' }
]

var statusTabs = [
  { label: '全部', value: '' },
  { label: '待支付', value: 1 },
  { label: '待接单', value: 2 },
  { label: '待上门', value: 3 },
  { label: '服务中', value: 4 },
  { label: '待确认', value: 5 },
  { label: '已完成', value: 6 },
  { label: '已取消', value: 7 },
  { label: '已关闭', value: 8 }
]

function formatTime(str) {
  if (!str) return '-'
  return String(str).substring(0, 16).replace('T', ' ')
}

function mapOrderItem(o) {
  var detailAddr = o.detailAddress && o.detailAddress.trim()
    ? o.detailAddress
    : ((o.address || '') + (o.doorNumber ? ' ' + o.doorNumber : '')).trim() || '-'
  return {
    id: o.id,
    orderNo: o.orderNo,
    orderType: o.orderType,
    status: o.status,
    statusText: orderStatus[o.status] || '未知',
    packageName: o.packageName,
    caregiverName: o.caregiverName,
    detailAddress: detailAddr,
    billingText: billingMethod[o.billingMethod] || '',
    buyQuantity: o.buyQuantity,
    totalAmount: o.totalAmount,
    expectStartTimeFmt: formatTime(o.expectStartTime),
    expectStartTime: o.expectStartTime,
    createTime: o.createTime,
    isReviewed: o.isReviewed === true
  }
}

Page({
  data: {
    categoryList: categoryList,
    statusTabs: statusTabs,
    selectedCategory: 0,
    selectedStatus: '',
    list: [],
    loading: false,
    noMore: true,
    showCancelModal: false,
    showCompleteModal: false,
    cancelOrderId: null,
    completeOrderId: null,
    completeOrderNo: '',
    completePackageName: '',
    completeCaregiverName: ''
  },

  onLoad() {
    var app = getApp()
    if (!app.globalData.token && !wx.getStorageSync('token')) {
      router.redirectTo({ url: '/pages/login/login' })
      return
    }
    this.loadList(true)
  },

  onShow() {
    if (this.data.list.length > 0) this.loadList(true)
  },

  onCategory(e) {
    var id = e.currentTarget.dataset.id
    this.setData({ selectedCategory: id, list: [], noMore: false, currentPage: 1 })
    this.loadList(true)
  },

  onStatus(e) {
    var value = e.currentTarget.dataset.value
    this.setData({ selectedStatus: value, list: [], noMore: false, currentPage: 1 })
    this.loadList(true)
  },

  loadList(reset) {
    if (this.data.loading) return
    var that = this
    var categoryId = this.data.selectedCategory
    var status = this.data.selectedStatus
    var current = reset ? 1 : (this.data.currentPage || 0) + 1
    var size = 20
    this.setData({ loading: true })
    var pageParams = { current: current, size: size }
    if (categoryId !== 0 && categoryId != null) pageParams.category = categoryId
    if (status !== '' && status != null) pageParams.status = status
    api.getOrderUserPage(pageParams).then(function (res) {
      var records = (res && res.records) || (res && res.data && res.data.records) || []
      var list = Array.isArray(records) ? records.map(mapOrderItem) : []
      var total = (res && res.total) != null ? res.total : (res && res.data && res.data.total)
      var noMore = list.length < size || (total != null && current * size >= total)
      var nextList = reset ? list : that.data.list.concat(list)
      that.setData({
        list: nextList,
        loading: false,
        noMore: noMore,
        currentPage: current
      }, function () {
        that.mergeReviewFlags()
      })
    }).catch(function (err) {
      that.setData({ loading: false })
      var msg = (err && err.data && err.data.message) || (err && err.message) || '加载失败'
      wx.showToast({ title: msg, icon: 'none' })
    })
  },

  onReachBottom() {
    if (this.data.noMore || this.data.loading) return
    this.loadList(false)
  },

  // 拉取我的评价列表，为已完成订单标记是否已评价（与详情页逻辑一致）
  mergeReviewFlags() {
    var that = this
    api.getReviewMyList({ page: 1, size: 100 }).then(function (res) {
      var reviews = res.records || (res.data && res.data.records) || res || []
      var currentList = that.data.list || []
      var updated = currentList.map(function (item) {
        if (item.status !== 6) return item
        var reviewed = reviews.some(function (r) {
          return (r.orderId != null && r.orderId == item.id) || (r.orderNo && r.orderNo === item.orderNo)
        })
        var next = {}
        for (var k in item) next[k] = item[k]
        next.isReviewed = reviewed
        return next
      })
      that.setData({ list: updated })
    }).catch(function () {
      // 失败时保持原列表，已完成订单默认显示「评价护工」
    })
  },

  goDetail(e) {
    var id = e.currentTarget.dataset.id
    router.navigateTo({ url: '/pages/order/detail/detail?id=' + id })
  },

  pay(e) {
    var id = e.currentTarget.dataset.id
    var item = this.data.list.find(function (o) { return o.id === parseInt(id, 10) })
    if (!item) return
    router.navigateTo({ url: '/pages/payment/payment?orderId=' + id + '&orderNo=' + encodeURIComponent(item.orderNo || '') + '&amount=' + (item.totalAmount || 0) })
  },

  cancel(e) {
    var id = e.currentTarget.dataset.id
    this.setData({ showCancelModal: true, cancelOrderId: id })
  },

  hideCancelModal() {
    this.setData({ showCancelModal: false, cancelOrderId: null })
  },

  confirmCancel() {
    var id = this.data.cancelOrderId
    var that = this
    this.setData({ showCancelModal: false, cancelOrderId: null })
    if (!id) return
    api.cancelOrder(id).then(function () {
      that.loadList(true)
      wx.showToast({ title: '已取消', icon: 'success' })
    }).catch(function () {
      wx.showToast({ title: '取消失败', icon: 'none' })
    })
  },

  contactService() {
    wx.showToast({ title: '客服电话：400-123-4567', icon: 'none' })
  },

  contactCaregiver(e) {
    var id = e.currentTarget.dataset.id
    router.navigateTo({ url: '/pages/order/detail/detail?id=' + id })
  },

  complete(e) {
    var id = e.currentTarget.dataset.id
    var item = this.data.list.find(function (o) { return o.id === parseInt(id, 10) })
    this.setData({
      showCompleteModal: true,
      completeOrderId: id,
      completeOrderNo: item ? item.orderNo : '',
      completePackageName: item ? item.packageName : '',
      completeCaregiverName: item ? (item.caregiverName || '护工') : '护工'
    })
  },

  hideCompleteModal() {
    this.setData({ showCompleteModal: false, completeOrderId: null })
  },

  confirmCompleteOnly() {
    var id = this.data.completeOrderId
    var that = this
    this.setData({ showCompleteModal: false, completeOrderId: null })
    if (id) {
      api.completeOrder(id).then(function () {
        that.loadList(true)
        wx.showToast({ title: '已确认完成', icon: 'success' })
      }).catch(function () {
        wx.showToast({ title: '操作失败', icon: 'none' })
      })
    }
  },

  goReviewFromModal() {
    var id = this.data.completeOrderId
    var orderno = this.data.completeOrderNo || ''
    var packagename = this.data.completePackageName || ''
    var caregivername = this.data.completeCaregiverName || '护工'
    this.setData({ showCompleteModal: false, completeOrderId: null })
    router.navigateTo({ url: '/pages/review/write/write?orderId=' + id + '&orderNo=' + encodeURIComponent(orderno) + '&packageName=' + encodeURIComponent(packagename) + '&caregiverName=' + encodeURIComponent(caregivername) })
  },

  goReview(e) {
    var id = e.currentTarget.dataset.id
    var orderno = e.currentTarget.dataset.orderno || ''
    var packagename = e.currentTarget.dataset.packagename || ''
    var caregivername = e.currentTarget.dataset.caregivername || '护工'
    router.navigateTo({ url: '/pages/review/write/write?orderId=' + id + '&orderNo=' + encodeURIComponent(orderno) + '&packageName=' + encodeURIComponent(packagename) + '&caregiverName=' + encodeURIComponent(caregivername) })
  }
})
