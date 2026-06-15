/**
 * 与后端 API 文档一致的枚举，供页面展示用
 * backend/care-link/docs/API接口文档.md 二、枚举与字典
 */
const orderStatus = {
  1: '待支付',
  2: '待接单',
  3: '待上门',
  4: '服务中',
  5: '待确认',
  6: '已完成',
  7: '已取消',
  8: '已关闭'
}

const orderType = {
  1: '系统匹配',
  2: '定向预约'
}

const category = {
  1: '居家陪护',
  2: '医院陪护',
  3: '周期护理',
  4: '家政服务',
  5: '陪诊服务',
  6: '母婴护理'
}

const billingMethod = {
  1: '按月',
  2: '按天',
  3: '按小时',
  4: '按次'
}

const gender = {
  0: '不限',
  1: '男',
  2: '女'
}

module.exports = {
  orderStatus,
  orderType,
  category,
  billingMethod,
  gender
}
