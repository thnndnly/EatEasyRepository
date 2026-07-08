import { apiFetch } from './apiClient'
import type { SuggestRequest, SuggestionResponse } from '@/types/suggestion'

const BASE = '/api/v1'

export function fetchSuggestions(
  token: string,
  householdId: string,
  request: SuggestRequest,
): Promise<SuggestionResponse> {
  return apiFetch<SuggestionResponse>(
    `${BASE}/households/${householdId}/suggestions`,
    { method: 'POST', body: request, token },
  )
}
