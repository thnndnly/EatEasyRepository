<script setup lang="ts">
import type { MemberDto } from '@/types/household'

interface Props {
  members: MemberDto[]
  canManage: boolean
  currentUserId: string | null
}

defineProps<Props>()
const emit = defineEmits<{ remove: [memberId: string] }>()

function onRemove(memberId: string): void {
  if (confirm('Mitglied wirklich entfernen?')) {
    emit('remove', memberId)
  }
}
</script>

<template>
  <ul class="divide-y divide-cream-200 rounded-lg border border-cream-200 bg-white">
    <li
      v-for="member in members"
      :key="member.userId"
      class="flex items-center justify-between gap-4 px-4 py-3"
    >
      <div>
        <p class="font-medium text-ink-900">{{ member.displayName }}</p>
        <p class="text-sm text-ink-500">{{ member.email }}</p>
      </div>
      <div class="flex items-center gap-3 text-sm">
        <span
          class="rounded px-2 py-0.5 font-medium"
          :class="
            member.role === 'OWNER'
              ? 'bg-butter-100 text-butter-700'
              : 'bg-cream-200 text-ink-700'
          "
        >
          {{ member.role === 'OWNER' ? 'Owner' : 'Mitglied' }}
        </span>
        <button
          v-if="canManage && member.userId !== currentUserId"
          type="button"
          class="rounded border border-rose-200 bg-white px-2 py-1 text-xs font-medium text-rose-700 hover:bg-rose-100"
          @click="onRemove(member.userId)"
        >
          Entfernen
        </button>
      </div>
    </li>
    <li v-if="members.length === 0" class="px-4 py-3 text-sm text-ink-500">
      Noch keine Mitglieder.
    </li>
  </ul>
</template>
