import { defineConfig, loadEnv, type PluginOption } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { sentryVitePlugin } from '@sentry/vite-plugin'

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, import.meta.dirname, '')

  if (command === 'build' && mode === 'production' && !env.VITE_API_BASE_URL) {
    throw new Error(
      'VITE_API_BASE_URL must be set for production builds (see .env.production.example).',
    )
  }

  const plugins: PluginOption[] = [react(), tailwindcss()]

  // Source maps are uploaded to Sentry and deleted, so they are never served.
  if (command === 'build' && env.SENTRY_AUTH_TOKEN) {
    plugins.push(
      sentryVitePlugin({
        org: env.SENTRY_ORG,
        project: env.SENTRY_PROJECT,
        authToken: env.SENTRY_AUTH_TOKEN,
        release: env.VITE_RELEASE ? { name: env.VITE_RELEASE } : undefined,
        sourcemaps: { filesToDeleteAfterUpload: ['./dist/**/*.map'] },
        telemetry: false,
      }),
    )
  }

  return {
    plugins,

    build: {
      sourcemap: env.SENTRY_AUTH_TOKEN ? 'hidden' : false,
    },

    resolve: {
      alias: {
        '@': import.meta.dirname + '/src',
        '@designcodeio/threeui/style.css':
          import.meta.dirname + '/src/shaders/threeui.css',
        '@designcodeio/threeui':
          import.meta.dirname + '/src/shaders/TextAnimationCollection.tsx',
      },
    },

    server: {
      port: 5173,

      proxy: {
        '/api': {
          target: 'http://localhost:8082',
          changeOrigin: true,
        },
      },
    },
  }
})
