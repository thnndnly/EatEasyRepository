import { apiFetch } from './apiClient'
import type {
  AddPantryItemRequest,
  PantryItemDto,
  UpdatePantryItemRequest,
} from '@/types/pantry'

const BASE = '/api/v1'

export function listPantry(token: string, householdId: string): Promise<PantryItemDto[]> {
  return apiFetch<PantryItemDto[]>(`${BASE}/households/${householdId}/pantry`, { token })
}

export function addPantryItem(
  token: string,
  householdId: string,
  request: AddPantryItemRequest,
): Promise<PantryItemDto> {
  return apiFetch<PantryItemDto>(`${BASE}/households/${householdId}/pantry`, {
    method: 'POST',
    body: request,
    token,
  })
}

export function updatePantryItem(
  token: string,
  itemId: string,
  request: UpdatePantryItemRequest,
): Promise<PantryItemDto> {
  return apiFetch<PantryItemDto>(`${BASE}/pantry/${itemId}`, {
    method: 'PATCH',
    body: request,
    token,
  })
}

export function deletePantryItem(token: string, itemId: string): Promise<void> {
  return apiFetch<void>(`${BASE}/pantry/${itemId}`, { method: 'DELETE', token })
}
