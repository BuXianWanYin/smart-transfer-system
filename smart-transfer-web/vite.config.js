import net from 'node:net'

// Node.js 官方文档: autoSelectFamilyAttemptTimeout 默认 250ms，Clumsy 500ms 延迟会触发 ETIMEDOUT
// https://nodejs.org/api/net.html#netsetdefaultautoselectfamilyattempttimeoutvalue
net.setDefaultAutoSelectFamilyAttemptTimeout(5000)

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

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
        api: 'modern' 
      }
    }
  },
  server: {
    port: Number(env.VITE_APP_PORT) || 3000,
    open: true,
    proxy: {
      '/api': {
        target: env.VITE_SERVER_URL || 'http://127.0.0.1:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
        // proxyTimeout=发出请求超时, timeout=接收响应超时
        proxyTimeout: 60000,
        timeout: 60000,
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

