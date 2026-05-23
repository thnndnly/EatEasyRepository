import { defineConfig, devices } from '@playwright/test'

/**
 * Playwright-Konfiguration fuer EatEasy E2E-Tests.
 *
 * Tests liegen unter `e2e/`, vitest blendet diesen Ordner aus
 * (vitest.config.ts -> exclude: ['e2e/**']).
 *
 * Der webServer-Block startet automatisch `npm run dev` (Vite) auf 5173,
 * sodass `npm run test:e2e` ohne separates Setup laeuft. Die Tests
 * sprechen die UI an — Backend-Anrufe werden per page.route() gemockt,
 * sodass kein laufender Quarkus-Server noetig ist (Smoke-Variante).
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
})
