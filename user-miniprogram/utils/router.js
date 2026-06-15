/**
 * 路由封装 + 前置守卫（类似 Vue Router beforeEach）
 * 使用 router.navigateTo / redirectTo / reLaunch 替代 wx 对应方法，即可在跳转前统一做登录校验等
 */

// 不需要登录即可访问的路径（路径前缀或完整路径）
var noAuthPaths = [
  '/pages/login/login',
  '/pages/register/register',
  '/pages/index/index',
  '/pages/package/list/list',
  '/pages/package/detail/detail',
  '/pages/caregiver/index/index',
  '/pages/caregiver/detail/detail',
  '/pages/search/search'
]

function getPath(url) {
  if (!url || typeof url !== 'string') return ''
  var path = url.split('?')[0].trim()
  if (path.indexOf('/') === 0) return path
  return '/' + path
}

function needAuth(path) {
  var p = getPath(path)
  for (var i = 0; i < noAuthPaths.length; i++) {
    if (noAuthPaths[i] === p || p.indexOf(noAuthPaths[i] + '?') === 0) return false
  }
  return true
}

function hasToken() {
  try {
    var app = getApp()
    if (app && app.globalData && app.globalData.token) return true
    return !!wx.getStorageSync('token')
  } catch (e) {
    return false
  }
}

/** 前置守卫：返回 true 表示放行，false 表示拦截（并由内部处理跳转登录等） */
var beforeEach = function (to) {
  if (!needAuth(to.url)) return true
  if (hasToken()) return true
  wx.reLaunch({ url: '/pages/login/login' })
  return false
}

/**
 * 设置前置守卫（可在 app.js 里调用以自定义逻辑）
 * @param {function} fn 守卫函数，参数 to = { url, path }，返回 true 放行，false 拦截
 */
function setBeforeEach(fn) {
  if (typeof fn === 'function') beforeEach = fn
}

function runGuard(url) {
  var path = getPath(url)
  return beforeEach({ url: url, path: path })
}

function navigateTo(options) {
  if (!runGuard(options.url)) return
  wx.navigateTo(options)
}

function redirectTo(options) {
  if (!runGuard(options.url)) return
  wx.redirectTo(options)
}

function reLaunch(options) {
  if (!runGuard(options.url)) return
  wx.reLaunch(options)
}

function switchTab(options) {
  if (!runGuard(options.url)) return
  wx.switchTab(options)
}

function navigateBack(options) {
  wx.navigateBack(options || {})
}

module.exports = {
  setBeforeEach: setBeforeEach,
  navigateTo: navigateTo,
  redirectTo: redirectTo,
  reLaunch: reLaunch,
  switchTab: switchTab,
  navigateBack: navigateBack,
  getPath: getPath,
  needAuth: needAuth,
  hasToken: hasToken
}
