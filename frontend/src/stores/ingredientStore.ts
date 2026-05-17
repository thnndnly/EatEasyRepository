import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as ingredientService from '@/services/ingredientService'
import { useRequireToken } from '@/composables/useRequireToken'
import type { IngredientDto } from '@/types/ingredient'

/**
 * Schlanker Store fuer Zutat-Autocomplete. Hauptzweck: Komponenten wie
 * `IngredientPicker` gehen ueber den Store statt direkt auf
 * `ingredientService.searchIngredients` zuzugreifen (Konvention).
 */
export const useIngredientStore = defineStore('ingredient', () => {
  const results = ref<IngredientDto[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  const requireToken = useRequireToken()

  async function search(query: string, limit = 10): Promise<IngredientDto[]> {
    loading.value = true
    error.value = null
    try {
      const fresh = await ingredientService.searchIngredients(requireToken(), query, limit)
      results.value = fresh
      return fresh
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Suche fehlgeschlagen'
      results.value = []
      throw err
    } finally {
      loading.value = false
    }
  }

  function reset(): void {
    results.value = []
    error.value = null
  }

  return { results, loading, error, search, reset }
})
