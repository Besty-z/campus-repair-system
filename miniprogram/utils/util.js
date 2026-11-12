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

/** 工单状态颜色映射（标签底色，与网页端设计系统一致） */
const STATUS_COLOR_MAP = {
  PENDING: '#f5a524',
  IN_PROGRESS: '#2c6bf2',
  FINISHED: '#0fb583',
  REJECTED: '#ef4b4b'
};

/** 工单状态说明 */
const STATUS_DESC_MAP = {
  PENDING: '等待维修管理员接单',
  IN_PROGRESS: '维修人员正在处理中',
  FINISHED: '工单已完成，可查看维修备注',
  REJECTED: '工单已被驳回，请查看驳回原因'
};

/** 角色中文映射 */
const ROLE_MAP = {
  USER: '普通用户',
  VIP: '会员',
  ADMIN: '维修管理员',
  WORKER: '维修工'
};

/** 角色颜色映射：普通用户灰、会员金、管理员蓝、维修工紫 */
const ROLE_COLOR_MAP = {
  USER: '#7a879b',
  VIP: '#c8a063',
  ADMIN: '#2c6bf2',
  WORKER: '#6f5ce0'
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
 * 获取工单状态说明
 * @param {string} status 状态枚举
 * @returns {string}
 */
function statusDesc(status) {
  return STATUS_DESC_MAP[status] || '';
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

/**
 * 金额格式化：保留两位小数并去掉末尾多余的 0
 * @param {number|string} value 金额
 * @returns {string} 如 80、64.5、64.25；无效值返回空串
 */
function formatMoney(value) {
  const num = Number(value);
  if (!isFinite(num)) {
    return '';
  }
  return (Math.round(num * 100) / 100).toString();
}

/**
 * 拼接图片完整访问地址
 * 后端返回的图片相对路径形如 /uploads/2026/11/08/a.jpg，
 * 由于后端 context-path 为 /api，图片真实地址在 /api 下
 * @param {string} path 图片相对路径
 * @returns {string} 完整图片地址（空值返回空串，http 开头的地址原样返回）
 */
function imageUrl(path) {
  if (!path) {
    return '';
  }
  if (path.indexOf('http') === 0) {
    return path;
  }
  const app = getApp();
  const base = (app && app.globalData && app.globalData.imageBaseUrl) || 'http://localhost:8080/api';
  return base + path;
}

module.exports = {
  formatTime,
  formatMoney,
  imageUrl,
  statusText,
  statusColor,
  statusDesc,
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
