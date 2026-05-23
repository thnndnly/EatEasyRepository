import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { http, HttpResponse } from 'msw'
import { server } from '@/test/mocks/server'
import { fixtures } from '@/test/mocks/handlers'
import { useAuthStore } from './authStore'

describe('authStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('persistiert Token und User nach erfolgreichem Login', async () => {
    const store = useAuthStore()

    await store.login({ email: 'test@eateasy.local', password: 'secret123' })

    expect(store.token).toBe(fixtures.token)
    expect(store.user?.email).toBe('test@eateasy.local')
    expect(store.isAuthenticated).toBe(true)
    expect(localStorage.getItem('eateasy.auth.token')).toBe(fixtures.token)
  })

  it('wirft bei falschen Anmeldedaten und persistiert nichts', async () => {
    const store = useAuthStore()

    await expect(
      store.login({ email: 'fail@eateasy.local', password: 'wrong' }),
    ).rejects.toThrow()

    expect(store.token).toBeNull()
    expect(store.isAuthenticated).toBe(false)
  })

  it('leert die Session, wenn /auth/me beim Restore 401 zurueckgibt', async () => {
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

  it('restoreSession verifiziert ein gueltiges Token und laedt den User', async () => {
    localStorage.setItem('eateasy.auth.token', fixtures.token)

    const store = useAuthStore()
    await store.restoreSession()

    expect(store.user?.id).toBe(fixtures.user.id)
    expect(store.isAuthenticated).toBe(true)
  })
})
