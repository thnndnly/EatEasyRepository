import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { http, HttpResponse } from 'msw'
import { server } from '@/test/mocks/server'
import {
  TEST_HOUSEHOLD,
  TEST_MEAL_PLAN,
  TEST_RECIPE_MINI,
  TEST_TOKEN,
} from '@/test/fixtures'
import type { MealPlanEntryDto } from '@/types/mealplan'
import { useAuthStore } from './authStore'
import { addDays, mondayOf, useMealPlanStore } from './mealPlanStore'

const ENTRY: MealPlanEntryDto = {
  id: 'entry-1',
  dayOfWeek: 'MONDAY',
  mealType: 'DINNER',
  servings: 2,
  recipe: TEST_RECIPE_MINI,
}

describe('mealPlanStore utilities', () => {
  it('mondayOf liefert den Montag derselben Woche', () => {
    // 2026-05-27 ist ein Mittwoch
    expect(mondayOf(new Date('2026-05-27T00:00:00Z'))).toBe('2026-05-25')
  })

  it('mondayOf am Sonntag liefert den vorhergehenden Montag', () => {
    // 2026-05-31 ist ein Sonntag
    expect(mondayOf(new Date('2026-05-31T00:00:00Z'))).toBe('2026-05-25')
  })

  it('mondayOf am Montag ist idempotent', () => {
    expect(mondayOf(new Date('2026-05-25T00:00:00Z'))).toBe('2026-05-25')
  })

  it('addDays addiert (und subtrahiert) Tage korrekt', () => {
    expect(addDays('2026-05-25', 7)).toBe('2026-06-01')
    expect(addDays('2026-05-25', -7)).toBe('2026-05-18')
  })
})

describe('mealPlanStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    useAuthStore().$patch({ token: TEST_TOKEN })
  })

  it('load setzt plan und householdId', async () => {
    server.use(
      http.get(`/api/v1/households/${TEST_HOUSEHOLD.id}/mealplans`, () =>
        HttpResponse.json(TEST_MEAL_PLAN),
      ),
    )
    const store = useMealPlanStore()

    await store.load(TEST_HOUSEHOLD.id)

    expect(store.plan).toEqual(TEST_MEAL_PLAN)
    expect(store.householdId).toBe(TEST_HOUSEHOLD.id)
  })

  it('load mit explizitem Week-Start uebernimmt diesen Wert', async () => {
    server.use(
      http.get(`/api/v1/households/${TEST_HOUSEHOLD.id}/mealplans`, ({ request }) => {
        expect(new URL(request.url).searchParams.get('weekStart')).toBe('2026-06-01')
        return HttpResponse.json({ ...TEST_MEAL_PLAN, weekStart: '2026-06-01' })
      }),
    )
    const store = useMealPlanStore()

    await store.load(TEST_HOUSEHOLD.id, '2026-06-01')

    expect(store.weekStart).toBe('2026-06-01')
  })

  it('load bei Fehler setzt error und cleared plan', async () => {
    server.use(
      http.get(`/api/v1/households/${TEST_HOUSEHOLD.id}/mealplans`, () =>
        HttpResponse.json({ error: 'kaputt' }, { status: 500 }),
      ),
    )
    const store = useMealPlanStore()

    await store.load(TEST_HOUSEHOLD.id)

    expect(store.plan).toBeNull()
    expect(store.error).toBe('kaputt')
  })

  it('gotoWeek schiebt weekStart um die gegebene Delta', async () => {
    server.use(
      http.get(`/api/v1/households/${TEST_HOUSEHOLD.id}/mealplans`, () =>
        HttpResponse.json(TEST_MEAL_PLAN),
      ),
    )
    const store = useMealPlanStore()
    await store.load(TEST_HOUSEHOLD.id, '2026-05-25')

    await store.gotoWeek(7)

    expect(store.weekStart).toBe('2026-06-01')
  })

  it('weekRangeLabel umfasst Montag bis Sonntag', async () => {
    server.use(
      http.get(`/api/v1/households/${TEST_HOUSEHOLD.id}/mealplans`, () =>
        HttpResponse.json(TEST_MEAL_PLAN),
      ),
    )
    const store = useMealPlanStore()
    await store.load(TEST_HOUSEHOLD.id, '2026-05-25')

    expect(store.weekRangeLabel).toBe('2026-05-25 – 2026-05-31')
  })

  it('setEntry wirft ohne geladenen Plan', async () => {
    const store = useMealPlanStore()

    await expect(
      store.setEntry({
        dayOfWeek: 'MONDAY',
        mealType: 'DINNER',
        recipeId: TEST_RECIPE_MINI.id,
        servings: 2,
      }),
    ).rejects.toThrow('Kein Wochenplan geladen')
  })

  it('setEntry merged den neuen Eintrag in plan.entries und ueberschreibt Duplikate', async () => {
    server.use(
      http.get(`/api/v1/households/${TEST_HOUSEHOLD.id}/mealplans`, () =>
        HttpResponse.json({ ...TEST_MEAL_PLAN, entries: [ENTRY] }),
      ),
      http.put(`/api/v1/mealplans/${TEST_MEAL_PLAN.id}/entries`, () =>
        HttpResponse.json({ ...ENTRY, servings: 4 }),
      ),
    )
    const store = useMealPlanStore()
    await store.load(TEST_HOUSEHOLD.id)

    await store.setEntry({
      dayOfWeek: 'MONDAY',
      mealType: 'DINNER',
      recipeId: TEST_RECIPE_MINI.id,
      servings: 4,
    })

    expect(store.plan?.entries).toHaveLength(1)
    expect(store.plan?.entries[0]!.servings).toBe(4)
  })

  it('removeEntry entfernt den Eintrag aus plan.entries', async () => {
    server.use(
      http.get(`/api/v1/households/${TEST_HOUSEHOLD.id}/mealplans`, () =>
        HttpResponse.json({ ...TEST_MEAL_PLAN, entries: [ENTRY] }),
      ),
      http.delete(
        `/api/v1/mealplans/${TEST_MEAL_PLAN.id}/entries/MONDAY/DINNER`,
        () => new HttpResponse(null, { status: 204 }),
      ),
    )
    const store = useMealPlanStore()
    await store.load(TEST_HOUSEHOLD.id)

    await store.removeEntry('MONDAY', 'DINNER')

    expect(store.plan?.entries).toEqual([])
  })

  it('entryAt findet einen Eintrag nach Tag und Mealtype', async () => {
    server.use(
      http.get(`/api/v1/households/${TEST_HOUSEHOLD.id}/mealplans`, () =>
        HttpResponse.json({ ...TEST_MEAL_PLAN, entries: [ENTRY] }),
      ),
    )
    const store = useMealPlanStore()
    await store.load(TEST_HOUSEHOLD.id)

    expect(store.entryAt('MONDAY', 'DINNER')?.id).toBe('entry-1')
    expect(store.entryAt('TUESDAY', 'DINNER')).toBeNull()
  })

  it('reset cleared plan, householdId und setzt weekStart auf aktuellen Montag', async () => {
    server.use(
      http.get(`/api/v1/households/${TEST_HOUSEHOLD.id}/mealplans`, () =>
        HttpResponse.json(TEST_MEAL_PLAN),
      ),
    )
    const store = useMealPlanStore()
    await store.load(TEST_HOUSEHOLD.id, '2026-05-25')

    store.reset()

    expect(store.plan).toBeNull()
    expect(store.householdId).toBeNull()
    expect(store.weekStart).toBe(mondayOf(new Date()))
  })
})
