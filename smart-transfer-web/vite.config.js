import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  
  return {
  plugins: [
    vue(),
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      resolvers: [ElementPlusResolver()],
      dts: 'src/types/auto-imports.d.js'
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/types/components.d.js'
    })
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        api: 'modern' // 使用新版 Sass API，消除弃用警告
      }
    }
  },
  server: {
    port: Number(env.VITE_APP_PORT) || 3000,
    open: true,
    proxy: {
      '/api': {
        target: env.VITE_SERVER_URL || 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
        // http-proxy 超时：Clumsy 500ms 延迟时请求约 1.3s，默认可能过短导致 500
        proxyTimeout: 60000,  // 代理到后端的等待超时 60s
        timeout: 60000,       // 等待后端响应的超时 60s
        configure: (proxy) => {
          proxy.on('error', (err, req, res) => {
            console.error('[Vite Proxy Error]', err.message)
            if (!res.headersSent) {
              res.writeHead(500, { 'Content-Type': 'application/json' })
              res.end(JSON.stringify({
                code: 500,
                message: '代理转发失败: ' + err.message
              }))
            }
          })
        }
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    minify: 'terser',
    chunkSizeWarningLimit: 1500
  }
}})

