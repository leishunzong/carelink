var app = getApp()
var router = require('../../utils/router.js')
var api = require('../../utils/api.js')
var location = require('../../utils/location.js')
var textFormatter = require('../../utils/textFormatter.js')
var marked
try {
  marked = require('marked')
} catch (e) {
  marked = null
}

// AI Markdown → marked (breaks+gfm) / simpleMarkdown，优化流式渲染效果
function markdownToHtml(content) {
  if (content == null || content === '') return ''
  var str = String(content).trim()
  
  // 如果内容很短或没有 markdown 语法，直接返回纯文本处理
  if (str.length < 10 && !/[*#\-_`]/.test(str)) {
    return '<p style="margin: 8rpx 0; line-height: 1.6; color: #374151;">' + escapeHtml(str) + '</p>'
  }
  
  try {
    // 优先使用 marked，配置适合聊天的参数
    if (marked && typeof marked.parse === 'function') {
      return marked.parse(str, { 
        breaks: true, 
        gfm: true,
        headerIds: false,
        mangle: false
      })
    }
    if (marked && typeof marked === 'function') {
      return marked(str, { 
        breaks: true, 
        gfm: true,
        headerIds: false,
        mangle: false
      })
    }
    // 使用优化后的 simpleMarkdown
    return simpleMarkdown.simpleMarkdownToHtml(str)
  } catch (e) {
    // 兜底：简单的换行处理
    return '<p style="margin: 8rpx 0; line-height: 1.6; color: #374151;">' + 
           escapeHtml(str).replace(/\n/g, '<br/>') + '</p>'
  }
}

// HTML 转义函数
function escapeHtml(str) {
  if (!str) return ''
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

var WELCOME_MESSAGE = (function () {
  var content = `您好！欢迎来到护联平台，我是您的居家养老护理顾问助手。😊

我很高兴为您提供专业的居家养老护理服务咨询和帮助。无论您需要：

**主要服务内容：**
- **护理知识咨询** - 回答各类居家养老护理问题，提供专业建议
- **护工查找服务** - 帮您搜索附近的护工，查看护工详细信息、技能和评价  
- **平台服务了解** - 介绍平台的服务内容、流程和收费标准
- **订单管理协助** - 帮助查询订单状态，创建订单等

我都可以为您提供帮助。

请告诉我您今天需要什么帮助呢？

**比如：**
- 想找一位在北京的护工
- 咨询老人日常护理注意事项  
- 了解平台的服务项目和价格
- 其他任何关于居家养老护理的问题

我会用专业、温暖的态度为您提供贴心的服务！`
  var formatted = textFormatter.formatMarkdownText(content)
  console.log('WELCOME_MESSAGE formattedContent 长度:', formatted.length)
  return { role: 'assistant', content: content, formattedContent: formatted, _msgKey: '0_r' }
})()

// 转为 iOS 可解析的日期字符串（iOS 仅支持 yyyy-MM-ddTHH:mm:ss 等格式，不支持 "yyyy-MM-dd HH:mm:ss"）
function toIOSSafeDateString(str) {
  if (str == null || str === '') return ''
  var s = String(str).trim()
  if (!s) return ''
  s = s.replace(/\s+/, 'T')
  if (/T\d{1,2}:\d{2}$/.test(s)) s += ':00'
  return s
}

function formatConversationTime(updateTime) {
  if (!updateTime) return ''
  var str = toIOSSafeDateString(updateTime)
  if (!str) return ''
  var date = new Date(str)
  if (isNaN(date.getTime())) return String(updateTime).substring(0, 16)
  var now = new Date()
  var today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  var d = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  var diff = (today - d) / 86400000
  var h = date.getHours()
  var m = date.getMinutes()
  var t = (h < 10 ? '0' : '') + h + ':' + (m < 10 ? '0' : '') + m
  if (diff === 0) return '今天 ' + t
  if (diff === 1) return '昨天 ' + t
  if (diff < 30) return (date.getMonth() + 1) + '月' + date.getDate() + '日'
  return (date.getMonth() + 1) + '月' + date.getDate() + '日'
}

function mapConversationList(res) {
  var records = (res && res.records) ? res.records : (Array.isArray(res) ? res : [])
  if (!Array.isArray(records)) return []
  return records
    .sort(function (a, b) {
      if (a.isPinned && !b.isPinned) return -1
      if (!a.isPinned && b.isPinned) return 1
      var ta = new Date(toIOSSafeDateString(a.updateTime || '')).getTime()
      var tb = new Date(toIOSSafeDateString(b.updateTime || '')).getTime()
      return tb - ta
    })
    .map(function (r) {
      return {
        id: r.id,
        conversationId: r.conversationId || '',
        title: r.title || r.lastQuestion || '新对话',
        updateTime: r.updateTime,
        updateTimeText: formatConversationTime(r.updateTime),
        isPinned: !!r.isPinned,
        isFavorite: !!r.isFavorite
      }
    })
}

Page({
  data: {
    messages: [WELCOME_MESSAGE],
    message: '',
    inputPlaceholder: '发消息...', // 默认文字输入；点击语音按钮后可为「按住说话」
    isTyping: false,
    showTypingDots: true,
    scrollToId: '',
    conversationId: '',
    showHistory: false,
    conversationList: [],
    historyLoading: false,
    showClearModal: false,
    streamingIndex: -1
  },

  onShow() {
    if (app.globalData.justLoggedIn) {
      app.globalData.justLoggedIn = false
    }
    var token = wx.getStorageSync('token') || app.globalData.token
    if (!token) {
      router.reLaunch({ url: '/pages/login/login' })
      return
    }
    // 进入聊天页时获取一次实时定位，缓存供AI对话使用
    this._refreshLocation()
  },

  /**
   * 获取小程序实时定位（经纬度 + 逆地址解析得到 cityCode），缓存到页面实例
   * 静默执行，失败不影响正常聊天
   */
  _refreshLocation() {
    var that = this
    wx.getLocation({
      type: 'gcj02',
      success: function (res) {
        console.log('[chat] 获取实时定位成功:', res.longitude, res.latitude)
        that._locationInfo = {
          longitude: res.longitude,
          latitude: res.latitude,
          cityCode: ''
        }
        // 缓存经纬度到全局
        api.setStoredLocation(res.longitude, res.latitude)
        // 逆地址解析获取 cityCode
        location.reverseGeocode(res.latitude, res.longitude).then(function (geo) {
          if (geo && geo.cityCode) {
            that._locationInfo.cityCode = geo.cityCode
            console.log('[chat] 逆地址解析成功, cityCode:', geo.cityCode, 'city:', geo.city)
          }
        }).catch(function (err) {
          console.warn('[chat] 逆地址解析失败，将使用用户表城市编码:', err)
        })
      },
      fail: function (err) {
        console.warn('[chat] 获取定位失败，AI搜索附近护工将使用默认地址:', err)
        // 尝试使用缓存的位置
        var stored = api.getStoredLocation()
        if (stored) {
          that._locationInfo = {
            longitude: stored.longitude,
            latitude: stored.latitude,
            cityCode: api.getCityCode(api.getStoredCityName())
          }
        }
      }
    })
  },

  onOpenHistory() {
    this.setData({ showHistory: true, historyLoading: true })
    this.loadConversationList()
  },

  onCloseHistory() {
    this.setData({ showHistory: false })
  },

  onNewConversation() {
    this.setData({
      showHistory: false,
      messages: [WELCOME_MESSAGE],
      conversationId: ''
    })
  },

  loadConversationList() {
    var that = this
    api.getAiConversations({ current: 1, size: 50 }).then(function (res) {
      var list = mapConversationList(res)
      that.setData({ conversationList: list, historyLoading: false })
    }).catch(function () {
      that.setData({ historyLoading: false, conversationList: [] })
    })
  },

  onSelectConversation(e) {
    var cid = e.currentTarget.dataset.cid
    if (!cid) return
    this.setData({ showHistory: false })
    this.reloadCurrentConversation(cid)
  },

  reloadCurrentConversation(cid) {
    if (!cid) return
    var that = this
    wx.showLoading({ title: '加载中…' })
    api.getAiConversationMessages(cid, { current: 1, size: 100 }).then(function (res) {
      var records = (res && res.records) || (res && res.list) || (Array.isArray(res) ? res : [])
      var list = records.sort(function (a, b) { return (a.seq || 0) - (b.seq || 0) }).map(function (m, i) {
        var role = m.role === 1 ? 'user' : 'assistant'
        var content = m.content || ''
        var item = { role: role, content: content, _msgKey: i + (role === 'assistant' ? '_r' : '_u') }
        if (role === 'assistant') item.formattedContent = textFormatter.formatMarkdownText(content)
        return item
      })
      if (list.length === 0) list = [WELCOME_MESSAGE]
      var lastIndex = list.length - 1
      that.setData({
        messages: list,
        conversationId: cid,
        scrollToId: 'msg-' + lastIndex
      })
      wx.hideLoading()
    }).catch(function () {
      wx.hideLoading()
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  onTogglePin(e) {
    var cid = e.currentTarget.dataset.cid
    var pinned = e.currentTarget.dataset.pinned
    var that = this
    api.pinAiConversation(cid, !pinned).then(function () {
      that.loadConversationList()
    }).catch(function () {
      wx.showToast({ title: '操作失败', icon: 'none' })
    })
  },

  onToggleFavorite(e) {
    var cid = e.currentTarget.dataset.cid
    var favorite = e.currentTarget.dataset.favorite
    var that = this
    api.favoriteAiConversation(cid, !favorite).then(function () {
      that.loadConversationList()
    }).catch(function () {
      wx.showToast({ title: '操作失败', icon: 'none' })
    })
  },

  onDeleteConversation(e) {
    var cid = e.currentTarget.dataset.cid
    var that = this
    wx.showModal({
      title: '删除对话',
      content: '确定删除这条对话记录吗？',
      success: function (res) {
        if (!res.confirm) return
        api.deleteAiConversation(cid).then(function () {
          if (that.data.conversationId === cid) {
            that.setData({ messages: [WELCOME_MESSAGE], conversationId: '' })
          }
          that.loadConversationList()
          wx.showToast({ title: '已删除', icon: 'none' })
        }).catch(function () {
          wx.showToast({ title: '删除失败', icon: 'none' })
        })
      }
    })
  },

  onConfirmClear() {
    this.setData({ showClearModal: true })
  },

  onCloseClearModal() {
    this.setData({ showClearModal: false })
  },

  onClearAll() {
    var that = this
    api.clearAiConversations().then(function () {
      that.setData({
        showClearModal: false,
        showHistory: false,
        conversationList: [],
        messages: [WELCOME_MESSAGE],
        conversationId: ''
      })
      wx.showToast({ title: '已清空', icon: 'none' })
    }).catch(function () {
      wx.showToast({ title: '清空失败', icon: 'none' })
    })
  },

  onInput(e) {
    this.setData({ message: e.detail.value })
  },

  onSend() {
    var msg = (this.data.message || '').trim()
    if (!msg) return
    var that = this
    var prev = this.data.messages
    var messages = prev.concat([{ role: 'user', content: msg, _msgKey: prev.length + '_u' }])
    var assistantIndex = messages.length
    messages.push({ role: 'assistant', content: '', _msgKey: assistantIndex + '_s' })
    this.setData({
      messages: messages,
      message: '',
      isTyping: true,
      showTypingDots: true,
      scrollToId: 'msg-' + assistantIndex,
      streamingIndex: assistantIndex
    })

    // 构建回调参数，附带实时位置信息
    var streamCallbacks = {
      onToken: function (token) {
        var list = that.data.messages
        var index = list.length - 1
        var last = list[index]
        if (last && last.role === 'assistant') {
          var newContent = (last.content || '') + token
          var update = {
            ['messages[' + index + '].content']: newContent,
            ['messages[' + index + '].formattedContent']: ''
          }
          if (that.data.showTypingDots) update.showTypingDots = false
          that.setData(update)
        }
      },
      onDone: function (conversationId) {
        var list = that.data.messages
        var lastIdx = list.length - 1
        var lastMsg = list[lastIdx]
        that.setData({
          isTyping: false,
          showTypingDots: false,
          streamingIndex: -1,
          conversationId: conversationId || that.data.conversationId
        })
        that.loadConversationList()
        
        // AI 回复完成后，自动重新加载当前对话以触发格式化显示
        if (conversationId && lastMsg && lastMsg.role === 'assistant' && lastMsg.content) {
          console.log('AI 回复完成，重新加载对话以显示格式化内容')
          setTimeout(function () {
            that.reloadCurrentConversation(conversationId)
          }, 100)
        }
      },
      onError: function (err) {
        var list = that.data.messages.slice()
        var last = list[list.length - 1]
        if (last && last.role === 'assistant' && !last.content) {
          var errMsg = '回复失败，请稍后再试。'
          var idx = list.length - 1
          list[idx] = { role: 'assistant', content: errMsg, formattedContent: textFormatter.formatMarkdownText(errMsg), _msgKey: idx + '_r' }
        }
        list = list.map(function (item, i) {
          return Object.assign({}, item, { _msgKey: item._msgKey || (i + (item.formattedContent ? '_r' : '_u')) })
        })
        that.setData({
          messages: list,
          isTyping: false,
          showTypingDots: false,
          streamingIndex: -1
        })
        wx.showToast({ title: (err && err.errMsg) || '网络错误', icon: 'none' })
      }
    }
    // 附加实时位置信息到请求
    if (that._locationInfo) {
      if (that._locationInfo.longitude != null) streamCallbacks.longitude = that._locationInfo.longitude
      if (that._locationInfo.latitude != null) streamCallbacks.latitude = that._locationInfo.latitude
      if (that._locationInfo.cityCode) streamCallbacks.cityCode = that._locationInfo.cityCode
    }
    var streamTask = api.requestChatStream(msg, this.data.conversationId || null, streamCallbacks)
    this._streamTask = streamTask
  },

  onQuickAction(e) {
    var action = e.currentTarget.dataset.action
    if (action === 'search') {
      router.switchTab({ url: '/pages/caregiver/index/index' })
      return
    }
    if (action === 'recommend') {
      this.sendWithScene('请根据我的需求推荐合适的护理方案', 'care_recommend')
      return
    }
    var text = action === 'service'
      ? '我想了解你们提供哪些护理服务？'
      : '我想预约护工服务，怎么操作？'
    this.setData({ message: text })
  },

  /** Send a message with a specific scene tag */
  sendWithScene(msg, scene) {
    if (!msg) return
    var that = this
    var prev = this.data.messages
    var messages = prev.concat([{ role: 'user', content: msg, _msgKey: prev.length + '_u' }])
    var assistantIndex = messages.length
    messages.push({ role: 'assistant', content: '', _msgKey: assistantIndex + '_s' })
    this.setData({
      messages: messages,
      message: '',
      isTyping: true,
      showTypingDots: true,
      scrollToId: 'msg-' + assistantIndex,
      streamingIndex: assistantIndex
    })

    // 构建回调参数，附带场景和实时位置信息
    var streamCallbacks = {
      scene: scene,
      onToken: function (token) {
        var list = that.data.messages
        var index = list.length - 1
        var last = list[index]
        if (last && last.role === 'assistant') {
          var newContent = (last.content || '') + token
          var update = {
            ['messages[' + index + '].content']: newContent,
            ['messages[' + index + '].formattedContent']: ''
          }
          if (that.data.showTypingDots) update.showTypingDots = false
          that.setData(update)
        }
      },
      onDone: function (conversationId) {
        that.setData({
          isTyping: false,
          showTypingDots: false,
          streamingIndex: -1,
          conversationId: conversationId || that.data.conversationId
        })
        that.loadConversationList()
        if (conversationId) {
          setTimeout(function () {
            that.reloadCurrentConversation(conversationId)
          }, 100)
        }
      },
      onError: function (err) {
        var list = that.data.messages.slice()
        var last = list[list.length - 1]
        if (last && last.role === 'assistant' && !last.content) {
          var errMsg = '回复失败，请稍后再试。'
          var idx = list.length - 1
          list[idx] = { role: 'assistant', content: errMsg, formattedContent: textFormatter.formatMarkdownText(errMsg), _msgKey: idx + '_r' }
        }
        list = list.map(function (item, i) {
          return Object.assign({}, item, { _msgKey: item._msgKey || (i + (item.formattedContent ? '_r' : '_u')) })
        })
        that.setData({
          messages: list,
          isTyping: false,
          showTypingDots: false,
          streamingIndex: -1
        })
        wx.showToast({ title: (err && err.errMsg) || '网络错误', icon: 'none' })
      }
    }
    // 附加实时位置信息到请求
    if (that._locationInfo) {
      if (that._locationInfo.longitude != null) streamCallbacks.longitude = that._locationInfo.longitude
      if (that._locationInfo.latitude != null) streamCallbacks.latitude = that._locationInfo.latitude
      if (that._locationInfo.cityCode) streamCallbacks.cityCode = that._locationInfo.cityCode
    }
    var streamTask = api.requestChatStream(msg, this.data.conversationId || null, streamCallbacks)
    this._streamTask = streamTask
  }
})
