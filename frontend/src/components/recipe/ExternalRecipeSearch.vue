<script setup lang="ts">
import { ref } from 'vue'
import * as externalRecipeService from '@/services/externalRecipeService'
import { useAuthStore } from '@/stores/authStore'
import { useHouseholdStore } from '@/stores/householdStore'
import type { ExternalRecipePreviewDto } from '@/types/externalRecipe'
import type { RecipeDto } from '@/types/recipe'

const emit = defineEmits<{
  imported: [recipe: RecipeDto]
  close: []
}>()

const authStore = useAuthStore()
const householdStore = useHouseholdStore()

const source = ref<string>('themealdb')
const query = ref('')
const householdId = ref<string>('')
const results = ref<ExternalRecipePreviewDto[]>([])
const searching = ref(false)
const importingId = ref<string | null>(null)
const error = ref<string | null>(null)

async function onSearch(): Promise<void> {
  if (!authStore.token || !query.value.trim()) {
    return
  }
  searching.value = true
  error.value = null
  try {
    results.value = await externalRecipeService.searchExternal(
      authStore.token,
      source.value,
      query.value.trim(),
    )
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : 'Suche fehlgeschlagen'
    results.value = []
  } finally {
    searching.value = false
  }
}

async function onImport(preview: ExternalRecipePreviewDto): Promise<void> {
  if (!authStore.token) {
    return
  }
  importingId.value = preview.externalId
  error.value = null
  try {
    const recipe = await externalRecipeService.importExternalRecipe(authStore.token, {
      source: preview.source,
      externalId: preview.externalId,
      householdId: householdId.value || null,
    })
    emit('imported', recipe)
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : 'Import fehlgeschlagen'
  } finally {
    importingId.value = null
  }
}
</script>

<template>
  <div
    class="fixed inset-0 z-30 flex items-center justify-center bg-ink-900/40 px-4"
    @click.self="emit('close')"
  >
    <div class="flex max-h-[90vh] w-full max-w-3xl flex-col rounded-2xl bg-white shadow-[0_20px_60px_-15px_rgba(45,42,50,0.3)]">
      <header class="flex items-center justify-between border-b border-cream-200 px-5 py-4">
        <div>
          <h2 class="text-base font-semibold text-ink-900">Rezept aus Quelle importieren</h2>
          <p class="text-xs text-ink-500">
            TheMealDB ist kostenlos und braucht keinen API-Key.
          </p>
        </div>
        <button
          type="button"
          class="rounded text-ink-400 hover:text-ink-700"
          @click="emit('close')"
        >
          ✕
        </button>
      </header>

      <div class="space-y-3 border-b border-cream-200 px-5 py-4">
        <div class="grid grid-cols-12 gap-3">
          <div class="col-span-3">
            <label for="ext-source" class="block text-xs font-medium text-ink-500">Quelle</label>
            <select
              id="ext-source"
              v-model="source"
              class="mt-1 w-full rounded border border-cream-300 px-3 py-2 focus:border-peach-400 focus:outline-none"
            >
              <option value="themealdb">TheMealDB</option>
            </select>
          </div>
          <div class="col-span-9">
            <label for="ext-query" class="block text-xs font-medium text-ink-500">Suche</label>
            <form class="mt-1 flex gap-2" @submit.prevent="onSearch">
              <input
                id="ext-query"
                v-model="query"
                type="text"
                placeholder="z. B. pasta, chicken ..."
                class="flex-1 rounded border border-cream-300 px-3 py-2 focus:border-peach-400 focus:outline-none"
              />
              <button
                type="submit"
                :disabled="searching"
                class="ee-btn-primary"
              >
                {{ searching ? 'Suche ...' : 'Suchen' }}
              </button>
            </form>
          </div>
        </div>

        <div v-if="householdStore.households.length > 0">
          <label for="ext-hh" class="block text-xs font-medium text-ink-500">
            Sichtbarkeit
          </label>
          <select
            id="ext-hh"
            v-model="householdId"
            class="mt-1 w-full rounded border border-cream-300 px-3 py-2 focus:border-peach-400 focus:outline-none"
          >
            <option value="">Privat (nur ich)</option>
            <option
              v-for="household in householdStore.households"
              :key="household.id"
              :value="household.id"
            >
              {{ household.name }}
            </option>
          </select>
        </div>
      </div>

      <div class="flex-1 overflow-auto px-5 py-4">
        <p v-if="error" class="rounded-2xl border border-rose-200 bg-rose-100 px-3 py-2 text-sm font-medium text-rose-700">
          {{ error }}
        </p>

        <ul v-if="results.length > 0" class="space-y-2">
          <li
            v-for="preview in results"
            :key="preview.externalId"
            class="flex items-center gap-3 rounded border border-cream-200 px-3 py-2 hover:border-peach-300"
          >
            <img
              v-if="preview.thumbnailUrl"
              :src="preview.thumbnailUrl"
              :alt="preview.title"
              class="h-12 w-12 flex-shrink-0 rounded object-cover"
            />
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm font-medium text-ink-900">{{ preview.title }}</p>
              <p class="truncate text-xs text-ink-500">
                <span v-if="preview.category">{{ preview.category }}</span>
                <span v-if="preview.category && preview.area"> · </span>
                <span v-if="preview.area">{{ preview.area }}</span>
              </p>
            </div>
            <button
              type="button"
              :disabled="importingId !== null"
              class="ee-btn-primary ee-btn-sm"
              @click="onImport(preview)"
            >
              {{ importingId === preview.externalId ? 'Importiere ...' : 'Importieren' }}
            </button>
          </li>
        </ul>

        <p
          v-else-if="!searching && !error"
          class="text-sm text-ink-500"
        >
          Suche eintippen und auf "Suchen" klicken — oder bei TheMealDB einfach mal "pasta" probieren.
        </p>
      </div>
    </div>
  </div>
</template>
