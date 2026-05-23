import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { http, HttpResponse } from 'msw'
import { server } from '@/test/mocks/server'
import { TEST_INGREDIENT, TEST_TOKEN } from '@/test/fixtures'
import { useAuthStore } from './authStore'
import { useIngredientStore } from './ingredientStore'

describe('ingredientStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    // Stores nutzen useRequireToken() — Auth muss "eingeloggt" sein.
    useAuthStore().$patch({ token: TEST_TOKEN })
  })

  it('search uebernimmt die Server-Antwort in results', async () => {
    server.use(
      http.get('/api/v1/ingredients', () => HttpResponse.json([TEST_INGREDIENT])),
    )
    const store = useIngredientStore()

    const result = await store.search('toma')

    expect(result).toEqual([TEST_INGREDIENT])
    expect(store.results).toEqual([TEST_INGREDIENT])
    expect(store.error).toBeNull()
  })

  it('search setzt loading waehrend des Calls und cleared danach', async () => {
    server.use(
      http.get('/api/v1/ingredients', () => HttpResponse.json([])),
    )
    const store = useIngredientStore()

    const promise = store.search('x')
    expect(store.loading).toBe(true)
    await promise
    expect(store.loading).toBe(false)
  })

  it('bei Server-Fehler wird error gesetzt, results geleert, und der Fehler propagiert', async () => {
    server.use(
      http.get('/api/v1/ingredients', () =>
        HttpResponse.json({ error: 'kaputt' }, { status: 500 }),
      ),
    )
    const store = useIngredientStore()

    await expect(store.search('x')).rejects.toThrow()

    expect(store.results).toEqual([])
    expect(store.error).toBe('kaputt')
  })

  it('reset leert results und error', async () => {
    server.use(http.get('/api/v1/ingredients', () => HttpResponse.json([TEST_INGREDIENT])))
    const store = useIngredientStore()
    await store.search('x')

    store.reset()

    expect(store.results).toEqual([])
    expect(store.error).toBeNull()
  })
})
