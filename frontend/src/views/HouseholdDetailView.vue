<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as householdService from '@/services/householdService'
import { useAuthStore } from '@/stores/authStore'
import { useHouseholdStore } from '@/stores/householdStore'
import DietTagSelector from '@/components/common/DietTagSelector.vue'
import MemberList from '@/components/household/MemberList.vue'
import InviteForm from '@/components/household/InviteForm.vue'
import type { DietTag } from '@/types/dietTags'
import type { MemberDto } from '@/types/household'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const householdStore = useHouseholdStore()

const householdId = computed<string>(() => String(route.params.id))
const household = computed(() =>
  householdStore.households.find((h) => h.id === householdId.value) ?? null,
)

const members = ref<MemberDto[]>([])
const loadError = ref<string | null>(null)

const editName = ref('')
const editTags = ref<DietTag[]>([])
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
    members.value = await householdService.listMembers(authStore.token, householdId.value)
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
    })
  } catch (err: unknown) {
    saveError.value = err instanceof Error ? err.message : 'Speichern fehlgeschlagen'
  } finally {
    saving.value = false
  }
}

async function onRemoveMember(memberId: string): Promise<void> {
  if (!authStore.token) {
    return
  }
  try {
    await householdService.removeMember(authStore.token, householdId.value, memberId)
    members.value = members.value.filter((m) => m.userId !== memberId)
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
      class="text-sm font-medium text-emerald-700 hover:underline"
      @click="back"
    >
      &larr; Zurueck zur Liste
    </button>

    <p v-if="loadError" class="rounded border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700">
      {{ loadError }}
    </p>

    <template v-if="household">
      <header>
        <h1 class="text-2xl font-semibold">{{ household.name }}</h1>
        <p class="mt-1 text-sm text-slate-500">
          Rolle: {{ household.role === 'OWNER' ? 'Owner' : 'Mitglied' }}
        </p>
      </header>

      <form
        v-if="isOwner"
        class="space-y-4 rounded-lg border border-slate-200 bg-white p-5 shadow-sm"
        @submit.prevent="onSave"
      >
        <h2 class="text-base font-semibold text-slate-800">Stammdaten</h2>

        <div class="space-y-1">
          <label for="hh-edit-name" class="block text-sm font-medium text-slate-700">Name</label>
          <input
            id="hh-edit-name"
            v-model="editName"
            type="text"
            required
            maxlength="100"
            class="w-full rounded border border-slate-300 px-3 py-2 focus:border-emerald-500 focus:outline-none"
          />
        </div>

        <div class="space-y-2">
          <span class="block text-sm font-medium text-slate-700">Standard-Diaeten</span>
          <DietTagSelector v-model="editTags" />
        </div>

        <p v-if="saveError" class="rounded border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700">
          {{ saveError }}
        </p>

        <button
          type="submit"
          :disabled="saving"
          class="rounded bg-emerald-600 px-4 py-2 font-medium text-white hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {{ saving ? 'Speichere ...' : 'Speichern' }}
        </button>
      </form>

      <section class="space-y-3">
        <h2 class="text-base font-semibold text-slate-800">Mitglieder</h2>
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

    <p v-else-if="!loadError" class="text-slate-500">Lade ...</p>
  </section>
</template>
