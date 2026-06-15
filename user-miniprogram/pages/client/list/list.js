const api = require('../../../utils/api.js')
var router = require('../../../utils/router.js')
const SELECT_FROM_KEY = 'carelink_select_client_from'

function calcAge(birthday) {
  if (!birthday) return null
  var str = String(birthday)
  var y = parseInt(str.slice(0, 4), 10)
  if (isNaN(y)) return null
  return new Date().getFullYear() - y
}

function mapSubjectList(data) {
  if (!Array.isArray(data)) return []
  return data.map(function (c) {
    var age = c.age != null ? c.age : calcAge(c.birthday)
    return { id: c.id, name: c.name, relationship: c.relationship, birthday: c.birthday, gender: c.gender, age: age, selfCareAbility: c.selfCareAbility || c.selfCare, intellectStatus: c.intellectStatus, isDefault: c.isDefault }
  })
}

Page({
  data: {
    isSelectMode: false,
    list: [],
    loading: false
  },

  onLoad(options) {
    const from = options.from || ''
    const isSelectMode = !!from
    wx.setNavigationBarTitle({ title: isSelectMode ? '选择服务对象' : '服务对象' })
    if (from) wx.setStorageSync(SELECT_FROM_KEY, from)
    this.setData({ isSelectMode })
    this.loadList()
  },

  onShow() {
    this.loadList()
  },

  loadList() {
    var that = this
    if (this.data.loading) return
    this.setData({ loading: true })
    api.getSubjectList().then(function (res) {
      var raw = Array.isArray(res) ? res : (res && (res.records || res.data)) || []
      if (!Array.isArray(raw)) raw = []
      var list = mapSubjectList(raw)
      that.setData({ list: list, loading: false })
    }).catch(function (err) {
      that.setData({ loading: false })
      wx.showToast({ title: (err && err.message) || '加载失败', icon: 'none' })
    })
  },

  onCardTap(e) {
    var id = e.currentTarget.dataset.id
    var list = this.data.list
    var item = list ? list.find(function (c) { return c.id == id }) : null
    if (!item) return
    if (this.data.isSelectMode) {
      wx.setStorageSync('carelink_selected_client', {
        id: item.id,
        name: item.name,
        gender: item.gender,
        age: item.age != null ? item.age : calcAge(item.birthday),
        selfCareAbility: item.selfCareAbility || item.selfCare
      })
      wx.removeStorageSync(SELECT_FROM_KEY)
      wx.navigateBack()
      return
    }
    router.navigateTo({ url: '/pages/client/edit/edit?id=' + item.id })
  },

  onDelete(e) {
    const id = e.currentTarget.dataset.id
    const that = this
    wx.showModal({
      title: '确定要删除这个服务对象吗？',
      success: function (res) {
        if (!res.confirm) return
        api.deleteSubject(id).then(function () {
          that.loadList()
          wx.showToast({ title: '已删除', icon: 'success' })
        }).catch(function () {
          wx.showToast({ title: '删除失败', icon: 'none' })
        })
      }
    })
  },

  goAdd() {
    router.navigateTo({ url: '/pages/client/edit/edit' })
  }
})
