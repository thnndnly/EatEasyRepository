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
    class="cursor-pointer rounded-lg border border-slate-200 bg-white p-5 shadow-sm transition hover:border-emerald-400"
  >
    <header class="flex items-start justify-between gap-3">
      <h3 class="text-base font-semibold text-slate-800">{{ recipe.title }}</h3>
      <span
        v-if="recipe.householdId"
        class="rounded bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800"
      >
        Haushalt
      </span>
    </header>

    <p
      v-if="recipe.description"
      class="mt-2 line-clamp-2 text-sm text-slate-600"
    >
      {{ recipe.description }}
    </p>

    <dl class="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-500">
      <div class="flex items-center gap-1">
        <dt>Portionen:</dt>
        <dd class="font-medium text-slate-700">{{ recipe.servings }}</dd>
      </div>
      <div v-if="recipe.prepMinutes !== null" class="flex items-center gap-1">
        <dt>Zeit:</dt>
        <dd class="font-medium text-slate-700">{{ recipe.prepMinutes }} min</dd>
      </div>
      <div class="flex items-center gap-1">
        <dt>Zutaten:</dt>
        <dd class="font-medium text-slate-700">{{ recipe.ingredients.length }}</dd>
      </div>
    </dl>

    <ul v-if="recipe.dietTags.length > 0" class="mt-3 flex flex-wrap gap-1">
      <li
        v-for="tag in recipe.dietTags"
        :key="tag"
        class="rounded bg-emerald-50 px-2 py-0.5 text-xs font-medium text-emerald-800"
      >
        {{ DIET_TAG_LABELS[tag as DietTag] }}
      </li>
    </ul>
  </article>
</template>
