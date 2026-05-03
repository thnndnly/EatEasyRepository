<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { useRecipeStore } from '@/stores/recipeStore'
import { DIET_TAG_LABELS, type DietTag } from '@/types/dietTags'
import { UNIT_ABBREV } from '@/types/units'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const recipeStore = useRecipeStore()

const recipeId = computed<string>(() => String(route.params.id))
const error = ref<string | null>(null)

const recipe = computed(() => recipeStore.current)
const isOwner = computed(() =>
  Boolean(recipe.value && authStore.user && recipe.value.ownerId === authStore.user.id),
)

async function load(): Promise<void> {
  error.value = null
  try {
    await recipeStore.fetchById(recipeId.value)
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : 'Laden fehlgeschlagen'
  }
}

async function onDelete(): Promise<void> {
  if (!recipe.value || !confirm('Rezept wirklich loeschen?')) {
    return
  }
  try {
    await recipeStore.remove(recipe.value.id)
    await router.replace({ name: 'recipes' })
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : 'Loeschen fehlgeschlagen'
  }
}

onMounted(load)
watch(recipeId, load)
</script>

<template>
  <section class="space-y-6">
    <button
      type="button"
      class="text-sm font-medium text-emerald-700 hover:underline"
      @click="router.push({ name: 'recipes' })"
    >
      &larr; Zurueck zur Liste
    </button>

    <p v-if="error" class="rounded border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700">
      {{ error }}
    </p>

    <template v-if="recipe">
      <header class="flex items-start justify-between gap-4">
        <div>
          <h1 class="text-2xl font-semibold">{{ recipe.title }}</h1>
          <p v-if="recipe.description" class="mt-2 text-slate-600">{{ recipe.description }}</p>
          <ul v-if="recipe.dietTags.length > 0" class="mt-3 flex flex-wrap gap-1">
            <li
              v-for="tag in recipe.dietTags"
              :key="tag"
              class="rounded bg-emerald-50 px-2 py-0.5 text-xs font-medium text-emerald-800"
            >
              {{ DIET_TAG_LABELS[tag as DietTag] }}
            </li>
          </ul>
        </div>
        <div v-if="isOwner" class="flex gap-2">
          <button
            type="button"
            class="rounded border border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
            @click="router.push({ name: 'recipe-edit', params: { id: recipe.id } })"
          >
            Bearbeiten
          </button>
          <button
            type="button"
            class="rounded border border-red-300 bg-white px-3 py-1.5 text-sm font-medium text-red-700 hover:bg-red-50"
            @click="onDelete"
          >
            Loeschen
          </button>
        </div>
      </header>

      <dl class="flex flex-wrap gap-x-6 gap-y-1 text-sm text-slate-600">
        <div class="flex items-center gap-1">
          <dt>Portionen:</dt>
          <dd class="font-medium text-slate-800">{{ recipe.servings }}</dd>
        </div>
        <div v-if="recipe.prepMinutes !== null" class="flex items-center gap-1">
          <dt>Zubereitung:</dt>
          <dd class="font-medium text-slate-800">{{ recipe.prepMinutes }} min</dd>
        </div>
        <div v-if="recipe.householdId" class="flex items-center gap-1">
          <dt>Sichtbarkeit:</dt>
          <dd class="font-medium text-slate-800">Haushalt</dd>
        </div>
      </dl>

      <section class="space-y-3 rounded-lg border border-slate-200 bg-white p-5">
        <h2 class="text-base font-semibold text-slate-800">Zutaten</h2>
        <ul class="divide-y divide-slate-200">
          <li
            v-for="ingredient in recipe.ingredients"
            :key="ingredient.id"
            class="flex items-baseline justify-between gap-2 py-2 text-sm"
          >
            <span class="font-medium text-slate-800">{{ ingredient.ingredientName }}</span>
            <span class="text-slate-600">
              {{ ingredient.amount }} {{ UNIT_ABBREV[ingredient.unit] }}
              <span v-if="ingredient.note" class="ml-2 italic text-slate-500">
                ({{ ingredient.note }})
              </span>
            </span>
          </li>
        </ul>
      </section>

      <section class="space-y-2 rounded-lg border border-slate-200 bg-white p-5">
        <h2 class="text-base font-semibold text-slate-800">Zubereitung</h2>
        <p class="whitespace-pre-line text-sm text-slate-700">{{ recipe.instructions }}</p>
      </section>
    </template>

    <p v-else-if="!error" class="text-slate-500">Lade ...</p>
  </section>
</template>
