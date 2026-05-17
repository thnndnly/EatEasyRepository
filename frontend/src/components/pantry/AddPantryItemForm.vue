<script setup lang="ts">
import { ref } from 'vue'
import IngredientPicker from '@/components/recipe/IngredientPicker.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import { UNITS, UNIT_ABBREV, type Unit } from '@/types/units'

const emit = defineEmits<{
  submit: [
    payload: {
      ingredientId: string | null
      ingredientName: string
      amount: number
      unit: Unit
      bestBefore: string | null
    },
  ]
}>()

const picker = ref<{ id: string | null; name: string }>({ id: null, name: '' })
const amount = ref<number>(0)
const unit = ref<Unit>('GRAM')
const bestBefore = ref<string>('')
const error = ref<string | null>(null)

function onSubmit(): void {
  error.value = null
  const name = picker.value.name.trim()
  if (!picker.value.id && !name) {
    error.value = 'Bitte Zutat auswaehlen oder Namen eingeben'
    return
  }
  if (!amount.value || amount.value <= 0) {
    error.value = 'Menge muss positiv sein'
    return
  }
  emit('submit', {
    ingredientId: picker.value.id,
    ingredientName: name,
    amount: amount.value,
    unit: unit.value,
    bestBefore: bestBefore.value || null,
  })
  picker.value = { id: null, name: '' }
  amount.value = 0
  bestBefore.value = ''
}
</script>

<template>
  <form
    class="space-y-3 rounded-lg border border-cream-200 bg-white p-5 shadow-sm"
    @submit.prevent="onSubmit"
  >
    <h2 class="text-base font-semibold text-ink-900">Eintrag hinzufuegen</h2>

    <div class="grid grid-cols-12 gap-2">
      <div class="col-span-5">
        <IngredientPicker v-model="picker" placeholder="Zutat ..." />
      </div>
      <div class="col-span-2">
        <input
          v-model.number="amount"
          type="number"
          step="0.01"
          min="0.01"
          placeholder="Menge"
          required
          class="w-full rounded border border-cream-300 px-2 py-2 focus:border-peach-400 focus:outline-none"
        />
      </div>
      <div class="col-span-2">
        <select
          v-model="unit"
          class="w-full rounded border border-cream-300 px-2 py-2 focus:border-peach-400 focus:outline-none"
        >
          <option v-for="u in UNITS" :key="u" :value="u">{{ UNIT_ABBREV[u] }}</option>
        </select>
      </div>
      <div class="col-span-3">
        <input
          v-model="bestBefore"
          type="date"
          placeholder="MHD"
          class="w-full rounded border border-cream-300 px-2 py-2 focus:border-peach-400 focus:outline-none"
        />
      </div>
    </div>

    <ErrorMessage :message="error ?? ''" />

    <button
      type="submit"
      class="ee-btn-primary"
    >
      Hinzufuegen
    </button>
  </form>
</template>
