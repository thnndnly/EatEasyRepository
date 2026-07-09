<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { fetchHealth } from '@/services/healthService'
import { useAuthStore } from '@/stores/authStore'
import { useHouseholdStore } from '@/stores/householdStore'
import { usePantryStore } from '@/stores/pantryStore'
import { useToastStore } from '@/stores/toastStore'
import { useSuggestionStore } from '@/stores/suggestionStore'
import { DIET_TAG_LABELS, type DietTag } from '@/types/dietTags'
import type { SuggestionDto } from '@/types/suggestion'
import AddToMealPlanDialog from '@/components/suggestion/AddToMealPlanDialog.vue'
import ExpiringSoonWidget from '@/components/pantry/ExpiringSoonWidget.vue'

const router = useRouter()
const authStore = useAuthStore()
const householdStore = useHouseholdStore()
const pantryStore = usePantryStore()
const toastStore = useToastStore()
const suggestionStore = useSuggestionStore()

const status = ref<'loading' | 'ok' | 'error'>('loading')
const detail = ref<string>('Prüfe Backend ...')

const planTarget = ref<SuggestionDto | null>(null)

const selected = computed(() => householdStore.selected)

async function refresh(): Promise<void> {
  status.value = 'loading'
  detail.value = 'Prüfe Backend ...'
  try {
    // Bewusste Ausnahme von "API nur über Stores": der Health-Check ist ein
    // reiner Infrastruktur-Ping (kein Domänen-State), der nirgends geteilt wird.
    const health = await fetchHealth()
    status.value = health.status === 'ok' ? 'ok' : 'error'
    detail.value = `Backend status: ${health.status}`
  } catch (error: unknown) {
    status.value = 'error'
    detail.value = error instanceof Error ? error.message : 'Unbekannter Fehler'
  }
}

async function onSuggest(): Promise<void> {
  if (!selected.value) {
    return
  }
  try {
    await suggestionStore.fetch(selected.value.id, { numSuggestions: 3 })
  } catch {
    // suggestionStore.error ist bereits gesetzt.
  }
}

function openRecipe(id: string): void {
  void router.push({ name: 'recipe-detail', params: { id } })
}

function startPlanAdd(suggestion: SuggestionDto): void {
  planTarget.value = suggestion
}

function onPlanSaved(): void {
  if (planTarget.value) {
    toastStore.success(`"${planTarget.value.recipe.title}" in den Wochenplan übernommen`)
  }
  planTarget.value = null
}

function coveragePercent(value: number): string {
  return `${Math.round(value * 100)} %`
}

onMounted(async () => {
  // Health-Check und Haushaltsliste sind unabhängig — parallel laden
  // spart eine Round-Trip-Wartezeit beim Dashboard-Mount.
  await Promise.all([refresh(), householdStore.load()])
})

// Vorrat für das MHD-Widget nachladen, sobald ein Haushalt feststeht
// (initial und beim Wechsel über die Topbar).
watch(
  () => selected.value?.id,
  async (id) => {
    if (id) {
      await pantryStore.load(id)
    } else {
      pantryStore.reset()
    }
  },
  { immediate: true },
)
</script>

