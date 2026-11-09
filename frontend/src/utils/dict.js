// ===================== 公共字典工具 =====================
// 统一维护 角色 / 工单状态 / 地点类型 的中文映射与 Element Plus tag 颜色

// 角色字典：USER 普通用户 / VIP 会员 / ADMIN 维修管理员
export const ROLE_MAP = {
  USER: { label: '普通用户', tagType: 'info' },
  VIP: { label: '会员', tagType: 'warning' },
  ADMIN: { label: '维修管理员', tagType: 'danger' }
}

// 工单状态字典：PENDING 待接单 / IN_PROGRESS 维修中 / FINISHED 已完成 / REJECTED 驳回
export const STATUS_MAP = {
  PENDING: { label: '待接单', tagType: 'warning' },
  IN_PROGRESS: { label: '维修中', tagType: 'primary' },
  FINISHED: { label: '已完成', tagType: 'success' },
  REJECTED: { label: '已驳回', tagType: 'danger' }
}

// 地点类型字典：DORM 宿舍楼内 / PUBLIC 校园公共区域
export const LOCATION_TYPE_MAP = {
  DORM: { label: '宿舍楼内', tagType: 'primary' },
  PUBLIC: { label: '校园公共区域', tagType: 'success' }
}

// 状态选项（用于下拉筛选）
export const STATUS_OPTIONS = Object.keys(STATUS_MAP).map((value) => ({
  value,
  label: STATUS_MAP[value].label
}))

// 地点类型选项（用于下拉筛选）
export const LOCATION_TYPE_OPTIONS = Object.keys(LOCATION_TYPE_MAP).map((value) => ({
  value,
  label: LOCATION_TYPE_MAP[value].label
}))

// 角色选项（用于用户管理筛选）
export const ROLE_OPTIONS = Object.keys(ROLE_MAP).map((value) => ({
  value,
  label: ROLE_MAP[value].label
}))

/**
 * 获取字典中文名（未匹配时返回原值，避免页面空白）
 * @param {Object} map 字典对象
 * @param {string} key 字典键
 * @returns {string} 中文名
 */
export function dictLabel(map, key) {
  return map[key]?.label ?? key ?? '-'
}

/**
 * 获取字典 tag 颜色类型
 * @param {Object} map 字典对象
 * @param {string} key 字典键
 * @returns {string} Element Plus tag type
 */
export function dictTagType(map, key) {
  return map[key]?.tagType ?? 'info'
}

/**
 * 拼接工单的地点展示文本
 * @param {Object} order 工单对象
 * @returns {string} 地点文本
 */
export function formatLocation(order) {
  if (!order) return '-'
  if (order.locationType === 'DORM') {
    return `${order.buildingNo ?? ''} ${order.roomNo ?? ''}`.trim() || '-'
  }
  return order.publicLocation || '-'
}
