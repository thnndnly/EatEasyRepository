<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useHouseholdStore } from '@/stores/householdStore'
import { useMealPlanStore, mondayOf } from '@/stores/mealPlanStore'
import { useShoppingListStore } from '@/stores/shoppingListStore'
import { useToastStore } from '@/stores/toastStore'
import ShoppingListItem from '@/components/shoppinglist/ShoppingListItem.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const router = useRouter()
const householdStore = useHouseholdStore()
const mealPlanStore = useMealPlanStore()
const shoppingListStore = useShoppingListStore()
const toastStore = useToastStore()

const selected = computed(() => householdStore.selected)

async function ensureLoaded(): Promise<void> {
  await householdStore.load()
  if (!selected.value) {
    shoppingListStore.reset()
    return
  }
  // Wir brauchen den aktuellen MealPlan, um darauf die ShoppingList zu generieren.
  const week = mealPlanStore.weekStart || mondayOf(new Date())
  await mealPlanStore.load(selected.value.id, week)
  if (mealPlanStore.plan) {
    await shoppingListStore.load(mealPlanStore.plan.id)
  }
}

onMounted(ensureLoaded)
watch(() => selected.value?.id, ensureLoaded)
watch(() => mealPlanStore.weekStart, ensureLoaded)

const totals = computed(() => {
  const items = shoppingListStore.list?.items ?? []
  const checked = items.filter((i) => i.checked).length
  return { checked, total: items.length }
})

async function onRegenerate(): Promise<void> {
  if (!confirm('Liste komplett neu berechnen? Abgehakte Eintraege landen im Vorrat und fallen aus der Liste.')) {
    return
  }
  await shoppingListStore.regenerate()
  toastStore.info('Einkaufsliste neu berechnet')
}

async function onToggle(id: string, checked: boolean): Promise<void> {
  const item = shoppingListStore.list?.items.find((i) => i.id === id)
  await shoppingListStore.toggle(id, checked)
  if (checked && item) {
    toastStore.success(`"${item.ingredientName}" in den Vorrat uebernommen`)
  }
}
</script>

<template>
  <section class="space-y-6">
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="text-2xl font-extrabold tracking-tight">🛒 Einkaufsliste</h1>
        <p class="mt-1 text-sm text-ink-500">
          {{ selected ? selected.name : 'Keinen Haushalt ausgewaehlt' }}
          <span v-if="mealPlanStore.plan"> · Woche {{ mealPlanStore.weekRangeLabel }}</span>
        </p>
      </div>

      <div class="flex items-center gap-2">
        <button type="button" class="ee-btn-secondary" @click="mealPlanStore.gotoWeek(-7).then(ensureLoaded)">
          ‹ Vorherige
        </button>
        <button type="button" class="ee-btn-secondary" @click="mealPlanStore.gotoToday().then(ensureLoaded)">
          Heute
        </button>
        <button type="button" class="ee-btn-secondary" @click="mealPlanStore.gotoWeek(7).then(ensureLoaded)">
          Naechste ›
        </button>
      </div>
    </div>

    <EmptyState v-if="!selected">
      Lege zuerst einen Haushalt an oder waehle in der Topbar einen aus.
      <button type="button" class="ee-link ml-2" @click="router.push({ name: 'households' })">
        Haushalte oeffnen
      </button>
    </EmptyState>

    <template v-else>
      <p
        v-if="shoppingListStore.error"
        class="rounded-2xl border border-rose-200 bg-rose-100 px-4 py-3 text-sm font-medium text-rose-700"
      >
        {{ shoppingListStore.error }}
      </p>

      <div class="flex items-center justify-between rounded-2xl border border-cream-200 bg-white px-5 py-3 text-sm">
        <span class="font-medium text-ink-700">
          <span class="text-peach-600">{{ totals.checked }}</span> von {{ totals.total }} abgehakt
        </span>
        <button
          type="button"
          class="ee-btn-secondary"
          :disabled="shoppingListStore.loading"
          @click="onRegenerate"
        >
          🔄 Neu berechnen
        </button>
      </div>

      <p v-if="shoppingListStore.loading" class="text-ink-500">Lade Einkaufsliste ...</p>

      <ul
        v-else-if="shoppingListStore.sortedItems.length > 0"
        class="divide-y divide-cream-100 overflow-hidden rounded-2xl border border-cream-200 bg-white"
      >
        <ShoppingListItem
          v-for="item in shoppingListStore.sortedItems"
          :key="item.id"
          :item="item"
          @toggle="onToggle"
        />
      </ul>

      <EmptyState v-else>
        Liste ist leer — entweder gibt es keine Wochenplan-Eintraege oder der Vorrat deckt
        bereits alles ab.
      </EmptyState>
    </template>
  </section>
</template>
