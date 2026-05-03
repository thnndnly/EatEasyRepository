<script setup lang="ts">
import type { MealPlanEntryDto } from '@/types/mealplan'

interface Props {
  entry: MealPlanEntryDto | null
}

defineProps<Props>()
const emit = defineEmits<{ select: []; remove: [] }>()

function onRemove(event: MouseEvent): void {
  event.stopPropagation()
  emit('remove')
}
</script>

<template>
  <button
    type="button"
    class="group flex h-24 w-full flex-col items-stretch justify-between rounded border p-2 text-left text-xs transition"
    :class="
      entry
        ? 'border-emerald-200 bg-emerald-50 hover:border-emerald-400'
        : 'border-dashed border-slate-300 bg-white text-slate-400 hover:border-emerald-400 hover:text-emerald-700'
    "
    @click="emit('select')"
  >
    <template v-if="entry && entry.recipe">
      <div class="flex items-start justify-between gap-1">
        <span class="line-clamp-2 font-medium text-emerald-900">
          {{ entry.recipe.title }}
        </span>
        <span
          class="invisible cursor-pointer rounded text-slate-400 hover:text-red-700 group-hover:visible"
          role="button"
          aria-label="Entfernen"
          @click="onRemove"
        >
          ✕
        </span>
      </div>
      <span class="text-emerald-700">{{ entry.servings }} Portionen</span>
    </template>
    <template v-else-if="entry">
      <span class="font-medium text-slate-500 italic">(Rezept fehlt)</span>
      <span class="text-slate-400">{{ entry.servings }} Portionen</span>
    </template>
    <template v-else>
      <span class="self-center text-2xl font-light leading-none">+</span>
    </template>
  </button>
</template>
