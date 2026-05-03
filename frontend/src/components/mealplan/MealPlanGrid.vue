<script setup lang="ts">
import MealSlot from './MealSlot.vue'
import {
  DAYS_OF_WEEK,
  DAY_LABELS,
  MEAL_TYPES,
  MEAL_TYPE_LABELS,
  type DayOfWeek,
  type MealPlanEntryDto,
  type MealType,
} from '@/types/mealplan'

interface Props {
  entryAt: (day: DayOfWeek, mealType: MealType) => MealPlanEntryDto | null
}

defineProps<Props>()
const emit = defineEmits<{
  select: [day: DayOfWeek, mealType: MealType, entry: MealPlanEntryDto | null]
  remove: [day: DayOfWeek, mealType: MealType]
}>()
</script>

<template>
  <div class="overflow-auto rounded-lg border border-slate-200 bg-white">
    <table class="w-full table-fixed border-collapse text-sm">
      <thead>
        <tr class="bg-slate-50 text-xs font-medium uppercase tracking-wide text-slate-500">
          <th class="border-b border-slate-200 px-2 py-3 text-left">Mahlzeit</th>
          <th
            v-for="day in DAYS_OF_WEEK"
            :key="day"
            class="border-b border-slate-200 px-2 py-3 text-center"
          >
            {{ DAY_LABELS[day] }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="mealType in MEAL_TYPES" :key="mealType">
          <th class="w-24 border-b border-slate-100 px-2 py-2 text-left text-xs font-medium text-slate-600">
            {{ MEAL_TYPE_LABELS[mealType] }}
          </th>
          <td
            v-for="day in DAYS_OF_WEEK"
            :key="day"
            class="border-b border-slate-100 p-2 align-top"
          >
            <MealSlot
              :entry="entryAt(day, mealType)"
              @select="emit('select', day, mealType, entryAt(day, mealType))"
              @remove="emit('remove', day, mealType)"
            />
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
