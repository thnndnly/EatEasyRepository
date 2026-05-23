import { expect, test } from '@playwright/test'
import { TEST_TOKEN, TEST_USER } from '../src/test/fixtures'

/**
 * Smoke-Tests ohne Backend-Abhaengigkeit. Decken die ersten Schritte des
 * Login-Flows ab und stellen sicher, dass das Frontend bootet.
 *
 * beforeEach setzt Default-Mocks fuer alle erwarteten Endpunkte und einen
 * Catch-All, der unerwartete /api/*-Calls als Test-Fehler markiert — sonst
 * wuerden neue ungemockte Calls stumm durchrutschen (Vite-Proxy → kein
 * Backend → leise 500).
 */
const jsonResponse = (status: number, body: unknown) => ({
  status,
  contentType: 'application/json',
  body: JSON.stringify(body),
})

test.describe('Login-View Smoke', () => {
  test.beforeEach(async ({ page }) => {
    // Playwright probiert Route-Handler in umgekehrter Reihenfolge der
    // Registrierung. Catch-All zuerst registrieren, damit nachfolgende
    // spezifische Routen ihn ueberstimmen koennen. Ohne diesen Catch-All
    // wuerden ungemockte /api/*-Calls leise durch den Vite-Proxy ans
    // (im Test nicht laufende) Backend gehen.
    await page.route('**/api/v1/**', (route) => {
      throw new Error(`Unmocked API-Call: ${route.request().method()} ${route.request().url()}`)
    })
    await page.route('**/api/v1/auth/me', (route) =>
      route.fulfill(jsonResponse(401, { error: 'Unauthorized' })),
    )
    await page.route('**/api/v1/auth/login', (route) =>
      route.fulfill(jsonResponse(401, { error: 'Ungueltige Anmeldedaten' })),
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
    await page.goto('/login')
    await page.locator('#login-email').fill('foo@bar.local')
    await page.locator('#login-password').fill('secretsecret')
    await page.getByRole('button', { name: /Einloggen/ }).click()

    await expect(page.getByText('Ungueltige Anmeldedaten')).toBeVisible()
  })

  test('login erfolgreich -> Weiterleitung zur Startseite', async ({ page }) => {
    await page.route('**/api/v1/auth/login', (route) =>
      route.fulfill(jsonResponse(200, { token: TEST_TOKEN, user: TEST_USER })),
    )
    // HomeView mounted ruft /health und /households parallel auf — beides
    // explizit stubben, damit der Catch-All nicht zuschlaegt.
    await page.route('**/api/v1/health', (route) =>
      route.fulfill(jsonResponse(200, { status: 'ok' })),
    )
    await page.route('**/api/v1/households', (route) =>
      route.fulfill(jsonResponse(200, [])),
    )

    await page.goto('/login')
    await page.locator('#login-email').fill(TEST_USER.email)
    await page.locator('#login-password').fill('secretsecret')
    await page.getByRole('button', { name: /Einloggen/ }).click()

    await expect(page).toHaveURL(/\/$/)
    await expect(page.getByText(`Hallo, ${TEST_USER.displayName}`, { exact: false })).toBeVisible()
  })
})
