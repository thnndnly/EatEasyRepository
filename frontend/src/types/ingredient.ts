import type { Unit } from './units'

export interface IngredientDto {
  id: string
  name: string
  defaultUnit: Unit
}

export interface IngredientCreateRequest {
  name: string
  defaultUnit: Unit
}
