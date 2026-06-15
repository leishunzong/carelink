const api = require('../../../utils/api.js')

const relationOptions = ['本人', '父亲', '母亲', '爷爷', '奶奶', '外公', '外婆', '配偶', '子女', '其他']
const intellectOptions = ['正常', '轻度认知障碍', '中度认知障碍', '重度认知障碍']
const selfCareOptions = ['完全自理', '部分自理', '半自理', '完全不能自理']

function calcAge(birthday) {
  if (!birthday) return null
  const y = parseInt(birthday.slice(0, 4), 10)
  return new Date().getFullYear() - y
}

Page({
  data: {
    id: null,
    name: '',
    relationship: '母亲',
    relationText: '母亲',
    birthday: '',
    today: '',
    gender: 2,
    height: '',
    weight: '',
    intellectStatus: '正常',
    selfCareAbility: '完全自理',
    medicalHistory: '',
    remarks: '',
    isDefault: false,
    showPicker: false,
    pickerOptions: [],
    pickerType: ''
  },

  onLoad(options) {
    options = options || {}
    const now = new Date()
    const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
    const id = (options.id != null && options.id !== '') ? String(options.id).trim() || null : null
    this.setData({ id: id, today: today })
    if (id) {
      var that = this
      api.getSubject(id).then(function (res) {
        var item = res.data || res
        that.setData({
          name: item.name || '',
          relationship: item.relationship || '母亲',
          relationText: item.relationship || '母亲',
          birthday: item.birthday || '',
          gender: item.gender != null ? item.gender : 2,
          height: item.height != null ? String(item.height) : '',
          weight: item.weight != null ? String(item.weight) : '',
          intellectStatus: item.intellectStatus || '正常',
          selfCareAbility: item.selfCareAbility || item.selfCare || '完全自理',
          medicalHistory: item.medicalHistory || '',
          remarks: item.remarks || '',
          isDefault: !!item.isDefault
        })
      }).catch(function () {
        wx.showToast({ title: '加载失败', icon: 'none' })
      })
    }
  },

  onNameInput(e) {
    this.setData({ name: e.detail.value })
  },
  onBirthdayChange(e) {
    this.setData({ birthday: e.detail.value })
  },
  onHeightInput(e) {
    this.setData({ height: e.detail.value })
  },
  onWeightInput(e) {
    this.setData({ weight: e.detail.value })
  },
  onMedicalInput(e) {
    this.setData({ medicalHistory: e.detail.value })
  },
  onRemarksInput(e) {
    this.setData({ remarks: e.detail.value })
  },
  onDefaultChange(e) {
    this.setData({ isDefault: e.detail.value })
  },

  showRelationPicker() {
    this.setData({ showPicker: true, pickerOptions: relationOptions, pickerType: 'relation' })
  },
  showGenderPicker() {
    this.setData({ showPicker: true, pickerOptions: ['男', '女'], pickerType: 'gender' })
  },
  showIntellectPicker() {
    this.setData({ showPicker: true, pickerOptions: intellectOptions, pickerType: 'intellect' })
  },
  showSelfCarePicker() {
    this.setData({ showPicker: true, pickerOptions: selfCareOptions, pickerType: 'selfCare' })
  },

  hidePicker() {
    this.setData({ showPicker: false })
  },

  onPickerSelect(e) {
    const value = e.currentTarget.dataset.value
    const type = this.data.pickerType
    if (type === 'relation') {
      this.setData({ relationship: value, relationText: value })
    } else if (type === 'gender') {
      this.setData({ gender: value === '男' ? 1 : 2 })
    } else if (type === 'intellect') {
      this.setData({ intellectStatus: value })
    } else if (type === 'selfCare') {
      this.setData({ selfCareAbility: value })
    }
    this.setData({ showPicker: false })
  },

  onSave() {
    const { id, name, relationship, birthday, gender, intellectStatus, selfCareAbility, isDefault } = this.data
    if (!name || !name.trim()) {
      wx.showToast({ title: '请输入姓名', icon: 'none' })
      return
    }
    if (!birthday) {
      wx.showToast({ title: '请选择出生日期', icon: 'none' })
      return
    }
    const height = this.data.height ? parseInt(this.data.height, 10) : undefined
    const weight = this.data.weight ? parseInt(this.data.weight, 10) : undefined
    const payload = {
      name: name.trim(),
      relationship: relationship,
      birthday: birthday,
      gender: gender,
      height: height,
      weight: weight,
      intellectStatus: intellectStatus,
      selfCareAbility: selfCareAbility,
      medicalHistory: (this.data.medicalHistory || '').trim(),
      remarks: (this.data.remarks || '').trim(),
      isDefault: isDefault ? 1 : 0
    }
    var that = this
    var p = id ? api.updateSubject(id, payload) : api.addSubject(payload)
    p.then(function () {
      wx.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(function () { wx.navigateBack() }, 500)
    }).catch(function () {
      wx.showToast({ title: '保存失败', icon: 'none' })
    })
  }
})
