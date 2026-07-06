<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import BaseModal from '@/components/common/BaseModal.vue'
import { useReceiptStore } from '@/stores/receiptStore'
import { usePantryStore } from '@/stores/pantryStore'
import { UNIT_ABBREV, UNITS, type Unit } from '@/types/units'

interface Props {
  open: boolean
  householdId: string
}

interface EditableItem {
  selected: boolean
  name: string
  amount: number
  unit: Unit
  ingredientId: string | null
}

const props = defineProps<Props>()
const emit = defineEmits<{ close: []; added: [count: number] }>()

const receiptStore = useReceiptStore()
const pantryStore = usePantryStore()

const fileInput = ref<HTMLInputElement | null>(null)
const editableItems = ref<EditableItem[]>([])
const saving = ref(false)
const rawTextVisible = ref(false)

// Scan-Ergebnis in editierbare Zeilen uebernehmen; alle vorselektiert.
watch(
  () => receiptStore.result,
  (next) => {
    editableItems.value = (next?.items ?? []).map((item) => ({
      selected: true,
      name: item.name,
      amount: item.amount,
      unit: item.unit,
      ingredientId: item.ingredientId,
    }))
  },
)

watch(
  () => props.open,
  (isOpen) => {
    if (!isOpen) {
      receiptStore.reset()
      editableItems.value = []
      rawTextVisible.value = false
    }
  },
)

const selectedCount = computed(() => editableItems.value.filter((i) => i.selected).length)

async function onFileChange(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) {
    return
  }
  await receiptStore.scan(props.householdId, file)
  // Gleiche Datei nochmal waehlbar machen (change feuert sonst nicht).
  input.value = ''
}

function updateItem(index: number, patch: Partial<EditableItem>): void {
  editableItems.value = editableItems.value.map((item, i) =>
    i === index ? { ...item, ...patch } : item,
  )
}

async function onConfirm(): Promise<void> {
  const toAdd = editableItems.value.filter(
    (i) => i.selected && i.name.trim() !== '' && i.amount > 0,
  )
  if (toAdd.length === 0) {
    return
  }
  saving.value = true
  try {
    // Sequenziell statt parallel: PantryService aggregiert gleiche Zutaten —
    // parallele Requests koennten sich gegenseitig ueberschreiben.
    for (const item of toAdd) {
      await pantryStore.addItem({
        ingredientId: item.ingredientId,
        ingredientName: item.ingredientId ? null : item.name.trim(),
        amount: item.amount,
        unit: item.unit,
        bestBefore: null,
      })
    }
    emit('added', toAdd.length)
  } catch {
    // pantryStore.error ist gesetzt und wird in der View angezeigt.
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <BaseModal :open="open" @close="emit('close')">
    <template #header>
      <h2 class="text-lg font-bold text-ink-900">🧾 Beleg scannen</h2>
    </template>

    <div class="space-y-4">
      <!-- Schritt 1: Foto waehlen -->
      <div v-if="!receiptStore.result" class="space-y-3">
        <p class="text-sm text-ink-500">
          Fotografiere deinen Kassenbon oder waehle ein Foto aus. Der Text wird per
          OCR erkannt und die Lebensmittel automatisch extrahiert — du bestaetigst
          das Ergebnis, bevor etwas im Vorrat landet.
        </p>
        <input
          ref="fileInput"
          type="file"
          accept="image/*"
          capture="environment"
          class="hidden"
          @change="onFileChange"
        />
        <button
          type="button"
          class="ee-btn-primary ee-btn-lg w-full"
          :disabled="receiptStore.scanning"
          @click="fileInput?.click()"
        >
          {{ receiptStore.scanning ? 'Erkenne Text ...' : '📷 Foto auswaehlen' }}
        </button>
        <p
          v-if="receiptStore.error"
          class="rounded-2xl border border-rose-200 bg-rose-100 px-4 py-3 text-sm font-medium text-rose-700"
        >
          {{ receiptStore.error }}
        </p>
      </div>

      <!-- Schritt 2: Vorschau bestaetigen -->
      <div v-else class="space-y-4">
        <p v-if="editableItems.length === 0" class="rounded-2xl border border-dashed border-cream-300 bg-cream-50 px-4 py-3 text-sm text-ink-500">
          Es konnten keine Lebensmittel extrahiert werden. Wirf einen Blick in den
          erkannten Text und lege die Posten ggf. manuell an.
        </p>

        <ul v-else class="divide-y divide-cream-100 overflow-hidden rounded-2xl border border-cream-200">
          <li
            v-for="(item, index) in editableItems"
            :key="index"
            class="flex items-center gap-2 px-3 py-2"
            :class="item.selected ? '' : 'opacity-50'"
          >
            <input
              type="checkbox"
              :checked="item.selected"
              class="h-4 w-4 shrink-0 cursor-pointer rounded accent-peach-500"
              @change="updateItem(index, { selected: ($event.target as HTMLInputElement).checked })"
            />
            <input
              :value="item.name"
              type="text"
              class="ee-input min-w-0 flex-1 text-sm"
              @input="updateItem(index, { name: ($event.target as HTMLInputElement).value })"
            />
            <input
              :value="item.amount"
              type="number"
              min="0.01"
              step="0.01"
              class="ee-input w-20 text-sm tabular-nums"
              @input="updateItem(index, { amount: Number(($event.target as HTMLInputElement).value) })"
            />
            <select
              :value="item.unit"
              class="ee-input w-20 text-sm"
              @change="updateItem(index, { unit: ($event.target as HTMLSelectElement).value as Unit })"
            >
              <option v-for="unit in UNITS" :key="unit" :value="unit">
                {{ UNIT_ABBREV[unit] }}
              </option>
            </select>
            <span
              v-if="item.ingredientId"
              class="ee-chip-mint shrink-0"
              title="Zutat existiert bereits"
            >
              bekannt
            </span>
            <span v-else class="ee-chip-neutral shrink-0" title="Wird neu angelegt">
              neu
            </span>
          </li>
        </ul>

        <button type="button" class="ee-link text-xs" @click="rawTextVisible = !rawTextVisible">
          {{ rawTextVisible ? 'Erkannten Text ausblenden' : 'Erkannten Text anzeigen' }}
        </button>
        <pre
          v-if="rawTextVisible"
          class="max-h-40 overflow-auto rounded-2xl bg-cream-50 p-3 text-xs text-ink-700"
        >{{ receiptStore.result.rawText }}</pre>

        <div class="flex items-center justify-between gap-3">
          <button type="button" class="ee-btn-secondary" :disabled="saving" @click="receiptStore.reset()">
            ↺ Anderes Foto
          </button>
          <button
            type="button"
            class="ee-btn-primary"
            :disabled="saving || selectedCount === 0"
            @click="onConfirm"
          >
            {{ saving ? 'Uebernehme ...' : `${selectedCount} Posten in den Vorrat` }}
          </button>
        </div>
      </div>
    </div>
  </BaseModal>
</template>
