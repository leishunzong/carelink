// 服务包详情：对接 GET /package/{id}
var api = require('../../../utils/api.js')
var router = require('../../../utils/router.js')

function buildMethods(p) {
  var methods = []
  if (p.allowMonth && p.priceMonth != null) methods.push({ name: '按月', price: p.priceMonth, unit: '月' })
  if (p.allowDay && p.priceDay != null) methods.push({ name: '按天', price: p.priceDay, unit: '天' })
  if (p.allowHour && p.priceHour != null) methods.push({ name: '按小时', price: p.priceHour, unit: '小时' })
  if (p.allowTimes && p.priceTimes != null) methods.push({ name: '按次', price: p.priceTimes, unit: '次' })
  return methods.length ? methods : [{ name: '按次', price: 0, unit: '次' }]
}

Page({
  data: {
    id: null,
    caregiverId: '',
    detail: {}
  },

  onLoad(options) {
    var id = options.id
    var caregiverId = options.caregiverId || ''
    if (!id) {
      wx.showToast({ title: '参数错误', icon: 'none' })
      return
    }
    this.setData({ id: id, caregiverId: caregiverId })
    this.loadDetail()
  },

  loadDetail() {
    var id = this.data.id
    var that = this
    api.getPackageDetail(id).then(function (res) {
      var p = res.data || res
      var methods = buildMethods(p)
      that.setData({
        detail: {
          id: p.id,
          name: p.name,
          description: p.description || '',
          detailedDescription: p.detailedDescription || p.detail || '',
          detail: p.detail || '',
          coverImage: p.coverImage || p.image || '',
          image: p.coverImage || p.image || '',
          sales: p.sales || 0,
          methods: methods
        }
      })
    }).catch(function () {
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  goBook() {
    var id = this.data.id
    var caregiverId = this.data.caregiverId
    if (caregiverId) {
      router.navigateTo({ url: '/pages/order/direct/direct?caregiverId=' + caregiverId + '&packageId=' + id })
    } else {
      router.navigateTo({ url: '/pages/order/create/create?packageId=' + id })
    }
  }
})
