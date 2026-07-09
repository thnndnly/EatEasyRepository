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

  function clear(): void {
    token.value = null
    user.value = null
  }

  async function register(request: RegisterRequest): Promise<void> {
    const response = await authService.register(request)
    token.value = response.token
    user.value = response.user
  }

  async function login(request: LoginRequest): Promise<void> {
    const response = await authService.login(request)
    token.value = response.token
    user.value = response.user
  }

  async function loginWithGoogle(idToken: string): Promise<void> {
    const response = await authService.loginWithGoogle(idToken)
    token.value = response.token
    user.value = response.user
  }

  /**
   * Verifiziert ein bereits persistiertes Token per /auth/me.
   * Bei 401 oder anderem Fehler wird die Session geleert.
   * Idempotent — läuft nur einmal pro App-Lifecycle.
   */
  async function restoreSession(): Promise<void> {
    if (initialized.value) {
      return
    }
    initialized.value = true

    if (!token.value) {
      return
    }

    try {
      user.value = await authService.getMe(token.value)
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
    loginWithGoogle,
    logout: clear,
    restoreSession,
  }
})
