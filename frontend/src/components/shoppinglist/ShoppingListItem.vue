<script setup lang="ts">
import { UNIT_ABBREV } from '@/types/units'
import type { ShoppingListItemDto } from '@/types/shoppingList'
import {
  CATEGORY_LABELS,
  INGREDIENT_CATEGORIES,
  type IngredientCategory,
} from '@/types/ingredient'

interface Props {
  item: ShoppingListItemDto
}

defineProps<Props>()
const emit = defineEmits<{
  toggle: [id: string, checked: boolean]
  changeCategory: [ingredientId: string, category: IngredientCategory]
}>()

function onCategoryChange(ingredientId: string, event: Event): void {
  const value = (event.target as HTMLSelectElement).value as IngredientCategory
  emit('changeCategory', ingredientId, value)
}
</script>

<template>
  <li
    class="flex items-center gap-3 px-5 py-3 transition-colors"
    :class="item.checked ? 'bg-mint-50 text-ink-400' : 'text-ink-900 hover:bg-cream-50'"
  >
    <input
      type="checkbox"
      :checked="item.checked"
      class="h-5 w-5 cursor-pointer rounded-md accent-peach-500"
      @change="emit('toggle', item.id, ($event.target as HTMLInputElement).checked)"
    />
    <span
      class="flex-1 text-sm font-medium"
      :class="{ 'line-through': item.checked }"
    >
      {{ item.ingredientName }}
    </span>
    <select
      :value="item.category"
      class="ee-category-select max-w-[9rem] cursor-pointer rounded-lg border border-cream-200 bg-white px-2 py-1 text-xs text-ink-500 print:hidden"
      title="Kategorie der Zutat aendern"
      @change="onCategoryChange(item.ingredientId, $event)"
    >
      <option v-for="cat in INGREDIENT_CATEGORIES" :key="cat" :value="cat">
        {{ CATEGORY_LABELS[cat] }}
      </option>
    </select>
    <span class="text-sm font-semibold tabular-nums text-ink-700" :class="{ 'text-ink-400': item.checked }">
      {{ item.amount }} {{ UNIT_ABBREV[item.unit] }}
    </span>
  </li>
</template>
