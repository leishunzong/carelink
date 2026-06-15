// pages/profile/earnings/earnings.js
const api = require('../../../utils/api');

Page({
  data: {
    // 收益概览
    summary: {
      totalEarnings: 0,
      monthEarnings: 0,
      withdrawableAmount: 0
    },
    
    // 列表数据
    earningsList: [],
    loading: false,
    hasMore: true,
    pageNum: 1,
    pageSize: 10,
    activeTab: 'all',
    
    // 状态映射
    statusMap: {
      'pending': '处理中',
      'success': '已完成',
      'failed': '失败'
    },
    
    // 提现弹窗
    showWithdrawDialog: false,
    withdrawAmount: ''
  },

  onLoad() {
    this.loadSummary();
    this.loadEarningsList();
  },

  // 加载收益概览
  async loadSummary() {
    try {
      const res = await api.earnings.getSummary();
      this.setData({ summary: res.data });
    } catch (err) {
      console.error('加载收益概览失败', err);
    }
  },

  // 加载收益列表
  async loadEarningsList(refresh = false) {
    if (this.data.loading) return;
    
    this.setData({ loading: true });
    
    try {
      const params = {
        pageNum: refresh ? 1 : this.data.pageNum,
        pageSize: this.data.pageSize,
        type: this.data.activeTab
      };
      
      const res = await api.earnings.getList(params);
      
      const newList = refresh 
        ? res.data.list 
        : [...this.data.earningsList, ...res.data.list];
      
      this.setData({
        earningsList: newList,
        hasMore: res.data.hasNext,
        pageNum: refresh ? 1 : this.data.pageNum,
        loading: false
      });
    } catch (err) {
      console.error('加载收益列表失败', err);
      wx.showToast({
        title: err.message || '加载失败',
        icon: 'none'
      });
      this.setData({ loading: false });
    }
  },

  // 切换标签
  onTabChange(e) {
    this.setData({ 
      activeTab: e.detail.name,
      pageNum: 1
    });
    this.loadEarningsList(true);
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.loadSummary();
    this.loadEarningsList(true);
    setTimeout(() => {
      wx.stopPullDownRefresh();
    }, 1000);
  },

  // 触底加载
  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.setData({ pageNum: this.data.pageNum + 1 });
      this.loadEarningsList();
    }
  },

  // 申请提现
  onWithdraw() {
    this.setData({
      showWithdrawDialog: true,
      withdrawAmount: ''
    });
  },

  // 提现金额变化
  onWithdrawAmountChange(e) {
    this.setData({ withdrawAmount: e.detail });
  },

  // 确认提现
  async onConfirmWithdraw() {
    const amount = parseFloat(this.data.withdrawAmount);
    
    // 验证
    if (!amount || amount <= 0) {
      wx.showToast({
        title: '请输入有效金额',
        icon: 'none'
      });
      return Promise.reject();
    }
    
    if (amount > this.data.summary.withdrawableAmount) {
      wx.showToast({
        title: '提现金额超过可用余额',
        icon: 'none'
      });
      return Promise.reject();
    }
    
    try {
      await api.earnings.withdraw({ amount });
      
      wx.showToast({
        title: '提现申请已提交',
        icon: 'success'
      });
      
      this.setData({ showWithdrawDialog: false });
      this.loadSummary();
      this.loadEarningsList(true);
    } catch (err) {
      wx.showToast({
        title: err.message || '提现失败',
        icon: 'none'
      });
      return Promise.reject();
    }
  },

  // 取消提现
  onCancelWithdraw() {
    this.setData({ showWithdrawDialog: false });
  }
});
