import { expect, test } from '@playwright/test'

/**
 * Smoke-Tests ohne Backend-Abhaengigkeit. Decken die ersten paar Schritte
 * der Login-Flow ab und stellen sicher, dass das Frontend bootet.
 *
 * Backend-Calls (/api/v1/*) werden hier per page.route() abgefangen, sodass
 * der Test auch ohne laufenden Quarkus-Server gruen wird.
 */
test.describe('Login-View Smoke', () => {
  test.beforeEach(async ({ page }) => {
    await page.route('**/api/v1/auth/me', (route) =>
      route.fulfill({ status: 401, contentType: 'application/json', body: '{"error":"Unauthorized"}' }),
    )
  })

  test('zeigt das Login-Formular', async ({ page }) => {
    await page.goto('/login')

    await expect(page.getByRole('heading', { name: /Willkommen zurueck/ })).toBeVisible()
    await expect(page.locator('#login-email')).toBeVisible()
    await expect(page.locator('#login-password')).toBeVisible()
    await expect(page.getByRole('button', { name: /Einloggen/ })).toBeVisible()
  })

  test('zeigt eine Fehlermeldung bei falschen Anmeldedaten', async ({ page }) => {
    await page.route('**/api/v1/auth/login', (route) =>
      route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Ungueltige Anmeldedaten' }),
      }),
    )

    await page.goto('/login')
    await page.locator('#login-email').fill('foo@bar.local')
    await page.locator('#login-password').fill('secretsecret')
    await page.getByRole('button', { name: /Einloggen/ }).click()

    await expect(page.getByText('Ungueltige Anmeldedaten')).toBeVisible()
  })

  test('login erfolgreich -> Weiterleitung zur Startseite', async ({ page }) => {
    await page.route('**/api/v1/auth/login', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          token: 'e2e.fake.token',
          user: {
            id: '11111111-1111-1111-1111-111111111111',
            email: 'foo@bar.local',
            displayName: 'Foo',
            createdAt: '2026-01-01T00:00:00Z',
          },
        }),
      }),
    )
    // Home-View loadet ggf. /api/v1/health — pauschal mocken.
    await page.route('**/api/v1/health', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '{"status":"ok"}' }),
    )

    await page.goto('/login')
    await page.locator('#login-email').fill('foo@bar.local')
    await page.locator('#login-password').fill('secretsecret')
    await page.getByRole('button', { name: /Einloggen/ }).click()

    await expect(page).toHaveURL(/\/$/)
  })
})
