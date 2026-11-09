import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Vite 配置：开发服务器端口 5173
// 代理说明：前端以 /api 访问后端接口、以 /uploads 访问后端本地图片，统一转发到 8080
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // 后端接口代理
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // 图片等静态资源代理（工单图片相对路径 /uploads/xxx 直接可访问）
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
