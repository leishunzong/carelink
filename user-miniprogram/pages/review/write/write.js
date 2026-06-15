// 写评价：对接 GET /order/user/{orderId}、GET /tag/list、POST /review/user/create
var api = require('../../../utils/api.js')

Page({
  data: {
    orderId: '',
    orderNo: '',
    packageName: '基础陪护',
    caregiverName: '护工',
    caregiverId: null,
    rating: 5,
    reviewType: 'positive',
    selectedTags: [],
    tagList: [],
    tagIdMap: {},
    selectedTagFlags: [], // 与 tagList 一一对应，true 表示该项已选中，用于可靠高亮
    content: '',
    isAnonymous: false
  },

  onLoad(options) {
    var orderId = (options.orderId || '').trim()
    var orderNo = (options.orderNo || ('ORD' + Date.now())).trim()
    var packageName = (options.packageName || '护理服务').trim()
    var caregiverName = (options.caregiverName || '护工').trim()
    try {
      packageName = decodeURIComponent(packageName) || packageName
      caregiverName = decodeURIComponent(caregiverName) || caregiverName
      orderNo = decodeURIComponent(orderNo) || orderNo
    } catch (e) {}
    this.setData({ orderId: orderId, orderNo: orderNo, packageName: packageName, caregiverName: caregiverName })
    var that = this
    if (orderId) {
      api.getOrderDetail(orderId).then(function (res) {
        var d = res.data || res
        if (d.caregiverId) that.setData({ caregiverId: d.caregiverId })
      }).catch(function () {})
    }
    api.getTagList().then(function (res) {
      var raw = res
      if (raw && Array.isArray(raw)) {
        // 直接是数组
      } else if (raw && (raw.records || raw.list)) {
        raw = raw.records || raw.list
      } else if (raw && raw.data && Array.isArray(raw.data)) {
        raw = raw.data
      } else {
        raw = []
      }
      if (!Array.isArray(raw)) raw = []
      var names = raw.map(function (t) {
        if (typeof t === 'string') return t
        var n = t && (t.tagName !== undefined && t.tagName !== null ? t.tagName : t.name)
        return (n !== undefined && n !== null && n !== '') ? String(n) : ''
      }).filter(Boolean)
      var tagIdMap = {}
      raw.forEach(function (t) {
        if (typeof t === 'string') return
        var id = t.tagId != null ? t.tagId : t.id
        var name = t.tagName != null ? t.tagName : (t.name != null ? t.name : '')
        if (id != null && name) tagIdMap[name] = id
      })
      that.setData({ tagList: names, tagIdMap: tagIdMap, selectedTagFlags: names.map(function () { return false }) })
    }).catch(function () {
      var fallback = ['服务态度好', '专业技能强', '耐心细致', '沟通顺畅', '经验丰富', '服务周到']
      that.setData({ tagList: fallback, selectedTagFlags: fallback.map(function () { return false }) })
    })
  },

  onStar(e) {
    const r = e.currentTarget.dataset.r
    this.setData({ rating: r })
  },

  setType(e) {
    this.setData({ reviewType: e.currentTarget.dataset.type })
  },

  onTag(e) {
    var index = e.currentTarget.dataset.index
    var tag = e.currentTarget.dataset.tag
    if (tag === undefined || tag === null) {
      var tagList = this.data.tagList || []
      var i = parseInt(index, 10)
      if (!isNaN(i) && tagList[i] != null) tag = String(tagList[i])
    } else {
      tag = String(tag)
    }
    if (tag === '') return
    var selectedTags = (this.data.selectedTags || []).slice()
    var idx = selectedTags.indexOf(tag)
    if (idx >= 0) selectedTags.splice(idx, 1)
    else selectedTags.push(tag)
    var tagList = this.data.tagList || []
    var selectedTagFlags = tagList.map(function (t) { return selectedTags.indexOf(String(t)) >= 0 })
    this.setData({ selectedTags: selectedTags, selectedTagFlags: selectedTagFlags })
  },

  onContent(e) {
    this.setData({ content: e.detail.value })
  },

  onAnonymous(e) {
    this.setData({ isAnonymous: e.detail.value })
  },

  onSubmit() {
    var orderId = this.data.orderId
    var caregiverId = this.data.caregiverId
    var rating = this.data.rating
    var reviewType = this.data.reviewType
    var selectedTags = this.data.selectedTags || []
    var content = (this.data.content || '').trim()
    var tagIdMap = this.data.tagIdMap || {}
    if (!content) {
      wx.showToast({ title: '请输入评价内容', icon: 'none' })
      return
    }
    if (selectedTags.length === 0) {
      wx.showToast({ title: '请至少选择一个评价标签', icon: 'none' })
      return
    }
    if (!orderId || !caregiverId) {
      wx.showToast({ title: '订单或护工信息缺失', icon: 'none' })
      return
    }
    var tagIds = selectedTags.map(function (name) { return tagIdMap[name] }).filter(function (id) { return id != null })
    var that = this
    api.createReview({
      orderId: orderId,
      caregiverId: caregiverId,
      content: content,
      type: reviewType === 'positive' ? 1 : 2,
      stars: rating,
      isAnonymous: this.data.isAnonymous ? 1 : 0,
      tagIds: tagIds
    }).then(function () {
      wx.showToast({ title: '评价提交成功', icon: 'success' })
      setTimeout(function () { wx.navigateBack() }, 800)
    }).catch(function () {
      wx.showToast({ title: '提交失败', icon: 'none' })
    })
  }
})
