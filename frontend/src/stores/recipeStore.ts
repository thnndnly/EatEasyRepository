import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as recipeService from '@/services/recipeService'
import { useRequireToken } from '@/composables/useRequireToken'
import type {
  RecipeCreateRequest,
  RecipeDto,
  RecipeFilter,
  RecipeUpdateRequest,
} from '@/types/recipe'

/**
 * Hält die letzte Liste, den aktuell betrachteten Datensatz und die zuletzt
 * verwendeten Filter. Filter sind ein UI-Detail (kein Server-State), bleiben
 * aber im Store, damit Wechsel zwischen List- und Detail-View den Filter
 * nicht wegwirft.
 */
export const useRecipeStore = defineStore('recipe', () => {
  const recipes = ref<RecipeDto[]>([])
  const current = ref<RecipeDto | null>(null)
  const filter = ref<RecipeFilter>({})
  const loading = ref(false)
  const error = ref<string | null>(null)

  const requireToken = useRequireToken()

  async function load(nextFilter?: RecipeFilter): Promise<void> {
    if (nextFilter) {
      filter.value = nextFilter
    }
    loading.value = true
    error.value = null
    try {
      recipes.value = await recipeService.listRecipes(requireToken(), filter.value)
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Laden fehlgeschlagen'
      recipes.value = []
    } finally {
      loading.value = false
    }
  }

  async function fetchById(id: string): Promise<RecipeDto> {
    loading.value = true
    error.value = null
    try {
      const recipe = await recipeService.getRecipe(requireToken(), id)
      current.value = recipe
      return recipe
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Laden fehlgeschlagen'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function create(request: RecipeCreateRequest): Promise<RecipeDto> {
    error.value = null
    try {
      const created = await recipeService.createRecipe(requireToken(), request)
      recipes.value = [...recipes.value, created]
      current.value = created
      return created
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Anlegen fehlgeschlagen'
      throw err
    }
  }

  async function update(id: string, request: RecipeUpdateRequest): Promise<RecipeDto> {
    error.value = null
    try {
      const updated = await recipeService.updateRecipe(requireToken(), id, request)
      recipes.value = recipes.value.map((r) => (r.id === id ? updated : r))
      if (current.value?.id === id) {
        current.value = updated
      }
      return updated
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Speichern fehlgeschlagen'
      throw err
    }
  }

  async function remove(id: string): Promise<void> {
    error.value = null
    try {
      await recipeService.deleteRecipe(requireToken(), id)
      recipes.value = recipes.value.filter((r) => r.id !== id)
      if (current.value?.id === id) {
        current.value = null
      }
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Löschen fehlgeschlagen'
      throw err
    }
  }

  async function toggleFavorite(id: string): Promise<void> {
    const target = recipes.value.find((r) => r.id === id) ?? current.value
    if (!target || target.id !== id) {
      return
    }
    const next = !target.favorite
    error.value = null
    try {
      await recipeService.setRecipeFavorite(requireToken(), id, next)
      recipes.value = recipes.value.map((r) => (r.id === id ? { ...r, favorite: next } : r))
      if (current.value?.id === id) {
        current.value = { ...current.value, favorite: next }
      }
    } catch (err: unknown) {
      // Bewusst kein rethrow: der Favoriten-Toggle ist eine Fire-and-forget-
      // UI-Aktion; die aufrufende View prüft nach dem await `store.error`
      // (statt zu catchen), um Erfolgs-Toast/Fehleranzeige zu steuern.
      error.value = err instanceof Error ? err.message : 'Favorit ändern fehlgeschlagen'
    }
  }

  function reset(): void {
    recipes.value = []
    current.value = null
    filter.value = {}
    error.value = null
  }

  const hasResults = computed(() => recipes.value.length > 0)

  return {
    recipes,
    current,
    filter,
    loading,
    error,
    hasResults,
    load,
    fetchById,
    create,
    update,
    remove,
    toggleFavorite,
    reset,
  }
})
