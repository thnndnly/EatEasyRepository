<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { UNIT_ABBREV } from '@/types/units'
import { useConfirmDialog } from '@/composables/useConfirmDialog'
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

const confirmDialog = useConfirmDialog()

const editing = ref(false)
const draftAmount = ref<number>(props.item.amount)
const draftBestBefore = ref<string>(props.item.bestBefore ?? '')

const MS_PER_DAY = 1000 * 60 * 60 * 24

function daysUntil(isoDate: string): number {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const target = new Date(isoDate)
  target.setHours(0, 0, 0, 0)
  return Math.round((target.getTime() - today.getTime()) / MS_PER_DAY)
}

interface MhdStatus {
  level: 'expired' | 'urgent' | 'soon' | 'ok'
  label: string
  rowClass: string
  chipClass: string
}

const mhdStatus = computed<MhdStatus | null>(() => {
  if (!props.item.bestBefore) {
    return null
  }
  const days = daysUntil(props.item.bestBefore)
  if (days < 0) {
    return {
      level: 'expired',
      label: days === -1 ? 'gestern abgelaufen' : `vor ${-days} Tagen abgelaufen`,
      rowClass: 'bg-rose-50',
      chipClass: 'ee-chip-rose',
    }
  }
  if (days <= 3) {
    return {
      level: 'urgent',
      label: days === 0 ? 'heute' : days === 1 ? 'morgen' : `in ${days} Tagen`,
      rowClass: 'bg-rose-50',
      chipClass: 'ee-chip-rose',
    }
  }
  if (days <= 7) {
    return {
      level: 'soon',
      label: `in ${days} Tagen`,
      rowClass: 'bg-butter-100/60',
      chipClass: 'ee-chip-butter',
    }
  }
  return {
    level: 'ok',
    label: `in ${days} Tagen`,
    rowClass: '',
    chipClass: 'ee-chip-neutral',
  }
})

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

async function onRemove(): Promise<void> {
  const ok = await confirmDialog('Eintrag wirklich loeschen?')
  if (ok) {
    emit('remove', props.item.id)
  }
}
</script>

<template>
  <tr class="border-b border-cream-100 last:border-b-0 transition-colors" :class="mhdStatus?.rowClass">
    <td class="px-4 py-3 text-sm font-semibold text-ink-900">{{ item.ingredientName }}</td>

    <td class="px-4 py-3 text-sm text-ink-700">
      <input v-if="editing" v-model.number="draftAmount" type="number" step="0.01" min="0.01" class="w-24" />
      <span v-else>{{ item.amount }}</span>
    </td>

    <td class="px-4 py-3 text-sm text-ink-700">{{ UNIT_ABBREV[item.unit] }}</td>

    <td class="px-4 py-3 text-sm text-ink-700">
      <input v-if="editing" v-model="draftBestBefore" type="date" />
      <template v-else-if="item.bestBefore">
        <div class="flex items-center gap-2">
          <span>{{ item.bestBefore }}</span>
          <span v-if="mhdStatus" :class="mhdStatus.chipClass">
            {{ mhdStatus.label }}
          </span>
        </div>
      </template>
      <span v-else class="text-ink-400">–</span>
    </td>

    <td class="px-4 py-3 text-right text-sm">
      <template v-if="editing">
        <button type="button" class="ee-btn-primary ee-btn-sm" @click="save">
          Speichern
        </button>
        <button type="button" class="ee-btn-ghost ee-btn-sm ml-2" @click="cancel">
          Abbrechen
        </button>
      </template>
      <template v-else>
        <button type="button" class="ee-btn-secondary ee-btn-sm" @click="startEdit">
          Bearbeiten
        </button>
        <button type="button" class="ee-btn-danger ee-btn-sm ml-2" @click="onRemove">
          Loeschen
        </button>
      </template>
    </td>
  </tr>
</template>
