import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { StorageSerializers, useStorage } from '@vueuse/core'
import * as authService from '@/services/authService'
import type { LoginRequest, RegisterRequest, UserDto } from '@/types/auth'

const STORAGE_TOKEN = 'eateasy.auth.token'
const STORAGE_USER = 'eateasy.auth.user'

export const useAuthStore = defineStore('auth', () => {
  // Token + User werden via VueUse useStorage automatisch mit localStorage
  // synchronisiert (selbe Keys wie zuvor, damit bestehende Sessions weiterlaufen).
  const token = useStorage<string | null>(STORAGE_TOKEN, null)
  const user = useStorage<UserDto | null>(STORAGE_USER, null, undefined, {
    serializer: StorageSerializers.object,
  })
  const initialized = ref(false)

  const isAuthenticated = computed<boolean>(() => Boolean(token.value && user.value))

  function persist(nextToken: string, nextUser: UserDto): void {
    token.value = nextToken
    user.value = nextUser
  }

  function clear(): void {
    token.value = null
    user.value = null
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
   * Verifiziert ein bereits persistiertes Token per /auth/me.
   * Bei 401 oder anderem Fehler wird die Session geleert.
   * Idempotent — laeuft nur einmal pro App-Lifecycle.
   */
  async function restoreSession(): Promise<void> {
    if (initialized.value) {
      return
    }
    initialized.value = true

    const storedToken = token.value
    if (!storedToken) {
      return
    }

    try {
      const me = await authService.getMe(storedToken)
      user.value = me
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
