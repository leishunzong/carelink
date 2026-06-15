// 搜索页：热门词对接 GET /package/hot-keywords，搜索跳转服务包列表
var api = require('../../utils/api.js')
var router = require('../../utils/router.js')
var HISTORY_KEY = 'carelink_search_history'
var HISTORY_MAX = 10

Page({
  data: {
    keyword: '',
    focus: true,
    hotKeywords: ['基础陪护', '慢病照护', '月嫂', '术后康复', '陪诊', '家政保洁', '老年照护', '高级陪护'],
    historyList: []
  },

  onLoad() {
    var history = wx.getStorageSync(HISTORY_KEY) || []
    this.setData({ historyList: Array.isArray(history) ? history : [] })
    var that = this
    api.getHotKeywords(10).then(function (res) {
      console.log('热门关键词响应:', res)
      var words = res.data || res
      if (Array.isArray(words) && words.length) {
        console.log('加载热门关键词:', words)
        that.setData({ hotKeywords: words })
      } else {
        console.log('热门关键词为空，使用默认关键词')
      }
    }).catch(function (err) {
      console.error('热门关键词加载失败，使用默认关键词:', err)
      // 后端不可用时使用默认关键词
    })
  },

  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value })
  },

  onSearch() {
    var keyword = (this.data.keyword || '').trim()
    if (!keyword) {
      wx.showToast({ title: '请输入关键词', icon: 'none' })
      return
    }
    console.log('搜索关键词:', keyword)
    this.addHistory(keyword)
    this.navigateToSearch(keyword)
  },

  onHotTap(e) {
    var keyword = e.currentTarget.dataset.keyword || ''
    console.log('点击热门关键词:', keyword)
    this.setData({ keyword: keyword })
    this.addHistory(keyword)
    this.navigateToSearch(keyword)
  },

  onHistoryTap(e) {
    var keyword = e.currentTarget.dataset.keyword || ''
    console.log('点击历史关键词:', keyword)
    this.setData({ keyword: keyword })
    this.addHistory(keyword)
    this.navigateToSearch(keyword)
  },

  navigateToSearch(keyword) {
    if (!keyword) return
    var encodedKeyword = encodeURIComponent(keyword)
    console.log('编码后的关键词:', encodedKeyword)
    router.navigateTo({ 
      url: '/pages/package/list/list?from=search&keyword=' + encodedKeyword 
    })
  },

  addHistory(keyword) {
    if (!keyword) return
    var list = wx.getStorageSync(HISTORY_KEY) || []
    if (!Array.isArray(list)) list = []
    list = list.filter(function (item) { return item !== keyword })
    list.unshift(keyword)
    list = list.slice(0, HISTORY_MAX)
    wx.setStorageSync(HISTORY_KEY, list)
    this.setData({ historyList: list })
  },

  clearHistory() {
    wx.setStorageSync(HISTORY_KEY, [])
    this.setData({ historyList: [] })
    wx.showToast({ title: '已清空', icon: 'none' })
  },

  onCancel() {
    wx.navigateBack()
  }
})
