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
    class="group flex h-24 w-full flex-col items-stretch justify-between rounded-xl border p-2 text-left text-xs transition-all hover:-translate-y-0.5"
    :class="
      entry
        ? 'border-peach-200 bg-peach-50 hover:border-peach-300 hover:shadow-[0_4px_12px_-4px_rgba(255,154,133,0.4)]'
        : 'border-dashed border-cream-300 bg-cream-50 text-ink-400 hover:border-peach-200 hover:bg-white hover:text-peach-600'
    "
    @click="emit('select')"
  >
    <template v-if="entry && entry.recipe">
      <div class="flex items-start justify-between gap-1">
        <span class="line-clamp-2 font-semibold text-peach-700">
          {{ entry.recipe.title }}
        </span>
        <span
          class="invisible cursor-pointer rounded-full text-ink-400 hover:text-rose-700 group-hover:visible"
          role="button"
          aria-label="Entfernen"
          @click="onRemove"
        >
          ✕
        </span>
      </div>
      <span class="text-peach-600 font-medium">{{ entry.servings }} Portionen</span>
    </template>
    <template v-else-if="entry">
      <span class="font-medium italic text-ink-500">(Rezept fehlt)</span>
      <span class="text-ink-400">{{ entry.servings }} Portionen</span>
    </template>
    <template v-else>
      <span class="self-center text-2xl font-light leading-none">+</span>
    </template>
  </button>
</template>
