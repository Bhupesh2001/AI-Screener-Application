import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { fileURLToPath, URL } from 'node:url'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      // Mirrors the "@/*" path in tsconfig.app.json - tsc only type-checks
      // against tsconfig paths, it doesn't rewrite imports, so Vite/Rollup
      // needs this alias too or the build will fail to resolve "@/..." imports.
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // During dev, forward /api requests to the Spring Boot backend so the
      // frontend can just call relative paths like "/api/dashboard" without
      // worrying about CORS or hardcoding a host.
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
