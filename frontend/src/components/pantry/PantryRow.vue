<script setup lang="ts">
import { ref, watch } from 'vue'
import { UNIT_ABBREV } from '@/types/units'
import type { PantryItemDto } from '@/types/pantry'

interface Props {
  item: PantryItemDto
}

const props = defineProps<Props>()
const emit = defineEmits<{
  save: [
    update: { id: string; amount: number; bestBefore: string | null },
  ]
  remove: [id: string]
}>()

const editing = ref(false)
const draftAmount = ref<number>(props.item.amount)
const draftBestBefore = ref<string>(props.item.bestBefore ?? '')

watch(
  () => props.item,
  (next) => {
    draftAmount.value = next.amount
    draftBestBefore.value = next.bestBefore ?? ''
    editing.value = false
  },
)

function startEdit(): void {
  draftAmount.value = props.item.amount
  draftBestBefore.value = props.item.bestBefore ?? ''
  editing.value = true
}

function cancel(): void {
  editing.value = false
}

function save(): void {
  if (!draftAmount.value || draftAmount.value <= 0) {
    return
  }
  emit('save', {
    id: props.item.id,
    amount: draftAmount.value,
    bestBefore: draftBestBefore.value || null,
  })
  editing.value = false
}

function onRemove(): void {
  if (confirm('Eintrag wirklich loeschen?')) {
    emit('remove', props.item.id)
  }
}
</script>

<template>
  <tr class="border-b border-slate-100 last:border-b-0">
    <td class="px-3 py-2 text-sm font-medium text-slate-800">{{ item.ingredientName }}</td>

    <td class="px-3 py-2 text-sm text-slate-700">
      <input
        v-if="editing"
        v-model.number="draftAmount"
        type="number"
        step="0.01"
        min="0.01"
        class="w-24 rounded border border-slate-300 px-2 py-1 focus:border-emerald-500 focus:outline-none"
      />
      <span v-else>{{ item.amount }}</span>
    </td>

    <td class="px-3 py-2 text-sm text-slate-700">{{ UNIT_ABBREV[item.unit] }}</td>

    <td class="px-3 py-2 text-sm text-slate-700">
      <input
        v-if="editing"
        v-model="draftBestBefore"
        type="date"
        class="rounded border border-slate-300 px-2 py-1 focus:border-emerald-500 focus:outline-none"
      />
      <span v-else>{{ item.bestBefore ?? '–' }}</span>
    </td>

    <td class="px-3 py-2 text-right text-sm">
      <template v-if="editing">
        <button
          type="button"
          class="rounded bg-emerald-600 px-2 py-1 text-xs font-medium text-white hover:bg-emerald-700"
          @click="save"
        >
          Speichern
        </button>
        <button
          type="button"
          class="ml-2 rounded border border-slate-300 bg-white px-2 py-1 text-xs font-medium text-slate-700 hover:bg-slate-50"
          @click="cancel"
        >
          Abbrechen
        </button>
      </template>
      <template v-else>
        <button
          type="button"
          class="rounded border border-slate-300 bg-white px-2 py-1 text-xs font-medium text-slate-700 hover:bg-slate-50"
          @click="startEdit"
        >
          Bearbeiten
        </button>
        <button
          type="button"
          class="ml-2 rounded border border-red-300 bg-white px-2 py-1 text-xs font-medium text-red-700 hover:bg-red-50"
          @click="onRemove"
        >
          Loeschen
        </button>
      </template>
    </td>
  </tr>
</template>
