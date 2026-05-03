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
    class="flex items-center gap-3 px-4 py-3"
    :class="item.checked ? 'bg-slate-50 text-slate-400' : 'text-slate-800'"
  >
    <input
      type="checkbox"
      :checked="item.checked"
      class="h-4 w-4 rounded border-slate-300 text-emerald-600 focus:ring-emerald-500"
      @change="emit('toggle', item.id, ($event.target as HTMLInputElement).checked)"
    />
    <span
      class="flex-1 text-sm"
      :class="{ 'line-through': item.checked }"
    >
      {{ item.ingredientName }}
    </span>
    <span class="text-sm tabular-nums">
      {{ item.amount }} {{ UNIT_ABBREV[item.unit] }}
    </span>
  </li>
</template>
