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

// 指派维修工（仅管理员，workerId 为 WorkerVO.userId）
export function assignOrder(id, workerId) {
  return request.put(`/orders/admin/${id}/assign`, { workerId })
}

// ===================== 维修工工单接口 =====================

// 我的任务分页（维修工本人）
export function getWorkerTasks(params) {
  return request.get('/orders/worker/page', { params })
}

// 抢单池分页（维修工本人，待接单且未指派）
export function getWorkerPool(params) {
  return request.get('/orders/worker/pool', { params })
}

// 抢单 / 接单（维修工本人）
export function acceptWorkerOrder(id) {
  return request.put(`/orders/worker/${id}/accept`)
}

// 完成维修（维修工本人，status 固定为 FINISHED）
export function finishWorkerOrder(id, data) {
  return request.put(`/orders/worker/${id}/status`, data)
}

// ===================== 用户评价接口 =====================

// 提交工单评分（USER / VIP，score 1-5）
export function rateOrder(id, data) {
  return request.post(`/orders/${id}/rating`, data)
}
