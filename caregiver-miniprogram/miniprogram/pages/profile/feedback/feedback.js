// pages/profile/feedback/feedback.js
const api = require('../../../utils/api');

Page({
  data: {
    feedbackType: '',
    content: '',
    images: [],
    contact: '',
    
    showTypePicker: false,
    typeActions: [
      { name: '功能异常', value: 'bug' },
      { name: '功能建议', value: 'feature' },
      { name: '订单问题', value: 'order' },
      { name: '收益问题', value: 'payment' },
      { name: '其他问题', value: 'other' }
    ]
  },

  onShowTypePicker() {
    this.setData({ showTypePicker: true });
  },

  onSelectType(e) {
    this.setData({
      feedbackType: e.detail.name,
      showTypePicker: false
    });
  },

  onCloseTypePicker() {
    this.setData({ showTypePicker: false });
  },

  onContentChange(e) {
    this.setData({ content: e.detail });
  },

  onContactChange(e) {
    this.setData({ contact: e.detail });
  },

  async onUploadImage(e) {
    const { file } = e.detail;
    
    try {
      // TODO: 上传到云存储
      const newImages = [...this.data.images, {
        url: file.url,
        isImage: true
      }];
      this.setData({ images: newImages });
    } catch (err) {
      wx.showToast({
        title: '上传失败',
        icon: 'none'
      });
    }
  },

  onDeleteImage(e) {
    const { index } = e.detail;
    const newImages = this.data.images.filter((_, i) => i !== index);
    this.setData({ images: newImages });
  },

  async onSubmit() {
    const { feedbackType, content } = this.data;
    
    if (!feedbackType) {
      wx.showToast({ title: '请选择问题类型', icon: 'none' });
      return;
    }
    
    if (!content.trim()) {
      wx.showToast({ title: '请描述您的问题', icon: 'none' });
      return;
    }
    
    try {
      wx.showLoading({ title: '提交中...', mask: true });
      
      await api.feedback.submit({
        type: feedbackType,
        content: content.trim(),
        images: this.data.images.map(img => img.url),
        contact: this.data.contact
      });
      
      wx.showToast({
        title: '提交成功，感谢您的反馈',
        icon: 'success',
        duration: 2000
      });
      
      setTimeout(() => {
        wx.navigateBack();
      }, 2000);
    } catch (err) {
      wx.showToast({
        title: err.message || '提交失败',
        icon: 'none'
      });
    } finally {
      wx.hideLoading();
    }
  }
});
