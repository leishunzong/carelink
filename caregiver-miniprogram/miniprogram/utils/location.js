// 腾讯位置服务封装
const QQ_MAP_KEY = 'XIRBZ-RFPAC-BUQ26-AHM4G-HYJIZ-ORBPS'
const QQ_MAP_BASE_URL = 'https://apis.map.qq.com'

/**
 * 逆地址解析（坐标位置描述）
 * @param {number} latitude 纬度
 * @param {number} longitude 经度
 * @param {object} options 可选参数
 * @param {number} options.radius 吸附半径（米），默认0，最大5000
 * @param {number} options.get_poi 是否返回周边POI，0不返回，1返回
 * @param {string} options.poi_options POI控制参数
 * @returns {Promise<object>} 返回地址解析结果
 */
function reverseGeocoder(latitude, longitude, options = {}) {
  return new Promise((resolve, reject) => {
    // 构建请求参数
    const params = {
      key: QQ_MAP_KEY,
      location: `${latitude},${longitude}`,
      ...options
    }

    // 构建查询字符串
    const queryString = Object.keys(params)
      .filter(key => params[key] !== undefined && params[key] !== null)
      .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
      .join('&')

    const url = `${QQ_MAP_BASE_URL}/ws/geocoder/v1/?${queryString}`

    wx.request({
      url,
      method: 'GET',
      success: (res) => {
        const { statusCode, data } = res

        if (statusCode === 200) {
          if (data.status === 0) {
            // 成功
            resolve(data.result)
          } else {
            // 业务错误
            wx.showToast({
              title: data.message || '地址解析失败',
              icon: 'none'
            })
            reject(data)
          }
        } else {
          // HTTP错误
          wx.showToast({
            title: `请求失败(${statusCode})`,
            icon: 'none'
          })
          reject(res)
        }
      },
      fail: (err) => {
        wx.showToast({
          title: '网络请求失败',
          icon: 'none'
        })
        reject(err)
      }
    })
  })
}

/**
 * 获取当前位置（包含详细地址信息）
 * @param {object} options 可选参数
 * @param {number} options.get_poi 是否返回周边POI，0不返回，1返回
 * @param {string} options.poi_options POI控制参数
 * @returns {Promise<object>} 返回位置信息
 */
function getCurrentLocation(options = {}) {
  return new Promise((resolve, reject) => {
    wx.getLocation({
      type: 'gcj02', // 腾讯地图使用GCJ02坐标系
      success: async (res) => {
        const { latitude, longitude } = res

        try {
          // 调用逆地址解析获取详细地址
          const addressInfo = await reverseGeocoder(latitude, longitude, options)

          // 组合返回完整的位置信息
          const locationInfo = {
            latitude,
            longitude,
            // 标准地址
            address: addressInfo.address,
            // 格式化地址
            formatted_addresses: addressInfo.formatted_addresses,
            // 推荐地址
            recommendAddress: addressInfo.formatted_addresses?.recommend || addressInfo.address,
            // 地址组件
            nation: addressInfo.address_component?.nation || '',
            province: addressInfo.address_component?.province || '',
            city: addressInfo.address_component?.city || '',
            district: addressInfo.address_component?.district || '',
            street: addressInfo.address_component?.street || '',
            street_number: addressInfo.address_component?.street_number || '',
            // 行政区划信息
            adcode: addressInfo.ad_info?.adcode || '',
            city_code: addressInfo.ad_info?.city_code || '',
            // 周边POI
            pois: addressInfo.pois || [],
            // 原始数据
            raw: addressInfo
          }

          resolve(locationInfo)
        } catch (err) {
          console.error('地址解析失败:', err)
          // 即使地址解析失败，也返回基本的经纬度信息
          resolve({
            latitude,
            longitude,
            address: `${latitude.toFixed(6)}, ${longitude.toFixed(6)}`,
            recommendAddress: '定位成功，地址解析失败'
          })
        }
      },
      fail: (err) => {
        console.error('获取位置失败:', err)
        
        // 检查是否授权
        if (err.errMsg && err.errMsg.indexOf('auth deny') !== -1) {
          wx.showModal({
            title: '需要位置权限',
            content: '请在设置中开启位置权限以使用定位功能',
            confirmText: '去设置',
            success: (res) => {
              if (res.confirm) {
                wx.openSetting()
              }
            }
          })
        } else {
          wx.showToast({
            title: '获取位置失败',
            icon: 'none'
          })
        }
        
        reject(err)
      }
    })
  })
}

/**
 * 根据POI选项格式化地址
 * @param {object} addressInfo 地址信息
 * @param {string} format 格式类型：'short'短地址，'long'长地址（默认）
 * @returns {string} 格式化后的地址
 */
function formatAddress(addressInfo, format = 'long') {
  if (format === 'short') {
    // 短地址：区 + 街道 + 门牌
    const parts = [
      addressInfo.district,
      addressInfo.street,
      addressInfo.street_number
    ].filter(Boolean)
    
    return parts.join(' ')
  }
  
  // 长地址（默认）
  return addressInfo.address || addressInfo.recommendAddress
}

/**
 * 计算两点之间的距离（单位：米）
 * @param {number} lat1 第一个点的纬度
 * @param {number} lng1 第一个点的经度
 * @param {number} lat2 第二个点的纬度
 * @param {number} lng2 第二个点的经度
 * @returns {number} 距离（米）
 */
function calculateDistance(lat1, lng1, lat2, lng2) {
  const radLat1 = lat1 * Math.PI / 180
  const radLat2 = lat2 * Math.PI / 180
  const a = radLat1 - radLat2
  const b = lng1 * Math.PI / 180 - lng2 * Math.PI / 180
  
  let s = 2 * Math.asin(Math.sqrt(
    Math.pow(Math.sin(a / 2), 2) +
    Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)
  ))
  
  s = s * 6378137 // 地球半径（米）
  s = Math.round(s * 10000) / 10000
  
  return s
}

/**
 * 格式化距离显示
 * @param {number} distance 距离（米）
 * @returns {string} 格式化后的距离字符串
 */
function formatDistance(distance) {
  if (distance < 1000) {
    return `${Math.round(distance)}米`
  }
  
  return `${(distance / 1000).toFixed(1)}公里`
}

module.exports = {
  QQ_MAP_KEY,
  reverseGeocoder,
  getCurrentLocation,
  formatAddress,
  calculateDistance,
  formatDistance
}
