var api = require('../../../utils/api.js')
var router = require('../../../utils/router.js')

const categoryList = [
  { id: 0, name: '全部' },
  { id: 1, name: '居家陪护' },
  { id: 2, name: '医院陪护' },
  { id: 3, name: '周期护理' },
  { id: 4, name: '家政服务' },
  { id: 5, name: '陪诊服务' },
  { id: 6, name: '母婴护理' }
]

const sortOptions = [
  { value: 'register_time', label: '注册时间' },
  { value: 'rating_desc', label: '好评率' },
  { value: 'orders_desc', label: '完单量' },
  { value: 'experience_desc', label: '从业年限' }
]
const ageOptions = [
  { value: '', label: '不限' },
  { value: '20-30', label: '20-30岁' },
  { value: '30-40', label: '30-40岁' },
  { value: '40-50', label: '40-50岁' },
  { value: '50-60', label: '50-60岁' },
  { value: '60-70', label: '60-70岁' }
]
const genderOptions = [
  { value: '', label: '不限' },
  { value: '1', label: '男' },
  { value: '2', label: '女' }
]
const expOptions = [
  { value: '', label: '不限' },
  { value: '0-3', label: '0-3年' },
  { value: '3-10', label: '3-10年' },
  { value: '10-20', label: '10-20年' },
  { value: '20-30', label: '20-30年' }
]
const eduOptions = [
  { value: '', label: '不限' },
  { value: '初中', label: '初中' },
  { value: '高中', label: '高中' },
  { value: '中专', label: '中专' },
  { value: '大专', label: '大专' },
  { value: '本科', label: '本科' },
  { value: '硕士', label: '硕士' }
]

function workStateToStatus(workState) {
  if (workState === 1) return '接单中'
  if (workState === 2) return '服务中'
  return '休息中'
}
function toStatusCode(status) {
  if (status === '接单中') return 'accepting'
  if (status === '服务中') return 'serving'
  return 'rest'
}
function ageFromBirthday(birthday) {
  if (!birthday) return null
  var y = parseInt(String(birthday).slice(0, 4), 10)
  if (isNaN(y)) return null
  return new Date().getFullYear() - y
}

function mapCaregiverItem(c) {
  var status = workStateToStatus(c.workState)
  
  // 好评率处理：兼容小数和百分比数值
  var goodRate = 0
  if (c.goodReviewRate != null) {
    var rate = Number(c.goodReviewRate)
    if (rate > 1) {
      goodRate = Math.round(rate)
    } else {
      goodRate = Math.round(rate * 100)
    }
  }
  
  return {
    id: c.id,
    name: c.realName || c.name || '',
    avatar: c.avatar || '',
    rating: c.averageRating != null ? c.averageRating : 0,
    experience: (c.workYears != null ? c.workYears : 0) + '年经验',
    age: c.age != null ? c.age : ageFromBirthday(c.birthday) || 0,
    gender: c.gender === 1 ? '男' : '女',
    origin: c.nativePlace || '',
    education: c.education || '',
    status: status,
    statusCode: toStatusCode(status),
    services: c.services || [],
    price: c.price != null ? c.price : 0,
    completedOrders: c.orderCount != null ? c.orderCount : 0,
    positiveRate: goodRate
  }
}

const PAGE_SIZE = 10
var app = getApp()

