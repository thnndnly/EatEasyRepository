import type { Unit } from './units'
import type { IngredientCategory } from './ingredient'

export interface ShoppingListItemDto {
  id: string
  ingredientId: string
  ingredientName: string
  category: IngredientCategory
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
