<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useHouseholdStore } from '@/stores/householdStore'
import { useMealPlanStore } from '@/stores/mealPlanStore'
import MealPlanGrid from '@/components/mealplan/MealPlanGrid.vue'
import RecipePickerModal from '@/components/mealplan/RecipePickerModal.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { DayOfWeek, MealPlanEntryDto, MealType } from '@/types/mealplan'
import type { RecipeDto } from '@/types/recipe'
import type { DietTag } from '@/types/dietTags'

const router = useRouter()
const householdStore = useHouseholdStore()
const mealPlanStore = useMealPlanStore()

const pickerOpen = ref(false)
const pickerDay = ref<DayOfWeek | null>(null)
const pickerMealType = ref<MealType | null>(null)
const pickerInitialServings = ref(2)

const selectedHousehold = computed(() => householdStore.selected)
const householdDietTags = computed<DietTag[]>(
  () => (selectedHousehold.value?.defaultDietTags ?? []) as DietTag[],
)

async function ensureLoaded(): Promise<void> {
  await householdStore.load()
  if (selectedHousehold.value) {
    await mealPlanStore.load(selectedHousehold.value.id)
  } else {
    mealPlanStore.reset()
  }
}

onMounted(ensureLoaded)
watch(() => selectedHousehold.value?.id, ensureLoaded)

function entryAt(day: DayOfWeek, mealType: MealType): MealPlanEntryDto | null {
  return mealPlanStore.entryAt(day, mealType)
}

function openPicker(day: DayOfWeek, mealType: MealType, entry: MealPlanEntryDto | null): void {
  pickerDay.value = day
  pickerMealType.value = mealType
  pickerInitialServings.value = entry?.servings ?? 2
  pickerOpen.value = true
}

async function onPickerSelect(recipe: RecipeDto, servings: number): Promise<void> {
  if (!pickerDay.value || !pickerMealType.value) {
    return
  }
  try {
    await mealPlanStore.setEntry({
      dayOfWeek: pickerDay.value,
      mealType: pickerMealType.value,
      recipeId: recipe.id,
      servings,
    })
    pickerOpen.value = false
  } catch (err: unknown) {
    mealPlanStore.error = err instanceof Error ? err.message : 'Speichern fehlgeschlagen'
  }
}

async function onRemove(day: DayOfWeek, mealType: MealType): Promise<void> {
  if (!confirm('Slot wirklich leeren?')) {
    return
  }
  await mealPlanStore.removeEntry(day, mealType)
}
</script>

<template>
  <section class="space-y-6">
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="text-2xl font-extrabold tracking-tight">📅 Wochenplan</h1>
        <p class="mt-1 text-sm text-ink-500">
          {{ selectedHousehold ? selectedHousehold.name : 'Keinen Haushalt ausgewaehlt' }}
          <span v-if="mealPlanStore.plan"> · Woche {{ mealPlanStore.weekRangeLabel }}</span>
        </p>
      </div>

      <div class="flex items-center gap-2">
        <button type="button" class="ee-btn-secondary" @click="mealPlanStore.gotoWeek(-7)">
          ‹ Vorherige
        </button>
        <button type="button" class="ee-btn-secondary" @click="mealPlanStore.gotoToday()">
          Heute
        </button>
        <button type="button" class="ee-btn-secondary" @click="mealPlanStore.gotoWeek(7)">
          Naechste ›
        </button>
      </div>
    </div>

    <EmptyState v-if="!selectedHousehold">
      Lege zuerst einen Haushalt an oder waehle in der Topbar einen aus.
      <button type="button" class="ee-link ml-2" @click="router.push({ name: 'households' })">
        Haushalte oeffnen
      </button>
    </EmptyState>

    <p
      v-if="mealPlanStore.error"
      class="rounded-2xl border border-rose-200 bg-rose-100 px-4 py-3 text-sm font-medium text-rose-700"
    >
      {{ mealPlanStore.error }}
    </p>

    <p v-if="mealPlanStore.loading" class="text-ink-500">Lade Wochenplan ...</p>

    <MealPlanGrid
      v-else-if="mealPlanStore.plan"
      :entry-at="entryAt"
      @select="(day, mealType, entry) => openPicker(day, mealType, entry)"
      @remove="(day, mealType) => onRemove(day, mealType)"
    />

    <RecipePickerModal
      :open="pickerOpen"
      :default-diet-tags="householdDietTags"
      :initial-servings="pickerInitialServings"
      @close="pickerOpen = false"
      @select="onPickerSelect"
    />
  </section>
</template>
