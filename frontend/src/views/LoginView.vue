<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const router = useRouter()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const error = ref<string | null>(null)
const submitting = ref(false)

async function onSubmit(): Promise<void> {
  error.value = null
  submitting.value = true
  try {
    await authStore.login({ email: email.value.trim(), password: password.value })
    const redirect = router.currentRoute.value.query.redirect
    await router.replace(typeof redirect === 'string' ? redirect : '/')
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : 'Login fehlgeschlagen'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="mx-auto max-w-md space-y-6 py-6">
    <div class="text-center">
      <div class="mx-auto flex h-14 w-14 items-center justify-center rounded-3xl text-3xl shadow-md"
        style="background: linear-gradient(135deg, #ffb5a7 0%, #ffd47a 100%)">
        🍅
      </div>
      <h1 class="mt-4 text-2xl font-extrabold tracking-tight">Willkommen zurueck</h1>
      <p class="mt-1 text-sm text-ink-500">Logge dich in dein EatEasy-Konto ein.</p>
    </div>

    <form class="ee-card space-y-4" @submit.prevent="onSubmit">
      <div class="space-y-1">
        <label for="login-email" class="block text-sm font-medium">Email</label>
        <input
          id="login-email"
          v-model="email"
          type="email"
          autocomplete="email"
          required
          class="w-full"
        />
      </div>

      <div class="space-y-1">
        <label for="login-password" class="block text-sm font-medium">Passwort</label>
        <input
          id="login-password"
          v-model="password"
          type="password"
          autocomplete="current-password"
          required
          minlength="8"
          class="w-full"
        />
      </div>

      <p v-if="error" class="rounded-2xl border border-rose-200 bg-rose-100 px-3 py-2 text-sm font-medium text-rose-700">
        {{ error }}
      </p>

      <button type="submit" :disabled="submitting" class="ee-btn-primary ee-btn-lg w-full">
        {{ submitting ? 'Logge ein ...' : 'Einloggen' }}
      </button>
    </form>

    <p class="text-center text-sm text-ink-500">
      Noch kein Konto?
      <router-link
        :to="{ name: 'register', query: $route.query }"
        class="ee-link"
      >
        Registrieren
      </router-link>
    </p>
  </section>
</template>
