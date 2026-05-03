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
  <ul class="divide-y divide-slate-200 rounded-lg border border-slate-200 bg-white">
    <li
      v-for="member in members"
      :key="member.userId"
      class="flex items-center justify-between gap-4 px-4 py-3"
    >
      <div>
        <p class="font-medium text-slate-800">{{ member.displayName }}</p>
        <p class="text-sm text-slate-500">{{ member.email }}</p>
      </div>
      <div class="flex items-center gap-3 text-sm">
        <span
          class="rounded px-2 py-0.5 font-medium"
          :class="
            member.role === 'OWNER'
              ? 'bg-amber-100 text-amber-800'
              : 'bg-slate-100 text-slate-700'
          "
        >
          {{ member.role === 'OWNER' ? 'Owner' : 'Mitglied' }}
        </span>
        <button
          v-if="canManage && member.userId !== currentUserId"
          type="button"
          class="rounded border border-red-300 bg-white px-2 py-1 text-xs font-medium text-red-700 hover:bg-red-50"
          @click="onRemove(member.userId)"
        >
          Entfernen
        </button>
      </div>
    </li>
    <li v-if="members.length === 0" class="px-4 py-3 text-sm text-slate-500">
      Noch keine Mitglieder.
    </li>
  </ul>
</template>
