<script setup lang="ts">
import { onBeforeMount } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const authStore = useAuthStore()
const router = useRouter()

onBeforeMount(() => {
  void authStore.restoreSession()
})

async function onLogout(): Promise<void> {
  authStore.logout()
  await router.replace('/login')
}
</script>

<template>
  <div class="min-h-screen bg-slate-50 text-slate-900">
    <header class="border-b border-slate-200 bg-white">
      <div class="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
        <RouterLink to="/" class="text-lg font-semibold text-emerald-700">
          EatEasy EE
        </RouterLink>

        <nav class="flex items-center gap-4 text-sm">
          <template v-if="authStore.isAuthenticated">
            <span class="text-slate-600">
              {{ authStore.user?.displayName }}
            </span>
            <button
              type="button"
              class="rounded border border-slate-300 bg-white px-3 py-1 font-medium text-slate-700 hover:bg-slate-50"
              @click="onLogout"
            >
              Logout
            </button>
          </template>
          <template v-else>
            <RouterLink to="/login" class="text-slate-600 hover:text-emerald-700">Login</RouterLink>
            <RouterLink to="/register" class="text-slate-600 hover:text-emerald-700">Registrieren</RouterLink>
          </template>
        </nav>
      </div>
    </header>

    <main class="mx-auto max-w-5xl px-6 py-10">
      <RouterView />
    </main>
  </div>
</template>
