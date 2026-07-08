import type { RecipeMiniDto } from './recipe'

export type { RecipeMiniDto } from './recipe'

export interface SuggestionDto {
  recipe: RecipeMiniDto
  reason: string | null
  coverage: number
}

export interface SuggestionResponse {
  /** false = KI (Ollama) war nicht erreichbar; nur nach Vorrats-Abdeckung sortiert. */
  aiAvailable: boolean
  suggestions: SuggestionDto[]
}

export interface SuggestRequest {
  numSuggestions: number
}
