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
    await router.replace('/')
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : 'Registrierung fehlgeschlagen'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="mx-auto max-w-md space-y-6">
    <h1 class="text-2xl font-semibold">Konto anlegen</h1>

    <form class="space-y-4 rounded-lg border border-slate-200 bg-white p-6 shadow-sm" @submit.prevent="onSubmit">
      <div class="space-y-1">
        <label for="reg-displayName" class="block text-sm font-medium text-slate-700">Anzeigename</label>
        <input
          id="reg-displayName"
          v-model="displayName"
          type="text"
          autocomplete="name"
          required
          maxlength="100"
          class="w-full rounded border border-slate-300 px-3 py-2 focus:border-emerald-500 focus:outline-none"
        />
      </div>

      <div class="space-y-1">
        <label for="reg-email" class="block text-sm font-medium text-slate-700">Email</label>
        <input
          id="reg-email"
          v-model="email"
          type="email"
          autocomplete="email"
          required
          class="w-full rounded border border-slate-300 px-3 py-2 focus:border-emerald-500 focus:outline-none"
        />
      </div>

      <div class="space-y-1">
        <label for="reg-password" class="block text-sm font-medium text-slate-700">Passwort</label>
        <input
          id="reg-password"
          v-model="password"
          type="password"
          autocomplete="new-password"
          required
          minlength="8"
          maxlength="100"
          class="w-full rounded border border-slate-300 px-3 py-2 focus:border-emerald-500 focus:outline-none"
        />
        <p class="text-xs text-slate-500">Mindestens 8 Zeichen.</p>
      </div>

      <p v-if="error" class="rounded border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700">
        {{ error }}
      </p>

      <button
        type="submit"
        :disabled="submitting"
        class="w-full rounded bg-emerald-600 px-4 py-2 font-medium text-white hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-60"
      >
        {{ submitting ? 'Lege Konto an ...' : 'Registrieren' }}
      </button>
    </form>

    <p class="text-center text-sm text-slate-600">
      Bereits ein Konto?
      <router-link to="/login" class="font-medium text-emerald-700 hover:underline">Login</router-link>
    </p>
  </section>
</template>
