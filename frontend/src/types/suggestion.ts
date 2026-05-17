import type { DietTag } from './dietTags'

export interface RecipeMiniDto {
  id: string
  title: string
  servings: number
  prepMinutes: number | null
  dietTags: DietTag[]
}

export interface SuggestionDto {
  recipe: RecipeMiniDto
  reason: string | null
  coverage: number
}

export interface SuggestRequest {
  numSuggestions: number
}
