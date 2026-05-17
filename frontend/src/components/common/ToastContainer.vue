<script setup lang="ts">
import { useToastStore } from '@/stores/toastStore'

const toastStore = useToastStore()

const STYLES = {
  success: 'border-emerald-300 bg-emerald-50 text-emerald-900',
  error: 'border-red-300 bg-red-50 text-red-900',
  info: 'border-slate-300 bg-white text-slate-800',
} as const

const ICONS = {
  success: '✓',
  error: '✕',
  info: 'i',
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
        class="pointer-events-auto flex max-w-sm items-start gap-3 rounded-lg border px-4 py-3 text-sm shadow-md"
        :class="STYLES[toast.level]"
        role="status"
      >
        <span
          class="flex h-5 w-5 flex-shrink-0 items-center justify-center rounded-full text-xs font-bold"
          :class="{
            'bg-emerald-200 text-emerald-800': toast.level === 'success',
            'bg-red-200 text-red-800': toast.level === 'error',
            'bg-slate-200 text-slate-700': toast.level === 'info',
          }"
        >
          {{ ICONS[toast.level] }}
        </span>
        <span class="flex-1 leading-snug">{{ toast.message }}</span>
        <button
          type="button"
          class="text-slate-400 hover:text-slate-700"
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
  transform: translateX(20px);
}
.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
</style>
