const api = require('../../utils/api')
const util = require('../../utils/util')
const location = require('../../utils/location')

Page({
  data: {
    currentStep: 0,
    steps: [
      { text: '基本信息' },
      { text: '证件认证' },
      { text: '服务城市' }
    ],
    
    // 表单数据
    formData: {
      realName: '',
      gender: '1',
      birthday: '',
      nativePlace: '',
      education: '',
      ethnicity: '',
      workYears: '',
      idCardFrontUrl: '',
      idCardBackUrl: '',
      qualificationCertUrl: '',
      otherMaterialUrls: [],
      cityCode: '',
      cityName: '',
      residentAddress: '',
      longitude: '',
      latitude: ''
    },

    // 上传文件列表
    idCardFrontList: [],
    idCardBackList: [],
    qualificationCertList: [],
    otherMaterialsList: [],

    // 选择器
    showBirthdayPopup: false,
    birthdayValue: new Date('2000-01-01').getTime(),
    minDate: new Date('1950-01-01').getTime(),
    maxDate: new Date('2005-12-31').getTime(),
    
    showEducationPopup: false,
    educationOptions: ['初中及以下', '高中/中专', '大专', '本科', '硕士及以上'],

    locationLoading: false,
    submitting: false
  },

  onLoad() {
    // 检查是否已登录
    const app = getApp()
    if (!app.globalData.hasLogin) {
      wx.redirectTo({
        url: '/pages/login/login'
      })
      return
    }

    // 如果已经审核通过，跳转首页
    const userInfo = app.globalData.userInfo
    if (userInfo && userInfo.verifyStatus === 1) {
      wx.switchTab({
        url: '/pages/home/home'
      })
    }
  },

  // 字段输入
  onFieldChange(e) {
    const field = e.currentTarget.dataset.field
    this.setData({
      [`formData.${field}`]: e.detail
    })
  },

  // 性别选择
  onGenderChange(e) {
    this.setData({
      'formData.gender': e.detail
    })
  },

  // 显示生日选择器
  showBirthdayPicker() {
    this.setData({ showBirthdayPopup: true })
  },

  closeBirthdayPicker() {
    this.setData({ showBirthdayPopup: false })
  },

  onBirthdayConfirm(e) {
    const birthday = util.formatDate(new Date(e.detail))
    this.setData({
      'formData.birthday': birthday,
      showBirthdayPopup: false
    })
  },

  // 显示学历选择器
  showEducationPicker() {
    this.setData({ showEducationPopup: true })
  },

  closeEducationPicker() {
    this.setData({ showEducationPopup: false })
  },

  onEducationConfirm(e) {
    this.setData({
      'formData.education': e.detail.value,
      showEducationPopup: false
    })
  },

  // 上传身份证正面
  async onUploadIdCardFront(e) {
    const { file } = e.detail
    try {
      const url = await this.uploadImage(file.url)
      this.setData({
        'formData.idCardFrontUrl': url,
        idCardFrontList: [{ url: file.url, isImage: true }]
      })
      wx.showToast({title:'上传成功',icon:'success'})
    } catch (err) {
      wx.showToast({title:'上传失败',icon:'none'})
    }
  },

  onDeleteIdCardFront() {
    this.setData({
      'formData.idCardFrontUrl': '',
      idCardFrontList: []
    })
  },

  // 上传身份证反面
  async onUploadIdCardBack(e) {
    const { file } = e.detail
    try {
      const url = await this.uploadImage(file.url)
      this.setData({
        'formData.idCardBackUrl': url,
        idCardBackList: [{ url: file.url, isImage: true }]
      })
      wx.showToast({title:'上传成功',icon:'success'})
    } catch (err) {
      wx.showToast({title:'上传失败',icon:'none'})
    }
  },

  onDeleteIdCardBack() {
    this.setData({
      'formData.idCardBackUrl': '',
      idCardBackList: []
    })
  },

  // 上传资格证
  async onUploadQualificationCert(e) {
    const { file } = e.detail
    try {
      const url = await this.uploadImage(file.url)
      this.setData({
        'formData.qualificationCertUrl': url,
        qualificationCertList: [{ url: file.url, isImage: true }]
      })
      wx.showToast({title:'上传成功',icon:'success'})
    } catch (err) {
      wx.showToast({title:'上传失败',icon:'none'})
    }
  },

  onDeleteQualificationCert() {
    this.setData({
      'formData.qualificationCertUrl': '',
      qualificationCertList: []
    })
  },

  // 上传其他材料
  async onUploadOtherMaterials(e) {
    const { file } = e.detail
    try {
      const url = await this.uploadImage(file.url)
      const otherMaterialUrls = [...this.data.formData.otherMaterialUrls, url]
      const otherMaterialsList = [...this.data.otherMaterialsList, { url: file.url, isImage: true }]
      
      this.setData({
        'formData.otherMaterialUrls': otherMaterialUrls,
        otherMaterialsList: otherMaterialsList
      })
      wx.showToast({title:'上传成功',icon:'success'})
    } catch (err) {
      wx.showToast({title:'上传失败',icon:'none'})
    }
  },

  onDeleteOtherMaterials(e) {
    const { index } = e.detail
    const otherMaterialUrls = [...this.data.formData.otherMaterialUrls]
    const otherMaterialsList = [...this.data.otherMaterialsList]
    
    otherMaterialUrls.splice(index, 1)
    otherMaterialsList.splice(index, 1)
    
    this.setData({
      'formData.otherMaterialUrls': otherMaterialUrls,
      otherMaterialsList: otherMaterialsList
    })
  },

  // 上传图片到服务器
  async uploadImage(tempFilePath) {
    // TODO: 实现真实的图片上传逻辑
    // 这里返回模拟URL
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve('https://example.com/upload/' + Date.now() + '.jpg')
      }, 500)
    })
  },

  // 选择城市
  chooseCity() {
    wx.chooseLocation({
      success: async (res) => {
        const { latitude, longitude, address, name } = res
        
        try {
          // 调用逆地址解析获取准确的城市信息
          const addressInfo = await location.reverseGeocoder(latitude, longitude)
          const city = addressInfo.address_component?.city || ''
          const cityCode = addressInfo.ad_info?.city_code || addressInfo.ad_info?.adcode || ''
          const fullAddress = name ? `${address} ${name}` : address
          
          this.setData({
            'formData.cityCode': cityCode,
            'formData.cityName': city,
            'formData.residentAddress': fullAddress,
            'formData.longitude': longitude,
            'formData.latitude': latitude
          })
        } catch (err) {
          // 逆地址解析失败时，回退到正则匹配
          console.error('地址解析失败，使用正则匹配:', err)
          const cityMatch = (address || '').match(/(.+?[市])/)
          const cityName = cityMatch ? cityMatch[1] : '未知城市'
          
          this.setData({
            'formData.cityCode': `${latitude}_${longitude}`,
            'formData.cityName': cityName,
            'formData.residentAddress': address || '',
            'formData.longitude': longitude,
            'formData.latitude': latitude
          })
        }
      },
      fail: (err) => {
        console.error('选择位置失败:', err)
        wx.showToast({ title: '请授权位置权限', icon: 'none' })
      }
    })
  },

  // 获取当前位置
  async getLocation() {
    this.setData({ locationLoading: true })
    
    try {
      const locationInfo = await location.getCurrentLocation()
      
      this.setData({
        'formData.longitude': locationInfo.longitude,
        'formData.latitude': locationInfo.latitude,
        'formData.cityName': locationInfo.city || '',
        'formData.cityCode': locationInfo.city_code || locationInfo.adcode || '',
        'formData.residentAddress': locationInfo.recommendAddress || locationInfo.address || '',
        locationLoading: false
      })
      
      wx.showToast({ title: '定位成功', icon: 'success' })
    } catch (err) {
      console.error('定位失败:', err)
      this.setData({ locationLoading: false })
    }
  },

  // 验证步骤1
  validateStep1() {
    const { realName, gender, birthday, workYears } = this.data.formData
    
    if (!realName || !realName.trim()) {
      wx.showToast({title:'请输入真实姓名',icon:'none'})
      return false
    }

    if (!birthday) {
      wx.showToast({title:'请选择出生日期',icon:'none'})
      return false
    }

    return true
  },

  // 验证步骤2
  validateStep2() {
    const { idCardFrontUrl, idCardBackUrl, qualificationCertUrl } = this.data.formData
    
    if (!idCardFrontUrl) {
      wx.showToast({title:'请上传身份证正面',icon:'none'})
      return false
    }

    if (!idCardBackUrl) {
      wx.showToast({title:'请上传身份证反面',icon:'none'})
      return false
    }

    if (!qualificationCertUrl) {
      wx.showToast({title:'请上传护工资格证',icon:'none'})
      return false
    }

    return true
  },

  // 验证步骤3
  validateStep3() {
    const { cityName, residentAddress, longitude, latitude } = this.data.formData
    
    if (!cityName) {
      wx.showToast({title:'请选择服务城市',icon:'none'})
      return false
    }

    if (!residentAddress || !residentAddress.trim()) {
      wx.showToast({title:'请输入常驻地址',icon:'none'})
      return false
    }

    if (!longitude || !latitude) {
      wx.showToast({title:'请获取位置信息',icon:'none'})
      return false
    }

    return true
  },

  // 下一步
  nextStep() {
    const { currentStep } = this.data
    
    // 验证当前步骤
    if (currentStep === 0 && !this.validateStep1()) {
      return
    }
    if (currentStep === 1 && !this.validateStep2()) {
      return
    }

    this.setData({
      currentStep: currentStep + 1
    })
  },

  // 上一步
  prevStep() {
    this.setData({
      currentStep: this.data.currentStep - 1
    })
  },

  // 提交申请
  async submitApplication() {
    // 验证第三步
    if (!this.validateStep3()) {
      return
    }

    this.setData({ submitting: true })

    try {
      const { formData } = this.data
      
      // 转换性别为数字
      const submitData = {
        ...formData,
        gender: parseInt(formData.gender),
        workYears: parseInt(formData.workYears) || 0
      }

      await api.caregiver.settle(submitData)

      wx.showToast({title:'提交成功',icon:'success'})

      // 提示等待审核
      setTimeout(() => {
        wx.showModal({
          title: '提交成功',
          content: '您的入驻申请已提交，请耐心等待审核。审核结果将通过短信通知您。',
          showCancel: false,
          success: () => {
            wx.switchTab({
              url: '/pages/home/home'
            })
          }
        })
      }, 1500)

    } catch (err) {
      console.error('提交失败:', err)
      wx.showToast({title:err.message || '提交失败，请重试',icon:'none'})
    } finally {
      this.setData({ submitting: false })
    }
  }
})