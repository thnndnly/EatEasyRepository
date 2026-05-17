import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as pantryService from '@/services/pantryService'
import { useRequireToken } from '@/composables/useRequireToken'
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

  const requireToken = useRequireToken()

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
    error.value = null
    try {
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
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Hinzufuegen fehlgeschlagen'
      throw err
    }
  }

  async function updateItem(
    id: string,
    request: UpdatePantryItemRequest,
  ): Promise<PantryItemDto> {
    error.value = null
    try {
      const updated = await pantryService.updatePantryItem(requireToken(), id, request)
      items.value = items.value.map((i) => (i.id === id ? updated : i))
      return updated
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Speichern fehlgeschlagen'
      throw err
    }
  }

  async function removeItem(id: string): Promise<void> {
    error.value = null
    try {
      await pantryService.deletePantryItem(requireToken(), id)
      items.value = items.value.filter((i) => i.id !== id)
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Loeschen fehlgeschlagen'
      throw err
    }
  }

  function reset(): void {
    items.value = []
    householdId.value = null
    error.value = null
  }

  return { items, householdId, loading, error, load, addItem, updateItem, removeItem, reset }
})
