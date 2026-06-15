// 我的评价列表：对接 GET /review/user/my-list
var api = require('../../../utils/api.js')
var router = require('../../../utils/router.js')

var typeList = [
  { id: 'all', label: '全部' },
  { id: 'basic', label: '基础陪护' },
  { id: 'chronic', label: '慢病照护' },
  { id: 'senior', label: '老人看护' },
  { id: 'rehab', label: '术后康复' },
  { id: 'maternal', label: '母婴护理' },
  { id: 'medical', label: '医疗陪同' }
]

function mapReviewItem(r) {
  return {
    id: r.id,
    orderNo: r.orderNo,
    caregiverName: r.caregiverName,
    nickname: r.nickname || (r.isAnonymous ? '匿名用户' : '用户'),
    serviceName: r.packageName || r.serviceName || '服务',
    serviceType: r.serviceType || 'basic',
    rating: r.stars,
    type: r.type === 1 ? 'positive' : 'negative',
    content: r.content,
    tags: r.tags || [],
    serviceCompletedAt: r.serviceDate || r.createTime,
    createdAt: r.createTime,
    isAnonymous: !!r.isAnonymous
  }
}

Page({
  data: {
    typeList: typeList,
    selectedType: 'all',
    list: [],
    filteredList: [],
    loading: false
  },

  onLoad() {
    this.loadList()
  },

  onShow() {
    this.loadList()
  },

  loadList() {
    var that = this
    this.setData({ loading: true })
    api.getReviewMyList({ page: 1, size: 100 }).then(function (res) {
      var records = res.records || (res.data && res.data.records) || []
      var list = records.map(mapReviewItem)
      that.setData({ list: list, loading: false }, function () { that.filter() })
    }).catch(function () {
      that.setData({ loading: false }, function () { that.filter() })
    })
  },

  onType(e) {
    var id = e.currentTarget.dataset.id
    this.setData({ selectedType: id })
    this.filter()
  },

  filter() {
    var list = this.data.list
    var selectedType = this.data.selectedType
    var filteredList = selectedType === 'all' ? list : list.filter(function (r) { return r.serviceType === selectedType })
    this.setData({ filteredList: filteredList })
  },

  onReachBottom() {},

  goDetail(e) {
    var id = e.currentTarget.dataset.id
    router.navigateTo({ url: '/pages/review/detail/detail?id=' + id })
  }
})
