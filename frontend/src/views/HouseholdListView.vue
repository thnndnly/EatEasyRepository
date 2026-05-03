<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useHouseholdStore } from '@/stores/householdStore'
import DietTagSelector from '@/components/common/DietTagSelector.vue'
import type { DietTag } from '@/types/dietTags'

const router = useRouter()
const store = useHouseholdStore()

const showCreate = ref(false)
const newName = ref('')
const newTags = ref<DietTag[]>([])
const submitting = ref(false)
const error = ref<string | null>(null)

onMounted(async () => {
  await store.load()
})

async function onCreate(): Promise<void> {
  error.value = null
  submitting.value = true
  try {
    const created = await store.create({
      name: newName.value.trim(),
      defaultDietTags: newTags.value,
    })
    showCreate.value = false
    newName.value = ''
    newTags.value = []
    await router.push({ name: 'household-detail', params: { id: created.id } })
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : 'Anlegen fehlgeschlagen'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-semibold">Haushalte</h1>
        <p class="mt-1 text-slate-600">Verwalte deine Haushalte und Mitglieder.</p>
      </div>
      <button
        type="button"
        class="rounded bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700"
        @click="showCreate = !showCreate"
      >
        {{ showCreate ? 'Abbrechen' : 'Neuer Haushalt' }}
      </button>
    </div>

    <form
      v-if="showCreate"
      class="space-y-4 rounded-lg border border-slate-200 bg-white p-6 shadow-sm"
      @submit.prevent="onCreate"
    >
      <div class="space-y-1">
        <label for="hh-name" class="block text-sm font-medium text-slate-700">Name</label>
        <input
          id="hh-name"
          v-model="newName"
          type="text"
          required
          maxlength="100"
          class="w-full rounded border border-slate-300 px-3 py-2 focus:border-emerald-500 focus:outline-none"
        />
      </div>

      <div class="space-y-2">
        <span class="block text-sm font-medium text-slate-700">Standard-Diaeten</span>
        <DietTagSelector v-model="newTags" />
      </div>

      <p v-if="error" class="rounded border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700">
        {{ error }}
      </p>

      <button
        type="submit"
        :disabled="submitting"
        class="rounded bg-emerald-600 px-4 py-2 font-medium text-white hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-60"
      >
        {{ submitting ? 'Lege an ...' : 'Anlegen' }}
      </button>
    </form>

    <p v-if="store.loading" class="text-slate-500">Lade Haushalte ...</p>

    <ul v-else-if="store.households.length > 0" class="grid gap-4 sm:grid-cols-2">
      <li
        v-for="household in store.households"
        :key="household.id"
        class="cursor-pointer rounded-lg border border-slate-200 bg-white p-5 shadow-sm hover:border-emerald-400"
        @click="router.push({ name: 'household-detail', params: { id: household.id } })"
      >
        <div class="flex items-start justify-between gap-3">
          <h2 class="text-lg font-semibold text-slate-800">{{ household.name }}</h2>
          <span
            class="rounded px-2 py-0.5 text-xs font-medium"
            :class="
              household.role === 'OWNER'
                ? 'bg-amber-100 text-amber-800'
                : 'bg-slate-100 text-slate-700'
            "
          >
            {{ household.role === 'OWNER' ? 'Owner' : 'Mitglied' }}
          </span>
        </div>
        <p v-if="household.defaultDietTags.length > 0" class="mt-2 text-sm text-slate-500">
          {{ household.defaultDietTags.join(', ') }}
        </p>
      </li>
    </ul>

    <p v-else-if="!showCreate" class="rounded border border-dashed border-slate-300 bg-white p-6 text-center text-slate-500">
      Noch keine Haushalte. Lege deinen ersten Haushalt an oder loese eine Einladung ein.
    </p>
  </section>
</template>
