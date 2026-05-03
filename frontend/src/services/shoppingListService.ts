import { apiFetch } from './apiClient'
import type { ShoppingListDto, ShoppingListItemDto } from '@/types/shoppingList'

const BASE = '/api/v1'

export function getShoppingList(token: string, mealPlanId: string): Promise<ShoppingListDto> {
  return apiFetch<ShoppingListDto>(`${BASE}/mealplans/${mealPlanId}/shoppinglist`, { token })
}

export function regenerateShoppingList(
  token: string,
  mealPlanId: string,
): Promise<ShoppingListDto> {
  return apiFetch<ShoppingListDto>(
    `${BASE}/mealplans/${mealPlanId}/shoppinglist/regenerate`,
    { method: 'POST', token },
  )
}

export function toggleItemChecked(
  token: string,
  itemId: string,
  checked: boolean,
): Promise<ShoppingListItemDto> {
  return apiFetch<ShoppingListItemDto>(`${BASE}/shoppinglist/items/${itemId}`, {
    method: 'PATCH',
    body: { checked },
    token,
  })
}
