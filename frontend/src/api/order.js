import request from './request'

// ===================== 报修工单接口 =====================

// 提交报修工单（USER / VIP）
export function createOrder(data) {
  return request.post('/orders', data)
}

// 我的工单分页列表（USER / VIP）
export function getMyOrders(params) {
  return request.get('/orders/my', { params })
}

// 我的报修统计（仅 VIP）
export function getMyStats() {
  return request.get('/orders/my/stats')
}

// 工单详情（USER/VIP 仅本人，ADMIN 任意）
export function getOrderDetail(id) {
  return request.get(`/orders/${id}`)
}

// ===================== 管理员工单接口 =====================

// 工单分页查询（仅管理员，紧急工单优先排序）
export function getAdminOrderPage(params) {
  return request.get('/orders/admin/page', { params })
}

// 接单（仅管理员，PENDING -> IN_PROGRESS）
export function acceptOrder(id) {
  return request.put(`/orders/admin/${id}/accept`)
}

// 修改工单状态（仅管理员，可填写维修备注）
export function updateOrderStatus(id, data) {
  return request.put(`/orders/admin/${id}/status`, data)
}
