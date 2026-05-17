<script setup lang="ts">
import { useToastStore } from '@/stores/toastStore'

const toastStore = useToastStore()

const STYLES = {
  success: 'border-mint-200 bg-mint-50 text-mint-700',
  error: 'border-rose-200 bg-rose-100 text-rose-700',
  info: 'border-lavender-200 bg-lavender-100 text-lavender-700',
} as const

const ICONS = {
  success: '✓',
  error: '✕',
  info: 'i',
} as const

const ICON_BG = {
  success: 'bg-mint-200 text-mint-700',
  error: 'bg-rose-200 text-rose-700',
  info: 'bg-lavender-200 text-lavender-700',
} as const
</script>

<template>
  <div
    class="pointer-events-none fixed bottom-4 right-4 z-50 flex flex-col gap-2"
    aria-live="polite"
    aria-atomic="false"
  >
    <transition-group name="toast" tag="div" class="flex flex-col gap-2">
      <div
        v-for="toast in toastStore.toasts"
        :key="toast.id"
        class="pointer-events-auto flex max-w-sm items-start gap-3 rounded-2xl border px-4 py-3 text-sm shadow-[0_8px_24px_-8px_rgba(45,42,50,0.18)] backdrop-blur"
        :class="STYLES[toast.level]"
        role="status"
      >
        <span
          class="flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full text-xs font-bold"
          :class="ICON_BG[toast.level]"
        >
          {{ ICONS[toast.level] }}
        </span>
        <span class="flex-1 font-medium leading-snug">{{ toast.message }}</span>
        <button
          type="button"
          class="text-ink-400 transition-colors hover:text-ink-700"
          aria-label="Schliessen"
          @click="toastStore.dismiss(toast.id)"
        >
          ✕
        </button>
      </div>
    </transition-group>
  </div>
</template>

<style scoped>
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
</style>
