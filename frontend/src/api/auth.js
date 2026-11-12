import request from './request'

// ===================== 认证接口（无需 token） =====================

// 发送登录验证码（课程演示：验证码打印在后端控制台日志）
export function sendCode(phone) {
  return request.post('/auth/send-code', { phone })
}

// 手机号验证码登录（未注册的手机号后端会自动注册为普通用户）
export function login(data) {
  return request.post('/auth/login', data)
}

// 手机号注册（注册即普通用户 USER，注册成功直接返回登录态）
export function register(data) {
  return request.post('/auth/register', data)
}

// 获取当前登录用户信息
export function getProfile() {
  return request.get('/auth/profile')
}

// 切换当前账号身份（课程演示）：后端更新角色并签发新 token
export function switchRole(role) {
  return request.put('/auth/switch-role', { role })
}
