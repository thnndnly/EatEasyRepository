import type { ApiError } from './apiClient'
import type { ReceiptScanResponse } from '@/types/receipt'

/**
 * Multipart-Upload — bewusst nicht über apiFetch, das JSON-Bodies
 * serialisiert und den Content-Type fest setzt. Die Fehlerbehandlung
 * spiegelt apiClient (ApiError mit status + body).
 */
export async function scanReceipt(
  token: string,
  householdId: string,
  file: File,
): Promise<ReceiptScanResponse> {
  const form = new FormData()
  form.append('file', file, file.name)

  const response = await fetch(`/api/v1/households/${householdId}/receipts/scan`, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: form,
  })

  const contentType = response.headers.get('content-type') ?? ''
  const payload: unknown = contentType.includes('application/json')
    ? await response.json()
    : await response.text()

  if (!response.ok) {
    const message =
      payload && typeof payload === 'object' && 'error' in payload
        && typeof (payload as { error?: unknown }).error === 'string'
        ? ((payload as { error: string }).error)
        : `Scan fehlgeschlagen (HTTP ${response.status})`
    const error = new Error(message) as ApiError
    error.status = response.status
    error.body = payload
    throw error
  }

  return payload as ReceiptScanResponse
}
