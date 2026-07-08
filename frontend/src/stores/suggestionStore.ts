import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as suggestionService from '@/services/suggestionService'
import { useRequireToken } from '@/composables/useRequireToken'
import type { SuggestRequest, SuggestionDto } from '@/types/suggestion'

/**
 * Cached Vorschlaege pro Haushalt — Stretch-Feature, daher recht schlank
 * gehalten. Pinia hier nur, damit Views konsistent ueber Store gehen
 * (keine direkten Service-Aufrufe aus Components).
 */
export const useSuggestionStore = defineStore('suggestion', () => {
  const suggestions = ref<SuggestionDto[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const requested = ref(false)
  /** false, wenn die KI (Ollama) nicht erreichbar war — Views zeigen dann einen Hinweis. */
  const aiAvailable = ref(true)

  const requireToken = useRequireToken()

  async function fetch(householdId: string, request: SuggestRequest): Promise<SuggestionDto[]> {
    loading.value = true
    error.value = null
    requested.value = true
    try {
      const result = await suggestionService.fetchSuggestions(
        requireToken(),
        householdId,
        request,
      )
      suggestions.value = result.suggestions
      aiAvailable.value = result.aiAvailable
      return result.suggestions
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Vorschlaege konnten nicht geladen werden'
      suggestions.value = []
      throw err
    } finally {
      loading.value = false
    }
  }

  function reset(): void {
    suggestions.value = []
    error.value = null
    requested.value = false
    aiAvailable.value = true
  }

  return { suggestions, loading, error, requested, aiAvailable, fetch, reset }
})
