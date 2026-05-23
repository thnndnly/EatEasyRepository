import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { http, HttpResponse } from 'msw'
import { server } from '@/test/mocks/server'
import {
  TEST_HOUSEHOLD,
  TEST_INVITATION,
  TEST_MEMBER,
  TEST_TOKEN,
  TEST_USER,
} from '@/test/fixtures'
import { useAuthStore } from './authStore'
import { useHouseholdStore } from './householdStore'

describe('householdStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    useAuthStore().$patch({ token: TEST_TOKEN, user: TEST_USER })
  })

  it('load fetched die Haushalte und waehlt den ersten als Fallback', async () => {
    server.use(
      http.get('/api/v1/households', () => HttpResponse.json([TEST_HOUSEHOLD])),
    )
    const store = useHouseholdStore()

    await store.load()

    expect(store.households).toEqual([TEST_HOUSEHOLD])
    expect(store.selectedId).toBe(TEST_HOUSEHOLD.id)
    expect(store.selected?.id).toBe(TEST_HOUSEHOLD.id)
  })

  it('load uebernimmt persistierte Auswahl aus localStorage', async () => {
    const second = { ...TEST_HOUSEHOLD, id: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab', name: 'Zweiter' }
    localStorage.setItem('eateasy.household.selected', second.id)
    server.use(
      http.get('/api/v1/households', () =>
        HttpResponse.json([TEST_HOUSEHOLD, second]),
      ),
    )
    const store = useHouseholdStore()

    await store.load()

    expect(store.selectedId).toBe(second.id)
  })

  it('load verwirft ungueltige persistierte Auswahl', async () => {
    localStorage.setItem('eateasy.household.selected', 'nicht-existent')
    server.use(
      http.get('/api/v1/households', () => HttpResponse.json([TEST_HOUSEHOLD])),
    )
    const store = useHouseholdStore()

    await store.load()

    expect(store.selectedId).toBe(TEST_HOUSEHOLD.id)
  })

  it('load ohne Token resettet den Store', async () => {
    useAuthStore().$patch({ token: null, user: null })
    const store = useHouseholdStore()
    store.households = [TEST_HOUSEHOLD]

    await store.load()

    expect(store.households).toEqual([])
    expect(store.selectedId).toBeNull()
  })

  it('selectHousehold persistiert die ID in localStorage', () => {
    const store = useHouseholdStore()

    store.selectHousehold(TEST_HOUSEHOLD.id)

    expect(store.selectedId).toBe(TEST_HOUSEHOLD.id)
    expect(localStorage.getItem('eateasy.household.selected')).toBe(TEST_HOUSEHOLD.id)
  })

  it('selectHousehold(null) entfernt die persistierte Auswahl', () => {
    localStorage.setItem('eateasy.household.selected', TEST_HOUSEHOLD.id)
    const store = useHouseholdStore()

    store.selectHousehold(null)

    expect(store.selectedId).toBeNull()
    expect(localStorage.getItem('eateasy.household.selected')).toBeNull()
  })

  it('create fuegt den neuen Haushalt hinzu und selektiert ihn', async () => {
    server.use(
      http.post('/api/v1/households', () =>
        HttpResponse.json(TEST_HOUSEHOLD, { status: 201 }),
      ),
    )
    const store = useHouseholdStore()

    await store.create({ name: 'Neuer Haushalt' })

    expect(store.households).toContainEqual(TEST_HOUSEHOLD)
    expect(store.selectedId).toBe(TEST_HOUSEHOLD.id)
  })

  it('loadMembers cached pro householdId', async () => {
    server.use(
      http.get(`/api/v1/households/${TEST_HOUSEHOLD.id}/members`, () =>
        HttpResponse.json([TEST_MEMBER]),
      ),
    )
    const store = useHouseholdStore()

    await store.loadMembers(TEST_HOUSEHOLD.id)

    expect(store.membersOf(TEST_HOUSEHOLD.id)).toEqual([TEST_MEMBER])
    expect(store.membersOf('andere-id')).toEqual([])
  })

  it('invite setzt lastInvitation', async () => {
    server.use(
      http.post(`/api/v1/households/${TEST_HOUSEHOLD.id}/invitations`, () =>
        HttpResponse.json(TEST_INVITATION, { status: 201 }),
      ),
    )
    const store = useHouseholdStore()

    await store.invite(TEST_HOUSEHOLD.id, 'guest@eateasy.local')

    expect(store.lastInvitation).toEqual(TEST_INVITATION)
  })

  it('acceptInvitation dedupliziert bereits vorhandene Haushalte', async () => {
    server.use(
      http.post('/api/v1/invitations/accept', () => HttpResponse.json(TEST_HOUSEHOLD)),
    )
    const store = useHouseholdStore()
    store.households = [TEST_HOUSEHOLD]

    await store.acceptInvitation('inv-token')

    expect(store.households).toHaveLength(1)
    expect(store.selectedId).toBe(TEST_HOUSEHOLD.id)
  })

  it('removeMember entfernt das Mitglied aus dem Cache', async () => {
    server.use(
      http.get(`/api/v1/households/${TEST_HOUSEHOLD.id}/members`, () =>
        HttpResponse.json([TEST_MEMBER]),
      ),
      http.delete(
        `/api/v1/households/${TEST_HOUSEHOLD.id}/members/${TEST_MEMBER.userId}`,
        () => new HttpResponse(null, { status: 204 }),
      ),
    )
    const store = useHouseholdStore()
    await store.loadMembers(TEST_HOUSEHOLD.id)

    await store.removeMember(TEST_HOUSEHOLD.id, TEST_MEMBER.userId)

    expect(store.membersOf(TEST_HOUSEHOLD.id)).toEqual([])
  })
})
