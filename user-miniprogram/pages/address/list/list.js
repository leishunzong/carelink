const api = require('../../../utils/api.js')
var router = require('../../../utils/router.js')
const SELECT_FROM_KEY = 'carelink_select_address_from'

function mapAddressList(data) {
  if (!Array.isArray(data)) return []
  return data.map(function (a) {
    var address = a.address || ''
    var doorNumber = a.doorNumber || ''
    var full = address + (doorNumber ? ' ' + doorNumber : '')
    var contactName = a.contactName || a.receiver || ''
    var contactPhone = a.contactPhone || a.phone || a.mobile || ''
    return {
      id: a.id,
      address: address,
      doorNumber: doorNumber,
      isDefault: !!a.isDefault,
      full: full,
      contactName: contactName,
      contactPhone: contactPhone
    }
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
    wx.setNavigationBarTitle({ title: isSelectMode ? '选择服务地址' : '服务地址' })
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
    api.getAddressList().then(function (res) {
      var raw = Array.isArray(res) ? res : (res && (res.records || res.data)) || []
      var list = mapAddressList(raw)
      that.setData({ list: list, loading: false })
    }).catch(function (err) {
      that.setData({ loading: false })
      var msg = (err && err.data && err.data.message) || (err && err.message) || '加载失败'
      wx.showToast({ title: msg, icon: 'none' })
    })
  },

  onCardTap(e) {
    const item = e.currentTarget.dataset.item
    if (this.data.isSelectMode) {
      const from = wx.getStorageSync(SELECT_FROM_KEY) || ''
      wx.setStorageSync('carelink_selected_address', {
        id: item.id,
        address: item.address || '',
        doorNumber: item.doorNumber || '',
        full: item.full || (item.address || '') + (item.doorNumber ? ' ' + item.doorNumber : ''),
        contactName: item.contactName || '',
        contactPhone: item.contactPhone || ''
      })
      wx.removeStorageSync(SELECT_FROM_KEY)
      wx.navigateBack()
      return
    }
    router.navigateTo({ url: '/pages/address/edit/edit?id=' + item.id })
  },

  onDelete(e) {
    const id = e.currentTarget.dataset.id
    const that = this
    wx.showModal({
      title: '确定要删除这个地址吗？',
      success: function (res) {
        if (!res.confirm) return
        api.deleteAddress(id).then(function () {
          that.loadList()
          wx.showToast({ title: '已删除', icon: 'success' })
        }).catch(function () {
          wx.showToast({ title: '删除失败', icon: 'none' })
        })
      }
    })
  },

  goAdd() {
    router.navigateTo({ url: '/pages/address/edit/edit' })
  }
})
