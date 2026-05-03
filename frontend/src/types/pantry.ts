import type { Unit } from './units'

export interface PantryItemDto {
  id: string
  householdId: string
  ingredientId: string
  ingredientName: string
  amount: number
  unit: Unit
  bestBefore: string | null
}

export interface AddPantryItemRequest {
  ingredientId?: string | null
  ingredientName?: string | null
  amount: number
  unit: Unit
  bestBefore?: string | null
}

export interface UpdatePantryItemRequest {
  amount?: number
  unit?: Unit
  bestBefore?: string | null
}
