// 评价详情：无单条接口，通过 GET /review/user/my-list 拉取列表后按 id 查找
var api = require('../../../utils/api.js')

function mapReviewToDetail(r) {
  return {
    id: r.id,
    orderNo: r.orderNo,
    caregiverName: r.caregiverName,
    type: r.type === 1 ? 'positive' : 'negative',
    serviceName: r.packageName || r.serviceName || '服务',
    rating: r.stars,
    content: r.content,
    tags: r.tags || [],
    createdAt: r.createTime
  }
}

Page({
  data: {
    detail: {},
    loading: true
  },

  onLoad(options) {
    var that = this
    var id = options.id ? parseInt(options.id, 10) : 0
    if (!id) {
      that.setData({ detail: {}, loading: false })
      return
    }
    api.getReviewMyList({ page: 1, size: 100 }).then(function (res) {
      var records = res.records || (res.data && res.data.records) || []
      var raw = records.filter(function (r) { return r.id == id })[0]
      var detail = raw ? mapReviewToDetail(raw) : {}
      that.setData({ detail: detail, loading: false })
    }).catch(function () {
      that.setData({ detail: {}, loading: false })
    })
  }
})
