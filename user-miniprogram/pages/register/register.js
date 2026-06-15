var api = require('../../utils/api.js')

Page({
  data: {
    username: '',
    nickname: '',
    phone: '',
    password: '',
    confirmPassword: '',
    loading: false
  },

  onUsernameInput(e) { this.setData({ username: e.detail.value }) },
  onNicknameInput(e) { this.setData({ nickname: e.detail.value }) },
  onPhoneInput(e) { this.setData({ phone: e.detail.value }) },
  onPasswordInput(e) { this.setData({ password: e.detail.value }) },
  onConfirmInput(e) { this.setData({ confirmPassword: e.detail.value }) },

  onSubmit() {
    var username = (this.data.username || '').trim()
    var nickname = (this.data.nickname || '').trim()
    var phone = (this.data.phone || '').trim()
    var password = this.data.password
    var confirmPassword = this.data.confirmPassword
    if (!username || !nickname || !phone || !password || !confirmPassword) {
      wx.showToast({ title: '请填写完整信息', icon: 'none' })
      return
    }
    if (password !== confirmPassword) {
      wx.showToast({ title: '两次密码不一致', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    api.register({
      username: username,
      password: password,
      nickname: nickname,
      phone: phone
    }).then(function () {
      wx.showToast({ title: '注册成功', icon: 'success' })
      setTimeout(function () {
        require('../../utils/router.js').navigateTo({ url: '/pages/login/login' })
      }, 1500)
    }).catch(function () {}).finally(function () {
      this.setData({ loading: false })
    }.bind(this))
  },

  goLogin() {
    wx.navigateBack()
  }
})
