<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useRecipeStore } from '@/stores/recipeStore'
import { useHouseholdStore } from '@/stores/householdStore'
import RecipeCard from '@/components/recipe/RecipeCard.vue'
import DietTagSelector from '@/components/common/DietTagSelector.vue'
import type { DietTag } from '@/types/dietTags'

const router = useRouter()
const recipeStore = useRecipeStore()
const householdStore = useHouseholdStore()

const query = ref('')
const tags = ref<DietTag[]>([])
const householdFilter = ref<string>('')

let debounceTimer: ReturnType<typeof setTimeout> | null = null

function applyFilter(): void {
  void recipeStore.load({
    query: query.value.trim() || undefined,
    dietTags: tags.value.length > 0 ? tags.value : undefined,
    householdId: householdFilter.value || undefined,
  })
}

function onQueryInput(): void {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  debounceTimer = setTimeout(applyFilter, 250)
}

watch(tags, applyFilter, { deep: true })
watch(householdFilter, applyFilter)

onMounted(async () => {
  await householdStore.load()
  query.value = recipeStore.filter.query ?? ''
  tags.value = (recipeStore.filter.dietTags ?? []) as DietTag[]
  householdFilter.value = recipeStore.filter.householdId ?? ''
  await recipeStore.load()
})

const recipes = computed(() => recipeStore.recipes)
</script>

<template>
  <section class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-semibold">Rezepte</h1>
        <p class="mt-1 text-slate-600">Eigene und Haushalts-Rezepte mit Filter.</p>
      </div>
      <button
        type="button"
        class="rounded bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700"
        @click="router.push({ name: 'recipe-new' })"
      >
        Neues Rezept
      </button>
    </div>

    <div class="grid gap-4 lg:grid-cols-[220px_1fr]">
      <aside class="space-y-5 rounded-lg border border-slate-200 bg-white p-4">
        <div class="space-y-1">
          <label for="recipe-q" class="block text-sm font-medium text-slate-700">Suche</label>
          <input
            id="recipe-q"
            v-model="query"
            type="text"
            placeholder="Titel ..."
            class="w-full rounded border border-slate-300 px-3 py-2 focus:border-emerald-500 focus:outline-none"
            @input="onQueryInput"
          />
        </div>

        <div class="space-y-2">
          <span class="block text-sm font-medium text-slate-700">Diaet-Tags</span>
          <DietTagSelector v-model="tags" />
        </div>

        <div v-if="householdStore.households.length > 0" class="space-y-1">
          <label for="recipe-hh" class="block text-sm font-medium text-slate-700">Haushalt</label>
          <select
            id="recipe-hh"
            v-model="householdFilter"
            class="w-full rounded border border-slate-300 px-3 py-2 focus:border-emerald-500 focus:outline-none"
          >
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
        <p v-if="recipeStore.error" class="rounded border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700">
          {{ recipeStore.error }}
        </p>

        <p v-if="recipeStore.loading" class="text-slate-500">Lade Rezepte ...</p>

        <ul v-else-if="recipes.length > 0" class="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          <li
            v-for="recipe in recipes"
            :key="recipe.id"
            @click="router.push({ name: 'recipe-detail', params: { id: recipe.id } })"
          >
            <RecipeCard :recipe="recipe" />
          </li>
        </ul>

        <p v-else class="rounded border border-dashed border-slate-300 bg-white p-6 text-center text-slate-500">
          Keine Rezepte gefunden. Lege ein neues an.
        </p>
      </div>
    </div>
  </section>
</template>
