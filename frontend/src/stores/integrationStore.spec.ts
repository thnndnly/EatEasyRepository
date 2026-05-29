import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { http, HttpResponse } from 'msw'
import { server } from '@/test/mocks/server'
import { TEST_EXTERNAL_PREVIEW, TEST_RECIPE, TEST_TOKEN } from '@/test/fixtures'
import { useAuthStore } from './authStore'
import { useIntegrationStore } from './integrationStore'

describe('integrationStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    useAuthStore().$patch({ token: TEST_TOKEN })
  })

  it('searchExternal uebernimmt Treffer in results', async () => {
    server.use(
      http.get('/api/v1/integration/recipes/search', () =>
        HttpResponse.json([TEST_EXTERNAL_PREVIEW]),
      ),
    )
    const store = useIntegrationStore()

    const fresh = await store.searchExternal('themealdb', 'tomato')

    expect(fresh).toEqual([TEST_EXTERNAL_PREVIEW])
    expect(store.results).toEqual([TEST_EXTERNAL_PREVIEW])
  })

  it('importExternal setzt importingId waehrend des Calls und cleared danach', async () => {
    let observedImportingId: string | null = null
    server.use(
      http.post('/api/v1/recipes/import', () => {
        // Wenn der Handler greift, ist der Call gerade in Flight — der
        // Store muss importingId zu diesem Zeitpunkt gesetzt haben.
        observedImportingId = useIntegrationStore().importingId
        return HttpResponse.json(TEST_RECIPE)
      }),
    )
    const store = useIntegrationStore()

    await store.importExternal({ source: 'themealdb', externalId: 'ext-42' })

    expect(observedImportingId).toBe('ext-42')
    expect(store.importingId).toBeNull()
  })

  it('importExternal setzt error bei Fehler und cleared importingId', async () => {
    server.use(
      http.post('/api/v1/recipes/import', () =>
        HttpResponse.json({ error: 'Quelle nicht erreichbar' }, { status: 502 }),
      ),
    )
    const store = useIntegrationStore()

    await expect(
      store.importExternal({ source: 'themealdb', externalId: 'ext-x' }),
    ).rejects.toThrow('Quelle nicht erreichbar')

    expect(store.error).toBe('Quelle nicht erreichbar')
    expect(store.importingId).toBeNull()
  })

  it('reset leert results, error und importingId', async () => {
    server.use(
      http.get('/api/v1/integration/recipes/search', () =>
        HttpResponse.json([TEST_EXTERNAL_PREVIEW]),
      ),
    )
    const store = useIntegrationStore()
    await store.searchExternal('themealdb', 'x')

    store.reset()

    expect(store.results).toEqual([])
    expect(store.error).toBeNull()
    expect(store.importingId).toBeNull()
  })
})
