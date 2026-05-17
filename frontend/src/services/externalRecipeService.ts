import { apiFetch } from './apiClient'
import type {
  ExternalRecipePreviewDto,
  RecipeImportRequest,
} from '@/types/externalRecipe'
import type { RecipeDto } from '@/types/recipe'

const BASE = '/api/v1'

export function searchExternal(
  token: string,
  source: string,
  query: string,
): Promise<ExternalRecipePreviewDto[]> {
  const params = new URLSearchParams({ source, q: query })
  return apiFetch<ExternalRecipePreviewDto[]>(
    `${BASE}/integration/recipes/search?${params.toString()}`,
    { token },
  )
}

export function importExternalRecipe(
  token: string,
  request: RecipeImportRequest,
): Promise<RecipeDto> {
  return apiFetch<RecipeDto>(`${BASE}/recipes/import`, {
    method: 'POST',
    body: request,
    token,
  })
}
