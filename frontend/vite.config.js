import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 后端就绪后：把 .env 里 VITE_USE_MOCK 设为 false，即可走 /app 代理到 Spring Boot
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/app': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
