<script setup lang="ts">
import type { MealPlanEntryDto } from '@/types/mealplan'

const MIN_SERVINGS = 1
const MAX_SERVINGS = 20

interface Props {
  entry: MealPlanEntryDto | null
  /** true, solange ein Portionen-Update fuer diesen Slot laeuft. */
  saving?: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  select: []
  remove: []
  changeServings: [servings: number]
}>()

function stepServings(delta: number): void {
  if (!props.entry || props.saving) {
    return
  }
  const next = props.entry.servings + delta
  // Nur die Grenze in Klickrichtung pruefen: Werte ausserhalb 1-20 sind ueber
  // Picker/API erreichbar (dort gibt es kein Max) und muessen per Stepper
  // zurueck in den Bereich gefuehrt werden koennen.
  if (delta > 0 && next > MAX_SERVINGS) {
    return
  }
  if (delta < 0 && next < MIN_SERVINGS) {
    return
  }
  emit('changeServings', next)
}
</script>

<template>
  <!--
    Der Slot ist ein div mit einer vollflaechigen Klickflaeche (Button) darunter
    und den Bedien-Buttons (Stepper, Entfernen) darueber. Buttons duerfen nicht
    in Buttons verschachtelt werden — echte <button>-Elemente geben uns
    disabled-Semantik (kein Durchklicken auf den Picker) und Tastatur-Fokus.
  -->
  <div
    class="group relative flex h-24 w-full flex-col items-stretch justify-between rounded-xl border p-2 text-left text-xs transition-all hover:-translate-y-0.5"
    :class="
      entry
        ? 'border-peach-200 bg-peach-50 hover:border-peach-300 hover:shadow-[0_4px_12px_-4px_rgba(255,154,133,0.4)]'
        : 'border-dashed border-cream-300 bg-cream-50 text-ink-400 hover:border-peach-200 hover:bg-white hover:text-peach-600'
    "
  >
    <button
      type="button"
      class="absolute inset-0 rounded-xl"
      :aria-label="entry?.recipe ? `${entry.recipe.title} — Rezept aendern` : 'Rezept waehlen'"
      @click="emit('select')"
    ></button>

    <template v-if="entry && entry.recipe">
      <div class="pointer-events-none relative flex items-start justify-between gap-1">
        <span class="line-clamp-2 font-semibold text-peach-700">
          {{ entry.recipe.title }}
        </span>
        <button
          type="button"
          class="pointer-events-auto invisible cursor-pointer rounded-full text-ink-400 hover:text-rose-700 group-hover:visible group-focus-within:visible pointer-coarse:visible"
          aria-label="Entfernen"
          @click.stop="emit('remove')"
        >
          ✕
        </button>
      </div>
      <span class="pointer-events-none relative flex items-center gap-1 font-medium text-peach-600">
        <button
          type="button"
          class="pointer-events-auto invisible flex h-4 w-4 cursor-pointer items-center justify-center rounded-full bg-white leading-none text-ink-500 hover:bg-peach-100 hover:text-peach-700 disabled:cursor-default disabled:opacity-40 group-hover:visible group-focus-within:visible pointer-coarse:visible"
          :disabled="saving || entry.servings <= MIN_SERVINGS"
          aria-label="Weniger Portionen"
          @click.stop="stepServings(-1)"
        >
          −
        </button>
        {{ entry.servings }} Portionen
        <button
          type="button"
          class="pointer-events-auto invisible flex h-4 w-4 cursor-pointer items-center justify-center rounded-full bg-white leading-none text-ink-500 hover:bg-peach-100 hover:text-peach-700 disabled:cursor-default disabled:opacity-40 group-hover:visible group-focus-within:visible pointer-coarse:visible"
          :disabled="saving || entry.servings >= MAX_SERVINGS"
          aria-label="Mehr Portionen"
          @click.stop="stepServings(1)"
        >
          +
        </button>
      </span>
    </template>
    <template v-else-if="entry">
      <span class="pointer-events-none relative font-medium italic text-ink-500">(Rezept fehlt)</span>
      <span class="pointer-events-none relative text-ink-400">{{ entry.servings }} Portionen</span>
    </template>
    <template v-else>
      <span class="pointer-events-none relative self-center text-2xl font-light leading-none">+</span>
    </template>
  </div>
</template>
