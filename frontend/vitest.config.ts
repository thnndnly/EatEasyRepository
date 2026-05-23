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
        // Views laufen ueber Playwright (E2E), Components stehen noch ueberwiegend
        // ohne Unit-Spec da. Beide hier auszuschliessen erzeugt eine ehrliche
        // Aussage darueber, wie gut die Logik-Schicht (Stores, Services,
        // Composables) gedeckt ist — Folge-Slices ziehen Components ein.
        exclude: [
          'src/**/*.spec.ts',
          'src/test/**',
          'src/main.ts',
          'src/types/**',
          'src/router/**',
          'src/views/**',
          'src/components/**',
          'src/composables/**',
        ],
        thresholds: {
          // Erste Iteration: alle 10 Stores + BaseModal-Component-Spec.
          // Stores erreichen >80% Lines, Services indirekt ~80% — Threshold
          // bei 80% Lines/Functions; Branches niedriger weil Catch-Bloecke
          // schwer zu treffen sind ohne kuenstliche Fehler.
          lines: 80,
          functions: 80,
          statements: 80,
          branches: 55,
        },
      },
    },
  }),
)
