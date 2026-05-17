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

    <div class="rounded-lg border border-cream-200 bg-white p-6 shadow-sm">
      <template v-if="status === 'idle'">
        <p class="text-sm text-ink-500">
          Du wurdest zu einem Haushalt eingeladen. Klicke auf
          <strong>Annehmen</strong>, um beizutreten.
        </p>
        <button
          type="button"
          class="mt-4 w-full rounded bg-peach-400 px-4 py-2 font-medium text-white hover:bg-peach-500"
          @click="accept"
        >
          Annehmen
        </button>
      </template>

      <p v-else-if="status === 'submitting'" class="text-sm text-ink-500">
        Loese Einladung ein ...
      </p>

      <template v-else-if="status === 'done' && joined">
        <p class="text-sm text-peach-700">
          Willkommen bei <strong>{{ joined.name }}</strong>.
        </p>
        <button
          type="button"
          class="mt-4 w-full rounded bg-peach-400 px-4 py-2 font-medium text-white hover:bg-peach-500"
          @click="goToHousehold"
        >
          Zum Haushalt
        </button>
      </template>

      <template v-else-if="status === 'error'">
        <p class="rounded border border-rose-200 bg-rose-100 px-3 py-2 text-sm text-rose-700">
          {{ error }}
        </p>
        <button
          type="button"
          class="mt-4 w-full rounded border border-cream-300 bg-white px-4 py-2 font-medium text-ink-700 hover:bg-cream-50"
          @click="accept"
        >
          Erneut versuchen
        </button>
      </template>
    </div>
  </section>
</template>
