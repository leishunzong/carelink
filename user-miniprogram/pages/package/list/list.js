// 服务包列表：对接 GET /package/page 与 /package/search
var api = require('../../../utils/api.js')
var router = require('../../../utils/router.js')
var categoryList = [
  { id: 0, name: '全部' },
  { id: 1, name: '居家陪护' },
  { id: 2, name: '医院陪护' },
  { id: 3, name: '周期护理' },
  { id: 4, name: '家政服务' },
  { id: 5, name: '陪诊服务' },
  { id: 6, name: '母婴护理' }
]

function getPriceDisplay(s) {
  if (s.allowDay && s.priceDay != null) return { price: s.priceDay, unit: '天' }
  if (s.allowMonth && s.priceMonth != null) return { price: s.priceMonth, unit: '月' }
  if (s.allowTimes && s.priceTimes != null) return { price: s.priceTimes, unit: '次' }
  if (s.allowHour && s.priceHour != null) return { price: s.priceHour, unit: '小时' }
  return { price: 0, unit: '次' }
}

function mapPackageItem(s) {
  var pd = getPriceDisplay(s)
  return {
    id: s.id,
    name: s.name,
    description: s.description || '',
    image: s.coverImage || s.image || '',
    sales: s.sales || 0,
    displayPrice: pd.price,
    displayUnit: pd.unit
  }
}

Page({
  data: {
    keyword: '',
    fromSearch: false,
    categoryId: 0,
    categoryName: '全部',
    categoryList: categoryList,
    list: [],
    loading: false,
    noMore: true,
    currentPage: 1
  },

  getMockData(keyword, categoryId) {
    var mockPackages = [
      { id: 1, name: '基础陪护服务', description: '日常生活照料、陪伴聊天', coverImage: '', sales: 128, priceDay: 200, allowDay: true, category: 1 },
      { id: 2, name: '慢病照护', description: '慢性病患者专业护理', coverImage: '', sales: 86, priceDay: 280, allowDay: true, category: 1 },
      { id: 3, name: '月嫂服务', description: '专业月嫂，母婴护理，新生儿护理', coverImage: '', sales: 156, priceMonth: 8800, allowMonth: true, category: 6 },
      { id: 4, name: '术后康复', description: '手术后康复护理指导', coverImage: '', sales: 64, priceDay: 350, allowDay: true, category: 3 },
      { id: 5, name: '陪诊服务', description: '医院陪诊、挂号取药', coverImage: '', sales: 92, priceTimes: 150, allowTimes: true, category: 5 },
      { id: 6, name: '家政保洁', description: '家庭清洁、整理收纳', coverImage: '', sales: 203, priceTimes: 120, allowTimes: true, category: 4 },
      { id: 7, name: '老年照护', description: '专业老年人照护服务', coverImage: '', sales: 174, priceDay: 260, allowDay: true, category: 1 },
      { id: 8, name: '高级陪护', description: '高端个性化陪护服务', coverImage: '', sales: 45, priceDay: 450, allowDay: true, category: 1 },
      { id: 9, name: '新生儿护理', description: '新生儿专业护理，24小时照护', coverImage: '', sales: 89, priceDay: 380, allowDay: true, category: 6 },
      { id: 10, name: '产妇护理', description: '产后妈妈专业护理服务', coverImage: '', sales: 67, priceDay: 320, allowDay: true, category: 6 }
    ]
    
    var filtered = mockPackages
    
    // 按关键词过滤（支持中文模糊搜索）
    if (keyword) {
      var kw = keyword.trim()
      console.log('过滤关键词:', kw)
      filtered = filtered.filter(function (item) {
        return item.name.indexOf(kw) >= 0 || 
               item.description.indexOf(kw) >= 0
      })
      console.log('过滤后结果:', filtered.length, '条')
    }
    
    // 按分类过滤
    if (categoryId > 0) {
      filtered = filtered.filter(function (item) {
        return item.category === categoryId
      })
    }
    
    return filtered.map(mapPackageItem)
  },

  onLoad(options) {
    var keyword = (options.keyword || '').trim()
    // 解码 URL 编码的搜索关键词
    if (keyword) {
      try {
        keyword = decodeURIComponent(keyword)
      } catch (e) {
        console.warn('关键词解码失败:', keyword)
      }
    }
    var fromSearch = options.from === 'search'
    var categoryId = options.categoryId !== undefined && options.categoryId !== '' ? parseInt(options.categoryId, 10) : 0
    var category = categoryList.find(function (c) { return c.id === categoryId }) || categoryList[0]
    this.setData({
      keyword: keyword,
      fromSearch: fromSearch,
      categoryId: categoryId,
      categoryName: category.name
    })
    // 从搜索进入时显示"搜索结果"，否则显示分类名称
    if (fromSearch) {
      wx.setNavigationBarTitle({ title: '搜索结果' })
    } else {
      wx.setNavigationBarTitle({ title: category.name })
    }
    this.loadList()
  },

  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value })
  },

  onSearch() {
    this.setData({ currentPage: 1 })
    this.loadList(true)
  },

  onTab(e) {
    var id = e.currentTarget.dataset.id
    var category = categoryList.find(function (c) { return c.id === id }) || categoryList[0]
    this.setData({
      categoryId: id,
      categoryName: category.name,
      currentPage: 1
    })
    wx.setNavigationBarTitle({ title: category.name })
    this.loadList(true)
  },

  loadList(reset) {
    if (this.data.loading) return
    var that = this
    var keyword = (this.data.keyword || '').trim()
    var categoryId = this.data.categoryId
    var current = reset !== false ? 1 : (this.data.currentPage || 0) + 1
    var size = 20
    this.setData({ loading: true })
    // 「全部」时不传 category，后端按不传分类参数查全部分页
    var pageParams = { current: current, size: size }
    if (categoryId !== 0) pageParams.category = categoryId
    var searchParams = { keyword: keyword, current: current, size: size }
    if (categoryId !== 0) searchParams.category = categoryId
    var p = keyword
      ? api.searchPackage(searchParams)
      : api.getPackagePage(pageParams)
    
    console.log('搜索参数:', keyword ? searchParams : pageParams)
    
    p.then(function (res) {
      console.log('搜索响应:', res)
      var records = res.records || (res.data && res.data.records) || []
      if (!Array.isArray(records)) {
        console.warn('响应数据格式异常:', res)
        records = []
      }
      var list = records.map(mapPackageItem)
      var total = res.total != null ? res.total : (res.data && res.data.total)
      var noMore = list.length < size || (total != null && current * size >= total)
      console.log('解析结果:', list.length, '条记录，总数:', total)
      that.setData({
        list: reset !== false ? list : that.data.list.concat(list),
        loading: false,
        noMore: noMore,
        currentPage: current
      })
    }).catch(function (err) {
      console.error('搜索失败，使用 mock 数据:', err)
      // 后端不可用时使用 mock 数据
      var mockData = that.getMockData(keyword, categoryId)
      that.setData({
        list: reset !== false ? mockData : that.data.list.concat(mockData),
        loading: false,
        noMore: true,
        currentPage: current
      })
    })
  },

  onReachBottom() {
    if (this.data.noMore || this.data.loading) return
    this.loadList(false)
  },

  goDetail(e) {
    var id = e.currentTarget.dataset.id
    router.navigateTo({ url: '/pages/package/detail/detail?id=' + id })
  },

  goBook(e) {
    var id = e.currentTarget.dataset.id
    console.log('预约服务包:', id)
    // 从服务包列表直接预约，跳转到系统匹配下单页
    router.navigateTo({ url: '/pages/order/create/create?packageId=' + id })
  }
})
