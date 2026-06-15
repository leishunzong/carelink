// pages/profile/notification/notification.js
const api = require('../../../utils/api');

Page({
  data: {
    notifications: {
      newOrder: true,
      orderStatus: true,
      serviceReminder: true,
      evaluation: true,
      announcement: true,
      promotion: false
    }
  },

  onLoad() {
    this.loadNotificationSettings();
  },

  async loadNotificationSettings() {
    try {
      const res = await api.caregiver.getNotificationSettings();
      this.setData({ notifications: res.data });
    } catch (err) {
      console.error('加载通知设置失败', err);
    }
  },

  async onNotificationChange(e) {
    const key = e.currentTarget.dataset.key;
    const value = e.detail;
    
    try {
      await api.caregiver.updateNotificationSettings({
        [key]: value
      });
      
      this.setData({
        [`notifications.${key}`]: value
      });
    } catch (err) {
      wx.showToast({
        title: '设置失败',
        icon: 'none'
      });
    }
  }
});
