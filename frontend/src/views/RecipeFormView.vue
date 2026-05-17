<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useRecipeStore } from '@/stores/recipeStore'
import { useHouseholdStore } from '@/stores/householdStore'
import DietTagSelector from '@/components/common/DietTagSelector.vue'
import RecipeIngredientRow, {
  type RecipeIngredientFormRow,
} from '@/components/recipe/RecipeIngredientRow.vue'
import type { DietTag } from '@/types/dietTags'
import type { RecipeCreateRequest, RecipeIngredientRequest } from '@/types/recipe'
import type { Unit } from '@/types/units'
import ErrorMessage from '@/components/common/ErrorMessage.vue'

const route = useRoute()
const router = useRouter()
const recipeStore = useRecipeStore()
const householdStore = useHouseholdStore()

const editId = computed<string | null>(() => {
  const raw = route.params.id
  return typeof raw === 'string' && raw ? raw : null
})
const isEdit = computed(() => editId.value !== null)

const title = ref('')
const description = ref('')
const instructions = ref('')
const servings = ref<number>(2)
const prepMinutes = ref<number | null>(null)
const dietTags = ref<DietTag[]>([])
const householdId = ref<string>('')
const ingredients = ref<RecipeIngredientFormRow[]>([emptyRow()])

const submitting = ref(false)
const error = ref<string | null>(null)

function newRowId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  // Fallback fuer aeltere Umgebungen (Vitest-jsdom hat randomUUID seit Node 19).
  return `row-${Date.now()}-${Math.random().toString(36).slice(2)}`
}

function emptyRow(): RecipeIngredientFormRow {
  return {
    id: newRowId(),
    ingredientId: null,
    ingredientName: '',
    amount: 0,
    unit: 'GRAM' as Unit,
    note: '',
  }
}

function addRow(): void {
  ingredients.value = [...ingredients.value, emptyRow()]
}

function removeRow(index: number): void {
  ingredients.value = ingredients.value.filter((_, i) => i !== index)
}

function patchRow(index: number, value: RecipeIngredientFormRow): void {
  ingredients.value = ingredients.value.map((row, i) => (i === index ? value : row))
}

function buildRequest(): RecipeCreateRequest {
  const cleanedIngredients: RecipeIngredientRequest[] = ingredients.value
    .filter((row) => row.ingredientName.trim() !== '' || row.ingredientId)
    .map((row) => ({
      ingredientId: row.ingredientId,
      ingredientName: row.ingredientId ? null : row.ingredientName.trim(),
      amount: row.amount,
      unit: row.unit,
      note: row.note ? row.note : null,
    }))

  return {
    title: title.value.trim(),
    description: description.value.trim() || null,
    instructions: instructions.value.trim(),
    servings: servings.value,
    prepMinutes: prepMinutes.value,
    dietTags: dietTags.value,
    householdId: householdId.value || null,
    ingredients: cleanedIngredients,
  }
}

function validate(request: RecipeCreateRequest): string | null {
  if (!request.title) {
    return 'Titel darf nicht leer sein'
  }
  if (!request.instructions) {
    return 'Zubereitung darf nicht leer sein'
  }
  if (request.servings < 1) {
    return 'Portionen muss mindestens 1 sein'
  }
  if (request.ingredients.length === 0) {
    return 'Mindestens eine Zutat angeben'
  }
  for (const ing of request.ingredients) {
    if (!ing.amount || ing.amount <= 0) {
      return 'Jede Zutat braucht eine positive Menge'
    }
    if (!ing.ingredientId && (!ing.ingredientName || !ing.ingredientName.trim())) {
      return 'Jede Zutat braucht einen Namen'
    }
  }
  return null
}

async function onSubmit(): Promise<void> {
  const request = buildRequest()
  const validationError = validate(request)
  if (validationError) {
    error.value = validationError
    return
  }
  error.value = null
  submitting.value = true
  try {
    if (isEdit.value && editId.value) {
      await recipeStore.update(editId.value, request)
      await router.push({ name: 'recipe-detail', params: { id: editId.value } })
    } else {
      const created = await recipeStore.create(request)
      await router.push({ name: 'recipe-detail', params: { id: created.id } })
    }
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : 'Speichern fehlgeschlagen'
  } finally {
    submitting.value = false
  }
}

