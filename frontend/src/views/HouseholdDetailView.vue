<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { useHouseholdStore } from '@/stores/householdStore'
import DietTagSelector from '@/components/common/DietTagSelector.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import MemberList from '@/components/household/MemberList.vue'
import InviteForm from '@/components/household/InviteForm.vue'
import type { DietTag } from '@/types/dietTags'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const householdStore = useHouseholdStore()

const householdId = computed<string>(() => String(route.params.id))
const household = computed(() =>
  householdStore.households.find((h) => h.id === householdId.value) ?? null,
)

const members = computed(() => householdStore.membersOf(householdId.value))
const loadError = ref<string | null>(null)

const editName = ref('')
const editTags = ref<DietTag[]>([])
const editAutoRestock = ref(true)
const saving = ref(false)
const saveError = ref<string | null>(null)

const isOwner = computed(() => household.value?.role === 'OWNER')

async function loadAll(): Promise<void> {
  loadError.value = null
  if (!authStore.token) {
    return
  }
  try {
    if (householdStore.households.length === 0) {
      await householdStore.load()
    }
    if (!household.value) {
      await householdStore.refreshOne(householdId.value)
    }
    await householdStore.loadMembers(householdId.value)
  } catch (err: unknown) {
    loadError.value = err instanceof Error ? err.message : 'Laden fehlgeschlagen'
  }
}

watch(
  () => household.value,
  (next) => {
    if (next) {
      editName.value = next.name
      editTags.value = [...next.defaultDietTags]
      editAutoRestock.value = next.autoRestockEnabled
    }
  },
  { immediate: true },
)

onMounted(async () => {
  await loadAll()
  householdStore.selectHousehold(householdId.value)
})

watch(householdId, async () => {
  await loadAll()
  householdStore.selectHousehold(householdId.value)
})

async function onSave(): Promise<void> {
  saveError.value = null
  saving.value = true
  try {
    await householdStore.update(householdId.value, {
      name: editName.value.trim(),
      defaultDietTags: editTags.value,
      autoRestockEnabled: editAutoRestock.value,
    })
  } catch (err: unknown) {
    saveError.value = err instanceof Error ? err.message : 'Speichern fehlgeschlagen'
  } finally {
    saving.value = false
  }
}

async function onRemoveMember(memberId: string): Promise<void> {
  try {
    await householdStore.removeMember(householdId.value, memberId)
  } catch (err: unknown) {
    loadError.value = err instanceof Error ? err.message : 'Entfernen fehlgeschlagen'
  }
}

function back(): void {
  void router.push({ name: 'households' })
}
</script>

<template>
  <section class="space-y-6">
    <button
      type="button"
      class="text-sm font-medium text-peach-600 hover:underline"
      @click="back"
    >
      &larr; Zurück zur Liste
    </button>

    <ErrorMessage :message="loadError ?? ''" />

    <template v-if="household">
      <header>
        <h1 class="text-2xl font-semibold">{{ household.name }}</h1>
        <p class="mt-1 text-sm text-ink-500">
          Rolle: {{ household.role === 'OWNER' ? 'Owner' : 'Mitglied' }}
        </p>
      </header>

      <form
        v-if="isOwner"
        class="space-y-4 rounded-lg border border-cream-200 bg-white p-5 shadow-sm"
        @submit.prevent="onSave"
      >
        <h2 class="text-base font-semibold text-ink-900">Stammdaten</h2>

        <div class="space-y-1">
          <label for="hh-edit-name" class="block text-sm font-medium text-ink-700">Name</label>
          <input
            id="hh-edit-name"
            v-model="editName"
            type="text"
            required
            maxlength="100"
            class="ee-input w-full"
          />
        </div>

        <div class="space-y-2">
          <span class="block text-sm font-medium text-ink-700">Standard-Diäten</span>
          <DietTagSelector v-model="editTags" />
        </div>

        <label class="flex items-start gap-3">
          <input
            id="hh-edit-auto-restock"
            v-model="editAutoRestock"
            type="checkbox"
            class="mt-1 h-4 w-4 rounded border-cream-300 text-peach-600"
          />
          <span class="text-sm">
            <span class="block font-medium text-ink-700">Auto-Nachbuchen</span>
            <span class="block text-ink-500">
              Abgehakte Einkaufslisten-Artikel automatisch in den Vorrat übernehmen.
            </span>
          </span>
        </label>

        <ErrorMessage :message="saveError ?? ''" />

        <button
          type="submit"
          :disabled="saving"
          class="ee-btn-primary"
        >
          {{ saving ? 'Speichere ...' : 'Speichern' }}
        </button>
      </form>

      <section class="space-y-3">
        <h2 class="text-base font-semibold text-ink-900">Mitglieder</h2>
        <MemberList
          :members="members"
          :can-manage="isOwner"
          :current-user-id="authStore.user?.id ?? null"
          @remove="onRemoveMember"
        />
      </section>

      <section v-if="isOwner">
        <InviteForm :household-id="householdId" />
      </section>
    </template>

    <p v-else-if="!loadError" class="text-ink-500">Lade ...</p>
  </section>
</template>
