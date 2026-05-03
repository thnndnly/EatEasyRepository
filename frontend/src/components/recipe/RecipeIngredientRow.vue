<script setup lang="ts">
import { computed } from 'vue'
import IngredientPicker from './IngredientPicker.vue'
import { UNITS, UNIT_ABBREV, type Unit } from '@/types/units'

export interface RecipeIngredientFormRow {
  ingredientId: string | null
  ingredientName: string
  amount: number
  unit: Unit
  note: string
}

interface Props {
  modelValue: RecipeIngredientFormRow
  removable: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:modelValue': [value: RecipeIngredientFormRow]
  remove: []
}>()

const picker = computed({
  get: () => ({ id: props.modelValue.ingredientId, name: props.modelValue.ingredientName }),
  set: (next: { id: string | null; name: string }) => {
    emit('update:modelValue', {
      ...props.modelValue,
      ingredientId: next.id,
      ingredientName: next.name,
    })
  },
})

function patch<K extends keyof RecipeIngredientFormRow>(
  key: K,
  value: RecipeIngredientFormRow[K],
): void {
  emit('update:modelValue', { ...props.modelValue, [key]: value })
}
</script>

<template>
  <div class="grid grid-cols-12 gap-2">
    <div class="col-span-5">
      <IngredientPicker v-model="picker" />
    </div>
    <div class="col-span-2">
      <input
        type="number"
        step="0.01"
        min="0.01"
        :value="modelValue.amount"
        placeholder="Menge"
        class="w-full rounded border border-slate-300 px-2 py-2 focus:border-emerald-500 focus:outline-none"
        @input="patch('amount', Number(($event.target as HTMLInputElement).value))"
      />
    </div>
    <div class="col-span-2">
      <select
        :value="modelValue.unit"
        class="w-full rounded border border-slate-300 px-2 py-2 focus:border-emerald-500 focus:outline-none"
        @change="patch('unit', ($event.target as HTMLSelectElement).value as Unit)"
      >
        <option v-for="unit in UNITS" :key="unit" :value="unit">
          {{ UNIT_ABBREV[unit] }}
        </option>
      </select>
    </div>
    <div class="col-span-2">
      <input
        type="text"
        :value="modelValue.note"
        placeholder="Hinweis (optional)"
        maxlength="200"
        class="w-full rounded border border-slate-300 px-2 py-2 focus:border-emerald-500 focus:outline-none"
        @input="patch('note', ($event.target as HTMLInputElement).value)"
      />
    </div>
    <div class="col-span-1 flex items-center justify-center">
      <button
        v-if="removable"
        type="button"
        class="rounded border border-red-300 bg-white px-2 py-1.5 text-xs font-medium text-red-700 hover:bg-red-50"
        @click="emit('remove')"
      >
        ✕
      </button>
    </div>
  </div>
</template>
