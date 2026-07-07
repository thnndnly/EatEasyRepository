import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { http, HttpResponse } from 'msw'
import { server } from '@/test/mocks/server'
import { TEST_HOUSEHOLD, TEST_PANTRY_ITEM, TEST_TOKEN } from '@/test/fixtures'
import { useAuthStore } from './authStore'
import { usePantryStore } from './pantryStore'

describe('pantryStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    useAuthStore().$patch({ token: TEST_TOKEN })
  })

  it('load setzt items und merkt sich householdId', async () => {
    server.use(
      http.get(`/api/v1/households/${TEST_HOUSEHOLD.id}/pantry`, () =>
        HttpResponse.json([TEST_PANTRY_ITEM]),
      ),
    )
    const store = usePantryStore()

    await store.load(TEST_HOUSEHOLD.id)

    expect(store.items).toEqual([TEST_PANTRY_ITEM])
    expect(store.householdId).toBe(TEST_HOUSEHOLD.id)
  })

  it('load setzt bei Fehler error und leert items', async () => {
    server.use(
      http.get(`/api/v1/households/${TEST_HOUSEHOLD.id}/pantry`, () =>
        HttpResponse.json({ error: 'kein Zugriff' }, { status: 403 }),
      ),
    )
    const store = usePantryStore()

    await store.load(TEST_HOUSEHOLD.id)

    expect(store.items).toEqual([])
    expect(store.error).toBe('kein Zugriff')
  })

  describe('expiringSoon', () => {
    afterEach(() => {
      vi.useRealTimers()
    })

    it('liefert nur Items mit MHD ≤ 7 Tage, aufsteigend sortiert', async () => {
      vi.useFakeTimers()
      vi.setSystemTime(new Date('2026-07-01T12:00:00Z'))

      const inTwoDays = { ...TEST_PANTRY_ITEM, id: 'p-1', bestBefore: '2026-07-03' }
      const expired = { ...TEST_PANTRY_ITEM, id: 'p-2', bestBefore: '2026-06-29' }
      const farAway = { ...TEST_PANTRY_ITEM, id: 'p-3', bestBefore: '2026-08-01' }
      const noMhd = { ...TEST_PANTRY_ITEM, id: 'p-4', bestBefore: null }
      server.use(
        http.get(`/api/v1/households/${TEST_HOUSEHOLD.id}/pantry`, () =>
          HttpResponse.json([inTwoDays, expired, farAway, noMhd]),
        ),
      )
      const store = usePantryStore()
      await store.load(TEST_HOUSEHOLD.id)

      expect(store.expiringSoon.map((i) => i.id)).toEqual(['p-2', 'p-1'])
    })

    it('ist leer ohne geladene Items', () => {
      const store = usePantryStore()
      expect(store.expiringSoon).toEqual([])
    })
  })

  it('addItem wirft ohne ausgewaehlten Haushalt', async () => {
    const store = usePantryStore()

    await expect(
      store.addItem({ ingredientName: 'X', amount: 1, unit: 'PIECE' }),
    ).rejects.toThrow('Kein Haushalt ausgewaehlt')
  })

  it('addItem haengt neues Item an die Liste', async () => {
    server.use(
      http.get(`/api/v1/households/${TEST_HOUSEHOLD.id}/pantry`, () => HttpResponse.json([])),
      http.post(`/api/v1/households/${TEST_HOUSEHOLD.id}/pantry`, () =>
        HttpResponse.json(TEST_PANTRY_ITEM, { status: 201 }),
      ),
    )
    const store = usePantryStore()
    await store.load(TEST_HOUSEHOLD.id)

    const created = await store.addItem({
      ingredientName: 'Tomate',
      amount: 5,
      unit: 'PIECE',
    })

    expect(created).toEqual(TEST_PANTRY_ITEM)
    expect(store.items).toEqual([TEST_PANTRY_ITEM])
  })

  it('addItem ersetzt aggregiertes Item, wenn Server dieselbe ID zurueckgibt', async () => {
    const merged = { ...TEST_PANTRY_ITEM, amount: 8 }
    server.use(
      http.get(`/api/v1/households/${TEST_HOUSEHOLD.id}/pantry`, () =>
        HttpResponse.json([TEST_PANTRY_ITEM]),
      ),
      http.post(`/api/v1/households/${TEST_HOUSEHOLD.id}/pantry`, () =>
        HttpResponse.json(merged, { status: 201 }),
      ),
    )
    const store = usePantryStore()
    await store.load(TEST_HOUSEHOLD.id)

    await store.addItem({ ingredientName: 'Tomate', amount: 3, unit: 'PIECE' })

    expect(store.items).toHaveLength(1)
    expect(store.items[0]!.amount).toBe(8)
  })

  it('updateItem ersetzt das Item in der Liste', async () => {
    const patched = { ...TEST_PANTRY_ITEM, amount: 2 }
    server.use(
      http.get(`/api/v1/households/${TEST_HOUSEHOLD.id}/pantry`, () =>
        HttpResponse.json([TEST_PANTRY_ITEM]),
      ),
      http.patch(`/api/v1/pantry/${TEST_PANTRY_ITEM.id}`, () => HttpResponse.json(patched)),
    )
    const store = usePantryStore()
    await store.load(TEST_HOUSEHOLD.id)

    await store.updateItem(TEST_PANTRY_ITEM.id, { amount: 2 })

    expect(store.items[0]!.amount).toBe(2)
  })

  it('removeItem entfernt das Item aus der Liste', async () => {
    server.use(
      http.get(`/api/v1/households/${TEST_HOUSEHOLD.id}/pantry`, () =>
        HttpResponse.json([TEST_PANTRY_ITEM]),
      ),
      http.delete(`/api/v1/pantry/${TEST_PANTRY_ITEM.id}`, () => new HttpResponse(null, { status: 204 })),
    )
    const store = usePantryStore()
    await store.load(TEST_HOUSEHOLD.id)

    await store.removeItem(TEST_PANTRY_ITEM.id)

    expect(store.items).toEqual([])
  })

  it('reset leert items, householdId und error', async () => {
    server.use(
      http.get(`/api/v1/households/${TEST_HOUSEHOLD.id}/pantry`, () =>
        HttpResponse.json([TEST_PANTRY_ITEM]),
      ),
    )
    const store = usePantryStore()
    await store.load(TEST_HOUSEHOLD.id)

    store.reset()

    expect(store.items).toEqual([])
    expect(store.householdId).toBeNull()
    expect(store.error).toBeNull()
  })
})
