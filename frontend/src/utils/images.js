// 统一占位图与样板图地址：登录页背景、空状态插图、维修工头像
// 说明：图片已随项目放在后端 uploads/samples 下，由后端 /uploads/** 静态映射提供，
// 避免依赖外链（外链存在防盗链与域名白名单限制）
const SAMPLE_BASE = 'http://localhost:8080/api/uploads/samples'

export const LOGIN_BG = `${SAMPLE_BASE}/common/login-bg_1.jpg`
export const EMPTY_IMAGE = `${SAMPLE_BASE}/common/empty_1.jpg`
export const WORKER_AVATAR = `${SAMPLE_BASE}/common/worker_1.jpg`
