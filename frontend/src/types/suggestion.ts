import type { RecipeMiniDto } from './recipe'

export type { RecipeMiniDto } from './recipe'

export interface SuggestionDto {
  recipe: RecipeMiniDto
  reason: string | null
  coverage: number
}

export interface SuggestRequest {
  numSuggestions: number
}
