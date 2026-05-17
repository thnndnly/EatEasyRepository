<script setup lang="ts">
import { ref } from 'vue'
import * as externalRecipeService from '@/services/externalRecipeService'
import { useAuthStore } from '@/stores/authStore'
import { useHouseholdStore } from '@/stores/householdStore'
import BaseModal from '@/components/common/BaseModal.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import type { ExternalRecipePreviewDto } from '@/types/externalRecipe'
import type { RecipeDto } from '@/types/recipe'

interface Props {
  open: boolean
}

defineProps<Props>()
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
  <BaseModal :open="open" @close="emit('close')">
    <template #header>
      <h2 class="text-base font-semibold text-ink-900">Rezept aus Quelle importieren</h2>
      <p class="text-xs text-ink-500">
        TheMealDB ist kostenlos und braucht keinen API-Key.
      </p>
    </template>

    <div class="space-y-4">
      <div class="space-y-3">
        <div class="grid grid-cols-12 gap-3">
          <div class="col-span-3">
            <label for="ext-source" class="block text-xs font-medium text-ink-500">Quelle</label>
            <select
              id="ext-source"
              v-model="source"
              class="ee-input mt-1 w-full"
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
                class="ee-input flex-1"
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
            class="ee-input mt-1 w-full"
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

      <ErrorMessage :message="error ?? ''" />

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
  </BaseModal>
</template>