Page({
  data: {
    currentCity: '',
    searchQuery: '',
    selectedCategory: 0,
    categoryList,
    filters: { age: '', gender: '', experience: '', education: '' },
    sortBy: 'register_time',
    list: [],
    page: 1,
    loading: false,
    noMore: false,
    showPicker: false,
    pickerOptions: [],
    pickerType: '',
    pickerTitle: '',
    pickerSelected: ''
  },

  onLoad() {
    // 检查城市信息
    var cityName = api.getStoredCityName()
    console.log('[护工页] 页面加载，当前城市:', cityName)
    
    if (!cityName || cityName === '定位中…' || cityName === '选城市') {
      wx.showModal({
        title: '提示',
        content: '请先在首页选择您的城市，以查看该城市的护工信息',
        showCancel: false,
        success: function () {
          router.switchTab({ url: '/pages/index/index' })
        }
      })
      return
    }
    
    // 设置当前城市显示
    this.setData({ currentCity: cityName })
    this.loadList(true)
  },

  onShow() {
    var app = getApp()
    if (app.globalData.justLoggedIn) {
      app.globalData.justLoggedIn = false
    }
    var token = wx.getStorageSync('token') || app.globalData.token
    if (!token) {
      router.reLaunch({ url: '/pages/login/login' })
      return
    }
    
    // 检查城市是否变更，如果变更则刷新列表
    var currentCity = api.getStoredCityName()
    if (currentCity && currentCity !== this.data.currentCity) {
      console.log('[护工页] 城市已变更:', this.data.currentCity, '->', currentCity)
      this.setData({ currentCity: currentCity })
      this.loadList(true) // 重新加载护工列表
    }
  },

  onSearchInput(e) {
    this.setData({ searchQuery: e.detail.value })
  },

  onSearch() {
    this.loadList(true)
  },

  onCategory(e) {
    this.setData({ selectedCategory: e.currentTarget.dataset.id })
    this.loadList(true)
  },

  showSortPicker() {
    this.setData({ showPicker: true, pickerOptions: sortOptions, pickerType: 'sort', pickerTitle: '排序方式', pickerSelected: this.data.sortBy })
  },
  showAgePicker() {
    this.setData({ showPicker: true, pickerOptions: ageOptions, pickerType: 'age', pickerTitle: '年龄范围', pickerSelected: this.data.filters.age })
  },
  showGenderPicker() {
    this.setData({ showPicker: true, pickerOptions: genderOptions, pickerType: 'gender', pickerTitle: '性别', pickerSelected: this.data.filters.gender })
  },
  showExpPicker() {
    this.setData({ showPicker: true, pickerOptions: expOptions, pickerType: 'experience', pickerTitle: '从业经验', pickerSelected: this.data.filters.experience })
  },
  showEduPicker() {
    this.setData({ showPicker: true, pickerOptions: eduOptions, pickerType: 'education', pickerTitle: '学历', pickerSelected: this.data.filters.education })
  },

  hidePicker() {
    this.setData({ showPicker: false })
  },

  onPickerSelect(e) {
    const { value } = e.currentTarget.dataset
    const type = this.data.pickerType
    if (type === 'sort') {
      this.setData({ sortBy: value })
    } else {
      this.setData({ ['filters.' + type]: value })
    }
    this.setData({ showPicker: false })
    this.loadList(true)
  },

  loadList(reset) {
    if (this.data.loading) return
    if (this.data.noMore && !reset) return
    var that = this
    this.setData({ loading: true })
    if (reset) {
      this.setData({ page: 1, list: [], noMore: false })
    }
    var cityName = api.getStoredCityName()
    var cityCode = cityName ? api.getCityCode(cityName) : '156110100'
    
    console.log('[护工页] 当前城市信息 - 城市名:', cityName, '城市代码:', cityCode)
    
    // 如果没有城市信息，提示用户
    if (!cityName || cityName === '定位中…' || cityName === '选城市') {
      wx.showToast({ title: '请先选择城市', icon: 'none' })
      that.setData({ loading: false })
      return
    }
    var currentPage = reset ? 1 : that.data.page
    var filters = that.data.filters || {}
    var age = filters.age
    var minAge = null
    var maxAge = null
    if (age && age.indexOf('-') !== -1) {
      var parts = age.split('-')
      if (parts.length >= 2) { minAge = parseInt(parts[0], 10); maxAge = parseInt(parts[1], 10) }
    }
    var exp = filters.experience
    var minWorkYears = null
    var maxWorkYears = null
    if (exp && exp.indexOf('-') !== -1) {
      var p = exp.split('-')
      if (p.length >= 2) { minWorkYears = parseInt(p[0], 10); maxWorkYears = parseInt(p[1], 10) }
    }
    var gender = filters.gender
    if (gender === '男') gender = 1
    if (gender === '女') gender = 2
    if (filters.gender === '1' || filters.gender === '2') gender = parseInt(filters.gender, 10)
    var sortBy = that.data.sortBy || 'register_time'
    var sortField = sortBy === 'orders_desc' ? 'orderCount' : sortBy === 'rating_desc' ? 'goodReviewRate' : sortBy === 'experience_desc' ? 'workYears' : 'createTime'
    var sortOrder = 'DESC'
    var body = {
      cityCode: cityCode,
      nameKeyword: (that.data.searchQuery || '').trim() || undefined,
      gender: gender || undefined,
      minAge: minAge,
      maxAge: maxAge,
      minWorkYears: minWorkYears,
      maxWorkYears: maxWorkYears,
      education: (filters.education || '').trim() || undefined,
      packageCategory: that.data.selectedCategory === 0 ? undefined : that.data.selectedCategory,
      sortField: sortField,
      sortOrder: sortOrder,
      page: currentPage,
      size: PAGE_SIZE
    }
    console.log('[护工页] 请求参数 cityName=', cityName, 'cityCode=', cityCode, 'body=', JSON.stringify(body))
    api.searchCaregivers(body).then(function (res) {
      var records = res.records || (res.data && res.data.records) || []
      console.log('[护工页] 响应 res=', res, 'records.length=', records && records.length)
      
      // 验证返回的护工是否属于当前城市
      if (records && records.length > 0) {
        var wrongCityCount = 0
        records.forEach(function (item) {
          if (item.cityCode && item.cityCode !== cityCode) {
            wrongCityCount++
            console.warn('[护工页] 发现其他城市护工:', item.realName, '城市代码:', item.cityCode, '当前城市代码:', cityCode)
          }
        })
        if (wrongCityCount > 0) {
          console.warn('[护工页] 发现', wrongCityCount, '个其他城市的护工，可能需要后端接口优化')
        }
        
        // 前端过滤：只显示当前城市的护工
        records = records.filter(function (item) {
          return !item.cityCode || item.cityCode === cityCode
        })
        console.log('[护工页] 城市过滤后护工数量:', records.length)
      }
      
      var list = (records || []).map(mapCaregiverItem)
      var total = res.total != null ? res.total : (res.data && res.data.total)
      var noMore = list.length < PAGE_SIZE || (total != null && currentPage * PAGE_SIZE >= total)
      var nextList = reset ? list : that.data.list.concat(list)
      that.setData({
        list: nextList,
        page: currentPage + 1,
        loading: false,
        noMore: noMore
      })
    }).catch(function (err) {
      console.log('[护工页] 请求失败 err=', err)
      that.setData({ loading: false })
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  onReachBottom() {
    this.loadList(false)
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    router.navigateTo({ url: '/pages/caregiver/detail/detail?id=' + id })
  }
})
