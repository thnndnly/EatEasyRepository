import { http, HttpResponse } from 'msw'
import type { AuthResponse, UserDto } from '@/types/auth'

/**
 * MSW-Handler fuer Unit-Tests. Spiegelt das Backend-API auf
 * deterministische Stub-Antworten, damit Stores und Services ohne
 * laufenden Quarkus-Server getestet werden koennen.
 *
 * Reihenfolge: spezifischste Routen zuerst, damit dynamische Pfade
 * (z. B. /api/v1/recipes/:id) nicht von Catch-Alls verschluckt werden.
 */
const USER_FIXTURE: UserDto = {
  id: '11111111-1111-1111-1111-111111111111',
  email: 'test@eateasy.local',
  displayName: 'Test User',
  createdAt: '2026-01-01T00:00:00Z',
}

const TOKEN_FIXTURE = 'msw.fake.jwt.token'

export const handlers = [
  http.post('/api/v1/auth/register', async ({ request }) => {
    const body = (await request.json()) as { email?: string; displayName?: string }
    const response: AuthResponse = {
      token: TOKEN_FIXTURE,
      user: {
        ...USER_FIXTURE,
        email: body.email ?? USER_FIXTURE.email,
        displayName: body.displayName ?? USER_FIXTURE.displayName,
      },
    }
    return HttpResponse.json(response, { status: 201 })
  }),

  http.post('/api/v1/auth/login', async ({ request }) => {
    const body = (await request.json()) as { email?: string }
    if (body.email === 'fail@eateasy.local') {
      return HttpResponse.json({ error: 'Ungueltige Anmeldedaten' }, { status: 401 })
    }
    const response: AuthResponse = {
      token: TOKEN_FIXTURE,
      user: { ...USER_FIXTURE, email: body.email ?? USER_FIXTURE.email },
    }
    return HttpResponse.json(response)
  }),

  http.get('/api/v1/auth/me', ({ request }) => {
    const auth = request.headers.get('Authorization')
    if (auth !== `Bearer ${TOKEN_FIXTURE}`) {
      return HttpResponse.json({ error: 'Unauthorized' }, { status: 401 })
    }
    return HttpResponse.json(USER_FIXTURE)
  }),

  http.get('/api/v1/health', () => HttpResponse.json({ status: 'ok' })),
]

export const fixtures = {
  user: USER_FIXTURE,
  token: TOKEN_FIXTURE,
}
