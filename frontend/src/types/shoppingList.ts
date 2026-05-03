import type { Unit } from './units'

export interface ShoppingListItemDto {
  id: string
  ingredientId: string
  ingredientName: string
  amount: number
  unit: Unit
  checked: boolean
}

export interface ShoppingListDto {
  id: string
  householdId: string
  mealPlanId: string
  items: ShoppingListItemDto[]
  updatedAt: string
}
