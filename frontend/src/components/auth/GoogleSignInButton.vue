<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { FEATURE_GOOGLE_OAUTH, GOOGLE_CLIENT_ID } from '@/config/features'
import ErrorMessage from '@/components/common/ErrorMessage.vue'

const GIS_SRC = 'https://accounts.google.com/gsi/client'

const router = useRouter()
const authStore = useAuthStore()
const buttonEl = ref<HTMLDivElement | null>(null)
const error = ref<string | null>(null)

/** Laedt das GIS-Skript einmalig (idempotent) nach. */
function loadGisScript(): Promise<void> {
  return new Promise((resolve, reject) => {
    if (window.google?.accounts?.id) {
      resolve()
      return
    }
    const existing = document.querySelector<HTMLScriptElement>(`script[src="${GIS_SRC}"]`)
    if (existing) {
      existing.addEventListener('load', () => resolve())
      existing.addEventListener('error', () => reject(new Error('GIS-Skript-Fehler')))
      return
    }
    const script = document.createElement('script')
    script.src = GIS_SRC
    script.async = true
    script.defer = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('GIS-Skript-Fehler'))
    document.head.appendChild(script)
  })
}

async function onCredential(idToken: string): Promise<void> {
  error.value = null
  try {
    await authStore.loginWithGoogle(idToken)
    const redirect = router.currentRoute.value.query.redirect
    await router.replace(typeof redirect === 'string' ? redirect : '/')
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : 'Google-Login fehlgeschlagen'
  }
}

onMounted(async () => {
  if (!FEATURE_GOOGLE_OAUTH) {
    return
  }
  try {
    await loadGisScript()
    const gis = window.google?.accounts.id
    if (!gis || !buttonEl.value) {
      return
    }
    gis.initialize({
      client_id: GOOGLE_CLIENT_ID,
      callback: (response) => {
        void onCredential(response.credential)
      },
    })
    gis.renderButton(buttonEl.value, {
      theme: 'outline',
      size: 'large',
      text: 'continue_with',
      width: 320,
    })
  } catch {
    error.value = 'Google-Login ist gerade nicht verfuegbar.'
  }
})
</script>

<template>
  <div v-if="FEATURE_GOOGLE_OAUTH" class="space-y-3">
    <div class="flex items-center gap-3 text-xs text-ink-400">
      <span class="h-px flex-1 bg-cream-200"></span>
      <span>oder</span>
      <span class="h-px flex-1 bg-cream-200"></span>
    </div>
    <div class="flex justify-center">
      <div ref="buttonEl"></div>
    </div>
    <ErrorMessage :message="error ?? ''" />
  </div>
</template>
