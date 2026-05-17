import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as shoppingListService from '@/services/shoppingListService'
import { useRequireToken } from '@/composables/useRequireToken'
import type { ShoppingListDto, ShoppingListItemDto } from '@/types/shoppingList'

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

  return {
    list,
    mealPlanId,
    loading,
    error,
    sortedItems,
    load,
    regenerate,
    toggle,
    reset,
  }
})
