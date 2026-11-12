import { defineStore } from 'pinia'
import { switchRole as switchRoleApi } from '../api/auth'

// 用户状态管理：token 与 userInfo 持久化在 localStorage，刷新页面不丢失登录态
export const useUserStore = defineStore('user', {
  state: () => ({
    // JWT token（登录接口签发，有效期 7 天）
    token: localStorage.getItem('token') || '',
    // 当前登录用户信息：{ id, phone, nickname, role }
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null')
  }),
  getters: {
    // 是否已登录
    isLoggedIn: (state) => !!state.token,
    // 当前角色：USER / VIP / WORKER / ADMIN
    role: (state) => state.userInfo?.role || ''
  },
  actions: {
    // 登录成功后保存登录态（token + 用户信息）
    setLogin(token, userInfo) {
      this.token = token
      this.userInfo = userInfo
      localStorage.setItem('token', token)
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
    },
    // 切换身份（课程演示）：后端更新角色并签发新 token，同步刷新本地登录态
    async switchRole(role) {
      const res = await switchRoleApi(role)
      this.setLogin(res.data.token, res.data.user)
      return res.data.user
    },
    // 退出登录：清空状态与本地缓存
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
})
