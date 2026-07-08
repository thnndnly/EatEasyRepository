import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { http, HttpResponse } from 'msw'
import { server } from '@/test/mocks/server'
import { TEST_HOUSEHOLD, TEST_SUGGESTION, TEST_TOKEN } from '@/test/fixtures'
import { useAuthStore } from './authStore'
import { useSuggestionStore } from './suggestionStore'

describe('suggestionStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    useAuthStore().$patch({ token: TEST_TOKEN })
  })

  it('fetch setzt suggestions und requested=true', async () => {
    server.use(
      http.post(`/api/v1/households/${TEST_HOUSEHOLD.id}/suggestions`, () =>
        HttpResponse.json({ aiAvailable: true, suggestions: [TEST_SUGGESTION] }),
      ),
    )
    const store = useSuggestionStore()

    await store.fetch(TEST_HOUSEHOLD.id, { numSuggestions: 3 })

    expect(store.suggestions).toEqual([TEST_SUGGESTION])
    expect(store.requested).toBe(true)
    expect(store.error).toBeNull()
    expect(store.aiAvailable).toBe(true)
  })

  it('setzt aiAvailable=false, wenn die KI laut Backend nicht verfuegbar war', async () => {
    server.use(
      http.post(`/api/v1/households/${TEST_HOUSEHOLD.id}/suggestions`, () =>
        HttpResponse.json({ aiAvailable: false, suggestions: [TEST_SUGGESTION] }),
      ),
    )
    const store = useSuggestionStore()

    await store.fetch(TEST_HOUSEHOLD.id, { numSuggestions: 3 })

    expect(store.aiAvailable).toBe(false)
    expect(store.suggestions).toEqual([TEST_SUGGESTION])
  })

  it('Fehler setzt error, leert suggestions und propagiert', async () => {
    server.use(
      http.post(`/api/v1/households/${TEST_HOUSEHOLD.id}/suggestions`, () =>
        HttpResponse.json({ error: 'LLM nicht erreichbar' }, { status: 503 }),
      ),
    )
    const store = useSuggestionStore()

    await expect(store.fetch(TEST_HOUSEHOLD.id, { numSuggestions: 3 })).rejects.toThrow(
      'LLM nicht erreichbar',
    )

    expect(store.suggestions).toEqual([])
    expect(store.error).toBe('LLM nicht erreichbar')
    expect(store.requested).toBe(true)
  })

  it('reset leert alles inklusive requested-Flag', async () => {
    server.use(
      http.post(`/api/v1/households/${TEST_HOUSEHOLD.id}/suggestions`, () =>
        HttpResponse.json({ aiAvailable: true, suggestions: [TEST_SUGGESTION] }),
      ),
    )
    const store = useSuggestionStore()
    await store.fetch(TEST_HOUSEHOLD.id, { numSuggestions: 1 })

    store.reset()

    expect(store.suggestions).toEqual([])
    expect(store.error).toBeNull()
    expect(store.requested).toBe(false)
  })
})
