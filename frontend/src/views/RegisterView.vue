<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const router = useRouter()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const displayName = ref('')
const error = ref<string | null>(null)
const submitting = ref(false)

async function onSubmit(): Promise<void> {
  error.value = null
  submitting.value = true
  try {
    await authStore.register({
      email: email.value.trim(),
      password: password.value,
      displayName: displayName.value.trim(),
    })
    const redirect = router.currentRoute.value.query.redirect
    await router.replace(typeof redirect === 'string' ? redirect : '/')
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : 'Registrierung fehlgeschlagen'
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
      <h1 class="mt-4 text-2xl font-extrabold tracking-tight">Konto anlegen</h1>
      <p class="mt-1 text-sm text-ink-500">Starte mit EatEasy in nur 3 Feldern.</p>
    </div>

    <form class="ee-card space-y-4" @submit.prevent="onSubmit">
      <div class="space-y-1">
        <label for="reg-displayName" class="block text-sm font-medium">Anzeigename</label>
        <input id="reg-displayName" v-model="displayName" type="text" autocomplete="name"
          required maxlength="100" class="w-full" />
      </div>

      <div class="space-y-1">
        <label for="reg-email" class="block text-sm font-medium">Email</label>
        <input id="reg-email" v-model="email" type="email" autocomplete="email" required class="w-full" />
      </div>

      <div class="space-y-1">
        <label for="reg-password" class="block text-sm font-medium">Passwort</label>
        <input id="reg-password" v-model="password" type="password" autocomplete="new-password"
          required minlength="8" maxlength="100" class="w-full" />
        <p class="text-xs text-ink-500">Mindestens 8 Zeichen.</p>
      </div>

      <p v-if="error" class="rounded-2xl border border-rose-200 bg-rose-100 px-3 py-2 text-sm font-medium text-rose-700">
        {{ error }}
      </p>

      <button type="submit" :disabled="submitting" class="ee-btn-primary w-full">
        {{ submitting ? 'Lege Konto an ...' : 'Registrieren' }}
      </button>
    </form>

    <p class="text-center text-sm text-ink-500">
      Bereits ein Konto?
      <router-link :to="{ name: 'login', query: $route.query }" class="ee-link">
        Login
      </router-link>
    </p>
  </section>
</template>
