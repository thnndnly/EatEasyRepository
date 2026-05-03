import { apiFetch } from './apiClient'
import type { IngredientCreateRequest, IngredientDto } from '@/types/ingredient'

const BASE = '/api/v1/ingredients'

export function searchIngredients(
  token: string,
  query: string,
  limit = 20,
): Promise<IngredientDto[]> {
  const params = new URLSearchParams()
  if (query) {
    params.set('q', query)
  }
  params.set('limit', String(limit))
  return apiFetch<IngredientDto[]>(`${BASE}?${params.toString()}`, { token })
}

export function getIngredient(token: string, id: string): Promise<IngredientDto> {
  return apiFetch<IngredientDto>(`${BASE}/${id}`, { token })
}

export function createIngredient(
  token: string,
  request: IngredientCreateRequest,
): Promise<IngredientDto> {
  return apiFetch<IngredientDto>(BASE, { method: 'POST', body: request, token })
}
