const api = require('../../utils/api')
const app = getApp()

Page({
  data: {
    username: '',
    password: '',
    showPassword: false,
    loading: false
  },

  onLoad() {
    // 如果已登录，跳转到首页
    if (app.globalData.hasLogin) {
      wx.switchTab({
        url: '/pages/home/home'
      })
    }
  },

  // 输入账号
  onUsernameChange(e) {
    this.setData({
      username: e.detail.value
    })
  },

  // 输入密码
  onPasswordChange(e) {
    this.setData({
      password: e.detail.value
    })
  },

  // 切换密码显示/隐藏
  togglePassword() {
    this.setData({
      showPassword: !this.data.showPassword
    })
  },

  // 登录
  async handleLogin() {
    const { username, password } = this.data

    // 表单验证
    if (!username || !username.trim()) {
      wx.showToast({
        title: '请输入用户名',
        icon: 'none'
      })
      return
    }

    if (!password || !password.trim()) {
      wx.showToast({
        title: '请输入密码',
        icon: 'none'
      })
      return
    }

  // 发送登录请求
  this.setData({ loading: true })
  
  try {
    const res = await api.auth.login({ 
      username: username.trim(), 
      password: password.trim() 
    })
    
    // 先保存 token（LoginVO 只有 token 和 userType）
    app.login(res.token, null)

    // 再拉取护工完整信息
    try {
      const caregiverInfo = await api.caregiver.getInfo()
      app.setUserInfo(caregiverInfo)
    } catch (infoErr) {
      console.warn('获取护工信息失败，后续页面会重新加载:', infoErr)
    }
    
    wx.showToast({
      title: '登录成功',
      icon: 'success'
    })

    // 检查审核状态并跳转
    setTimeout(() => {
      const userInfo = app.getUserInfo()
      const verifyStatus = userInfo?.verifyStatus
      if (verifyStatus === 0 || verifyStatus === 2) {
        // 待审核或已拒绝，跳转到入驻申请页
        wx.redirectTo({
          url: '/pages/settlement/settlement'
        })
      } else {
        // 已通过，跳转到首页
        wx.switchTab({
          url: '/pages/home/home'
        })
      }
    }, 1000)
    
  } catch (err) {
    console.error('登录失败:', err)
    // Toast 已在 request.js 中统一处理
  } finally {
    this.setData({ loading: false })
  }
  },

  // 跳转注册
  goToRegister() {
    wx.navigateTo({
      url: '/pages/register/register'
    })
  }
})
