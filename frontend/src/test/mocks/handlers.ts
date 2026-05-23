import { http, HttpResponse } from 'msw'
import type { AuthResponse } from '@/types/auth'
import { TEST_TOKEN, TEST_USER } from '@/test/fixtures'

/**
 * MSW-Handler fuer Vitest. Spiegelt das Backend-API auf deterministische
 * Stubs, damit Stores und Services ohne laufenden Quarkus getestet werden.
 */
export const handlers = [
  http.post('/api/v1/auth/register', async ({ request }) => {
    const body = (await request.json()) as { email?: string; displayName?: string }
    const response: AuthResponse = {
      token: TEST_TOKEN,
      user: {
        ...TEST_USER,
        email: body.email ?? TEST_USER.email,
        displayName: body.displayName ?? TEST_USER.displayName,
      },
    }
    return HttpResponse.json(response, { status: 201 })
  }),

  http.post('/api/v1/auth/login', async ({ request }) => {
    const body = (await request.json()) as { email?: string }
    if (body.email === 'fail@eateasy.local') {
      return HttpResponse.json({ error: 'Ungueltige Anmeldedaten' }, { status: 401 })
    }
    return HttpResponse.json({
      token: TEST_TOKEN,
      user: { ...TEST_USER, email: body.email ?? TEST_USER.email },
    } satisfies AuthResponse)
  }),

  http.get('/api/v1/auth/me', ({ request }) => {
    if (request.headers.get('Authorization') !== `Bearer ${TEST_TOKEN}`) {
      return HttpResponse.json({ error: 'Unauthorized' }, { status: 401 })
    }
    return HttpResponse.json(TEST_USER)
  }),

  http.get('/api/v1/health', () => HttpResponse.json({ status: 'ok' })),
]