async function loadForEdit(): Promise<void> {
  if (!isEdit.value || !editId.value) {
    return
  }
  try {
    const recipe = await recipeStore.fetchById(editId.value)
    title.value = recipe.title
    description.value = recipe.description ?? ''
    instructions.value = recipe.instructions
    servings.value = recipe.servings
    prepMinutes.value = recipe.prepMinutes
    dietTags.value = [...recipe.dietTags]
    householdId.value = recipe.householdId ?? ''
    ingredients.value = recipe.ingredients.map((ing) => ({
      id: newRowId(),
      ingredientId: ing.ingredientId,
      ingredientName: ing.ingredientName,
      amount: Number(ing.amount),
      unit: ing.unit,
      note: ing.note ?? '',
    }))
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : 'Laden fehlgeschlagen'
  }
}

onMounted(async () => {
  await householdStore.load()
  await loadForEdit()
})

watch(editId, loadForEdit)
</script>

<template>
  <section class="space-y-6">
    <button
      type="button"
      class="text-sm font-medium text-peach-600 hover:underline"
      @click="router.back()"
    >
      &larr; Zurueck
    </button>

    <h1 class="text-2xl font-semibold">
      {{ isEdit ? 'Rezept bearbeiten' : 'Neues Rezept' }}
    </h1>

    <form
      class="space-y-6 rounded-lg border border-cream-200 bg-white p-6 shadow-sm"
      @submit.prevent="onSubmit"
    >
      <div class="space-y-1">
        <label for="recipe-title" class="block text-sm font-medium text-ink-700">Titel</label>
        <input
          id="recipe-title"
          v-model="title"
          type="text"
          required
          maxlength="200"
          class="ee-input w-full"
        />
      </div>

      <div class="space-y-1">
        <label for="recipe-desc" class="block text-sm font-medium text-ink-700">
          Beschreibung
        </label>
        <textarea
          id="recipe-desc"
          v-model="description"
          rows="2"
          class="ee-input w-full"
        />
      </div>

      <div class="grid grid-cols-2 gap-4">
        <div class="space-y-1">
          <label for="recipe-servings" class="block text-sm font-medium text-ink-700">
            Portionen
          </label>
          <input
            id="recipe-servings"
            v-model.number="servings"
            type="number"
            min="1"
            required
            class="ee-input w-full"
          />
        </div>
        <div class="space-y-1">
          <label for="recipe-prep" class="block text-sm font-medium text-ink-700">
            Zubereitungszeit (min)
          </label>
          <input
            id="recipe-prep"
            v-model.number="prepMinutes"
            type="number"
            min="0"
            class="ee-input w-full"
          />
        </div>
      </div>

      <div class="space-y-2">
        <span class="block text-sm font-medium text-ink-700">Diaet-Tags</span>
        <DietTagSelector v-model="dietTags" />
      </div>

      <div v-if="householdStore.households.length > 0" class="space-y-1">
        <label for="recipe-hh" class="block text-sm font-medium text-ink-700">
          Haushalt (optional, sonst privat)
        </label>
        <select
          id="recipe-hh"
          v-model="householdId"
          class="ee-input w-full"
        >
          <option value="">Privat (nur ich)</option>
          <option
            v-for="household in householdStore.households"
            :key="household.id"
            :value="household.id"
          >
            {{ household.name }}
          </option>
        </select>
      </div>

      <div class="space-y-3">
        <div class="flex items-center justify-between">
          <h2 class="text-base font-semibold text-ink-900">Zutaten</h2>
          <button
            type="button"
            class="ee-btn-secondary"
            @click="addRow"
          >
            + Zutat
          </button>
        </div>
        <div class="space-y-2">
          <RecipeIngredientRow
            v-for="(row, index) in ingredients"
            :key="row.id"
            :model-value="row"
            :removable="ingredients.length > 1"
            @update:model-value="patchRow(index, $event)"
            @remove="removeRow(index)"
          />
        </div>
      </div>

      <div class="space-y-1">
        <label for="recipe-instructions" class="block text-sm font-medium text-ink-700">
          Zubereitung
        </label>
        <textarea
          id="recipe-instructions"
          v-model="instructions"
          rows="6"
          required
          class="ee-input w-full"
        />
      </div>

      <ErrorMessage :message="error ?? ''" />

      <button
        type="submit"
        :disabled="submitting"
        class="ee-btn-primary"
      >
        {{ submitting ? 'Speichere ...' : isEdit ? 'Aenderungen speichern' : 'Rezept anlegen' }}
      </button>
    </form>
  </section>
</template>
