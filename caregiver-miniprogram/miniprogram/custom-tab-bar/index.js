Component({
  data: {
    active: 0,
    list: [
      {
        pagePath: '/pages/home/home',
        text: '首页'
      },
      {
        pagePath: '/pages/service/service',
        text: '服务'
      },
      {
        pagePath: '/pages/order/order',
        text: '订单'
      },
      {
        pagePath: '/pages/profile/profile',
        text: '我的'
      }
    ]
  },

  methods: {
    onChange(event) {
      const index = event.detail;
      // 如果点击的 tab 与当前 active 相同，不重复切换
      if (index === this.data.active) {
        return;
      }
      const page = this.data.list[index];
      this.setData({ active: index });
      wx.switchTab({
        url: page.pagePath
      });
    },

    // 更新选中状态（由页面调用）
    updateActive(active) {
      if (this.data.active !== active) {
        this.setData({ active });
      }
    }
  }
});
