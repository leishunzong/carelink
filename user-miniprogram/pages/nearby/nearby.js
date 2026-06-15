var router = require('../../utils/router.js')

Page({
  data: {
    list: [
      { id: 1, name: '王明慧', avatar: '', rating: 4.8, distance: '0.8km', experience: '5年经验', services: ['居家陪护', '医院陪护'], completedOrders: 328, positiveRate: 98 },
      { id: 2, name: '李秀芳', avatar: '', rating: 4.9, distance: '1.2km', experience: '8年经验', services: ['周期护理', '家政服务'], completedOrders: 562, positiveRate: 99 },
      { id: 3, name: '张翠珍', avatar: '', rating: 4.7, distance: '1.5km', experience: '6年经验', services: ['母婴护理', '居家陪护'], completedOrders: 426, positiveRate: 97 },
      { id: 4, name: '赵大姐', avatar: '', rating: 4.6, distance: '1.8km', experience: '4年经验', services: ['陪诊服务', '居家陪护'], completedOrders: 218, positiveRate: 95 },
      { id: 5, name: '孙阿姨', avatar: '', rating: 4.8, distance: '2.1km', experience: '7年经验', services: ['居家陪护', '周期护理'], completedOrders: 467, positiveRate: 98 }
    ],
    loading: false
  },

  onReachBottom() {},
  goDetail(e) {
    router.navigateTo({ url: '/pages/caregiver/detail/detail?id=' + e.currentTarget.dataset.id })
  }
})
