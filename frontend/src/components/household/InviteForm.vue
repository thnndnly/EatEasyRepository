<script setup lang="ts">
import { ref } from 'vue'
import { inviteMember } from '@/services/householdService'
import { useAuthStore } from '@/stores/authStore'
import type { InvitationDto } from '@/types/household'

interface Props {
  householdId: string
}

const props = defineProps<Props>()

const authStore = useAuthStore()

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
    email.value = ''
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : 'Einladung fehlgeschlagen'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="space-y-4 rounded-lg border border-slate-200 bg-white p-5">
    <div>
      <h3 class="text-base font-semibold text-slate-800">Mitglied einladen</h3>
      <p class="text-sm text-slate-500">
        Token-Link wird per E-Mail an die angegebene Adresse verschickt.
      </p>
    </div>

    <form class="flex flex-col gap-3 sm:flex-row sm:items-end" @submit.prevent="onSubmit">
      <div class="flex-1 space-y-1">
        <label for="invite-email" class="block text-sm font-medium text-slate-700">Email</label>
        <input
          id="invite-email"
          v-model="email"
          type="email"
          autocomplete="off"
          required
          class="w-full rounded border border-slate-300 px-3 py-2 focus:border-emerald-500 focus:outline-none"
        />
      </div>
      <button
        type="submit"
        :disabled="submitting"
        class="rounded bg-emerald-600 px-4 py-2 font-medium text-white hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-60"
      >
        {{ submitting ? 'Sende ...' : 'Einladen' }}
      </button>
    </form>

    <p v-if="error" class="rounded border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700">
      {{ error }}
    </p>

    <p
      v-if="lastInvitation"
      class="rounded border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-900"
    >
      Einladungsmail an <strong>{{ lastInvitation.email }}</strong> verschickt.
      Sie ist 7 Tage gueltig.
    </p>
  </div>
</template>
