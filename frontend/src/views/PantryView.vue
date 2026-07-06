<script setup lang="ts">
import { defineAsyncComponent, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useHouseholdStore } from '@/stores/householdStore'
import { usePantryStore } from '@/stores/pantryStore'
import { useToastStore } from '@/stores/toastStore'
import { FEATURE_RECEIPT } from '@/config/features'
import AddPantryItemForm from '@/components/pantry/AddPantryItemForm.vue'
import PantryRow from '@/components/pantry/PantryRow.vue'
import EmptyState from '@/components/common/EmptyState.vue'

// Lazy-loaded — der Scanner zieht @zxing/browser (~430 kB) nach. Wird erst
// beim Klick auf "Barcode scannen" geladen, damit das initiale Pantry-Bundle
// klein bleibt.
const BarcodeScanner = defineAsyncComponent(
  () => import('@/components/pantry/BarcodeScanner.vue'),
)
const ReceiptScanModal = defineAsyncComponent(
  () => import('@/components/pantry/ReceiptScanModal.vue'),
)

const router = useRouter()
const householdStore = useHouseholdStore()
const pantryStore = usePantryStore()
const toastStore = useToastStore()

const scannerOpen = ref(false)
const receiptOpen = ref(false)

async function ensureLoaded(): Promise<void> {
  await householdStore.load()
  if (householdStore.selected) {
    await pantryStore.load(householdStore.selected.id)
  } else {
    pantryStore.reset()
  }
}

onMounted(ensureLoaded)
watch(() => householdStore.selected?.id, ensureLoaded)

async function onAdd(payload: {
  ingredientId: string | null
  ingredientName: string
  amount: number
  unit: 'GRAM' | 'ML' | 'PIECE' | 'TBSP' | 'TSP'
  bestBefore: string | null
}): Promise<void> {
  // Fehler werden im Store ge-cached und via pantryStore.error im Template
  // angezeigt — wir muessen sie hier nur abfangen, damit die Promise nicht
  // floated.
  try {
    await pantryStore.addItem({
      ingredientId: payload.ingredientId,
      ingredientName: payload.ingredientId ? null : payload.ingredientName,
      amount: payload.amount,
      unit: payload.unit,
      bestBefore: payload.bestBefore,
    })
  } catch {
    // bewusst geschluckt — pantryStore.error ist gesetzt.
  }
}

async function onSave(update: {
  id: string
  amount: number
  bestBefore: string | null
}): Promise<void> {
  try {
    await pantryStore.updateItem(update.id, {
      amount: update.amount,
      bestBefore: update.bestBefore,
    })
  } catch {
    // bewusst geschluckt — pantryStore.error ist gesetzt.
  }
}

async function onRemove(id: string): Promise<void> {
  try {
    await pantryStore.removeItem(id)
  } catch {
    // bewusst geschluckt — pantryStore.error ist gesetzt.
  }
}

function onBarcodeAdded(item: { ingredientName: string }): void {
  scannerOpen.value = false
  toastStore.success(`"${item.ingredientName}" in den Vorrat uebernommen`)
}

function onReceiptAdded(count: number): void {
  receiptOpen.value = false
  toastStore.success(`${count} Posten in den Vorrat uebernommen`)
}
</script>

<template>
  <section class="space-y-6">
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="text-2xl font-extrabold tracking-tight">🧺 Vorrat</h1>
        <p class="mt-1 text-sm text-ink-500">
          {{ householdStore.selected ? householdStore.selected.name : 'Keinen Haushalt ausgewaehlt' }}
        </p>
      </div>
      <div v-if="householdStore.selected" class="flex flex-wrap gap-2">
        <button v-if="FEATURE_RECEIPT" type="button" class="ee-btn-secondary" @click="receiptOpen = true">
          🧾 Beleg scannen
        </button>
        <button type="button" class="ee-btn-secondary" @click="scannerOpen = true">
          📷 Barcode scannen
        </button>
      </div>
    </div>

    <BarcodeScanner
      v-if="householdStore.selected"
      :open="scannerOpen"
      :household-id="householdStore.selected.id"
      @close="scannerOpen = false"
      @added="onBarcodeAdded"
    />

    <ReceiptScanModal
      v-if="FEATURE_RECEIPT && householdStore.selected"
      :open="receiptOpen"
      :household-id="householdStore.selected.id"
      @close="receiptOpen = false"
      @added="onReceiptAdded"
    />

    <EmptyState v-if="!householdStore.selected">
      Lege zuerst einen Haushalt an oder waehle in der Topbar einen aus.
      <button type="button" class="ee-link ml-2" @click="router.push({ name: 'households' })">
        Haushalte oeffnen
      </button>
    </EmptyState>

    <template v-else>
      <p
        v-if="pantryStore.error"
        class="rounded-2xl border border-rose-200 bg-rose-100 px-4 py-3 text-sm font-medium text-rose-700"
      >
        {{ pantryStore.error }}
      </p>

      <AddPantryItemForm @submit="onAdd" />

      <div v-if="pantryStore.loading" class="text-ink-500">Lade Vorrat ...</div>

      <EmptyState v-else-if="pantryStore.items.length === 0">
        Vorrat ist leer. Fuege oben einen Eintrag hinzu oder scanne einen Barcode.
      </EmptyState>

      <div v-else class="overflow-hidden rounded-2xl border border-cream-200 bg-white">
        <table class="w-full">
          <thead>
            <tr class="border-b border-cream-200 bg-cream-50 text-left text-xs font-bold uppercase tracking-widest text-ink-500">
              <th class="px-4 py-3">Zutat</th>
              <th class="px-4 py-3">Menge</th>
              <th class="px-4 py-3">Einheit</th>
              <th class="px-4 py-3">MHD</th>
              <th class="px-4 py-3 text-right">Aktion</th>
            </tr>
          </thead>
          <tbody>
            <PantryRow
              v-for="item in pantryStore.items"
              :key="item.id"
              :item="item"
              @save="onSave"
              @remove="onRemove"
            />
          </tbody>
        </table>
      </div>
    </template>
  </section>
</template>
