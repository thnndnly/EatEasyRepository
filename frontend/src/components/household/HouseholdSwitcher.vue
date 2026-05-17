<script setup lang="ts">
import { useHouseholdStore } from '@/stores/householdStore'

const store = useHouseholdStore()

function onChange(event: Event): void {
  const target = event.target as HTMLSelectElement
  store.selectHousehold(target.value || null)
}
</script>

<template>
  <select
    v-if="store.households.length > 0"
    :value="store.selectedId ?? ''"
    class="!rounded-full !border-cream-300 !bg-white !px-3 !py-1.5 text-sm font-medium"
    @change="onChange"
  >
    <option v-for="household in store.households" :key="household.id" :value="household.id">
      🏠 {{ household.name }}
    </option>
  </select>
</template>
