import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as householdService from '@/services/householdService'
import { useAuthStore } from '@/stores/authStore'
import { useRequireToken } from '@/composables/useRequireToken'
import type {
  HouseholdCreateRequest,
  HouseholdDto,
  HouseholdUpdateRequest,
} from '@/types/household'

const STORAGE_SELECTED = 'eateasy.household.selected'

/**
 * Quelle der Wahrheit fuer alle Haushalte des eingeloggten Users plus die ID
 * des aktuell ausgewaehlten Haushalts (Topbar-Switcher). Auswahl wird in
 * localStorage persistiert, damit Reload den Kontext beibehaelt.
 */
export const useHouseholdStore = defineStore('household', () => {
  const households = ref<HouseholdDto[]>([])
  const selectedId = ref<string | null>(null)
  const loading = ref(false)
  const lastLoadedFor = ref<string | null>(null)

  const selected = computed<HouseholdDto | null>(() =>
    households.value.find((h) => h.id === selectedId.value) ?? null,
  )

  const requireToken = useRequireToken()

  function selectHousehold(id: string | null): void {
    selectedId.value = id
    if (id) {
      localStorage.setItem(STORAGE_SELECTED, id)
    } else {
      localStorage.removeItem(STORAGE_SELECTED)
    }
  }

  async function load(force = false): Promise<void> {
    const authStore = useAuthStore()
    const token = authStore.token
    const userId = authStore.user?.id ?? null
    if (!token || !userId) {
      reset()
      return
    }
    if (!force && lastLoadedFor.value === userId) {
      return
    }
    loading.value = true
    try {
      households.value = await householdService.listHouseholds(token)
      lastLoadedFor.value = userId
      const stored = localStorage.getItem(STORAGE_SELECTED)
      const validStored = stored && households.value.some((h) => h.id === stored)
        ? stored
        : null
      const fallback = households.value[0]?.id ?? null
      selectHousehold(validStored ?? fallback)
    } finally {
      loading.value = false
    }
  }

  async function create(request: HouseholdCreateRequest): Promise<HouseholdDto> {
    const created = await householdService.createHousehold(requireToken(), request)
    households.value = [...households.value, created]
    selectHousehold(created.id)
    return created
  }

  async function update(id: string, request: HouseholdUpdateRequest): Promise<HouseholdDto> {
    const updated = await householdService.updateHousehold(requireToken(), id, request)
    households.value = households.value.map((h) => (h.id === id ? updated : h))
    return updated
  }

  async function refreshOne(id: string): Promise<HouseholdDto> {
    const fresh = await householdService.getHousehold(requireToken(), id)
    households.value = households.value.map((h) => (h.id === id ? fresh : h))
    return fresh
  }

  async function acceptInvitation(token: string): Promise<HouseholdDto> {
    const joined = await householdService.acceptInvitation(requireToken(), { token })
    const exists = households.value.some((h) => h.id === joined.id)
    households.value = exists
      ? households.value.map((h) => (h.id === joined.id ? joined : h))
      : [...households.value, joined]
    selectHousehold(joined.id)
    return joined
  }

  function reset(): void {
    households.value = []
    selectedId.value = null
    lastLoadedFor.value = null
    localStorage.removeItem(STORAGE_SELECTED)
  }

  return {
    households,
    selectedId,
    selected,
    loading,
    selectHousehold,
    load,
    create,
    update,
    refreshOne,
    acceptInvitation,
    reset,
  }
})
