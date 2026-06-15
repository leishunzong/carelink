const api = require('../../../utils/api')

Page({
  data: {
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
    showOldPassword: false,
    showNewPassword: false,
    showConfirmPassword: false,
    passwordRules: {
      length: false,
      hasLetter: false,
      hasNumber: false
    },
    submitting: false
  },

  onOldPasswordChange(e) {
    this.setData({ oldPassword: e.detail })
  },

  onNewPasswordChange(e) {
    const password = e.detail
    this.setData({ newPassword: password })
    this.validatePassword(password)
  },

  onConfirmPasswordChange(e) {
    this.setData({ confirmPassword: e.detail })
  },

  // 切换密码可见性
  toggleOldPassword() {
    this.setData({ showOldPassword: !this.data.showOldPassword })
  },
  toggleNewPassword() {
    this.setData({ showNewPassword: !this.data.showNewPassword })
  },
  toggleConfirmPassword() {
    this.setData({ showConfirmPassword: !this.data.showConfirmPassword })
  },

  validatePassword(password) {
    const rules = {
      length: password.length >= 6 && password.length <= 20,
      hasLetter: /[a-zA-Z]/.test(password),
      hasNumber: /\d/.test(password)
    }
    this.setData({ passwordRules: rules })
    return rules.length && rules.hasLetter && rules.hasNumber
  },

  async handleSubmit() {
    const { oldPassword, newPassword, confirmPassword, passwordRules } = this.data

    if (!oldPassword || !oldPassword.trim()) {
      wx.showToast({ title: '请输入原密码', icon: 'none' })
      return
    }
    if (!newPassword || !newPassword.trim()) {
      wx.showToast({ title: '请输入新密码', icon: 'none' })
      return
    }
    if (!passwordRules.length || !passwordRules.hasLetter || !passwordRules.hasNumber) {
      wx.showToast({ title: '新密码不符合要求', icon: 'none' })
      return
    }
    if (!confirmPassword || !confirmPassword.trim()) {
      wx.showToast({ title: '请再次输入新密码', icon: 'none' })
      return
    }
    if (newPassword !== confirmPassword) {
      wx.showToast({ title: '两次输入的密码不一致', icon: 'none' })
      return
    }
    if (oldPassword === newPassword) {
      wx.showToast({ title: '新密码不能与原密码相同', icon: 'none' })
      return
    }

    this.setData({ submitting: true })

    try {
      await api.auth.changePassword({
        oldPassword: oldPassword.trim(),
        newPassword: newPassword.trim()
      })

      wx.showToast({ title: '密码修改成功', icon: 'success' })
      setTimeout(() => { wx.navigateBack() }, 1500)
    } catch (err) {
      console.error('修改密码失败:', err)
      wx.showToast({ title: err.message || '修改失败，请重试', icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  }
})
