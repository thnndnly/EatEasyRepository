import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { http, HttpResponse } from 'msw'
import { server } from '@/test/mocks/server'
import { TEST_TOKEN, TEST_USER } from '@/test/fixtures'
import { useAuthStore } from './authStore'

describe('authStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('persistiert Token und User nach erfolgreichem Login', async () => {
    const store = useAuthStore()

    await store.login({ email: 'test@eateasy.local', password: 'secret123' })

    expect(store.token).toBe(TEST_TOKEN)
    expect(store.user?.email).toBe('test@eateasy.local')
    expect(store.isAuthenticated).toBe(true)
    expect(localStorage.getItem('eateasy.auth.token')).toBe(TEST_TOKEN)
  })

  it('wirft bei falschen Anmeldedaten und persistiert nichts', async () => {
    const store = useAuthStore()

    await expect(
      store.login({ email: 'fail@eateasy.local', password: 'wrong' }),
    ).rejects.toThrow('Ungültige Anmeldedaten')

    expect(store.token).toBeNull()
    expect(store.isAuthenticated).toBe(false)
  })

  it('loginWithGoogle sendet das idToken und persistiert die Session', async () => {
    let receivedBody: unknown = null
    server.use(
      http.post('/api/v1/auth/google', async ({ request }) => {
        receivedBody = await request.json()
        return HttpResponse.json({ token: TEST_TOKEN, user: TEST_USER })
      }),
    )
    const store = useAuthStore()

    await store.loginWithGoogle('google-id-token-xyz')

    expect(receivedBody).toMatchObject({ idToken: 'google-id-token-xyz' })
    expect(store.token).toBe(TEST_TOKEN)
    expect(store.user?.email).toBe(TEST_USER.email)
    expect(store.isAuthenticated).toBe(true)
  })

  it('leert die Session, wenn /auth/me beim Restore 401 zurückgibt', async () => {
    localStorage.setItem('eateasy.auth.token', 'stale.token')
    server.use(
      http.get('/api/v1/auth/me', () =>
        HttpResponse.json({ error: 'Unauthorized' }, { status: 401 }),
      ),
    )

    const store = useAuthStore()
    await store.restoreSession()

    expect(store.token).toBeNull()
    expect(store.user).toBeNull()
    expect(localStorage.getItem('eateasy.auth.token')).toBeNull()
  })

  it('restoreSession verifiziert ein gültiges Token und lädt den User', async () => {
    localStorage.setItem('eateasy.auth.token', TEST_TOKEN)

    const store = useAuthStore()
    await store.restoreSession()

    expect(store.user?.id).toBe(TEST_USER.id)
    expect(store.isAuthenticated).toBe(true)
  })
})
