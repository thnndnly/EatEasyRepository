<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useHouseholdStore } from '@/stores/householdStore'
import { usePantryStore } from '@/stores/pantryStore'
import AddPantryItemForm from '@/components/pantry/AddPantryItemForm.vue'
import PantryRow from '@/components/pantry/PantryRow.vue'

const router = useRouter()
const householdStore = useHouseholdStore()
const pantryStore = usePantryStore()

const selected = computed(() => householdStore.selected)

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
</script>

<template>
  <section class="space-y-6">
    <div>
      <h1 class="text-2xl font-semibold">Vorrat</h1>
      <p class="mt-1 text-sm text-slate-600">
        {{ selected ? selected.name : 'Keinen Haushalt ausgewaehlt' }}
      </p>
    </div>

    <p
      v-if="!selected"
      class="rounded border border-dashed border-slate-300 bg-white p-6 text-center text-slate-500"
    >
      Lege zuerst einen Haushalt an oder waehle in der Topbar einen aus.
      <button
        type="button"
        class="ml-2 font-medium text-emerald-700 hover:underline"
        @click="router.push({ name: 'households' })"
      >
        Haushalte oeffnen
      </button>
    </p>

    <template v-else>
      <p
        v-if="pantryStore.error"
        class="rounded border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700"
      >
        {{ pantryStore.error }}
      </p>

      <AddPantryItemForm @submit="onAdd" />

      <div v-if="pantryStore.loading" class="text-slate-500">Lade Vorrat ...</div>

      <div
        v-else-if="pantryStore.items.length === 0"
        class="rounded border border-dashed border-slate-300 bg-white p-6 text-center text-slate-500"
      >
        Vorrat ist leer. Fuege oben einen Eintrag hinzu.
      </div>

      <div v-else class="overflow-hidden rounded-lg border border-slate-200 bg-white">
        <table class="w-full">
          <thead>
            <tr class="border-b border-slate-200 bg-slate-50 text-left text-xs font-medium uppercase tracking-wide text-slate-500">
              <th class="px-3 py-2">Zutat</th>
              <th class="px-3 py-2">Menge</th>
              <th class="px-3 py-2">Einheit</th>
              <th class="px-3 py-2">MHD</th>
              <th class="px-3 py-2 text-right">Aktion</th>
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
