<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchHealth } from '@/services/healthService'
import { useAuthStore } from '@/stores/authStore'

const authStore = useAuthStore()

const status = ref<'loading' | 'ok' | 'error'>('loading')
const detail = ref<string>('Pruefe Backend ...')

async function refresh(): Promise<void> {
  status.value = 'loading'
  detail.value = 'Pruefe Backend ...'
  try {
    const health = await fetchHealth()
    status.value = health.status === 'ok' ? 'ok' : 'error'
    detail.value = `Backend status: ${health.status}`
  } catch (error: unknown) {
    status.value = 'error'
    detail.value = error instanceof Error ? error.message : 'Unbekannter Fehler'
  }
}

onMounted(() => {
  void refresh()
})
</script>

<template>
  <section class="space-y-6">
    <div>
      <h1 class="text-2xl font-semibold">
        Hallo, {{ authStore.user?.displayName ?? 'Gast' }}
      </h1>
      <p class="mt-1 text-slate-600">
        Mahlzeitenplanung, Rezepte und Einkaufslisten fuer den ganzen Haushalt.
      </p>
    </div>

    <div
      class="rounded-lg border bg-white p-5 shadow-sm"
      :class="{
        'border-slate-200': status === 'loading',
        'border-emerald-300': status === 'ok',
        'border-red-300': status === 'error',
      }"
    >
      <div class="flex items-center justify-between">
        <div>
          <p class="text-sm font-medium uppercase tracking-wide text-slate-500">Backend-Status</p>
          <p
            class="mt-1 text-lg font-semibold"
            :class="{
              'text-slate-700': status === 'loading',
              'text-emerald-700': status === 'ok',
              'text-red-700': status === 'error',
            }"
          >
            {{ detail }}
          </p>
        </div>
        <button
          type="button"
          class="rounded border border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
          @click="refresh"
        >
          Erneut pruefen
        </button>
      </div>
    </div>
  </section>
</template>
