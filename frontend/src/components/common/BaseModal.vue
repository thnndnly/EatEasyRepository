<script setup lang="ts">
import { watch } from 'vue'
import { useEventListener, useScrollLock } from '@vueuse/core'

interface Props {
  open: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'close'): void
}>()

// Esc schliesst das Modal. VueUse räumt den Listener automatisch beim
// Unmount auf — kein manuelles onUnmounted nötig.
useEventListener(document, 'keydown', (event: KeyboardEvent) => {
  if (event.key === 'Escape' && props.open) {
    emit('close')
  }
})

// Body-Scroll sperren, solange das Modal offen ist.
const isBodyScrollLocked = useScrollLock(document.body)
watch(() => props.open, (next) => {
  isBodyScrollLocked.value = next
}, { immediate: true })

function onBackdropClick(): void {
  emit('close')
}
</script>

<template>
  <div
    v-if="open"
    class="fixed inset-0 z-50 flex items-center justify-center bg-ink-900/40 p-4"
    role="dialog"
    aria-modal="true"
    @click.self="onBackdropClick"
  >
    <div
      class="flex max-h-[90vh] w-full max-w-2xl flex-col overflow-hidden rounded-2xl bg-white shadow"
    >
      <header class="flex items-center justify-between gap-4 border-b border-cream-200 px-5 py-3">
        <div class="min-w-0 flex-1">
          <slot name="header" />
        </div>
        <button
          type="button"
          class="ee-btn-ghost ee-btn-sm"
          aria-label="Schliessen"
          @click="emit('close')"
        >
          ✕
        </button>
      </header>
      <div class="flex-1 overflow-y-auto p-5">
        <slot />
      </div>
    </div>
  </div>
</template>
