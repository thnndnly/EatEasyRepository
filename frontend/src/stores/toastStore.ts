import { defineStore } from 'pinia'
import { ref } from 'vue'

export type ToastLevel = 'success' | 'error' | 'info'

export interface Toast {
  id: number
  level: ToastLevel
  message: string
}

const DEFAULT_DURATION_MS = 4000

/**
 * Globaler Toast-Stack. Komponenten rufen `success("...")` oder `error("...")`,
 * der Container in App.vue rendert und entfernt nach {@link DEFAULT_DURATION_MS}
 * automatisch. Fuer User-Eingabefehler (Form-Validierung) bleiben Inline-
 * Meldungen besser geeignet — Toasts sind fuer Aktionen-Bestaetigungen.
 */
export const useToastStore = defineStore('toast', () => {
  const toasts = ref<Toast[]>([])
  let nextId = 1

  function show(level: ToastLevel, message: string, durationMs = DEFAULT_DURATION_MS): void {
    const id = nextId++
    toasts.value = [...toasts.value, { id, level, message }]
    if (durationMs > 0) {
      setTimeout(() => dismiss(id), durationMs)
    }
  }

  function success(message: string): void {
    show('success', message)
  }

  function error(message: string): void {
    show('error', message)
  }

  function info(message: string): void {
    show('info', message)
  }

  function dismiss(id: number): void {
    toasts.value = toasts.value.filter((t) => t.id !== id)
  }

  return { toasts, show, success, error, info, dismiss }
})
