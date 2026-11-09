// 通用工具：日期格式化、枚举中文映射与颜色

/**
 * 日期格式化为 yyyy-MM-dd HH:mm:ss
 * @param {Date} date 日期对象
 * @returns {string} 格式化后的字符串
 */
function formatTime(date) {
  const year = date.getFullYear();
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const hour = date.getHours();
  const minute = date.getMinutes();
  const second = date.getSeconds();
  return (
    [year, month, day].map(padZero).join('-') +
    ' ' +
    [hour, minute, second].map(padZero).join(':')
  );
}

/**
 * 数字补零
 * @param {number} n 数字
 * @returns {string} 两位字符串
 */
function padZero(n) {
  return n < 10 ? '0' + n : '' + n;
}

/** 工单状态中文映射 */
const STATUS_MAP = {
  PENDING: '待接单',
  IN_PROGRESS: '维修中',
  FINISHED: '已完成',
  REJECTED: '驳回'
};

/** 工单状态颜色映射（标签底色） */
const STATUS_COLOR_MAP = {
  PENDING: '#ff9500',
  IN_PROGRESS: '#3b7cff',
  FINISHED: '#07c160',
  REJECTED: '#999999'
};

/** 角色中文映射 */
const ROLE_MAP = {
  USER: '普通用户',
  VIP: '会员',
  ADMIN: '维修管理员'
};

/** 角色颜色映射：普通用户灰、会员金、管理员蓝 */
const ROLE_COLOR_MAP = {
  USER: '#999999',
  VIP: '#c8a063',
  ADMIN: '#3b7cff'
};

/** 地点类型中文映射 */
const LOCATION_TYPE_MAP = {
  DORM: '宿舍楼内',
  PUBLIC: '校园公共区域'
};

/**
 * 获取工单状态中文名
 * @param {string} status 状态枚举
 * @returns {string}
 */
function statusText(status) {
  return STATUS_MAP[status] || status || '';
}

/**
 * 获取工单状态颜色
 * @param {string} status 状态枚举
 * @returns {string}
 */
function statusColor(status) {
  return STATUS_COLOR_MAP[status] || '#999999';
}

/**
 * 获取角色中文名
 * @param {string} role 角色枚举
 * @returns {string}
 */
function roleText(role) {
  return ROLE_MAP[role] || role || '';
}

/**
 * 获取角色颜色
 * @param {string} role 角色枚举
 * @returns {string}
 */
function roleColor(role) {
  return ROLE_COLOR_MAP[role] || '#999999';
}

/**
 * 获取地点类型中文名
 * @param {string} type 地点类型枚举
 * @returns {string}
 */
function locationTypeText(type) {
  return LOCATION_TYPE_MAP[type] || type || '';
}

/**
 * 截取字符串作为摘要
 * @param {string} text 原文
 * @param {number} len 保留长度
 * @returns {string}
 */
function summary(text, len) {
  if (!text) return '';
  return text.length > len ? text.slice(0, len) + '…' : text;
}

module.exports = {
  formatTime,
  statusText,
  statusColor,
  roleText,
  roleColor,
  locationTypeText,
  summary,
  STATUS_MAP,
  STATUS_COLOR_MAP,
  ROLE_MAP,
  ROLE_COLOR_MAP,
  LOCATION_TYPE_MAP
};
