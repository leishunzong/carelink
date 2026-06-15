const api = require('../../utils/api')
const app = getApp()

Page({
  data: {},

  onLoad() {},

  onShow() {
    // 更新 tabBar 选中状态
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().updateActive(1)
    }
  },

  // 跳转到技能列表（申请技能）
  goToSkillList() {
    wx.navigateTo({
      url: '/pages/service/skill-list/skill-list'
    })
  },

  // 跳转到服务包列表（开通服务包）
  goToServicePackageList() {
    wx.navigateTo({
      url: '/pages/service/package-list/package-list'
    })
  },

  // 跳转到入驻申请
  goToSettlement() {
    // 检查审核状态
    const userInfo = app.globalData.userInfo
    if (userInfo && userInfo.verifyStatus === 1) {
      wx.showToast({
        title: '您已入驻成功，无需重复申请',
        icon: 'none'
      })
      return
    }

    wx.navigateTo({
      url: '/pages/settlement/settlement'
    })
  },

  // 跳转到技能管理
  goToSkillManagement() {
    wx.navigateTo({
      url: '/pages/service/skill-management/skill-management'
    })
  },

  // 跳转到服务包管理
  goToServicePackageManagement() {
    wx.navigateTo({
      url: '/pages/service/package-management/package-management'
    })
  },

  // 跳转到评价管理
  goToReviewManagement() {
    wx.navigateTo({
      url: '/pages/service/evaluation-list/evaluation-list'
    })
  }
})
