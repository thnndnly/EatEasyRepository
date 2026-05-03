<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { useHouseholdStore } from '@/stores/householdStore'
import type { HouseholdDto } from '@/types/household'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const householdStore = useHouseholdStore()

const token = computed<string>(() => {
  const raw = route.query.token
  return typeof raw === 'string' ? raw : ''
})

const status = ref<'idle' | 'submitting' | 'done' | 'error'>('idle')
const error = ref<string | null>(null)
const joined = ref<HouseholdDto | null>(null)

async function accept(): Promise<void> {
  if (!token.value) {
    status.value = 'error'
    error.value = 'Kein Einladungs-Token in der URL.'
    return
  }
  status.value = 'submitting'
  error.value = null
  try {
    joined.value = await householdStore.acceptInvitation(token.value)
    status.value = 'done'
  } catch (err: unknown) {
    status.value = 'error'
    error.value = err instanceof Error ? err.message : 'Annahme fehlgeschlagen'
  }
}

function goToHousehold(): void {
  if (joined.value) {
    void router.push({ name: 'household-detail', params: { id: joined.value.id } })
  }
}

onMounted(() => {
  if (!authStore.isAuthenticated) {
    void router.replace({
      name: 'login',
      query: { redirect: route.fullPath },
    })
  }
})
</script>

<template>
  <section class="mx-auto max-w-md space-y-6">
    <h1 class="text-2xl font-semibold">Einladung annehmen</h1>

    <div class="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
      <template v-if="status === 'idle'">
        <p class="text-sm text-slate-600">
          Du wurdest zu einem Haushalt eingeladen. Klicke auf
          <strong>Annehmen</strong>, um beizutreten.
        </p>
        <button
          type="button"
          class="mt-4 w-full rounded bg-emerald-600 px-4 py-2 font-medium text-white hover:bg-emerald-700"
          @click="accept"
        >
          Annehmen
        </button>
      </template>

      <p v-else-if="status === 'submitting'" class="text-sm text-slate-600">
        Loese Einladung ein ...
      </p>

      <template v-else-if="status === 'done' && joined">
        <p class="text-sm text-emerald-800">
          Willkommen bei <strong>{{ joined.name }}</strong>.
        </p>
        <button
          type="button"
          class="mt-4 w-full rounded bg-emerald-600 px-4 py-2 font-medium text-white hover:bg-emerald-700"
          @click="goToHousehold"
        >
          Zum Haushalt
        </button>
      </template>

      <template v-else-if="status === 'error'">
        <p class="rounded border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700">
          {{ error }}
        </p>
        <button
          type="button"
          class="mt-4 w-full rounded border border-slate-300 bg-white px-4 py-2 font-medium text-slate-700 hover:bg-slate-50"
          @click="accept"
        >
          Erneut versuchen
        </button>
      </template>
    </div>
  </section>
</template>
