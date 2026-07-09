import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as pantryService from '@/services/pantryService'
import * as barcodeService from '@/services/barcodeService'
import { useRequireToken } from '@/composables/useRequireToken'
import { daysUntil } from '@/utils/mhd'
import type {
  AddPantryItemRequest,
  PantryItemDto,
  UpdatePantryItemRequest,
} from '@/types/pantry'
import type { BarcodePantryRequest, BarcodeProductDto } from '@/types/barcode'

const EXPIRING_SOON_DAYS = 7

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
      throw new Error('Kein Haushalt ausgewählt')
    }
    error.value = null
    try {
      const created = await pantryService.addPantryItem(
        requireToken(),
        householdId.value,
        request,
      )
      // Server kann aggregieren → bei vorhandenem Item ersetzen, sonst anhängen.
      const exists = items.value.some((i) => i.id === created.id)
      items.value = exists
        ? items.value.map((i) => (i.id === created.id ? created : i))
        : [...items.value, created]
      return created
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Hinzufügen fehlgeschlagen'
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
      error.value = err instanceof Error ? err.message : 'Löschen fehlgeschlagen'
      throw err
    }
  }

  /** Barcode-Lookup (OpenFoodFacts) — Server-State-Zugriff läuft über den Store,
   *  nicht direkt aus der Komponente. */
  async function lookupByBarcode(barcode: string): Promise<BarcodeProductDto> {
    error.value = null
    try {
      return await barcodeService.lookupBarcode(requireToken(), barcode)
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Produkt konnte nicht geladen werden'
      throw err
    }
  }

  /** Legt einen Vorrats-Eintrag per Barcode an und pflegt die Liste wie addItem. */
  async function addByBarcode(request: BarcodePantryRequest): Promise<PantryItemDto> {
    if (!householdId.value) {
      throw new Error('Kein Haushalt ausgewählt')
    }
    error.value = null
    try {
      const created = await barcodeService.addPantryItemByBarcode(
        requireToken(),
        householdId.value,
        request,
      )
      const exists = items.value.some((i) => i.id === created.id)
      items.value = exists
        ? items.value.map((i) => (i.id === created.id ? created : i))
        : [...items.value, created]
      return created
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Hinzufügen fehlgeschlagen'
      throw err
    }
  }

  function reset(): void {
    items.value = []
    householdId.value = null
    error.value = null
  }

  // Items mit MHD in den nächsten 7 Tagen (inkl. bereits abgelaufener),
  // aufsteigend sortiert — Datengrundlage für das Dashboard-Widget.
  const expiringSoon = computed<PantryItemDto[]>(() =>
    items.value
      .filter((i) => i.bestBefore !== null && daysUntil(i.bestBefore) <= EXPIRING_SOON_DAYS)
      .slice()
      .sort((a, b) => (a.bestBefore ?? '').localeCompare(b.bestBefore ?? '')),
  )

  return {
    items,
    householdId,
    loading,
    error,
    expiringSoon,
    load,
    addItem,
    updateItem,
    removeItem,
    lookupByBarcode,
    addByBarcode,
    reset,
  }
})
