import { apiFetch } from './apiClient'
import type { SuggestRequest, SuggestionDto } from '@/types/suggestion'

const BASE = '/api/v1'

export function fetchSuggestions(
  token: string,
  householdId: string,
  request: SuggestRequest,
): Promise<SuggestionDto[]> {
  return apiFetch<SuggestionDto[]>(
    `${BASE}/households/${householdId}/suggestions`,
    { method: 'POST', body: request, token },
  )
}
