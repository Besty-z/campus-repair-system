import request from './request'

// ===================== 维修工接口 =====================

// 维修工分页查询（仅管理员，支持姓名 / 手机号关键词）
export function getWorkerPage(params) {
  return request.get('/workers/admin/page', { params })
}

// 新增维修工（仅管理员；同时创建登录账号）
export function createWorker(data) {
  return request.post('/workers/admin', data)
}

// 修改维修工资料 / 在职状态（仅管理员，id 为 WorkerVO.id）
export function updateWorker(id, data) {
  return request.put(`/workers/admin/${id}`, data)
}

// 在职维修工列表（仅管理员，用于工单指派下拉）
export function getEnabledWorkers() {
  return request.get('/workers/admin/enabled')
}

// 当前维修工本人资料
export function getMyWorkerProfile() {
  return request.get('/workers/me')
}

// 修改本人资料（简介 + 擅长分类）
export function updateMyWorkerProfile(data) {
  return request.put('/workers/me', data)
}
