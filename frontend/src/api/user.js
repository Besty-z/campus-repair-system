import request from './request'

// ===================== 用户管理接口（仅管理员） =====================

// 用户分页查询（keyword 模糊匹配手机号/昵称，role 可选）
export function getUserPage(params) {
  return request.get('/admin/users/page', { params })
}

// 设置用户身份（仅可设置 USER / VIP；管理员角色不允许变更）
export function updateUserRole(id, role) {
  return request.put(`/admin/users/${id}/role`, { role })
}
