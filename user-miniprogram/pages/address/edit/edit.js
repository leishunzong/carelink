var locationService = require('../../../utils/location.js')
var api = require('../../../utils/api.js')

Page({
  data: {
    id: null,
    contactName: '',
    contactPhone: '',
    addressMode: 'map', // 'map'(实时定位) | 'manual'(手动输入)
    addressText: '',     // 实时定位时为定位回显地址，手动输入时为用户输入；提交给后端为 address
    doorNumber: '',      // 门牌号
    province: '',
    city: '',
    district: '',
    town: '',
    longitude: null,
    latitude: null,
    isDefault: false
  },

  onLoad(options) {
    var id = options.id ? (options.id + '').trim() : null
    this.setData({ id: id })
    if (id) {
      var that = this
      api.getAddress(id).then(function (res) {
        var item = res.data || res
        that.setData({
          contactName: item.contactName || item.receiver || '',
          contactPhone: item.contactPhone || item.phone || item.mobile || '',
          addressText: item.address || '',
          doorNumber: item.doorNumber || '',
          isDefault: !!item.isDefault,
          addressMode: (item.address && item.address.trim()) ? 'manual' : 'map'
        })
      }).catch(function () {
        wx.showToast({ title: '加载地址失败', icon: 'none' })
      })
    }
  },

  onContactInput(e) {
    this.setData({ contactName: e.detail.value })
  },
  onPhoneInput(e) {
    this.setData({ contactPhone: e.detail.value })
  },
  onAddressModeChange(e) {
    var mode = e.currentTarget.dataset.mode
    var prevMode = this.data.addressMode
    this.setData({ addressMode: mode })
    // 切换到实时定位且尚未定位时，自动触发一次定位
    if (mode === 'map' && prevMode !== 'map' && !this.data.addressText) {
      this.onLocationTap()
    }
  },
  onAddressInput(e) {
    this.setData({ addressText: e.detail.value })
  },
  onDoorNumberInput(e) {
    this.setData({ doorNumber: e.detail.value })
  },
  onDefaultChange(e) {
    this.setData({ isDefault: e.detail.value })
  },

  /** 实时定位：调用 wx.getLocation -> 逆地址解析 -> 回显地址（省市区、街道等），门牌号需用户自己填 */
  onLocationTap() {
    var that = this
    wx.showLoading({ title: '定位中…' })
    wx.getLocation({
      type: 'gcj02',
      success: function (loc) {
        locationService.reverseGeocode(loc.latitude, loc.longitude).then(function (addr) {
          var town = addr.town || ''
          var streetPart = [addr.street, addr.street_number].filter(Boolean).join('') || addr.address || ''
          var regionText = [addr.province, addr.city, addr.district].filter(Boolean).join(' ')
          if (town) regionText += ' ' + town
          if (streetPart) regionText += ' ' + streetPart
          that.setData({
            province: addr.province,
            city: addr.city,
            district: addr.district,
            town: town,
            addressText: regionText.trim(),
            longitude: loc.longitude,
            latitude: loc.latitude
          })
          wx.hideLoading()
          wx.showToast({ title: '定位成功', icon: 'success' })
        }).catch(function (err) {
          wx.hideLoading()
          wx.showToast({ title: (err && err.message) || '逆地址解析失败', icon: 'none' })
        })
      },
      fail: function () {
        wx.hideLoading()
        wx.showToast({ title: '请开启定位权限', icon: 'none' })
      }
    })
  },

  onSave() {
    var contactName = (this.data.contactName || '').trim()
    var contactPhone = (this.data.contactPhone || '').trim()
    if (!contactName) {
      wx.showToast({ title: '请填写收货人', icon: 'none' })
      return
    }
    if (!contactPhone) {
      wx.showToast({ title: '请填写手机号', icon: 'none' })
      return
    }
    var addressText = (this.data.addressText || '').trim()
    var doorNumber = (this.data.doorNumber || '').trim()
    if (!addressText) {
      wx.showToast({ title: '请填写或选择地址', icon: 'none' })
      return
    }

    var payload = {
      contactName: contactName,
      contactPhone: contactPhone,
      address: addressText,
      doorNumber: doorNumber,
      isDefault: this.data.isDefault ? 1 : 0
    }
    if (this.data.longitude != null && this.data.latitude != null) {
      payload.longitude = this.data.longitude
      payload.latitude = this.data.latitude
    }
    var that = this
    var p = this.data.id ? api.updateAddress(this.data.id, payload) : api.addAddress(payload)
    p.then(function () {
      wx.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(function () { wx.navigateBack() }, 500)
    }).catch(function () {
      wx.showToast({ title: '保存失败', icon: 'none' })
    })
  }
})
