import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { http, HttpResponse } from 'msw'
import { server } from '@/test/mocks/server'
import { TEST_MEAL_PLAN, TEST_SHOPPING_LIST, TEST_TOKEN } from '@/test/fixtures'
import { useAuthStore } from './authStore'
import { useShoppingListStore } from './shoppingListStore'

describe('shoppingListStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    useAuthStore().$patch({ token: TEST_TOKEN })
  })

  it('load setzt list und mealPlanId', async () => {
    server.use(
      http.get(`/api/v1/mealplans/${TEST_MEAL_PLAN.id}/shoppinglist`, () =>
        HttpResponse.json(TEST_SHOPPING_LIST),
      ),
    )
    const store = useShoppingListStore()

    await store.load(TEST_MEAL_PLAN.id)

    expect(store.list).toEqual(TEST_SHOPPING_LIST)
    expect(store.mealPlanId).toBe(TEST_MEAL_PLAN.id)
  })

  it('regenerate macht nichts ohne geladene mealPlanId', async () => {
    const store = useShoppingListStore()
    await store.regenerate()
    expect(store.list).toBeNull()
  })

  it('regenerate POSTet und ersetzt die list', async () => {
    const fresh = { ...TEST_SHOPPING_LIST, updatedAt: '2026-12-31T00:00:00Z' }
    server.use(
      http.get(`/api/v1/mealplans/${TEST_MEAL_PLAN.id}/shoppinglist`, () =>
        HttpResponse.json(TEST_SHOPPING_LIST),
      ),
      http.post(`/api/v1/mealplans/${TEST_MEAL_PLAN.id}/shoppinglist/regenerate`, () =>
        HttpResponse.json(fresh),
      ),
    )
    const store = useShoppingListStore()
    await store.load(TEST_MEAL_PLAN.id)

    await store.regenerate()

    expect(store.list?.updatedAt).toBe('2026-12-31T00:00:00Z')
  })

  it('toggle flippt checked auf einem Item', async () => {
    const toggled = { ...TEST_SHOPPING_LIST.items[0]!, checked: true }
    server.use(
      http.get(`/api/v1/mealplans/${TEST_MEAL_PLAN.id}/shoppinglist`, () =>
        HttpResponse.json(TEST_SHOPPING_LIST),
      ),
      http.patch(`/api/v1/shoppinglist/items/${TEST_SHOPPING_LIST.items[0]!.id}`, () =>
        HttpResponse.json(toggled),
      ),
    )
    const store = useShoppingListStore()
    await store.load(TEST_MEAL_PLAN.id)

    await store.toggle(TEST_SHOPPING_LIST.items[0]!.id, true)

    expect(store.list?.items[0]!.checked).toBe(true)
    // andere Items unveraendert
    expect(store.list?.items[1]!.checked).toBe(true)
  })

  it('sortedItems: unchecked oben, checked unten', async () => {
    server.use(
      http.get(`/api/v1/mealplans/${TEST_MEAL_PLAN.id}/shoppinglist`, () =>
        HttpResponse.json(TEST_SHOPPING_LIST),
      ),
    )
    const store = useShoppingListStore()
    await store.load(TEST_MEAL_PLAN.id)

    const sorted = store.sortedItems
    expect(sorted[0]!.checked).toBe(false)
    expect(sorted[1]!.checked).toBe(true)
  })

  it('sortedItems ist leer ohne geladene list', () => {
    const store = useShoppingListStore()
    expect(store.sortedItems).toEqual([])
  })

  it('reset leert list, mealPlanId, error', async () => {
    server.use(
      http.get(`/api/v1/mealplans/${TEST_MEAL_PLAN.id}/shoppinglist`, () =>
        HttpResponse.json(TEST_SHOPPING_LIST),
      ),
    )
    const store = useShoppingListStore()
    await store.load(TEST_MEAL_PLAN.id)

    store.reset()

    expect(store.list).toBeNull()
    expect(store.mealPlanId).toBeNull()
    expect(store.error).toBeNull()
  })
})
