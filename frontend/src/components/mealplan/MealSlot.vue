<script setup lang="ts">
import type { MealPlanEntryDto } from '@/types/mealplan'

const MIN_SERVINGS = 1
const MAX_SERVINGS = 20

interface Props {
  entry: MealPlanEntryDto | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  select: []
  remove: []
  changeServings: [servings: number]
}>()

function onRemove(event: MouseEvent): void {
  event.stopPropagation()
  emit('remove')
}

function stepServings(event: MouseEvent, delta: number): void {
  event.stopPropagation()
  if (!props.entry) {
    return
  }
  const next = props.entry.servings + delta
  if (next < MIN_SERVINGS || next > MAX_SERVINGS) {
    return
  }
  emit('changeServings', next)
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
      <span class="flex items-center gap-1 font-medium text-peach-600">
        <span
          class="invisible flex h-4 w-4 cursor-pointer items-center justify-center rounded-full bg-white leading-none text-ink-500 hover:bg-peach-100 hover:text-peach-700 group-hover:visible"
          :class="{ 'pointer-events-none opacity-40': entry.servings <= MIN_SERVINGS }"
          role="button"
          aria-label="Weniger Portionen"
          @click="stepServings($event, -1)"
        >
          −
        </span>
        {{ entry.servings }} Portionen
        <span
          class="invisible flex h-4 w-4 cursor-pointer items-center justify-center rounded-full bg-white leading-none text-ink-500 hover:bg-peach-100 hover:text-peach-700 group-hover:visible"
          :class="{ 'pointer-events-none opacity-40': entry.servings >= MAX_SERVINGS }"
          role="button"
          aria-label="Mehr Portionen"
          @click="stepServings($event, 1)"
        >
          +
        </span>
      </span>
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
