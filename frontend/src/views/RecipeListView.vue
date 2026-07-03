<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useRecipeStore } from '@/stores/recipeStore'
import { useHouseholdStore } from '@/stores/householdStore'
import { useToastStore } from '@/stores/toastStore'
import RecipeCard from '@/components/recipe/RecipeCard.vue'
import DietTagSelector from '@/components/common/DietTagSelector.vue'
import ExternalRecipeSearch from '@/components/recipe/ExternalRecipeSearch.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { DietTag } from '@/types/dietTags'
import type { RecipeDto } from '@/types/recipe'

const router = useRouter()
const recipeStore = useRecipeStore()
const householdStore = useHouseholdStore()
const toastStore = useToastStore()

const query = ref('')
const tags = ref<DietTag[]>([])
const householdFilter = ref<string>('')
const favoritesOnly = ref(false)
const importOpen = ref(false)

async function onImported(recipe: RecipeDto): Promise<void> {
  importOpen.value = false
  toastStore.success(`"${recipe.title}" importiert`)
  await router.push({ name: 'recipe-detail', params: { id: recipe.id } })
}

let debounceTimer: ReturnType<typeof setTimeout> | null = null

function applyFilter(): void {
  void recipeStore.load({
    query: query.value.trim() || undefined,
    dietTags: tags.value.length > 0 ? tags.value : undefined,
    householdId: householdFilter.value || undefined,
    favorite: favoritesOnly.value || undefined,
  })
}

async function onToggleFavorite(id: string): Promise<void> {
  const recipe = recipeStore.recipes.find((r) => r.id === id)
  await recipeStore.toggleFavorite(id)
  if (!recipeStore.error && recipe) {
    toastStore.success(
      recipe.favorite
        ? `"${recipe.title}" aus Favoriten entfernt`
        : `"${recipe.title}" zu Favoriten hinzugefuegt`,
    )
  }
  // Bei aktivem Favoriten-Filter fliegt ein entfernter Favorit aus der Liste.
  if (favoritesOnly.value) {
    applyFilter()
  }
}

function onQueryInput(): void {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  debounceTimer = setTimeout(applyFilter, 250)
}

watch(tags, applyFilter, { deep: true })
watch(householdFilter, applyFilter)
watch(favoritesOnly, applyFilter)

onMounted(async () => {
  await householdStore.load()
  query.value = recipeStore.filter.query ?? ''
  tags.value = (recipeStore.filter.dietTags ?? []) as DietTag[]
  householdFilter.value = recipeStore.filter.householdId ?? ''
  favoritesOnly.value = recipeStore.filter.favorite ?? false
  await recipeStore.load()
})

const recipes = computed(() => recipeStore.recipes)
</script>

<template>
  <section class="space-y-6">
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="text-2xl font-extrabold tracking-tight">Rezepte</h1>
        <p class="mt-1 text-sm text-ink-500">Eigene und Haushalts-Rezepte mit Filter.</p>
      </div>
      <div class="flex gap-2">
        <button type="button" class="ee-btn-secondary" @click="importOpen = true">
          📥 Aus Quelle importieren
        </button>
        <button type="button" class="ee-btn-primary" @click="router.push({ name: 'recipe-new' })">
          + Neues Rezept
        </button>
      </div>
    </div>

    <ExternalRecipeSearch
      :open="importOpen"
      @close="importOpen = false"
      @imported="onImported"
    />

    <div class="grid gap-5 lg:grid-cols-[240px_1fr]">
      <aside class="ee-card space-y-5">
        <div class="space-y-1">
          <label for="recipe-q" class="block text-sm font-medium">Suche</label>
          <input
            id="recipe-q"
            v-model="query"
            type="text"
            placeholder="Titel ..."
            class="w-full"
            @input="onQueryInput"
          />
        </div>

        <div class="space-y-2">
          <span class="block text-sm font-medium">Diaet-Tags</span>
          <DietTagSelector v-model="tags" />
        </div>

        <label class="flex cursor-pointer items-center gap-2 text-sm font-medium">
          <input
            v-model="favoritesOnly"
            type="checkbox"
            class="h-4 w-4 cursor-pointer rounded accent-peach-500"
          />
          ❤️ Nur Favoriten
        </label>

        <div v-if="householdStore.households.length > 0" class="space-y-1">
          <label for="recipe-hh" class="block text-sm font-medium">Haushalt</label>
          <select id="recipe-hh" v-model="householdFilter" class="w-full">
            <option value="">Alle</option>
            <option
              v-for="household in householdStore.households"
              :key="household.id"
              :value="household.id"
            >
              {{ household.name }}
            </option>
          </select>
        </div>
      </aside>

      <div class="space-y-4">
        <p v-if="recipeStore.error" class="rounded-2xl border border-rose-200 bg-rose-100 px-4 py-3 text-sm font-medium text-rose-700">
          {{ recipeStore.error }}
        </p>

        <p v-if="recipeStore.loading" class="text-ink-500">Lade Rezepte ...</p>

        <ul v-else-if="recipes.length > 0" class="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          <li
            v-for="recipe in recipes"
            :key="recipe.id"
            @click="router.push({ name: 'recipe-detail', params: { id: recipe.id } })"
          >
            <RecipeCard :recipe="recipe" @toggle-favorite="onToggleFavorite" />
          </li>
        </ul>

        <EmptyState v-else>
          Noch keine Rezepte. Lege eins an oder importiere aus TheMealDB.
        </EmptyState>
      </div>
    </div>
  </section>
</template>
