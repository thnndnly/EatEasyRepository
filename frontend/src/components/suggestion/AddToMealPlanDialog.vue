<script setup lang="ts">
import { ref } from 'vue'
import * as mealPlanService from '@/services/mealPlanService'
import { useAuthStore } from '@/stores/authStore'
import {
  DAY_LONG_LABELS,
  DAYS_OF_WEEK,
  MEAL_TYPE_LABELS,
  MEAL_TYPES,
  type DayOfWeek,
  type MealType,
} from '@/types/mealplan'

const props = defineProps<{
  householdId: string
  recipeId: string
  recipeTitle: string
  defaultServings: number
}>()

const emit = defineEmits<{
  saved: []
  close: []
}>()

const authStore = useAuthStore()

const day = ref<DayOfWeek>('MONDAY')
const mealType = ref<MealType>('LUNCH')
const servings = ref<number>(props.defaultServings)
const saving = ref(false)
const error = ref<string | null>(null)

async function onSave(): Promise<void> {
  if (!authStore.token) {
    return
  }
  saving.value = true
  error.value = null
  try {
    const plan = await mealPlanService.getMealPlan(authStore.token, props.householdId)
    await mealPlanService.setEntry(authStore.token, plan.id, {
      dayOfWeek: day.value,
      mealType: mealType.value,
      recipeId: props.recipeId,
      servings: servings.value,
    })
    emit('saved')
  } catch (err: unknown) {
    error.value =
      err instanceof Error ? err.message : 'Speichern fehlgeschlagen'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div
    class="fixed inset-0 z-30 flex items-center justify-center bg-slate-900/40 px-4"
    @click.self="emit('close')"
  >
    <div class="w-full max-w-sm rounded-lg bg-white shadow-xl">
      <header class="flex items-center justify-between border-b border-slate-200 px-5 py-4">
        <div>
          <h2 class="text-base font-semibold text-slate-800">In Wochenplan uebernehmen</h2>
          <p class="text-xs text-slate-500">{{ recipeTitle }}</p>
        </div>
        <button
          type="button"
          class="rounded text-slate-400 hover:text-slate-700"
          @click="emit('close')"
        >
          ✕
        </button>
      </header>

      <form class="space-y-3 px-5 py-4" @submit.prevent="onSave">
        <p
          v-if="error"
          class="rounded border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700"
        >
          {{ error }}
        </p>

        <div>
          <label for="slot-day" class="block text-xs font-medium text-slate-600">Tag</label>
          <select
            id="slot-day"
            v-model="day"
            class="mt-1 w-full rounded border border-slate-300 px-3 py-2 focus:border-emerald-500 focus:outline-none"
          >
            <option v-for="d in DAYS_OF_WEEK" :key="d" :value="d">
              {{ DAY_LONG_LABELS[d] }}
            </option>
          </select>
        </div>

        <div>
          <label for="slot-meal" class="block text-xs font-medium text-slate-600">Mahlzeit</label>
          <select
            id="slot-meal"
            v-model="mealType"
            class="mt-1 w-full rounded border border-slate-300 px-3 py-2 focus:border-emerald-500 focus:outline-none"
          >
            <option v-for="m in MEAL_TYPES" :key="m" :value="m">
              {{ MEAL_TYPE_LABELS[m] }}
            </option>
          </select>
        </div>

        <div>
          <label for="slot-servings" class="block text-xs font-medium text-slate-600">Portionen</label>
          <input
            id="slot-servings"
            v-model.number="servings"
            type="number"
            min="1"
            required
            class="mt-1 w-full rounded border border-slate-300 px-3 py-2 focus:border-emerald-500 focus:outline-none"
          />
        </div>

        <button
          type="submit"
          :disabled="saving"
          class="w-full rounded bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {{ saving ? 'Speichere ...' : 'Slot setzen' }}
        </button>
      </form>
    </div>
  </div>
</template>
