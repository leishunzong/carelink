var api = require('../../../utils/api.js')
var PROFILE_KEY = api.USER_PROFILE_KEY

Page({
  data: {
    statusBarHeight: 20,
    navBarHeight: 64,
    avatar: '',
    nickname: '用户',
    phone: '',
    initialAvatar: '',
    initialNickname: '',
    initialPhone: '',
    uploading: false,
    saving: false
  },

  onLoad() {
    var that = this
    var sys = (typeof wx.getWindowInfoSync === 'function' ? wx.getWindowInfoSync() : wx.getSystemInfoSync()) || {}
    var statusBarHeight = sys.statusBarHeight || 20
    var navBarHeight = statusBarHeight + 44
    that.setData({ statusBarHeight: statusBarHeight, navBarHeight: navBarHeight })
    var profile = wx.getStorageSync(PROFILE_KEY) || {}
    that.setData({
      avatar: profile.avatar || '',
      nickname: profile.nickname || '用户',
      phone: profile.phone || '',
      initialAvatar: profile.avatar || '',
      initialNickname: profile.nickname || '用户',
      initialPhone: profile.phone || ''
    })
    api.getUserInfo().then(function (data) {
      var avatar = data.avatar || ''
      var nickname = data.nickname || '用户'
      var phone = data.phone || ''
      that.setData({
        avatar: avatar,
        nickname: nickname,
        phone: phone,
        initialAvatar: avatar,
        initialNickname: nickname,
        initialPhone: phone
      })
    }).catch(function () {})
  },

  onBack() {
    wx.navigateBack()
  },

  /** 选择头像 -> 上传文件接口 -> 拿到 url 后仅更新页面展示，不提交；提交在点击「修改」时统一进行 */
  onAvatarTap() {
    var that = this
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      success: function (res) {
        var path = res.tempFiles[0].tempFilePath
        that.setData({ uploading: true })
        api.uploadImage(path).then(function (url) {
          that.setData({ avatar: url, uploading: false })
          wx.showToast({ title: '头像已更新', icon: 'success' })
        }).catch(function (err) {
          that.setData({ uploading: false })
          wx.showToast({ title: (err && err.message) || '上传失败', icon: 'none' })
        })
      }
    })
  },

  onNicknameInput(e) {
    this.setData({ nickname: e.detail.value })
  },

  onPhoneInput(e) {
    this.setData({ phone: e.detail.value })
  },

  /** 底部「修改」：只提交有变更的字段（头像 / 昵称 / 手机号），成功后同步到本地并返回，我的页会重新拉取或使用已同步数据回显 */
  onSave() {
    var that = this
    var d = this.data
    if (d.saving) return
    var payload = {}
    if ((d.avatar || '') !== (d.initialAvatar || '')) payload.avatar = d.avatar || ''
    if ((d.nickname || '').trim() !== (d.initialNickname || '').trim()) payload.nickname = (d.nickname || '').trim()
    if ((d.phone || '').trim() !== (d.initialPhone || '').trim()) payload.phone = (d.phone || '').trim()
    if (Object.keys(payload).length === 0) {
      wx.showToast({ title: '请修改后再保存', icon: 'none' })
      return
    }
    that.setData({ saving: true })
    api.setUserInfo(payload).then(function () {
      var merged = {
        ...wx.getStorageSync(PROFILE_KEY),
        avatar: d.avatar,
        nickname: (d.nickname || '').trim(),
        phone: (d.phone || '').trim()
      }
      api.syncUserInfoToLocal(merged)
      that.setData({ saving: false })
      wx.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(function () { wx.navigateBack() }, 500)
    }).catch(function () {
      that.setData({ saving: false })
      wx.showToast({ title: '保存失败', icon: 'none' })
    })
  }
})
