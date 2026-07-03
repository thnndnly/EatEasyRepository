<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { usePantryStore } from '@/stores/pantryStore'
import { mhdStatusFor } from '@/utils/mhd'
import { UNIT_ABBREV } from '@/types/units'

const MAX_VISIBLE = 5

const router = useRouter()
const pantryStore = usePantryStore()

const visible = computed(() => pantryStore.expiringSoon.slice(0, MAX_VISIBLE))
const hiddenCount = computed(() => pantryStore.expiringSoon.length - visible.value.length)
</script>

<template>
  <div class="ee-card">
    <div class="flex items-start justify-between gap-4">
      <div>
        <h2 class="flex items-center gap-2 text-lg font-bold text-ink-900">
          <span>⏳</span> Demnaechst ablaufend
        </h2>
        <p class="mt-1 text-sm text-ink-500">
          Vorrat mit MHD in den naechsten 7 Tagen — am besten zuerst verkochen.
        </p>
      </div>
      <button
        type="button"
        class="ee-btn-secondary shrink-0"
        @click="router.push({ name: 'pantry' })"
      >
        Zum Vorrat
      </button>
    </div>

    <p
      v-if="pantryStore.expiringSoon.length === 0"
      class="mt-4 rounded-2xl border border-dashed border-cream-300 bg-cream-50 px-4 py-3 text-sm text-ink-500"
    >
      Nichts laeuft demnaechst ab. 🎉
    </p>

    <ul v-else class="mt-4 divide-y divide-cream-100 overflow-hidden rounded-2xl border border-cream-200">
      <li
        v-for="item in visible"
        :key="item.id"
        class="flex items-center gap-3 px-4 py-2.5"
        :class="mhdStatusFor(item.bestBefore!).rowClass"
      >
        <span class="flex-1 text-sm font-medium text-ink-900">
          {{ item.ingredientName }}
        </span>
        <span class="text-xs tabular-nums text-ink-500">
          {{ item.amount }} {{ UNIT_ABBREV[item.unit] }}
        </span>
        <span :class="mhdStatusFor(item.bestBefore!).chipClass">
          {{ mhdStatusFor(item.bestBefore!).label }}
        </span>
      </li>
    </ul>

    <p v-if="hiddenCount > 0" class="mt-2 text-xs text-ink-400">
      + {{ hiddenCount }} weitere im Vorrat
    </p>
  </div>
</template>
