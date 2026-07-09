<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRecipeStore } from '@/stores/recipeStore'
import DietTagSelector from '@/components/common/DietTagSelector.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import { DIET_TAG_LABELS, type DietTag } from '@/types/dietTags'
import type { RecipeDto } from '@/types/recipe'

interface Props {
  open: boolean
  defaultDietTags: DietTag[]
  /** Initial-Servings (z. B. aus Haushaltsgröße oder vorhandenem Slot). */
  initialServings: number
}

const props = defineProps<Props>()
const emit = defineEmits<{
  close: []
  select: [recipe: RecipeDto, servings: number]
}>()

const recipeStore = useRecipeStore()

const query = ref('')
const tags = ref<DietTag[]>([])
const servings = ref(props.initialServings)

let debounceTimer: ReturnType<typeof setTimeout> | null = null

watch(
  () => props.open,
  (isOpen) => {
    if (!isOpen) {
      return
    }
    // Beim Öffnen: Default-Diäten des Haushalts vorschlagen, Servings zurücksetzen.
    tags.value = [...props.defaultDietTags]
    servings.value = props.initialServings
    query.value = ''
    void runSearch()
  },
)

async function runSearch(): Promise<void> {
  try {
    await recipeStore.load({
      query: query.value.trim() || undefined,
      dietTags: tags.value.length > 0 ? tags.value : undefined,
    })
  } catch {
    // recipeStore.error ist gesetzt.
  }
}

function onQueryInput(): void {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  debounceTimer = setTimeout(runSearch, 250)
}

watch(tags, runSearch, { deep: true })

const sortedRecipes = computed(() => recipeStore.recipes)

function selectRecipe(recipe: RecipeDto): void {
  if (servings.value < 1) {
    servings.value = 1
  }
  emit('select', recipe, servings.value)
}
</script>

<template>
  <BaseModal :open="open" @close="emit('close')">
    <template #header>
      <h2 class="text-base font-semibold text-ink-900">Rezept auswählen</h2>
    </template>

    <div class="space-y-4">
      <div class="space-y-3">
        <div class="grid grid-cols-[1fr_auto] gap-3">
          <input
            v-model="query"
            type="text"
            placeholder="Titel suchen ..."
            class="ee-input"
            @input="onQueryInput"
          />
          <div class="flex items-center gap-2">
            <label for="picker-servings" class="text-sm text-ink-500">Portionen:</label>
            <input
              id="picker-servings"
              v-model.number="servings"
              type="number"
              min="1"
              class="ee-input w-20"
            />
          </div>
        </div>
        <DietTagSelector v-model="tags" />
      </div>

      <ErrorMessage :message="recipeStore.error ?? ''" />

      <p v-if="recipeStore.loading" class="text-sm text-ink-500">Suche ...</p>

      <ul v-else-if="sortedRecipes.length > 0" class="space-y-2">
        <li
          v-for="recipe in sortedRecipes"
          :key="recipe.id"
          class="flex items-center justify-between gap-3 rounded border border-cream-200 px-3 py-2 hover:border-peach-300"
        >
          <div class="min-w-0">
            <p class="truncate text-sm font-medium text-ink-900">{{ recipe.title }}</p>
            <p class="text-xs text-ink-500">
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
            class="ee-btn-primary ee-btn-sm"
            @click="selectRecipe(recipe)"
          >
            Auswählen
          </button>
        </li>
      </ul>

      <p v-else-if="!recipeStore.error" class="text-sm text-ink-500">Keine Treffer. Anderen Filter probieren.</p>
    </div>
  </BaseModal>
</template>
