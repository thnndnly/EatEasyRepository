<script setup lang="ts">
import { computed, ref } from 'vue'
import { useHouseholdStore } from '@/stores/householdStore'
import { useToastStore } from '@/stores/toastStore'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import type { InvitationDto } from '@/types/household'

interface Props {
  householdId: string
}

const props = defineProps<Props>()

const householdStore = useHouseholdStore()
const toastStore = useToastStore()

const email = ref('')
const submitting = ref(false)
const error = ref<string | null>(null)
const lastInvitation = ref<InvitationDto | null>(null)

// Direkter Beitritts-Link als Fallback, falls die Einladungs-Mail nicht ankommt
// (z. B. wenn der Mailversand in der Demo-/Dev-Umgebung gemockt ist). Zeigt auf
// die Accept-Seite dieser Frontend-Instanz; der Token steht im Query-Parameter.
const acceptUrl = computed<string>(() =>
  lastInvitation.value
    ? `${window.location.origin}/invitations/accept?token=${lastInvitation.value.token}`
    : '',
)

async function onSubmit(): Promise<void> {
  error.value = null
  submitting.value = true
  try {
    const created = await householdStore.invite(props.householdId, email.value.trim())
    lastInvitation.value = created
    toastStore.success(`Einladung für ${created.email} erstellt`)
    email.value = ''
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : 'Einladung fehlgeschlagen'
  } finally {
    submitting.value = false
  }
}

async function copyLink(): Promise<void> {
  if (!acceptUrl.value) {
    return
  }
  try {
    await navigator.clipboard.writeText(acceptUrl.value)
    toastStore.success('Beitritts-Link kopiert')
  } catch {
    // Clipboard-API nicht verfügbar (z. B. kein HTTPS) — der Link steht sichtbar
    // im Feld und kann manuell markiert werden.
    toastStore.info('Link markieren und manuell kopieren')
  }
}
</script>

<template>
  <div class="space-y-4 rounded-lg border border-cream-200 bg-white p-5">
    <div>
      <h3 class="text-base font-semibold text-ink-900">Mitglied einladen</h3>
      <p class="text-sm text-ink-500">
        Die eingeladene Person bekommt einen Beitritts-Link per E-Mail. Kommt keine
        Mail an, kannst du den Link unten direkt weitergeben.
      </p>
    </div>

    <form class="flex flex-col gap-3 sm:flex-row sm:items-end" @submit.prevent="onSubmit">
      <div class="flex-1 space-y-1">
        <label for="invite-email" class="block text-sm font-medium text-ink-700">Email</label>
        <input
          id="invite-email"
          v-model="email"
          type="email"
          autocomplete="off"
          required
          class="ee-input w-full"
        />
      </div>
      <button
        type="submit"
        :disabled="submitting"
        class="ee-btn-primary"
      >
        {{ submitting ? 'Sende ...' : 'Einladen' }}
      </button>
    </form>

    <ErrorMessage :message="error ?? ''" />

    <div
      v-if="lastInvitation"
      class="space-y-2 rounded-2xl border border-mint-200 bg-mint-50 px-4 py-3 text-sm text-mint-800"
    >
      <p>
        Einladung für <strong>{{ lastInvitation.email }}</strong> erstellt — 7 Tage gültig.
      </p>
      <p class="text-mint-700">
        Falls keine E-Mail ankommt, teile diesen Beitritts-Link direkt mit der Person:
      </p>
      <div class="flex items-center gap-2">
        <input
          :value="acceptUrl"
          readonly
          aria-label="Beitritts-Link"
          class="ee-input w-full text-xs"
          @focus="(event) => (event.target as HTMLInputElement).select()"
        />
        <button
          type="button"
          class="ee-btn-secondary ee-btn-sm shrink-0"
          @click="copyLink"
        >
          Kopieren
        </button>
      </div>
    </div>
  </div>
</template>
