/**
 * Schlanker fetch-Wrapper, der Bearer-Tokens injiziert und Backend-Fehler
 * (Status >= 400) auf eine konsistente {@link ApiError} abbildet.
 */

export interface ApiError extends Error {
  status: number
  body?: unknown
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
  token?: string | null
  headers?: Record<string, string>
}

export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, token, headers = {} } = options

  const finalHeaders: Record<string, string> = {
    Accept: 'application/json',
    ...headers,
  }
  if (body !== undefined) {
    finalHeaders['Content-Type'] = 'application/json'
  }
  if (token) {
    finalHeaders['Authorization'] = `Bearer ${token}`
  }

  const response = await fetch(path, {
    method,
    headers: finalHeaders,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (response.status === 204) {
    return undefined as T
  }

  const contentType = response.headers.get('content-type') ?? ''
  const payload: unknown = contentType.includes('application/json')
    ? await response.json()
    : await response.text()

  if (!response.ok) {
    const message = extractErrorMessage(payload, response.status)
    const error = new Error(message) as ApiError
    error.status = response.status
    error.body = payload
    throw error
  }

  return payload as T
}

function extractErrorMessage(payload: unknown, status: number): string {
  if (payload && typeof payload === 'object' && 'error' in payload) {
    const message = (payload as { error?: unknown }).error
    if (typeof message === 'string' && message.length > 0) {
      return message
    }
  }
  return `Request failed (HTTP ${status})`
}
