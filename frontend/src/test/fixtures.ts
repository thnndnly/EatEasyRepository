import type { UserDto } from '@/types/auth'

/**
 * Gemeinsame Test-Fixtures fuer MSW (Vitest) und Playwright. Bewusst hier
 * statt in mocks/ abgelegt, damit beide Test-Layer ohne Umweg importieren
 * koennen (Playwright via relativem Pfad, Vitest via @-Alias).
 */
export const TEST_USER: UserDto = {
  id: '11111111-1111-1111-1111-111111111111',
  email: 'test@eateasy.local',
  displayName: 'Test User',
  createdAt: '2026-01-01T00:00:00Z',
}

export const TEST_TOKEN = 'test.fake.jwt.token'
