import { apiFetch } from './apiClient'
import type {
  RecipeCreateRequest,
  RecipeDto,
  RecipeFilter,
  RecipeUpdateRequest,
} from '@/types/recipe'

const BASE = '/api/v1/recipes'

function buildQuery(filter: RecipeFilter): string {
  const params = new URLSearchParams()
  if (filter.query) {
    params.set('q', filter.query)
  }
  if (filter.dietTags && filter.dietTags.length > 0) {
    params.set('dietTags', filter.dietTags.join(','))
  }
  if (filter.householdId) {
    params.set('householdId', filter.householdId)
  }
  if (filter.favorite) {
    params.set('favorite', 'true')
  }
  const qs = params.toString()
  return qs ? `?${qs}` : ''
}

export function listRecipes(token: string, filter: RecipeFilter = {}): Promise<RecipeDto[]> {
  return apiFetch<RecipeDto[]>(`${BASE}${buildQuery(filter)}`, { token })
}

export function getRecipe(token: string, id: string): Promise<RecipeDto> {
  return apiFetch<RecipeDto>(`${BASE}/${id}`, { token })
}

export function createRecipe(token: string, request: RecipeCreateRequest): Promise<RecipeDto> {
  return apiFetch<RecipeDto>(BASE, { method: 'POST', body: request, token })
}

export function updateRecipe(
  token: string,
  id: string,
  request: RecipeUpdateRequest,
): Promise<RecipeDto> {
  return apiFetch<RecipeDto>(`${BASE}/${id}`, { method: 'PATCH', body: request, token })
}

export function deleteRecipe(token: string, id: string): Promise<void> {
  return apiFetch<void>(`${BASE}/${id}`, { method: 'DELETE', token })
}

export function setRecipeFavorite(
  token: string,
  id: string,
  favorite: boolean,
): Promise<void> {
  return apiFetch<void>(`${BASE}/${id}/favorite`, {
    method: 'PUT',
    body: { favorite },
    token,
  })
}
