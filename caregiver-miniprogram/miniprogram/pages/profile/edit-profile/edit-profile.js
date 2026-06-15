const api = require('../../../utils/api')
const { EDUCATION_OPTIONS, ETHNICITY_OPTIONS, PROVINCE_OPTIONS } = require('../../../utils/constants')
const app = getApp()

Page({
  data: {
    formData: {
      avatar: '',
      realName: '',
      phone: '',
      gender: null,
      birthday: '',
      nativePlace: '',
      education: '',
      ethnicity: '',
      workYears: '',
      residentAddress: ''
    },
    verifyStatus: 0,
    loading: true,

    // 性别选项
    genderOptions: ['男', '女'],
    genderIndex: -1,

    // 学历选项
    educationOptions: EDUCATION_OPTIONS || ['初中', '高中', '中专', '大专', '本科', '硕士', '博士'],
    educationIndex: -1,

    // 民族选项
    ethnicityOptions: ETHNICITY_OPTIONS || ['汉族'],
    ethnicityIndex: -1,

    // 籍贯选项
    provinceOptions: PROVINCE_OPTIONS || ['北京', '天津', '上海'],
    provinceIndex: -1
  },

  onLoad() {
    this.loadUserInfo()
  },

  async loadUserInfo() {
    try {
      wx.showLoading({ title: '加载中...', mask: true })
      const res = await api.caregiver.getInfo()

      const genderIndex = res.gender ? (res.gender === 1 ? 0 : 1) : -1
      const educationIndex = this.data.educationOptions.indexOf(res.education)
      const ethnicityIndex = this.data.ethnicityOptions.indexOf(res.ethnicity)
      const provinceIndex = this.data.provinceOptions.indexOf(res.nativePlace)

      this.setData({
        formData: {
          avatar: res.avatar || '',
          realName: res.realName || '',
          phone: res.phone || '',
          gender: res.gender || null,
          birthday: res.birthday || '',
          nativePlace: res.nativePlace || '',
          education: res.education || '',
          ethnicity: res.ethnicity || '',
          workYears: res.workYears != null ? String(res.workYears) : '',
          residentAddress: res.residentAddress || ''
        },
        verifyStatus: res.verifyStatus || 0,
        genderIndex,
        educationIndex: educationIndex >= 0 ? educationIndex : -1,
        ethnicityIndex: ethnicityIndex >= 0 ? ethnicityIndex : -1,
        provinceIndex: provinceIndex >= 0 ? provinceIndex : -1,
        loading: false
      })
    } catch (err) {
      console.error('加载用户信息失败:', err)
      wx.showToast({ title: '加载失败', icon: 'none' })
      this.setData({ loading: false })
    } finally {
      wx.hideLoading()
    }
  },

  // 选择头像
  onChooseAvatar() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: async (res) => {
        const tempFilePath = res.tempFiles[0].tempFilePath
        try {
          wx.showLoading({ title: '上传中...', mask: true })
          const url = await api.file.uploadImage(tempFilePath)
          this.setData({ 'formData.avatar': url })
          wx.showToast({ title: '上传成功', icon: 'success' })
        } catch (err) {
          console.error('上传头像失败:', err)
          wx.showToast({ title: '上传失败', icon: 'none' })
        } finally {
          wx.hideLoading()
        }
      }
    })
  },

  // 字段输入
  onFieldChange(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`formData.${field}`]: e.detail })
  },

  // 性别选择
  onGenderChange(e) {
    const index = Number(e.detail.value)
    this.setData({
      genderIndex: index,
      'formData.gender': index === 0 ? 1 : 2
    })
  },

  // 生日选择
  onBirthdayChange(e) {
    this.setData({ 'formData.birthday': e.detail.value })
  },

  // 学历选择
  onEducationChange(e) {
    const index = Number(e.detail.value)
    this.setData({
      educationIndex: index,
      'formData.education': this.data.educationOptions[index]
    })
  },

  // 民族选择
  onEthnicityChange(e) {
    const index = Number(e.detail.value)
    this.setData({
      ethnicityIndex: index,
      'formData.ethnicity': this.data.ethnicityOptions[index]
    })
  },

  // 籍贯选择
  onProvinceChange(e) {
    const index = Number(e.detail.value)
    this.setData({
      provinceIndex: index,
      'formData.nativePlace': this.data.provinceOptions[index]
    })
  },

  // 提交保存
  async onSubmit() {
    const { realName, phone, gender, residentAddress } = this.data.formData

    if (!realName || !realName.trim()) {
      wx.showToast({ title: '请输入姓名', icon: 'none' })
      return
    }
    if (!phone || !/^1[3-9]\d{9}$/.test(phone)) {
      wx.showToast({ title: '请输入有效手机号', icon: 'none' })
      return
    }
    if (!gender) {
      wx.showToast({ title: '请选择性别', icon: 'none' })
      return
    }

    try {
      wx.showLoading({ title: '保存中...', mask: true })

      const data = {
        ...this.data.formData,
        workYears: this.data.formData.workYears ? parseInt(this.data.formData.workYears) : null
      }

      await api.caregiver.updateInfo(data)

      wx.showToast({ title: '保存成功', icon: 'success' })

      // 更新全局用户信息
      app.updateUserInfo(data)

      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
    } catch (err) {
      console.error('保存失败:', err)
      wx.showToast({ title: err.message || '保存失败', icon: 'none' })
    } finally {
      wx.hideLoading()
    }
  }
})
