import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as shoppingListService from '@/services/shoppingListService'
import { updateIngredientCategory } from '@/services/ingredientService'
import { useRequireToken } from '@/composables/useRequireToken'
import type { ShoppingListDto, ShoppingListItemDto } from '@/types/shoppingList'
import { INGREDIENT_CATEGORIES, type IngredientCategory } from '@/types/ingredient'

export interface CategoryGroup {
  category: IngredientCategory
  items: ShoppingListItemDto[]
}

export const useShoppingListStore = defineStore('shoppingList', () => {
  const list = ref<ShoppingListDto | null>(null)
  const mealPlanId = ref<string | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const requireToken = useRequireToken()

  async function load(targetMealPlanId: string): Promise<void> {
    mealPlanId.value = targetMealPlanId
    loading.value = true
    error.value = null
    try {
      list.value = await shoppingListService.getShoppingList(requireToken(), targetMealPlanId)
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Laden fehlgeschlagen'
      list.value = null
    } finally {
      loading.value = false
    }
  }

  async function regenerate(): Promise<void> {
    if (!mealPlanId.value) {
      return
    }
    loading.value = true
    error.value = null
    try {
      list.value = await shoppingListService.regenerateShoppingList(
        requireToken(),
        mealPlanId.value,
      )
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Neuberechnen fehlgeschlagen'
    } finally {
      loading.value = false
    }
  }

  async function toggle(itemId: string, checked: boolean): Promise<void> {
    if (!list.value) {
      return
    }
    try {
      const updated = await shoppingListService.toggleItemChecked(
        requireToken(),
        itemId,
        checked,
      )
      list.value = {
        ...list.value,
        items: list.value.items.map((i) => (i.id === itemId ? updated : i)),
      }
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Aktualisieren fehlgeschlagen'
    }
  }

  async function changeCategory(
    ingredientId: string,
    category: IngredientCategory,
  ): Promise<void> {
    if (!list.value) {
      return
    }
    error.value = null
    try {
      await updateIngredientCategory(requireToken(), ingredientId, category)
      // Kategorie gilt pro Zutat — alle Items dieser Zutat mitziehen.
      list.value = {
        ...list.value,
        items: list.value.items.map((i) =>
          i.ingredientId === ingredientId ? { ...i, category } : i,
        ),
      }
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Kategorie aendern fehlgeschlagen'
      // Fehlerfall: Kategorie bleibt unveraendert, aber die betroffenen Items
      // werden neu referenziert. Dadurch reagiert die UI (das native <select>)
      // und springt auf den tatsaechlichen, gespeicherten Zustand zurueck.
      if (list.value) {
        list.value = {
          ...list.value,
          items: list.value.items.map((i) =>
            i.ingredientId === ingredientId ? { ...i } : i,
          ),
        }
      }
    }
  }

  function reset(): void {
    list.value = null
    mealPlanId.value = null
    error.value = null
  }

  // Sortierung: unchecked oben, checked unten — innerhalb alphabetisch
  // (Server sortiert schon nach Name).
  const sortedItems = computed<ShoppingListItemDto[]>(() => {
    if (!list.value) {
      return []
    }
    const open: ShoppingListItemDto[] = []
    const done: ShoppingListItemDto[] = []
    for (const item of list.value.items) {
      ;(item.checked ? done : open).push(item)
    }
    return [...open, ...done]
  })

  // Gruppierung nach Supermarkt-Kategorie (Phase 16) — Reihenfolge wie ein
  // typischer Gang durch den Laden, innerhalb: unchecked vor checked.
  const groupedItems = computed<CategoryGroup[]>(() => {
    const groups: CategoryGroup[] = []
    for (const category of INGREDIENT_CATEGORIES) {
      const items = sortedItems.value.filter((i) => i.category === category)
      if (items.length > 0) {
        groups.push({ category, items })
      }
    }
    return groups
  })

  return {
    list,
    mealPlanId,
    loading,
    error,
    sortedItems,
    groupedItems,
    load,
    regenerate,
    toggle,
    changeCategory,
    reset,
  }
})
