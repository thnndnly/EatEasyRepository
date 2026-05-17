<script setup lang="ts">
import { DIET_TAG_LABELS, type DietTag } from '@/types/dietTags'
import type { RecipeDto } from '@/types/recipe'

interface Props {
  recipe: RecipeDto
}

defineProps<Props>()
</script>

<template>
  <article
    class="group cursor-pointer rounded-2xl border border-cream-200 bg-white p-5 transition-all duration-200 hover:-translate-y-0.5 hover:border-peach-200 hover:shadow-[0_10px_28px_-12px_rgba(255,154,133,0.4)]"
  >
    <header class="flex items-start justify-between gap-3">
      <h3 class="text-base font-bold text-ink-900 transition-colors group-hover:text-peach-600">
        {{ recipe.title }}
      </h3>
      <span v-if="recipe.householdId" class="ee-chip-butter shrink-0">
        Haushalt
      </span>
    </header>

    <p
      v-if="recipe.description"
      class="mt-2 line-clamp-2 text-sm leading-snug text-ink-500"
    >
      {{ recipe.description }}
    </p>

    <dl class="mt-4 flex flex-wrap gap-x-4 gap-y-1 text-xs text-ink-500">
      <div class="flex items-center gap-1">
        <span>🍽️</span>
        <dd class="font-semibold text-ink-700">{{ recipe.servings }} Portionen</dd>
      </div>
      <div v-if="recipe.prepMinutes !== null" class="flex items-center gap-1">
        <span>⏱️</span>
        <dd class="font-semibold text-ink-700">{{ recipe.prepMinutes }} min</dd>
      </div>
      <div class="flex items-center gap-1">
        <span>🥕</span>
        <dd class="font-semibold text-ink-700">{{ recipe.ingredients.length }} Zutaten</dd>
      </div>
    </dl>

    <ul v-if="recipe.dietTags.length > 0" class="mt-3 flex flex-wrap gap-1">
      <li v-for="tag in recipe.dietTags" :key="tag" class="ee-chip-mint">
        {{ DIET_TAG_LABELS[tag as DietTag] }}
      </li>
    </ul>
  </article>
</template>
