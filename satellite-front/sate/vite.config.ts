import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  const apiBaseUrl = env.VITE_API_BASE_URL || 'http://127.0.0.1:8080'
  const port = parseInt(env.VITE_PORT || '5173')
  const proxyEnabled = env.VITE_PROXY_ENABLED !== 'false'

  return {
    plugins: [vue()],
    base: '/sate/',
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
      },
    },
    server: {
      host: '0.0.0.0',
      port: port,
      proxy: proxyEnabled
        ? {
            '/api': {
              target: apiBaseUrl,
              changeOrigin: true,
              rewrite: (path) => path.replace(/^\/api/, ''),
              headers: {
                'ngrok-skip-browser-warning': 'true',
              },
            },
          }
        : undefined,
    },
    envPrefix: 'VITE_',
  }
})
