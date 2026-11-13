import request from './request'

// ===================== AI 智能报修助手接口 =====================

// 创建新会话（title 可选）
export function createSession(title) {
  return request.post('/ai/chat/sessions', null, { params: title ? { title } : {} })
}

// 查询当前用户的会话列表
export function listSessions() {
  return request.get('/ai/chat/sessions')
}

// 删除会话
export function deleteSession(id) {
  return request.delete(`/ai/chat/sessions/${id}`)
}

// 查询某会话的消息列表
export function listMessages(sessionId) {
  return request.get(`/ai/chat/sessions/${sessionId}/messages`)
}

// 发送一条消息并返回 AI 回答（sessionId 为空时自动创建新会话）
export function sendMessage(data) {
  return request.post('/ai/chat/messages', data)
}
