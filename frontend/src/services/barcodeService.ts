import { apiFetch } from './apiClient'
import type { BarcodePantryRequest, BarcodeProductDto } from '@/types/barcode'
import type { PantryItemDto } from '@/types/pantry'

const BASE = '/api/v1'

export function lookupBarcode(
  token: string,
  barcode: string,
): Promise<BarcodeProductDto> {
  return apiFetch<BarcodeProductDto>(
    `${BASE}/integration/products/${encodeURIComponent(barcode)}`,
    { token },
  )
}

export function addPantryItemByBarcode(
  token: string,
  householdId: string,
  request: BarcodePantryRequest,
): Promise<PantryItemDto> {
  return apiFetch<PantryItemDto>(
    `${BASE}/households/${householdId}/pantry/barcode`,
    { method: 'POST', body: request, token },
  )
}
