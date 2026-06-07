import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Frontend chạy ở cổng 5173, proxy mọi request /api sang backend Spring Boot (8080).
// HMR (hot reload) bật sẵn — sửa file là trình duyệt tự cập nhật, không cần tool ngoài.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    rollupOptions: {
      output: {
        // Tách vendor nặng/ổn định ra chunk riêng để cache tốt & giảm bundle chính.
        manualChunks: {
          react: ['react', 'react-dom', 'react-router-dom'],
          charts: ['recharts'],
          ui: ['react-bootstrap', 'bootstrap'],
        },
      },
    },
  },
})
