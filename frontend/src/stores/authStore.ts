import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authService from '@/services/authService'
import type { LoginRequest, RegisterRequest, UserDto } from '@/types/auth'

const STORAGE_TOKEN = 'eateasy.auth.token'
const STORAGE_USER = 'eateasy.auth.user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const user = ref<UserDto | null>(null)
  const initialized = ref(false)

  const isAuthenticated = computed<boolean>(() => Boolean(token.value && user.value))

  function persist(nextToken: string, nextUser: UserDto): void {
    token.value = nextToken
    user.value = nextUser
    localStorage.setItem(STORAGE_TOKEN, nextToken)
    localStorage.setItem(STORAGE_USER, JSON.stringify(nextUser))
  }

  function clear(): void {
    token.value = null
    user.value = null
    localStorage.removeItem(STORAGE_TOKEN)
    localStorage.removeItem(STORAGE_USER)
  }

  async function register(request: RegisterRequest): Promise<void> {
    const response = await authService.register(request)
    persist(response.token, response.user)
  }

  async function login(request: LoginRequest): Promise<void> {
    const response = await authService.login(request)
    persist(response.token, response.user)
  }

  function logout(): void {
    clear()
  }

  /**
   * Liest persistiertes Token aus localStorage und verifiziert es per /auth/me.
   * Bei 401 oder anderem Fehler wird die Session geleert.
   * Idempotent — laeuft nur einmal pro App-Lifecycle.
   */
  async function restoreSession(): Promise<void> {
    if (initialized.value) {
      return
    }
    initialized.value = true

    const storedToken = localStorage.getItem(STORAGE_TOKEN)
    if (!storedToken) {
      return
    }

    try {
      const me = await authService.getMe(storedToken)
      token.value = storedToken
      user.value = me
      localStorage.setItem(STORAGE_USER, JSON.stringify(me))
    } catch {
      clear()
    }
  }

  return {
    token,
    user,
    isAuthenticated,
    register,
    login,
    logout,
    restoreSession,
  }
})
