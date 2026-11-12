import request from './request'

// ===================== 故障分类接口 =====================

// 查询启用状态的分类列表（所有角色可用）
export function listCategories() {
  return request.get('/categories')
}

// 全量分类含停用（仅管理员）
export function listAllCategories() {
  return request.get('/categories/admin/all')
}

// 新增分类（仅管理员）
export function createCategory(data) {
  return request.post('/categories', data)
}

// 修改分类（仅管理员）
export function updateCategory(id, data) {
  return request.put(`/categories/${id}`, data)
}

// 删除分类（仅管理员；有工单时后端执行逻辑删除，message 说明实际行为）
export function deleteCategory(id) {
  return request.delete(`/categories/${id}`)
}
