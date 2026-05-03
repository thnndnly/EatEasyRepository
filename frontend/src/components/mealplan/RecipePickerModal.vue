<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { listRecipes } from '@/services/recipeService'
import { useAuthStore } from '@/stores/authStore'
import DietTagSelector from '@/components/common/DietTagSelector.vue'
import { DIET_TAG_LABELS, type DietTag } from '@/types/dietTags'
import type { RecipeDto } from '@/types/recipe'

interface Props {
  open: boolean
  defaultDietTags: DietTag[]
  /** Initial-Servings (z. B. aus Haushaltsgroesse oder vorhandenem Slot). */
  initialServings: number
}

const props = defineProps<Props>()
const emit = defineEmits<{
  close: []
  select: [recipe: RecipeDto, servings: number]
}>()

const authStore = useAuthStore()

const query = ref('')
const tags = ref<DietTag[]>([])
const recipes = ref<RecipeDto[]>([])
const loading = ref(false)
const servings = ref(props.initialServings)
const error = ref<string | null>(null)

let debounceTimer: ReturnType<typeof setTimeout> | null = null

watch(
  () => props.open,
  (isOpen) => {
    if (!isOpen) {
      return
    }
    // Beim Oeffnen: Default-Diaeten des Haushalts vorschlagen, Servings zuruecksetzen.
    tags.value = [...props.defaultDietTags]
    servings.value = props.initialServings
    query.value = ''
    void runSearch()
  },
)

async function runSearch(): Promise<void> {
  if (!authStore.token) {
    return
  }
  loading.value = true
  error.value = null
  try {
    recipes.value = await listRecipes(authStore.token, {
      query: query.value.trim() || undefined,
      dietTags: tags.value.length > 0 ? tags.value : undefined,
    })
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : 'Laden fehlgeschlagen'
    recipes.value = []
  } finally {
    loading.value = false
  }
}

function onQueryInput(): void {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  debounceTimer = setTimeout(runSearch, 250)
}

watch(tags, runSearch, { deep: true })

const sortedRecipes = computed(() => recipes.value)

function selectRecipe(recipe: RecipeDto): void {
  if (servings.value < 1) {
    servings.value = 1
  }
  emit('select', recipe, servings.value)
}
</script>

<template>
  <div
    v-if="open"
    class="fixed inset-0 z-30 flex items-center justify-center bg-slate-900/40 px-4"
    @click.self="emit('close')"
  >
    <div class="flex max-h-[85vh] w-full max-w-2xl flex-col rounded-lg bg-white shadow-xl">
      <header class="flex items-center justify-between border-b border-slate-200 px-5 py-4">
        <h2 class="text-base font-semibold text-slate-800">Rezept auswaehlen</h2>
        <button
          type="button"
          class="rounded text-slate-400 hover:text-slate-700"
          @click="emit('close')"
        >
          ✕
        </button>
      </header>

      <div class="space-y-3 border-b border-slate-200 px-5 py-4">
        <div class="grid grid-cols-[1fr_auto] gap-3">
          <input
            v-model="query"
            type="text"
            placeholder="Titel suchen ..."
            class="rounded border border-slate-300 px-3 py-2 focus:border-emerald-500 focus:outline-none"
            @input="onQueryInput"
          />
          <div class="flex items-center gap-2">
            <label for="picker-servings" class="text-sm text-slate-600">Portionen:</label>
            <input
              id="picker-servings"
              v-model.number="servings"
              type="number"
              min="1"
              class="w-20 rounded border border-slate-300 px-2 py-2 focus:border-emerald-500 focus:outline-none"
            />
          </div>
        </div>
        <DietTagSelector v-model="tags" />
      </div>

      <div class="flex-1 overflow-auto px-5 py-4">
        <p v-if="error" class="rounded border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700">
          {{ error }}
        </p>

        <p v-else-if="loading" class="text-sm text-slate-500">Suche ...</p>

        <ul v-else-if="sortedRecipes.length > 0" class="space-y-2">
          <li
            v-for="recipe in sortedRecipes"
            :key="recipe.id"
            class="flex items-center justify-between gap-3 rounded border border-slate-200 px-3 py-2 hover:border-emerald-400"
          >
            <div class="min-w-0">
              <p class="truncate text-sm font-medium text-slate-800">{{ recipe.title }}</p>
              <p class="text-xs text-slate-500">
                <span>{{ recipe.servings }} Portionen</span>
                <span v-if="recipe.prepMinutes !== null"> · {{ recipe.prepMinutes }} min</span>
                <span v-if="recipe.dietTags.length > 0">
                  ·
                  {{ recipe.dietTags.map((t) => DIET_TAG_LABELS[t as DietTag]).join(', ') }}
                </span>
              </p>
            </div>
            <button
              type="button"
              class="rounded bg-emerald-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-emerald-700"
              @click="selectRecipe(recipe)"
            >
              Auswaehlen
            </button>
          </li>
        </ul>

        <p v-else class="text-sm text-slate-500">Keine Treffer. Anderen Filter probieren.</p>
      </div>
    </div>
  </div>
</template>
