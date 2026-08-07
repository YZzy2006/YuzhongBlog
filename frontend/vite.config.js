import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import AutoImport from 'unplugin-auto-import/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    Components({
      resolvers: [ElementPlusResolver({ importStyle: 'css' })],
    }),
    AutoImport({
      resolvers: [ElementPlusResolver()],
    }),
  ],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/admin': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        bypass(req) {
          const accept = req.headers.accept || ''
          // API requests (axios sets Accept: application/json) → proxy to backend
          // Page navigation (Accept: text/html) → return undefined → Vite SPA fallback
          if (!accept.includes('application/json')) {
            return req.url
          }
        }
      }
    }
  },
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
    modulePreload: false,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            // Only split truly independent heavy libraries (no Vue dependency)
            if (id.includes('three')) return 'three'
            if (id.includes('echarts')) return 'echarts'
            if (id.includes('katex')) return 'katex'
            if (id.includes('mermaid')) return 'mermaid'
            if (id.includes('md-editor-v3')) return 'md-editor'
            // Everything else (vue, element-plus, etc.) stays in default chunk
            // to avoid circular dependency issues from manual splitting
          }
        }
      }
    },
    esbuild: {
      drop: ['console', 'debugger']
    }
  }
})
