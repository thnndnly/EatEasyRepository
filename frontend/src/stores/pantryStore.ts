import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as pantryService from '@/services/pantryService'
import { useAuthStore } from '@/stores/authStore'
import type {
  AddPantryItemRequest,
  PantryItemDto,
  UpdatePantryItemRequest,
} from '@/types/pantry'

export const usePantryStore = defineStore('pantry', () => {
  const items = ref<PantryItemDto[]>([])
  const householdId = ref<string | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  function requireToken(): string {
    const authStore = useAuthStore()
    if (!authStore.token) {
      throw new Error('Nicht eingeloggt')
    }
    return authStore.token
  }

  async function load(targetHouseholdId: string): Promise<void> {
    householdId.value = targetHouseholdId
    loading.value = true
    error.value = null
    try {
      items.value = await pantryService.listPantry(requireToken(), targetHouseholdId)
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Laden fehlgeschlagen'
      items.value = []
    } finally {
      loading.value = false
    }
  }

  async function addItem(request: AddPantryItemRequest): Promise<PantryItemDto> {
    if (!householdId.value) {
      throw new Error('Kein Haushalt ausgewaehlt')
    }
    const created = await pantryService.addPantryItem(
      requireToken(),
      householdId.value,
      request,
    )
    // Server kann aggregieren → bei vorhandenem Item ersetzen, sonst anhaengen.
    const exists = items.value.some((i) => i.id === created.id)
    items.value = exists
      ? items.value.map((i) => (i.id === created.id ? created : i))
      : [...items.value, created]
    return created
  }

  async function updateItem(
    id: string,
    request: UpdatePantryItemRequest,
  ): Promise<PantryItemDto> {
    const updated = await pantryService.updatePantryItem(requireToken(), id, request)
    items.value = items.value.map((i) => (i.id === id ? updated : i))
    return updated
  }

  async function removeItem(id: string): Promise<void> {
    await pantryService.deletePantryItem(requireToken(), id)
    items.value = items.value.filter((i) => i.id !== id)
  }

  function reset(): void {
    items.value = []
    householdId.value = null
    error.value = null
  }

  return { items, householdId, loading, error, load, addItem, updateItem, removeItem, reset }
})
