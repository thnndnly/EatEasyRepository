import { apiFetch } from './apiClient'
import type { AuthResponse, LoginRequest, RegisterRequest, UserDto } from '@/types/auth'

const BASE = '/api/v1/auth'

export function register(request: RegisterRequest): Promise<AuthResponse> {
  return apiFetch<AuthResponse>(`${BASE}/register`, { method: 'POST', body: request })
}

export function login(request: LoginRequest): Promise<AuthResponse> {
  return apiFetch<AuthResponse>(`${BASE}/login`, { method: 'POST', body: request })
}

export function loginWithGoogle(idToken: string): Promise<AuthResponse> {
  return apiFetch<AuthResponse>(`${BASE}/google`, { method: 'POST', body: { idToken } })
}

export function getMe(token: string): Promise<UserDto> {
  return apiFetch<UserDto>(`${BASE}/me`, { token })
}
