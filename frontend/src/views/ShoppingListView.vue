<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useHouseholdStore } from '@/stores/householdStore'
import { useMealPlanStore, mondayOf } from '@/stores/mealPlanStore'
import { useShoppingListStore } from '@/stores/shoppingListStore'
import { useToastStore } from '@/stores/toastStore'
import ShoppingListItem from '@/components/shoppinglist/ShoppingListItem.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useConfirmDialog } from '@/composables/useConfirmDialog'
import {
  CATEGORY_ICONS,
  CATEGORY_LABELS,
  type IngredientCategory,
} from '@/types/ingredient'

const router = useRouter()
const householdStore = useHouseholdStore()
const mealPlanStore = useMealPlanStore()
const shoppingListStore = useShoppingListStore()
const toastStore = useToastStore()
const confirmDialog = useConfirmDialog()

async function ensureLoaded(): Promise<void> {
  await householdStore.load()
  if (!householdStore.selected) {
    shoppingListStore.reset()
    return
  }
  // Wir brauchen den aktuellen MealPlan, um darauf die ShoppingList zu generieren.
  const week = mealPlanStore.weekStart || mondayOf(new Date())
  await mealPlanStore.load(householdStore.selected.id, week)
  if (mealPlanStore.plan) {
    await shoppingListStore.load(mealPlanStore.plan.id)
  }
}

onMounted(ensureLoaded)
watch(() => householdStore.selected?.id, ensureLoaded)
watch(() => mealPlanStore.weekStart, ensureLoaded)

async function changeWeek(delta: number): Promise<void> {
  try {
    await mealPlanStore.gotoWeek(delta)
    await ensureLoaded()
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : 'Woche wechseln fehlgeschlagen'
    toastStore.error(message)
  }
}

async function gotoToday(): Promise<void> {
  try {
    await mealPlanStore.gotoToday()
    await ensureLoaded()
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : 'Heute laden fehlgeschlagen'
    toastStore.error(message)
  }
}

const totals = computed(() => {
  const items = shoppingListStore.list?.items ?? []
  const checked = items.filter((i) => i.checked).length
  return { checked, total: items.length }
})

async function onRegenerate(): Promise<void> {
  const ok = await confirmDialog(
    'Liste komplett neu berechnen? Abgehakte Einträge landen im Vorrat und fallen aus der Liste.',
  )
  if (!ok) {
    return
  }
  await shoppingListStore.regenerate()
  toastStore.info('Einkaufsliste neu berechnet')
}

async function onToggle(id: string, checked: boolean): Promise<void> {
  const item = shoppingListStore.list?.items.find((i) => i.id === id)
  await shoppingListStore.toggle(id, checked)
  if (checked && item) {
    toastStore.success(`"${item.ingredientName}" in den Vorrat übernommen`)
  }
}

// Browserseitiger PDF-Export: der Print-Dialog bietet "Als PDF speichern".
// Die print:*-Klassen blenden App-Chrome und Bedienelemente aus.
function printList(): void {
  window.print()
}

const printDate = new Date().toLocaleDateString('de-DE')

async function onChangeCategory(
  ingredientId: string,
  category: IngredientCategory,
): Promise<void> {
  await shoppingListStore.changeCategory(ingredientId, category)
  if (!shoppingListStore.error) {
    toastStore.info(`Kategorie geändert: ${CATEGORY_LABELS[category]}`)
  }
}
</script>

<template>
  <section class="space-y-6">
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="text-2xl font-extrabold tracking-tight">🛒 Einkaufsliste</h1>
        <p class="mt-1 text-sm text-ink-500">
          {{ householdStore.selected ? householdStore.selected.name : 'Keinen Haushalt ausgewählt' }}
          <span v-if="mealPlanStore.plan"> · Woche {{ mealPlanStore.weekRangeLabel }}</span>
        </p>
        <p class="mt-1 hidden text-xs text-ink-500 print:block">
          Stand: {{ printDate }} · erstellt mit EatEasy
        </p>
      </div>

      <div class="flex items-center gap-2 print:hidden">
        <button type="button" class="ee-btn-secondary" @click="changeWeek(-7)">
          ‹ Vorherige
        </button>
        <button type="button" class="ee-btn-secondary" @click="gotoToday">
          Heute
        </button>
        <button type="button" class="ee-btn-secondary" @click="changeWeek(7)">
          Nächste ›
        </button>
      </div>
    </div>

    <EmptyState v-if="!householdStore.selected">
      Lege zuerst einen Haushalt an oder wähle in der Topbar einen aus.
      <button type="button" class="ee-link ml-2" @click="router.push({ name: 'households' })">
        Haushalte öffnen
      </button>
    </EmptyState>

    <template v-else>
      <p
        v-if="shoppingListStore.error"
        class="rounded-2xl border border-rose-200 bg-rose-100 px-4 py-3 text-sm font-medium text-rose-700 print:hidden"
      >
        {{ shoppingListStore.error }}
      </p>

      <div class="flex items-center justify-between rounded-2xl border border-cream-200 bg-white px-5 py-3 text-sm print:hidden">
        <span class="font-medium text-ink-700">
          <span class="text-peach-600">{{ totals.checked }}</span> von {{ totals.total }} abgehakt
        </span>
        <div class="flex items-center gap-2">
          <button
            type="button"
            class="ee-btn-secondary"
            :disabled="shoppingListStore.loading || totals.total === 0"
            @click="printList"
          >
            🖨️ Drucken / PDF
          </button>
          <button
            type="button"
            class="ee-btn-secondary"
            :disabled="shoppingListStore.loading"
            @click="onRegenerate"
          >
            🔄 Neu berechnen
          </button>
        </div>
      </div>

      <p v-if="shoppingListStore.loading" class="text-ink-500">Lade Einkaufsliste ...</p>

      <div v-else-if="shoppingListStore.sortedItems.length > 0" class="space-y-4">
        <section
          v-for="group in shoppingListStore.groupedItems"
          :key="group.category"
          class="overflow-hidden rounded-2xl border border-cream-200 bg-white"
        >
          <h2
            class="flex items-center gap-2 border-b border-cream-100 bg-cream-50 px-5 py-2 text-xs font-bold uppercase tracking-wide text-ink-500"
          >
            <span aria-hidden="true">{{ CATEGORY_ICONS[group.category] }}</span>
            {{ CATEGORY_LABELS[group.category] }}
            <span class="ml-auto font-medium normal-case tracking-normal text-ink-400">
              {{ group.items.length }}
            </span>
          </h2>
          <ul class="divide-y divide-cream-100">
            <ShoppingListItem
              v-for="item in group.items"
              :key="item.id"
              :item="item"
              @toggle="onToggle"
              @change-category="onChangeCategory"
            />
          </ul>
        </section>
      </div>

      <EmptyState v-else>
        Liste ist leer — entweder gibt es keine Wochenplan-Einträge oder der Vorrat deckt
        bereits alles ab.
      </EmptyState>
    </template>
  </section>
</template>
