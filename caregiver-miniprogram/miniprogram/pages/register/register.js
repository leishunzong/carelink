const api = require('../../utils/api')

Page({
  data: {
    username: '',
    password: '',
    phone: '',
    name: '',
    gender: '女',
    showPassword: false,
    loading: false
  },

  onUsernameChange(e) {
    this.setData({ username: e.detail.value })
  },

  onPasswordChange(e) {
    this.setData({ password: e.detail.value })
  },

  onPhoneChange(e) {
    this.setData({ phone: e.detail.value })
  },

  onNameChange(e) {
    this.setData({ name: e.detail.value })
  },

  selectGender(e) {
    this.setData({ gender: e.currentTarget.dataset.gender })
  },

  async handleRegister() {
    const { username, password, phone, name } = this.data

    // 表单验证
    if (!username || !password || !phone || !name) {
      wx.showToast({
        title: '请填写完整信息',
        icon: 'none'
      })
      return
    }

    if (username.trim().length < 4) {
      wx.showToast({
        title: '用户名至少4个字符',
        icon: 'none'
      })
      return
    }

    if (!/^1[3-9]\d{9}$/.test(phone.trim())) {
      wx.showToast({
        title: '手机号格式不正确',
        icon: 'none'
      })
      return
    }

    if (password.trim().length < 6) {
      wx.showToast({
        title: '密码至少6位',
        icon: 'none'
      })
      return
    }

    // 发送注册请求
    this.setData({ loading: true })

    try {
      await api.auth.register({
        username: username.trim(),
        phone: phone.trim(),
        password: password.trim(),
        realName: name.trim(),
        gender: this.data.gender === '男' ? 1 : 2
      })

      // 注册成功后自动登录，获取token
      const app = getApp()
      const loginRes = await api.auth.login({
        username: username.trim(),
        password: password.trim()
      })
      app.login(loginRes.token, null)

      // 获取护工信息并保存
      try {
        const caregiverInfo = await api.caregiver.getInfo()
        app.setUserInfo(caregiverInfo)
      } catch (e) {
        console.warn('获取护工信息失败:', e)
      }

      // 提示并跳转到入驻申请页
      wx.showModal({
        title: '注册成功',
        content: '请完成入驻申请，提交审核后即可开始接单',
        showCancel: false,
        success: () => {
          wx.redirectTo({
            url: '/pages/settlement/settlement'
          })
        }
      })

    } catch (err) {
      console.error('注册失败:', err)
      // Toast 已在 request.js 中统一处理
    } finally {
      this.setData({ loading: false })
    }
  },

  goToLogin() {
    wx.navigateBack()
  }
})
