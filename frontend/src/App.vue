<script setup lang="ts">
import { watch } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { useHouseholdStore } from '@/stores/householdStore'
import { useRecipeStore } from '@/stores/recipeStore'
import { useMealPlanStore } from '@/stores/mealPlanStore'
import { usePantryStore } from '@/stores/pantryStore'
import { useShoppingListStore } from '@/stores/shoppingListStore'
import HouseholdSwitcher from '@/components/household/HouseholdSwitcher.vue'
import ToastContainer from '@/components/common/ToastContainer.vue'
import AppLogo from '@/components/common/AppLogo.vue'

const authStore = useAuthStore()
const householdStore = useHouseholdStore()
const recipeStore = useRecipeStore()
const mealPlanStore = useMealPlanStore()
const pantryStore = usePantryStore()
const shoppingListStore = useShoppingListStore()
const router = useRouter()

const isDev = import.meta.env.DEV

const navLinks = [
  { name: 'home', label: 'Dashboard' },
  { name: 'households', label: 'Haushalte' },
  { name: 'recipes', label: 'Rezepte' },
  { name: 'mealplan', label: 'Wochenplan' },
  { name: 'pantry', label: 'Vorrat' },
  { name: 'shoppinglist', label: 'Einkaufsliste' },
] as const

// `authStore.restoreSession()` wird zentral im Router-Guard
// (router/index.ts:beforeEach) ausgeführt — ein zusätzlicher Aufruf im
// onBeforeMount der App-Komponente würde dieselbe Initialisierung
// doppelt ansteuern.
// Bei bereits eingeloggtem User triggert der Watcher unten den Lade-Pfad
// (siehe `useAuthStore` returns isAuthenticated=true sofort nach Guard).
if (authStore.isAuthenticated) {
  void householdStore.load()
}

// Synchronisiert den Haushalts-Store mit dem Auth-Status: Login → laden,
// Logout → Cache leeren, damit der nächste User keinen veralteten Switcher
// sieht.
watch(
  () => authStore.isAuthenticated,
  (next) => {
    if (next) {
      void householdStore.load(true)
    } else {
      householdStore.reset()
      recipeStore.reset()
      mealPlanStore.reset()
      pantryStore.reset()
      shoppingListStore.reset()
    }
  },
)

async function onLogout(): Promise<void> {
  // Stores werden zentral vom Watcher (oben) zurückgesetzt, sobald
  // authStore.isAuthenticated auf false wechselt — eine doppelte
  // Cleanup-Kette hier würde dieselben Aktionen redundant auslösen.
  authStore.logout()
  await router.replace('/login')
}
</script>

<template>
  <div class="min-h-screen text-ink-900">
    <header class="sticky top-0 z-10 border-b border-cream-200 bg-cream-50/85 backdrop-blur-md print:hidden">
      <div class="mx-auto flex max-w-5xl items-center justify-between gap-4 px-6 py-3">
        <div class="flex items-center gap-6">
          <RouterLink
            to="/"
            class="flex items-center gap-2 text-lg font-bold tracking-tight text-ink-900"
          >
            <AppLogo :size="36" />
            EatEasy
          </RouterLink>
          <nav v-if="authStore.isAuthenticated" class="hidden items-center gap-1 text-sm md:flex">
            <RouterLink
              v-for="link in navLinks"
              :key="link.name"
              :to="{ name: link.name }"
              class="rounded-full px-3 py-1.5 font-medium text-ink-700 transition-colors hover:bg-cream-200"
              active-class="!bg-peach-100 !text-peach-700"
            >
              {{ link.label }}
            </RouterLink>
          </nav>
        </div>

        <nav class="flex items-center gap-3 text-sm">
          <template v-if="authStore.isAuthenticated">
            <HouseholdSwitcher />
            <span class="hidden text-ink-500 lg:inline">
              {{ authStore.user?.displayName }}
            </span>
            <button type="button" class="ee-btn-secondary" @click="onLogout">
              Logout
            </button>
          </template>
          <template v-else>
            <RouterLink to="/login" class="ee-link">Login</RouterLink>
            <RouterLink to="/register" class="ee-btn-primary">Registrieren</RouterLink>
          </template>
        </nav>
      </div>
    </header>

    <main class="mx-auto max-w-5xl px-6 py-10 print:max-w-none print:p-0">
      <RouterView />
    </main>

    <ToastContainer />

    <footer
      v-if="isDev"
      class="border-t border-dashed border-butter-300 bg-butter-100/80 px-6 py-2 text-center text-xs text-butter-700 print:hidden"
    >
      Dev-Modus &mdash;
      <a
        href="http://localhost:1080"
        target="_blank"
        rel="noopener noreferrer"
        class="font-semibold underline hover:text-ink-900"
      >
        Maildev-Postfach öffnen (localhost:1080)
      </a>
      &middot; Einladungsmails landen dort, nicht im echten Postfach.
    </footer>
  </div>
</template>
