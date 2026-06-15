/**
 * 腾讯位置服务封装（逆地址解析、行政区划搜索、城市 -> cityCode）
 * 文档：https://lbs.qq.com/service/webService/webServiceGuide/webServiceGcoder
 * Key 请求时作为参数携带。
 * 小程序发布前需在后台将 https://apis.map.qq.com 加入 request 合法域名；开发时可勾选「不校验合法域名」。
 */
var KEY = 'XIRBZ-RFPAC-BUQ26-AHM4G-HYJIZ-ORBPS'
var BASE_GEOCODER = 'https://apis.map.qq.com/ws/geocoder/v1/'
var BASE_DISTRICT = 'https://apis.map.qq.com/ws/district/v1/search'

/** 中国国家码（3 位），cityCode = 国家码 + adcode（6 位）= 9 位 */
var NATION_CODE_CN = '156'

/** 将 adcode（6 位行政区划代码）拼接为国家码+adcode 的 9 位 cityCode */
function toCityCode(adcode) {
  if (!adcode) return ''
  var a = String(adcode).trim()
  var six = a.length >= 6 ? a.substring(0, 6) : a
  return NATION_CODE_CN + six
}

/** 逆地址返回的 adcode 可能是区/街道级，市级 adcode 后两位为 0，取前 4 位 + 00 得到城市级 6 位 adcode */
function toCityLevelAdcode(adcode) {
  if (!adcode) return ''
  var a = String(adcode).trim()
  var six = a.length >= 6 ? a.substring(0, 6) : a.padEnd(6, '0')
  return six.substring(0, 4) + '00'
}

/**
 * 逆地址解析（经纬度 -> 地址信息）
 * @param {number} lat 纬度（GCJ02）
 * @param {number} lng 经度（GCJ02）
 * @param {object} options 可选 { radius, get_poi }
 * @returns {Promise<object>} 仅返回所需字段：province, city, district, street, street_number, town, address, adcode, city_code
 */
function reverseGeocode(lat, lng, options) {
  var location = lat + ',' + lng
  var url = BASE_GEOCODER + '?location=' + location + '&key=' + KEY + '&output=json'
  if (options && options.radius != null) url += '&radius=' + options.radius
  if (options && options.get_poi === 1) url += '&get_poi=1'
  console.log('[location] 逆地址解析请求', { lat: lat, lng: lng })
  return new Promise(function (resolve, reject) {
    wx.request({
      url: url,
      method: 'GET',
      success: function (res) {
        var data = res.data
        console.log('[location] 逆地址解析响应 status=', data.status, 'message=', data.message)
        if (data.status !== 0) {
          reject(new Error(data.message || '逆地址解析失败'))
          return
        }
        var result = data.result || {}
        var comp = result.address_component || {}
        var adInfo = result.ad_info || {}
        var ref = result.address_reference || {}
        var townObj = ref.town || {}
        var adcode = (adInfo.adcode || '').trim()
        var province = (comp.province || '').trim()
        var cityRaw = (comp.city || '').trim()
        // 直辖市时 city 常为空或「市辖区」，用 province 作为城市名（如 北京市）
        var city = (cityRaw && cityRaw !== '市辖区') ? cityRaw : province
        // 市级 adcode 后两位为 0，逆地址可能返回区/街道级，统一转为城市级再生成 9 位 cityCode
        var cityLevelSix = toCityLevelAdcode(adcode)
        var out = {
          province: province,
          city: city,
          district: comp.district || '',
          street: comp.street || '',
          street_number: comp.street_number || '',
          town: townObj.title || '',
          address: result.address || '',
          adcode: adcode,
          cityCode: toCityCode(cityLevelSix)
        }
        console.log('[location] 逆地址解析结果 city=', out.city, 'adcode=', adcode, '城市级adcode=', cityLevelSix, 'cityCode=', out.cityCode)
        resolve(out)
      },
      fail: function (err) {
        console.log('[location] 逆地址解析请求失败', err)
        reject(err)
      }
    })
  })
}

/**
 * 行政区划搜索（关键词或 adcode）
 * @param {string} keyword 关键词，如「北京」，或 adcode 如 130681
 * @param {object} options 可选 { get_polygon, max_offset }
 * @returns {Promise<Array>} 区划列表，每项 { id(adcode), name, fullname, level, location, districts }
 */
function districtSearch(keyword, options) {
  if (!keyword || !String(keyword).trim()) {
    return Promise.resolve([])
  }
  var url = BASE_DISTRICT + '?keyword=' + encodeURIComponent(String(keyword).trim()) + '&key=' + KEY + '&output=json'
  if (options && options.get_polygon != null) url += '&get_polygon=' + options.get_polygon
  if (options && options.max_offset != null) url += '&max_offset=' + options.max_offset
  console.log('[location] 行政区划搜索请求 keyword=', keyword)
  return new Promise(function (resolve, reject) {
    wx.request({
      url: url,
      method: 'GET',
      success: function (res) {
        var data = res.data
        var r = data.result
        var len = Array.isArray(r) ? r.length : (r && Array.isArray(r[0]) ? r[0].length : 0)
        console.log('[location] 行政区划搜索响应 status=', data.status, 'result条数=', len)
        if (data.status !== 0) {
          reject(new Error(data.message || '行政区划搜索失败'))
          return
        }
        var result = data.result
        if (!result || !Array.isArray(result)) {
          resolve([])
          return
        }
        // 旧版：result[0] 为一级区划数组，result[1] 为二级…
        if (result.length > 0 && Array.isArray(result[0])) {
          resolve(result[0])
          return
        }
        // 新版：result 为 array of object
        resolve(result)
      },
      fail: function (err) {
        console.log('[location] 行政区划搜索请求失败', err)
        reject(err)
      }
    })
  })
}

/**
 * 根据城市关键词获取 cityCode（国家码 156 + adcode 6 位 = 9 位）
 * @param {string} keyword 城市名或关键词，如「北京」「北京市」
 * @returns {Promise<{ cityCode: string, cityName: string } | null>}
 */
function getCityCodeByKeyword(keyword) {
  return districtSearch(keyword).then(function (list) {
    if (!list || list.length === 0) return null
    var first = list[0]
    var adcode = first.id || ''
    var cityName = first.fullname || first.name || keyword
    if (!adcode) return null
    return { cityCode: toCityCode(adcode), cityName: cityName }
  })
}

module.exports = {
  reverseGeocode: reverseGeocode,
  districtSearch: districtSearch,
  getCityCodeByKeyword: getCityCodeByKeyword,
  toCityCode: toCityCode,
  NATION_CODE_CN: NATION_CODE_CN
}
