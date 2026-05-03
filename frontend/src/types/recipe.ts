import type { DietTag } from './dietTags'
import type { Unit } from './units'

export interface RecipeIngredientDto {
  id: string
  ingredientId: string
  ingredientName: string
  amount: number
  unit: Unit
  note: string | null
}

export interface RecipeDto {
  id: string
  ownerId: string
  householdId: string | null
  title: string
  description: string | null
  instructions: string
  servings: number
  prepMinutes: number | null
  dietTags: DietTag[]
  sourceUrl: string | null
  externalSource: string | null
  ingredients: RecipeIngredientDto[]
  createdAt: string
  updatedAt: string
}

export interface RecipeIngredientRequest {
  ingredientId?: string | null
  ingredientName?: string | null
  amount: number
  unit: Unit
  note?: string | null
}

export interface RecipeCreateRequest {
  title: string
  description?: string | null
  instructions: string
  servings: number
  prepMinutes?: number | null
  dietTags?: DietTag[]
  householdId?: string | null
  ingredients: RecipeIngredientRequest[]
}

export type RecipeUpdateRequest = RecipeCreateRequest

export interface RecipeFilter {
  query?: string
  dietTags?: DietTag[]
  householdId?: string | null
}
