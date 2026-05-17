<script setup lang="ts">
import { ref } from 'vue'
import { inviteMember } from '@/services/householdService'
import { useAuthStore } from '@/stores/authStore'
import { useToastStore } from '@/stores/toastStore'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import type { InvitationDto } from '@/types/household'

interface Props {
  householdId: string
}

const props = defineProps<Props>()

const authStore = useAuthStore()
const toastStore = useToastStore()

const email = ref('')
const submitting = ref(false)
const error = ref<string | null>(null)
const lastInvitation = ref<InvitationDto | null>(null)

async function onSubmit(): Promise<void> {
  if (!authStore.token) {
    error.value = 'Nicht eingeloggt'
    return
  }
  error.value = null
  submitting.value = true
  try {
    lastInvitation.value = await inviteMember(authStore.token, props.householdId, {
      email: email.value.trim(),
    })
    toastStore.success(`Einladung an ${lastInvitation.value.email} verschickt`)
    email.value = ''
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : 'Einladung fehlgeschlagen'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="space-y-4 rounded-lg border border-cream-200 bg-white p-5">
    <div>
      <h3 class="text-base font-semibold text-ink-900">Mitglied einladen</h3>
      <p class="text-sm text-ink-500">
        Token-Link wird per E-Mail an die angegebene Adresse verschickt.
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

    <p
      v-if="lastInvitation"
      class="rounded-2xl border border-mint-200 bg-mint-50 px-4 py-3 text-sm font-medium text-mint-700"
    >
      Einladungsmail an <strong>{{ lastInvitation.email }}</strong> verschickt.
      Sie ist 7 Tage gueltig.
    </p>
  </div>
</template>
