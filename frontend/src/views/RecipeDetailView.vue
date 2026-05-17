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
      class="text-sm font-medium text-peach-600 hover:underline"
      @click="router.push({ name: 'recipes' })"
    >
      &larr; Zurueck zur Liste
    </button>

    <p v-if="error" class="rounded border border-rose-200 bg-rose-100 px-3 py-2 text-sm text-rose-700">
      {{ error }}
    </p>

    <template v-if="recipe">
      <header class="flex items-start justify-between gap-4">
        <div>
          <h1 class="text-2xl font-semibold">{{ recipe.title }}</h1>
          <p v-if="recipe.description" class="mt-2 text-ink-500">{{ recipe.description }}</p>
          <ul v-if="recipe.dietTags.length > 0" class="mt-3 flex flex-wrap gap-1">
            <li
              v-for="tag in recipe.dietTags"
              :key="tag"
              class="rounded bg-peach-50 px-2 py-0.5 text-xs font-medium text-peach-700"
            >
              {{ DIET_TAG_LABELS[tag as DietTag] }}
            </li>
          </ul>
        </div>
        <div v-if="isOwner" class="flex gap-2">
          <button
            type="button"
            class="rounded border border-cream-300 bg-white px-3 py-1.5 text-sm font-medium text-ink-700 hover:bg-cream-50"
            @click="router.push({ name: 'recipe-edit', params: { id: recipe.id } })"
          >
            Bearbeiten
          </button>
          <button
            type="button"
            class="rounded border border-rose-200 bg-white px-3 py-1.5 text-sm font-medium text-rose-700 hover:bg-rose-100"
            @click="onDelete"
          >
            Loeschen
          </button>
        </div>
      </header>

      <dl class="flex flex-wrap gap-x-6 gap-y-1 text-sm text-ink-500">
        <div class="flex items-center gap-1">
          <dt>Portionen:</dt>
          <dd class="font-medium text-ink-900">{{ recipe.servings }}</dd>
        </div>
        <div v-if="recipe.prepMinutes !== null" class="flex items-center gap-1">
          <dt>Zubereitung:</dt>
          <dd class="font-medium text-ink-900">{{ recipe.prepMinutes }} min</dd>
        </div>
        <div v-if="recipe.householdId" class="flex items-center gap-1">
          <dt>Sichtbarkeit:</dt>
          <dd class="font-medium text-ink-900">Haushalt</dd>
        </div>
        <div v-if="recipe.sourceUrl" class="flex items-center gap-1">
          <dt>Quelle:</dt>
          <dd class="font-medium">
            <a
              :href="recipe.sourceUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="text-peach-600 hover:underline"
            >
              {{ recipe.externalSource ?? 'Externer Link' }} ↗
            </a>
          </dd>
        </div>
      </dl>

      <section class="space-y-3 rounded-lg border border-cream-200 bg-white p-5">
        <h2 class="text-base font-semibold text-ink-900">Zutaten</h2>
        <ul class="divide-y divide-cream-200">
          <li
            v-for="ingredient in recipe.ingredients"
            :key="ingredient.id"
            class="flex items-baseline justify-between gap-2 py-2 text-sm"
          >
            <span class="font-medium text-ink-900">{{ ingredient.ingredientName }}</span>
            <span class="text-ink-500">
              {{ ingredient.amount }} {{ UNIT_ABBREV[ingredient.unit] }}
              <span v-if="ingredient.note" class="ml-2 italic text-ink-500">
                ({{ ingredient.note }})
              </span>
            </span>
          </li>
        </ul>
      </section>

      <section class="space-y-2 rounded-lg border border-cream-200 bg-white p-5">
        <h2 class="text-base font-semibold text-ink-900">Zubereitung</h2>
        <p class="whitespace-pre-line text-sm text-ink-700">{{ recipe.instructions }}</p>
      </section>
    </template>

    <p v-else-if="!error" class="text-ink-500">Lade ...</p>
  </section>
</template>
