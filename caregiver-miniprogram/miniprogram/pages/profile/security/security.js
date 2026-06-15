// pages/profile/security/security.js
const api = require('../../../utils/api');

Page({
  data: {
    phoneNumber: '',
    isVerified: false,
    settings: {
      searchable: true,
      showTrack: true
    }
  },

  onLoad() {
    this.loadSecurityInfo();
  },

  async loadSecurityInfo() {
    try {
      const res = await api.caregiver.getInfo();
      this.setData({
        phoneNumber: res.data.phone || '未绑定',
        isVerified: res.data.verified || false,
        settings: {
          searchable: res.data.searchable !== false,
          showTrack: res.data.showTrack !== false
        }
      });
    } catch (err) {
      console.error('加载安全信息失败', err);
    }
  },

  async onSettingChange(e) {
    const key = e.currentTarget.dataset.key;
    const value = e.detail;
    
    try {
      await api.caregiver.updateSettings({
        [key]: value
      });
      
      this.setData({
        [`settings.${key}`]: value
      });
    } catch (err) {
      wx.showToast({
        title: '设置失败',
        icon: 'none'
      });
    }
  }
});
