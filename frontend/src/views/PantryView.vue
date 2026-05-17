<script setup lang="ts">
import { computed, defineAsyncComponent, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useHouseholdStore } from '@/stores/householdStore'
import { usePantryStore } from '@/stores/pantryStore'
import { useToastStore } from '@/stores/toastStore'
import AddPantryItemForm from '@/components/pantry/AddPantryItemForm.vue'
import PantryRow from '@/components/pantry/PantryRow.vue'

// Lazy-loaded — der Scanner zieht @zxing/browser (~430 kB) nach. Wird erst
// beim Klick auf "Barcode scannen" geladen, damit das initiale Pantry-Bundle
// klein bleibt.
const BarcodeScanner = defineAsyncComponent(
  () => import('@/components/pantry/BarcodeScanner.vue'),
)

const router = useRouter()
const householdStore = useHouseholdStore()
const pantryStore = usePantryStore()
const toastStore = useToastStore()

const selected = computed(() => householdStore.selected)
const scannerOpen = ref(false)

async function ensureLoaded(): Promise<void> {
  await householdStore.load()
  if (selected.value) {
    await pantryStore.load(selected.value.id)
  } else {
    pantryStore.reset()
  }
}

onMounted(ensureLoaded)
watch(() => selected.value?.id, ensureLoaded)

async function onAdd(payload: {
  ingredientId: string | null
  ingredientName: string
  amount: number
  unit: 'GRAM' | 'ML' | 'PIECE' | 'TBSP' | 'TSP'
  bestBefore: string | null
}): Promise<void> {
  try {
    await pantryStore.addItem({
      ingredientId: payload.ingredientId,
      ingredientName: payload.ingredientId ? null : payload.ingredientName,
      amount: payload.amount,
      unit: payload.unit,
      bestBefore: payload.bestBefore,
    })
  } catch (err: unknown) {
    pantryStore.error = err instanceof Error ? err.message : 'Hinzufuegen fehlgeschlagen'
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
  } catch (err: unknown) {
    pantryStore.error = err instanceof Error ? err.message : 'Speichern fehlgeschlagen'
  }
}

async function onRemove(id: string): Promise<void> {
  try {
    await pantryStore.removeItem(id)
  } catch (err: unknown) {
    pantryStore.error = err instanceof Error ? err.message : 'Loeschen fehlgeschlagen'
  }
}

function onBarcodeAdded(item: { ingredientName: string }): void {
  scannerOpen.value = false
  toastStore.success(`"${item.ingredientName}" in den Vorrat uebernommen`)
}
</script>

<template>
  <section class="space-y-6">
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="text-2xl font-extrabold tracking-tight">🧺 Vorrat</h1>
        <p class="mt-1 text-sm text-ink-500">
          {{ selected ? selected.name : 'Keinen Haushalt ausgewaehlt' }}
        </p>
      </div>
      <button v-if="selected" type="button" class="ee-btn-secondary" @click="scannerOpen = true">
        📷 Barcode scannen
      </button>
    </div>

    <BarcodeScanner
      v-if="scannerOpen && selected"
      :household-id="selected.id"
      @close="scannerOpen = false"
      @added="onBarcodeAdded"
    />

    <p
      v-if="!selected"
      class="rounded-2xl border border-dashed border-cream-300 bg-cream-50 p-8 text-center text-ink-500"
    >
      Lege zuerst einen Haushalt an oder waehle in der Topbar einen aus.
      <button type="button" class="ee-link ml-2" @click="router.push({ name: 'households' })">
        Haushalte oeffnen
      </button>
    </p>

    <template v-else>
      <p
        v-if="pantryStore.error"
        class="rounded-2xl border border-rose-200 bg-rose-100 px-4 py-3 text-sm font-medium text-rose-700"
      >
        {{ pantryStore.error }}
      </p>

      <AddPantryItemForm @submit="onAdd" />

      <div v-if="pantryStore.loading" class="text-ink-500">Lade Vorrat ...</div>

      <div
        v-else-if="pantryStore.items.length === 0"
        class="rounded-2xl border border-dashed border-cream-300 bg-cream-50 p-8 text-center text-ink-500"
      >
        Vorrat ist leer. Fuege oben einen Eintrag hinzu oder scanne einen Barcode.
      </div>

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
