import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// axios 实例：统一 baseURL 为 /api（由 vite 代理转发到 http://localhost:8080）
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器：自动从 localStorage 读取 token 并放入 Authorization 请求头
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理后端约定 { code, message, data }
// code !== 200 时弹出错误提示；401 时清除登录态并跳转登录页
request.interceptors.response.use(
  (response) => {
    // 文件上传等场景同样返回统一 JSON 结构
    const res = response.data
    if (res.code === 200) {
      return res
    }
    // 业务失败：提示后端返回的中文 message
    ElMessage.error(res.message || '请求失败')
    if (res.code === 401) {
      clearLoginAndRedirect()
    }
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    // HTTP 层异常（网络错误、500 等）
    const status = error.response?.status
    const message = error.response?.data?.message || '网络异常，请稍后重试'
    ElMessage.error(message)
    if (status === 401) {
      clearLoginAndRedirect()
    }
    return Promise.reject(error)
  }
)

// 清除本地登录态并跳转登录页
function clearLoginAndRedirect() {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  // 避免在登录页重复跳转
  if (router.currentRoute.value.path !== '/login') {
    router.push('/login')
  }
}

export default request
