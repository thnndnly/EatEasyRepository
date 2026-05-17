<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchHealth } from '@/services/healthService'
import { fetchSuggestions } from '@/services/suggestionService'
import { useAuthStore } from '@/stores/authStore'
import { useHouseholdStore } from '@/stores/householdStore'
import { DIET_TAG_LABELS, type DietTag } from '@/types/dietTags'
import type { SuggestionDto } from '@/types/suggestion'
import AddToMealPlanDialog from '@/components/suggestion/AddToMealPlanDialog.vue'

const router = useRouter()
const authStore = useAuthStore()
const householdStore = useHouseholdStore()

const status = ref<'loading' | 'ok' | 'error'>('loading')
const detail = ref<string>('Pruefe Backend ...')

const suggestions = ref<SuggestionDto[]>([])
const suggestLoading = ref(false)
const suggestError = ref<string | null>(null)
const suggestRequested = ref(false)
const planTarget = ref<SuggestionDto | null>(null)
const planFeedback = ref<string | null>(null)

const selected = computed(() => householdStore.selected)

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

async function onSuggest(): Promise<void> {
  if (!authStore.token || !selected.value) {
    return
  }
  suggestLoading.value = true
  suggestError.value = null
  suggestRequested.value = true
  try {
    suggestions.value = await fetchSuggestions(authStore.token, selected.value.id, {
      numSuggestions: 3,
    })
  } catch (err: unknown) {
    suggestError.value =
      err instanceof Error ? err.message : 'Vorschlaege konnten nicht geladen werden'
    suggestions.value = []
  } finally {
    suggestLoading.value = false
  }
}

function openRecipe(id: string): void {
  void router.push({ name: 'recipe-detail', params: { id } })
}

function startPlanAdd(suggestion: SuggestionDto): void {
  planTarget.value = suggestion
  planFeedback.value = null
}

function onPlanSaved(): void {
  if (planTarget.value) {
    planFeedback.value = `"${planTarget.value.recipe.title}" wurde in den Wochenplan uebernommen.`
  }
  planTarget.value = null
}

function coveragePercent(value: number): string {
  return `${Math.round(value * 100)} %`
}

onMounted(async () => {
  await refresh()
  await householdStore.load()
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

    <!-- Smart-Suggestion -->
    <div class="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
      <div class="flex items-start justify-between gap-4">
        <div>
          <h2 class="text-base font-semibold text-slate-800">Was kann ich kochen?</h2>
          <p class="mt-1 text-sm text-slate-600">
            {{ selected ? `Vorschlaege aus dem Vorrat von ${selected.name}.` : 'Waehle zuerst einen Haushalt.' }}
          </p>
        </div>
        <button
          type="button"
          :disabled="!selected || suggestLoading"
          class="rounded bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-60"
          @click="onSuggest"
        >
          {{ suggestLoading ? 'Denke nach ...' : 'Vorschlaege holen' }}
        </button>
      </div>

      <p
        v-if="suggestError"
        class="mt-3 rounded border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700"
      >
        {{ suggestError }}
      </p>

      <p
        v-else-if="suggestRequested && !suggestLoading && suggestions.length === 0"
        class="mt-3 rounded border border-dashed border-slate-300 px-3 py-2 text-sm text-slate-500"
      >
        Keine passenden Rezepte gefunden. Mehr Vorrat anlegen oder Rezepte hinzufuegen.
      </p>

      <p
        v-if="planFeedback"
        class="mt-3 rounded border border-emerald-300 bg-emerald-50 px-3 py-2 text-sm text-emerald-800"
      >
        {{ planFeedback }}
      </p>

      <ul v-if="suggestions.length > 0" class="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        <li
          v-for="s in suggestions"
          :key="s.recipe.id"
          class="rounded border border-slate-200 px-3 py-3 hover:border-emerald-400"
        >
          <div class="flex items-center justify-between gap-2">
            <button
              type="button"
              class="text-left text-sm font-medium text-slate-800 hover:text-emerald-700"
              @click="openRecipe(s.recipe.id)"
            >
              {{ s.recipe.title }}
            </button>
            <span class="shrink-0 rounded bg-emerald-50 px-2 py-0.5 text-xs font-medium text-emerald-800">
              {{ coveragePercent(s.coverage) }} Vorrat
            </span>
          </div>
          <p
            v-if="s.reason"
            class="mt-2 text-xs italic text-slate-600"
          >
            „{{ s.reason }}"
          </p>
          <p
            v-else
            class="mt-2 text-xs text-slate-400"
          >
            Aus Vorratsabdeckung berechnet (KI-Begruendung nicht verfuegbar).
          </p>
          <ul
            v-if="s.recipe.dietTags.length > 0"
            class="mt-2 flex flex-wrap gap-1"
          >
            <li
              v-for="tag in s.recipe.dietTags"
              :key="tag"
              class="rounded bg-slate-100 px-2 py-0.5 text-[0.65rem] font-medium text-slate-600"
            >
              {{ DIET_TAG_LABELS[tag as DietTag] }}
            </li>
          </ul>
          <button
            type="button"
            class="mt-3 w-full rounded border border-emerald-300 bg-white px-3 py-1.5 text-xs font-medium text-emerald-700 hover:bg-emerald-50"
            @click="startPlanAdd(s)"
          >
            Zum Wochenplan
          </button>
        </li>
      </ul>
    </div>

    <AddToMealPlanDialog
      v-if="planTarget && selected"
      :household-id="selected.id"
      :recipe-id="planTarget.recipe.id"
      :recipe-title="planTarget.recipe.title"
      :default-servings="planTarget.recipe.servings"
      @close="planTarget = null"
      @saved="onPlanSaved"
    />

    <!-- Backend-Status (Health-Check) -->
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
