import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { http, HttpResponse } from 'msw'
import { server } from '@/test/mocks/server'
import { TEST_RECIPE, TEST_TOKEN } from '@/test/fixtures'
import type { RecipeCreateRequest } from '@/types/recipe'
import { useAuthStore } from './authStore'
import { useRecipeStore } from './recipeStore'

const baseRequest: RecipeCreateRequest = {
  title: 'New Recipe',
  instructions: 'Mix and serve',
  servings: 2,
  ingredients: [
    { ingredientName: 'Salt', amount: 1, unit: 'TSP' },
  ],
}

describe('recipeStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    useAuthStore().$patch({ token: TEST_TOKEN })
  })

  it('load setzt recipes und merkt sich den Filter', async () => {
    server.use(
      http.get('/api/v1/recipes', ({ request }) => {
        const url = new URL(request.url)
        // Filter muss an den Server gehen
        expect(url.searchParams.get('q')).toBe('toma')
        return HttpResponse.json([TEST_RECIPE])
      }),
    )
    const store = useRecipeStore()

    await store.load({ query: 'toma' })

    expect(store.recipes).toEqual([TEST_RECIPE])
    expect(store.filter).toEqual({ query: 'toma' })
    expect(store.hasResults).toBe(true)
  })

  it('load bei Fehler: error gesetzt, recipes geleert, hasResults=false', async () => {
    server.use(
      http.get('/api/v1/recipes', () =>
        HttpResponse.json({ error: 'boom' }, { status: 500 }),
      ),
    )
    const store = useRecipeStore()

    await store.load()

    expect(store.recipes).toEqual([])
    expect(store.error).toBe('boom')
    expect(store.hasResults).toBe(false)
  })

  it('fetchById setzt current und liefert das Rezept', async () => {
    server.use(
      http.get(`/api/v1/recipes/${TEST_RECIPE.id}`, () => HttpResponse.json(TEST_RECIPE)),
    )
    const store = useRecipeStore()

    const result = await store.fetchById(TEST_RECIPE.id)

    expect(result).toEqual(TEST_RECIPE)
    expect(store.current).toEqual(TEST_RECIPE)
  })

  it('create fügt das neue Rezept zur Liste hinzu und setzt current', async () => {
    server.use(
      http.post('/api/v1/recipes', () => HttpResponse.json(TEST_RECIPE, { status: 201 })),
    )
    const store = useRecipeStore()

    const created = await store.create(baseRequest)

    expect(created).toEqual(TEST_RECIPE)
    expect(store.recipes).toContainEqual(TEST_RECIPE)
    expect(store.current).toEqual(TEST_RECIPE)
  })

  it('update ersetzt das Rezept in der Liste und in current', async () => {
    const updated = { ...TEST_RECIPE, title: 'Tomatensuppe Deluxe' }
    server.use(
      http.get('/api/v1/recipes', () => HttpResponse.json([TEST_RECIPE])),
      http.patch(`/api/v1/recipes/${TEST_RECIPE.id}`, () => HttpResponse.json(updated)),
    )
    const store = useRecipeStore()
    await store.load()
    store.current = TEST_RECIPE

    await store.update(TEST_RECIPE.id, { ...baseRequest, title: 'Tomatensuppe Deluxe' })

    expect(store.recipes[0]!.title).toBe('Tomatensuppe Deluxe')
    expect(store.current?.title).toBe('Tomatensuppe Deluxe')
  })

  it('remove entfernt das Rezept aus der Liste und cleart current bei Match', async () => {
    server.use(
      http.get('/api/v1/recipes', () => HttpResponse.json([TEST_RECIPE])),
      http.delete(`/api/v1/recipes/${TEST_RECIPE.id}`, () => new HttpResponse(null, { status: 204 })),
    )
    const store = useRecipeStore()
    await store.load()
    store.current = TEST_RECIPE

    await store.remove(TEST_RECIPE.id)

    expect(store.recipes).toEqual([])
    expect(store.current).toBeNull()
  })

  it('toggleFavorite PUTtet und flippt das Flag in Liste und current', async () => {
    server.use(
      http.get('/api/v1/recipes', () => HttpResponse.json([TEST_RECIPE])),
      http.get(`/api/v1/recipes/${TEST_RECIPE.id}`, () => HttpResponse.json(TEST_RECIPE)),
      http.put(`/api/v1/recipes/${TEST_RECIPE.id}/favorite`, async ({ request }) => {
        const body = (await request.json()) as { favorite: boolean }
        expect(body.favorite).toBe(true)
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const store = useRecipeStore()
    await store.load()
    await store.fetchById(TEST_RECIPE.id)

    await store.toggleFavorite(TEST_RECIPE.id)

    expect(store.error).toBeNull()
    expect(store.recipes[0]!.favorite).toBe(true)
    expect(store.current?.favorite).toBe(true)
  })

  it('toggleFavorite setzt error bei Server-Fehler und lässt Flag unverändert', async () => {
    server.use(
      http.get('/api/v1/recipes', () => HttpResponse.json([TEST_RECIPE])),
      http.put(`/api/v1/recipes/${TEST_RECIPE.id}/favorite`, () =>
        HttpResponse.json({ message: 'boom' }, { status: 500 }),
      ),
    )
    const store = useRecipeStore()
    await store.load()

    await store.toggleFavorite(TEST_RECIPE.id)

    expect(store.error).not.toBeNull()
    expect(store.recipes[0]!.favorite).toBe(false)
  })

  it('reset leert alles und entfernt den Filter', async () => {
    server.use(http.get('/api/v1/recipes', () => HttpResponse.json([TEST_RECIPE])))
    const store = useRecipeStore()
    await store.load({ query: 'x' })

    store.reset()

    expect(store.recipes).toEqual([])
    expect(store.current).toBeNull()
    expect(store.filter).toEqual({})
    expect(store.error).toBeNull()
  })
})
