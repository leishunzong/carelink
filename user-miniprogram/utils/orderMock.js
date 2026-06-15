/**
 * 订单模拟数据，列表页与详情页共用，保证状态一致
 */
var mockOrders = [
  { id: 1001, orderNo: 'ORD20260228001', orderType: 1, status: 1, packageId: 1, packageName: '基础居家陪护', caregiverName: null, caregiverPhone: null, detailAddress: '北京市海淀区中关村大街1号', billingMethod: 2, buyQuantity: 7, totalAmount: 1960, expectStartTime: '2026-03-01 09:00:00', createTime: '2026-02-27 15:30:00' },
  { id: 1002, orderNo: 'ORD20260228002', orderType: 2, status: 4, packageId: 2, packageName: '医院24小时陪护', caregiverName: '王明慧', caregiverPhone: '138****5678', detailAddress: '北京市朝阳区建国门外大街8号', billingMethod: 2, buyQuantity: 3, totalAmount: 840, expectStartTime: '2026-02-28 09:00:00', createTime: '2026-02-28 10:00:00' },
  { id: 1003, orderNo: 'ORD20260228003', orderType: 1, status: 6, packageId: 3, packageName: '术后康复护理', caregiverName: '李秀芳', caregiverPhone: '139****1234', detailAddress: '北京市西城区金融街9号', billingMethod: 1, buyQuantity: 1, totalAmount: 4800, expectStartTime: '2026-02-20 09:00:00', createTime: '2026-02-19 14:00:00' },
  { id: 1004, orderNo: 'ORD20260228004', orderType: 2, status: 2, packageId: 6, packageName: '月嫂服务', caregiverName: null, caregiverPhone: null, detailAddress: '北京市东城区王府井大街138号', billingMethod: 1, buyQuantity: 1, totalAmount: 8800, expectStartTime: '2026-03-10 08:00:00', createTime: '2026-02-28 16:00:00' },
  { id: 1005, orderNo: 'ORD20260228005', orderType: 1, status: 5, packageId: 2, packageName: '医院术后陪护', caregiverName: '张翠珍', caregiverPhone: '137****5678', detailAddress: '北京市丰台区南三环西路16号', billingMethod: 2, buyQuantity: 5, totalAmount: 1400, expectStartTime: '2026-02-25 09:00:00', createTime: '2026-02-24 11:00:00' }
]

function getOrderById(id) {
  var oid = typeof id === 'number' ? id : parseInt(id, 10)
  return mockOrders.find(function (o) { return o.id === oid }) || null
}

function updateOrderStatus(id, status) {
  var oid = typeof id === 'number' ? id : parseInt(id, 10)
  var idx = mockOrders.findIndex(function (o) { return o.id === oid })
  if (idx >= 0) {
    mockOrders[idx].status = status
    return true
  }
  return false
}

module.exports = {
  mockOrders: mockOrders,
  getOrderById: getOrderById,
  updateOrderStatus: updateOrderStatus
}
