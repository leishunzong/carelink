// utils/util.js

/**
 * 格式化时间
 */
const formatTime = date => {
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = date.getHours()
  const minute = date.getMinutes()
  const second = date.getSeconds()

  return `${[year, month, day].map(formatNumber).join('-')} ${[hour, minute, second].map(formatNumber).join(':')}`
}

/**
 * 格式化日期
 */
const formatDate = date => {
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()

  return `${[year, month, day].map(formatNumber).join('-')}`
}

const formatNumber = n => {
  n = n.toString()
  return n[1] ? n : `0${n}`
}

/**
 * 计算年龄
 */
const calculateAge = (birthday) => {
  if (!birthday) return 0
  const birthDate = new Date(birthday)
  const today = new Date()
  let age = today.getFullYear() - birthDate.getFullYear()
  const monthDiff = today.getMonth() - birthDate.getMonth()
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
    age--
  }
  return age
}

/**
 * 计算星座
 */
const calculateZodiac = (birthday) => {
  if (!birthday) return ''
  const date = new Date(birthday)
  const month = date.getMonth() + 1
  const day = date.getDate()
  
  const zodiacArr = ['水瓶座', '双鱼座', '白羊座', '金牛座', '双子座', '巨蟹座', 
                     '狮子座', '处女座', '天秤座', '天蝎座', '射手座', '摩羯座']
  const zodiacDays = [20, 19, 21, 21, 21, 22, 23, 23, 23, 23, 22, 22]
  
  let zodiacIndex = month - 1
  if (day < zodiacDays[zodiacIndex]) {
    zodiacIndex = zodiacIndex - 1
    if (zodiacIndex < 0) {
      zodiacIndex = 11
    }
  }
  
  return zodiacArr[zodiacIndex]
}

/**
 * 获取当前位置
 */
const getCurrentLocation = () => {
  return new Promise((resolve, reject) => {
    wx.getLocation({
      type: 'gcj02',
      success: (res) => {
        resolve({
          latitude: res.latitude,
          longitude: res.longitude
        })
      },
      fail: (err) => {
        wx.showToast({
          title: '获取位置失败',
          icon: 'none'
        })
        reject(err)
      }
    })
  })
}

/**
 * 选择图片
 */
const chooseImage = (count = 1) => {
  return new Promise((resolve, reject) => {
    wx.chooseImage({
      count,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        resolve(res.tempFilePaths)
      },
      fail: reject
    })
  })
}

/**
 * 防抖函数
 */
const debounce = (fn, delay = 500) => {
  let timer = null
  return function (...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn.apply(this, args)
    }, delay)
  }
}

/**
 * 节流函数
 */
const throttle = (fn, delay = 500) => {
  let last = 0
  return function (...args) {
    const now = Date.now()
    if (now - last > delay) {
      fn.apply(this, args)
      last = now
    }
  }
}

module.exports = {
  formatTime,
  formatDate,
  calculateAge,
  calculateZodiac,
  getCurrentLocation,
  chooseImage,
  debounce,
  throttle
}