<template>
  <section class="space-y-8">
    <div class="rounded-3xl border border-cream-200 bg-gradient-to-br from-peach-100 via-cream-50 to-butter-100 px-7 py-8 shadow-[0_2px_18px_rgba(255,181,167,0.18)]">
      <p class="text-xs font-semibold uppercase tracking-widest text-peach-700">Willkommen zurück</p>
      <h1 class="mt-1 text-3xl font-extrabold tracking-tight text-ink-900">
        Hallo, {{ authStore.user?.displayName ?? 'Gast' }} 👋
      </h1>
      <p class="mt-2 max-w-xl text-ink-700">
        Plane Mahlzeiten, behalte Vorrat und Rezepte im Blick — und lass dir den nächsten Wocheneinkauf zusammenstellen.
      </p>
    </div>

    <!-- Smart-Suggestion -->
    <div class="ee-card">
      <div class="flex items-start justify-between gap-4">
        <div>
          <h2 class="flex items-center gap-2 text-lg font-bold text-ink-900">
            <span>🥘</span> Was kann ich kochen?
          </h2>
          <p class="mt-1 text-sm text-ink-500">
            {{ selected ? `Vorschläge aus dem Vorrat von ${selected.name}.` : 'Wähle zuerst einen Haushalt.' }}
          </p>
        </div>
        <button
          type="button"
          class="ee-btn-primary ee-btn-lg"
          :disabled="!selected || suggestionStore.loading"
          @click="onSuggest"
        >
          {{ suggestionStore.loading ? 'Denke nach ...' : 'Vorschläge holen' }}
        </button>
      </div>

      <p
        v-if="suggestionStore.error"
        class="mt-4 rounded-2xl border border-rose-200 bg-rose-100 px-4 py-3 text-sm font-medium text-rose-700"
      >
        {{ suggestionStore.error }}
      </p>

      <p
        v-else-if="suggestionStore.requested && !suggestionStore.loading && suggestionStore.suggestions.length === 0"
        class="mt-4 rounded-2xl border border-dashed border-cream-300 bg-cream-50 px-4 py-3 text-sm text-ink-500"
      >
        Keine passenden Rezepte gefunden. Mehr Vorrat anlegen oder Rezepte hinzufügen.
      </p>

      <p
        v-if="suggestionStore.suggestions.length > 0 && !suggestionStore.aiAvailable"
        class="mt-4 rounded-2xl border border-butter-200 bg-butter-100/60 px-4 py-3 text-sm text-ink-600"
      >
        ⚠️ KI-Begründungen gerade nicht verfügbar — sortiert nach Vorrats-Abdeckung.
      </p>

      <ul v-if="suggestionStore.suggestions.length > 0" class="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        <li
          v-for="s in suggestionStore.suggestions"
          :key="s.recipe.id"
          class="group rounded-2xl border border-cream-200 bg-cream-50 p-4 transition-all hover:-translate-y-0.5 hover:border-peach-200 hover:bg-white hover:shadow-[0_8px_24px_-12px_rgba(255,154,133,0.4)]"
        >
          <div class="flex items-start justify-between gap-2">
            <button
              type="button"
              class="text-left text-sm font-semibold text-ink-900 hover:text-peach-600"
              @click="openRecipe(s.recipe.id)"
            >
              {{ s.recipe.title }}
            </button>
            <span class="ee-chip-mint shrink-0">
              {{ coveragePercent(s.coverage) }}
            </span>
          </div>
          <p v-if="s.reason" class="mt-2 text-xs italic leading-snug text-ink-700">
            „{{ s.reason }}"
          </p>
          <p v-else class="mt-2 text-xs text-ink-400">
            Aus Vorratsabdeckung berechnet.
          </p>
          <ul
            v-if="s.recipe.dietTags.length > 0"
            class="mt-3 flex flex-wrap gap-1"
          >
            <li
              v-for="tag in s.recipe.dietTags"
              :key="tag"
              class="ee-chip-neutral"
            >
              {{ DIET_TAG_LABELS[tag as DietTag] }}
            </li>
          </ul>
          <button
            type="button"
            class="ee-btn-secondary ee-btn-sm mt-3 w-full"
            @click="startPlanAdd(s)"
          >
            Zum Wochenplan
          </button>
        </li>
      </ul>
    </div>

    <AddToMealPlanDialog
      v-if="planTarget && selected"
      :open="planTarget !== null && selected !== null"
      :household-id="selected.id"
      :recipe-id="planTarget.recipe.id"
      :recipe-title="planTarget.recipe.title"
      :default-servings="planTarget.recipe.servings"
      @close="planTarget = null"
      @saved="onPlanSaved"
    />

    <!-- MHD-Widget: demnächst ablaufender Vorrat -->
    <ExpiringSoonWidget v-if="selected" />

    <!-- Backend-Status (Health-Check) -->
    <div class="ee-card">
      <div class="flex items-center justify-between gap-4">
        <div class="flex items-center gap-3">
          <span
            class="flex h-10 w-10 items-center justify-center rounded-2xl text-base font-bold"
            :class="{
              'bg-cream-200 text-ink-500': status === 'loading',
              'bg-mint-100 text-mint-700': status === 'ok',
              'bg-rose-100 text-rose-700': status === 'error',
            }"
          >
            {{ status === 'ok' ? '✓' : status === 'error' ? '✕' : '…' }}
          </span>
          <div>
            <p class="text-xs font-semibold uppercase tracking-widest text-ink-500">Backend-Status</p>
            <p class="text-sm font-semibold text-ink-900">{{ detail }}</p>
          </div>
        </div>
        <button type="button" class="ee-btn-secondary" @click="refresh">
          Erneut prüfen
        </button>
      </div>
    </div>
  </section>
</template>
