import { fileURLToPath } from 'node:url'
import { mergeConfig, defineConfig, configDefaults } from 'vitest/config'
import viteConfig from './vite.config'

export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      environment: 'jsdom',
      exclude: [...configDefaults.exclude, 'e2e/**'],
      root: fileURLToPath(new URL('./', import.meta.url)),
      setupFiles: ['src/test/setup.ts'],
      coverage: {
        provider: 'v8',
        reporter: ['text', 'html', 'json-summary'],
        include: ['src/**/*.{ts,vue}'],
        exclude: [
          'src/**/*.spec.ts',
          'src/test/**',
          'src/main.ts',
          'src/types/**',
          'src/router/**',
        ],
        thresholds: {
          // Erste Iteration: Stores + ein Component-Spec. Schwelle bewusst
          // moderat, damit weitere Slices den Wert hoeher ziehen koennen.
          lines: 60,
          functions: 60,
          statements: 60,
          branches: 50,
        },
      },
    },
  }),
)
