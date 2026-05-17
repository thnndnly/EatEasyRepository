<script setup lang="ts">
import { ref } from 'vue'
import { useMealPlanStore } from '@/stores/mealPlanStore'
import BaseModal from '@/components/common/BaseModal.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import {
  DAY_LONG_LABELS,
  DAYS_OF_WEEK,
  MEAL_TYPE_LABELS,
  MEAL_TYPES,
  type DayOfWeek,
  type MealType,
} from '@/types/mealplan'

const props = defineProps<{
  open: boolean
  householdId: string
  recipeId: string
  recipeTitle: string
  defaultServings: number
}>()

const emit = defineEmits<{
  saved: []
  close: []
}>()

const mealPlanStore = useMealPlanStore()

const day = ref<DayOfWeek>('MONDAY')
const mealType = ref<MealType>('LUNCH')
const servings = ref<number>(props.defaultServings)
const saving = ref(false)
const error = ref<string | null>(null)

async function onSave(): Promise<void> {
  saving.value = true
  error.value = null
  try {
    await mealPlanStore.setEntryForHousehold(props.householdId, {
      dayOfWeek: day.value,
      mealType: mealType.value,
      recipeId: props.recipeId,
      servings: servings.value,
    })
    emit('saved')
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : 'Speichern fehlgeschlagen'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <BaseModal :open="open" @close="emit('close')">
    <template #header>
      <h2 class="text-base font-semibold text-ink-900">In Wochenplan uebernehmen</h2>
      <p class="text-xs text-ink-500">{{ recipeTitle }}</p>
    </template>

    <form class="space-y-3" @submit.prevent="onSave">
      <ErrorMessage :message="error ?? ''" />

      <div>
        <label for="slot-day" class="block text-xs font-medium text-ink-500">Tag</label>
        <select
          id="slot-day"
          v-model="day"
          class="ee-input mt-1 w-full"
        >
          <option v-for="d in DAYS_OF_WEEK" :key="d" :value="d">
            {{ DAY_LONG_LABELS[d] }}
          </option>
        </select>
      </div>

      <div>
        <label for="slot-meal" class="block text-xs font-medium text-ink-500">Mahlzeit</label>
        <select
          id="slot-meal"
          v-model="mealType"
          class="ee-input mt-1 w-full"
        >
          <option v-for="m in MEAL_TYPES" :key="m" :value="m">
            {{ MEAL_TYPE_LABELS[m] }}
          </option>
        </select>
      </div>

      <div>
        <label for="slot-servings" class="block text-xs font-medium text-ink-500">Portionen</label>
        <input
          id="slot-servings"
          v-model.number="servings"
          type="number"
          min="1"
          required
          class="ee-input mt-1 w-full"
        />
      </div>

      <button
        type="submit"
        :disabled="saving"
        class="ee-btn-primary ee-btn-lg w-full"
      >
        {{ saving ? 'Speichere ...' : 'Slot setzen' }}
      </button>
    </form>
  </BaseModal>
</template>
