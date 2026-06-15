// 护工详情：对接 GET /caregiver/public/{id}/detail、/stats、/review/user/caregiver/{id}
var api = require('../../../utils/api.js')
var router = require('../../../utils/router.js')

function ageFromBirthday(birthday) {
  if (!birthday) return null
  var y = parseInt(String(birthday).slice(0, 4), 10)
  return isNaN(y) ? null : new Date().getFullYear() - y
}

function mapSkillGroups(skills) {
  if (!Array.isArray(skills) || skills.length === 0) return []
  var byGroup = {}
  skills.forEach(function (s) {
    var title = s.groupName || s.category || '技能'
    if (!byGroup[title]) byGroup[title] = []
    byGroup[title].push(s.skillName || s.name || '')
  })
  return Object.keys(byGroup).map(function (title) { return { title: title, items: byGroup[title] } })
}

Page({
  data: {
    id: null,
    tab: 'basic',
    info: {},
    stats: {},
    tagStats: [],
    skillGroups: [],
    reviews: [],
    packages: [],
    aiSummary: '',
    aiSummaryLoading: true
  },

  onLoad(options) {
    var id = options.id
    if (!id) {
      wx.showToast({ title: '参数错误', icon: 'none' })
      return
    }
    this.setData({ id: id })
    this.loadDetail()
    this.loadReviews()
    this.loadPackages()
    this.loadAiSummary()
    this.startAiSummaryPolling()
  },

  loadDetail() {
    var id = this.data.id
    var that = this
    api.getCaregiverDetail(id).then(function (res) {
      var data = res.data || res
      var basic = data.basicInfo || data
      var info = {
        id: basic.id,
        realName: basic.realName,
        name: basic.realName || basic.name,
        avatar: basic.avatar || '',
        gender: basic.gender === 1 ? '男' : '女',
        age: basic.age != null ? basic.age : ageFromBirthday(basic.birthday),
        origin: basic.nativePlace,
        nativePlace: basic.nativePlace,
        workYears: basic.workYears,
        education: basic.education || '',
        rating: (data.stats && data.stats.averageRating) || 0
      }
      // 智能处理好评率：兼容小数格式（0.9091）和百分比格式（90.91）
      var goodReviewRatePercent = 0
      if (data.stats && data.stats.goodReviewRate != null) {
        var rate = Number(data.stats.goodReviewRate)
        if (rate > 1) {
          // 如果大于 1，说明后端返回的已经是百分比数值（如 90.91）
          goodReviewRatePercent = Math.round(rate)
        } else {
          // 如果小于等于 1，说明是小数（如 0.9091），需要乘以 100
          goodReviewRatePercent = Math.round(rate * 100)
        }
      }
      var stats = (data.stats && { 
        orderCount: data.stats.orderCount, 
        reviewCount: data.stats.reviewCount, 
        goodReviewRate: data.stats.goodReviewRate, 
        goodReviewRatePercent: goodReviewRatePercent, 
        cancelCount: data.stats.cancelCount, 
        averageRating: data.stats.averageRating 
      }) || {}
      var skillGroups = mapSkillGroups(data.skills || [])
      that.setData({ info: info, stats: stats, skillGroups: skillGroups })
    }).catch(function () {
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
    api.getCaregiverTags(id).then(function (res) {
      var arr = res.data || res
      if (Array.isArray(arr)) that.setData({ tagStats: arr })
    }).catch(function () {})
  },

  loadReviews() {
    var id = this.data.id
    var that = this
    api.getReviewCaregiverList(id, { page: 1, size: 20 }).then(function (res) {
      var records = res.records || (res.data && res.data.records) || []
      var list = records.map(function (r) {
        return { id: r.id, nickname: r.nickname || (r.isAnonymous ? '匿名用户' : '用户'), rating: r.stars, stars: r.stars, content: r.content, date: r.serviceDate || r.createTime, createTime: r.createTime, type: r.type }
      })
      that.setData({ reviews: list })
    }).catch(function () {})
  },

  loadPackages() {
    var id = this.data.id
    var that = this
    api.getCaregiverPackages(id).then(function (res) {
      var arr = res.data || res
      that.setData({ packages: Array.isArray(arr) ? arr : [] })
    }).catch(function () {})
  },

  loadAiSummary() {
    var id = this.data.id
    var that = this
    api.getCaregiverReviewSummary(id).then(function (res) {
      // request.js resolves body.data, so res is the summary string directly
      that.setData({ aiSummary: res || '', aiSummaryLoading: false })
    }).catch(function () {
      that.setData({ aiSummary: '', aiSummaryLoading: false })
    })
  },

  /** Poll AI summary every 2min to keep it fresh */
  startAiSummaryPolling() {
    this.stopAiSummaryPolling()
    var that = this
    this._aiPollTimer = setInterval(function () {
      that.loadAiSummary()
    }, 120000)
  },

  stopAiSummaryPolling() {
    if (this._aiPollTimer) {
      clearInterval(this._aiPollTimer)
      this._aiPollTimer = null
    }
  },

  onUnload() {
    this.stopAiSummaryPolling()
  },

  onHide() {
    this.stopAiSummaryPolling()
  },

  onShow() {
    // Resume polling when page is shown again
    if (this.data.id) {
      this.startAiSummaryPolling()
    }
  },

  onTab(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({ tab })
  },

  // 从服务包 Tab 内点击某服务包的「预约」→ 定向预约（带 caregiverId + packageId）
  goDirectOrder(e) {
    const packageId = e.currentTarget.dataset.packageId
    const caregiverId = this.data.id
    const url = '/pages/order/direct/direct?caregiverId=' + caregiverId + (packageId ? '&packageId=' + packageId : '')
    router.navigateTo({ url: url })
  },

  // 底部「预约该护工」→ 定向预约（只带 caregiverId）
  goDirectOrderMain() {
    const caregiverId = this.data.id
    router.navigateTo({ url: '/pages/order/direct/direct?caregiverId=' + caregiverId })
  }
})
