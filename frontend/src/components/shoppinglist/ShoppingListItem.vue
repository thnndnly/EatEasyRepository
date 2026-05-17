<script setup lang="ts">
import { UNIT_ABBREV } from '@/types/units'
import type { ShoppingListItemDto } from '@/types/shoppingList'

interface Props {
  item: ShoppingListItemDto
}

defineProps<Props>()
const emit = defineEmits<{ toggle: [id: string, checked: boolean] }>()
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
    <span class="text-sm font-semibold tabular-nums text-ink-700" :class="{ 'text-ink-400': item.checked }">
      {{ item.amount }} {{ UNIT_ABBREV[item.unit] }}
    </span>
  </li>
</template>
