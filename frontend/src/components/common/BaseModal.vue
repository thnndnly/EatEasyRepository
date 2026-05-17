<script setup lang="ts">
import { onMounted, onUnmounted, watch } from 'vue'

interface Props {
  open: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'close'): void
}>()

function onKeyDown(event: KeyboardEvent): void {
  if (event.key === 'Escape' && props.open) {
    emit('close')
  }
}

onMounted(() => {
  document.addEventListener('keydown', onKeyDown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', onKeyDown)
})

// Body-Scroll sperren, solange Modal offen.
watch(
  () => props.open,
  (next) => {
    if (typeof document === 'undefined') return
    document.body.style.overflow = next ? 'hidden' : ''
  },
  { immediate: true },
)

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
