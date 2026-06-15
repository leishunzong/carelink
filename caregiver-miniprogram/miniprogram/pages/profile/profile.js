const api = require('../../utils/api')
const app = getApp()

Page({
  data: {
    userInfo: {},
    stats: {
      positiveRate: 0,      // 好评率
      completedOrders: 0,   // 完单量
      reviewCount: 0,       // 评论数
      noShowCount: 0        // 爽约数
    },
    moreOptions: [
      {
        icon: '🔒',
        label: '修改密码',
        action: 'changePassword',
        color: 'purple',
        bg: 'purple'
      },
      {
        icon: '📞',
        label: '联系客服',
        action: 'contactService',
        color: 'blue',
        bg: 'blue'
      },
      {
        icon: '⚙️',
        label: '系统设置',
        action: 'settings',
        color: 'gray',
        bg: 'gray'
      },
      {
        icon: '🔔',
        label: '平台公告',
        action: 'announcements',
        color: 'orange',
        bg: 'orange'
      },
      {
        icon: '💬',
        label: '意见反馈',
        action: 'feedback',
        color: 'green',
        bg: 'green'
      }
    ]
  },

  onLoad() {
    this.loadUserInfo()
    this.loadStats()
  },

  onShow() {
    // 更新 tabBar 选中状态
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().updateActive(3)
    }
    
    // 每次显示页面都刷新数据
    this.loadUserInfo()
    this.loadStats()
  },

  // 下拉刷新
  onPullDownRefresh() {
    Promise.all([
      this.loadUserInfo(),
      this.loadStats()
    ]).finally(() => {
      wx.stopPullDownRefresh()
    })
  },

  // 计算年龄
  calcAge(birthday) {
    if (!birthday) return null
    const today = new Date()
    const birthDate = new Date(birthday)
    let age = today.getFullYear() - birthDate.getFullYear()
    const monthDiff = today.getMonth() - birthDate.getMonth()
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
      age--
    }
    return age
  },

  // 将后端返回数据转为页面展示格式
  formatUserInfo(info) {
    return {
      ...info,
      age: this.calcAge(info.birthday),
      auditStatus: info.verifyStatus,
      origin: info.nativePlace
    }
  },

  // 加载用户信息
  async loadUserInfo() {
    try {
      const cachedInfo = app.getUserInfo()
      
      // 先用缓存数据渲染，避免页面闪烁
      if (cachedInfo) {
        this.setData({
          userInfo: this.formatUserInfo(cachedInfo)
        })
      }

      // 请求后端获取最新数据
      const res = await api.caregiver.getInfo()
      
      this.setData({
        userInfo: this.formatUserInfo(res)
      })

      // 更新全局用户信息
      app.setUserInfo(res)
    } catch (err) {
      console.error('加载用户信息失败:', err)
      // 接口失败且无缓存时，显示未登录
      if (!app.getUserInfo()) {
        this.setData({
          userInfo: {
            realName: '未登录',
            phone: '',
            verifyStatus: -1
          }
        })
      }
    }
  },

  // 加载统计数据
  async loadStats() {
    try {
      const res = await api.stats.getMyStats()
      
      // 直接使用后端返回的好评率（已经是百分比形式）
      const positiveRate = res.goodReviewRate ? Math.round(res.goodReviewRate) : 0
      
      this.setData({
        stats: {
          positiveRate: positiveRate,
          completedOrders: res.orderCount || 0,
          reviewCount: res.reviewCount || 0,
          noShowCount: res.cancelCount || 0
        }
      })
    } catch (err) {
      console.error('加载统计数据失败:', err)
      // 即使统计数据加载失败，也显示默认值
      this.setData({
        stats: {
          positiveRate: 0,
          completedOrders: 0,
          reviewCount: 0,
          noShowCount: 0
        }
      })
    }
  },

  // 跳转到个人信息编辑
  goToProfile() {
    wx.navigateTo({
      url: '/pages/profile/edit-profile/edit-profile'
    })
  },

  // 处理菜单点击
  handleMenuClick(e) {
    const { action } = e.currentTarget.dataset
    
    switch (action) {
      case 'changePassword':
        this.goToChangePassword()
        break
      case 'contactService':
        this.contactService()
        break
      case 'settings':
        this.openSettings()
        break
      case 'announcements':
        this.viewAnnouncements()
        break
      case 'feedback':
        this.submitFeedback()
        break
      default:
        wx.showToast({
          title: '功能开发中',
          icon: 'none'
        })
    }
  },

  // 跳转到修改密码
  goToChangePassword() {
    wx.navigateTo({
      url: '/pages/profile/change-password/change-password'
    })
  },

  // 联系客服
  contactService() {
    wx.makePhoneCall({
      phoneNumber: '400-123-4567',
      fail: () => {
        wx.showToast({
          title: '客服电话: 400-123-4567',
          icon: 'none',
          duration: 3000
        })
      }
    })
  },

  // 打开系统设置
  openSettings() {
    wx.openSetting({
      success: (res) => {
        console.log('设置结果:', res.authSetting)
      }
    })
  },

  // 查看平台公告
  viewAnnouncements() {
    wx.showToast({
      title: '功能开发中',
      icon: 'none'
    })
  },

  // 提交意见反馈
  submitFeedback() {
    wx.showToast({
      title: '功能开发中',
      icon: 'none'
    })
  },

  // 退出登录
  handleLogout() {
    wx.showModal({
      title: '退出登录',
      content: '确定要退出登录吗？',
      confirmColor: '#ef4444',
      success: (res) => {
        if (res.confirm) {
          app.logout()
        }
      }
    })
  }
})
