import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as externalRecipeService from '@/services/externalRecipeService'
import { useRequireToken } from '@/composables/useRequireToken'
import type {
  ExternalRecipePreviewDto,
  RecipeImportRequest,
} from '@/types/externalRecipe'
import type { RecipeDto } from '@/types/recipe'

/**
 * Vermittler zwischen Frontend und Externe-API-Adapter (TheMealDB, etc.).
 * Components rufen Suche/Import über den Store, damit Service-Calls nicht
 * direkt in Templates passieren.
 */
export const useIntegrationStore = defineStore('integration', () => {
  const results = ref<ExternalRecipePreviewDto[]>([])
  const searching = ref(false)
  const importingId = ref<string | null>(null)
  const error = ref<string | null>(null)

  const requireToken = useRequireToken()

  async function searchExternal(source: string, query: string): Promise<ExternalRecipePreviewDto[]> {
    searching.value = true
    error.value = null
    try {
      const fresh = await externalRecipeService.searchExternal(requireToken(), source, query)
      results.value = fresh
      return fresh
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Suche fehlgeschlagen'
      results.value = []
      throw err
    } finally {
      searching.value = false
    }
  }

  async function importExternal(request: RecipeImportRequest): Promise<RecipeDto> {
    importingId.value = request.externalId
    error.value = null
    try {
      return await externalRecipeService.importExternalRecipe(requireToken(), request)
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Import fehlgeschlagen'
      throw err
    } finally {
      importingId.value = null
    }
  }

  function reset(): void {
    results.value = []
    error.value = null
    importingId.value = null
  }

  return {
    results,
    searching,
    importingId,
    error,
    searchExternal,
    importExternal,
    reset,
  }
})
