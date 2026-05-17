<script setup lang="ts">
import { onBeforeMount, watch } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { useHouseholdStore } from '@/stores/householdStore'
import { useRecipeStore } from '@/stores/recipeStore'
import { useMealPlanStore } from '@/stores/mealPlanStore'
import { usePantryStore } from '@/stores/pantryStore'
import { useShoppingListStore } from '@/stores/shoppingListStore'
import HouseholdSwitcher from '@/components/household/HouseholdSwitcher.vue'
import ToastContainer from '@/components/common/ToastContainer.vue'

const authStore = useAuthStore()
const householdStore = useHouseholdStore()
const recipeStore = useRecipeStore()
const mealPlanStore = useMealPlanStore()
const pantryStore = usePantryStore()
const shoppingListStore = useShoppingListStore()
const router = useRouter()

const isDev = import.meta.env.DEV

onBeforeMount(async () => {
  await authStore.restoreSession()
  if (authStore.isAuthenticated) {
    void householdStore.load()
  }
})

// Synchronisiert den Haushalts-Store mit dem Auth-Status: Login → laden,
// Logout → Cache leeren, damit der naechste User keinen veralteten Switcher
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
  authStore.logout()
  householdStore.reset()
  recipeStore.reset()
  mealPlanStore.reset()
  pantryStore.reset()
  shoppingListStore.reset()
  await router.replace('/login')
}
</script>

<template>
  <div class="min-h-screen bg-slate-50 text-slate-900">
    <header class="border-b border-slate-200 bg-white">
      <div class="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
        <div class="flex items-center gap-6">
          <RouterLink to="/" class="text-lg font-semibold text-emerald-700">
            EatEasy EE
          </RouterLink>
          <nav v-if="authStore.isAuthenticated" class="flex items-center gap-4 text-sm">
            <RouterLink
              :to="{ name: 'home' }"
              class="text-slate-600 hover:text-emerald-700"
              active-class="text-emerald-700"
            >
              Dashboard
            </RouterLink>
            <RouterLink
              :to="{ name: 'households' }"
              class="text-slate-600 hover:text-emerald-700"
              active-class="text-emerald-700"
            >
              Haushalte
            </RouterLink>
            <RouterLink
              :to="{ name: 'recipes' }"
              class="text-slate-600 hover:text-emerald-700"
              active-class="text-emerald-700"
            >
              Rezepte
            </RouterLink>
            <RouterLink
              :to="{ name: 'mealplan' }"
              class="text-slate-600 hover:text-emerald-700"
              active-class="text-emerald-700"
            >
              Wochenplan
            </RouterLink>
            <RouterLink
              :to="{ name: 'pantry' }"
              class="text-slate-600 hover:text-emerald-700"
              active-class="text-emerald-700"
            >
              Vorrat
            </RouterLink>
            <RouterLink
              :to="{ name: 'shoppinglist' }"
              class="text-slate-600 hover:text-emerald-700"
              active-class="text-emerald-700"
            >
              Einkaufsliste
            </RouterLink>
          </nav>
        </div>

        <nav class="flex items-center gap-4 text-sm">
          <template v-if="authStore.isAuthenticated">
            <HouseholdSwitcher />
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

    <ToastContainer />

    <footer
      v-if="isDev"
      class="border-t border-dashed border-amber-300 bg-amber-50 px-6 py-2 text-center text-xs text-amber-800"
    >
      Dev-Modus &mdash;
      <a
        href="http://localhost:1080"
        target="_blank"
        rel="noopener noreferrer"
        class="font-medium underline hover:text-amber-900"
      >
        Maildev-Postfach oeffnen (localhost:1080)
      </a>
      &middot; Einladungsmails landen dort, nicht im echten Postfach.
    </footer>
  </div>
</template>
