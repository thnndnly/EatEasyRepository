<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { BrowserMultiFormatReader, type IScannerControls } from '@zxing/browser'
import { useAuthStore } from '@/stores/authStore'
import { usePantryStore } from '@/stores/pantryStore'
import BaseModal from '@/components/common/BaseModal.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import type { BarcodeProductDto } from '@/types/barcode'
import { UNITS, UNIT_ABBREV, type Unit } from '@/types/units'
import type { PantryItemDto } from '@/types/pantry'

const emit = defineEmits<{
  added: [item: PantryItemDto]
  close: []
}>()

const props = defineProps<{
  open: boolean
  householdId: string
}>()

type Stage = 'scan' | 'manual' | 'confirm'

const authStore = useAuthStore()
const pantryStore = usePantryStore()

const stage = ref<Stage>('scan')
const video = ref<HTMLVideoElement | null>(null)
const manualCode = ref('')
const product = ref<BarcodeProductDto | null>(null)
const amount = ref<number>(1)
const unit = ref<Unit>('PIECE')
const bestBefore = ref<string>('')
const loading = ref(false)
const error = ref<string | null>(null)

let reader: BrowserMultiFormatReader | null = null
let controls: IScannerControls | null = null

async function startScanner(): Promise<void> {
  if (!video.value) {
    return
  }
  error.value = null
  reader = new BrowserMultiFormatReader()
  try {
    controls = await reader.decodeFromVideoDevice(
      undefined,
      video.value,
      (result, err) => {
        if (result) {
          const text = result.getText()
          stopScanner()
          void onDetected(text)
        }
        // err ist meist NotFoundException pro Frame, ignorieren.
        void err
      },
    )
  } catch (err: unknown) {
    // Kamera nicht verfuegbar (kein Geraet / kein Permission)
    stage.value = 'manual'
    error.value =
      err instanceof Error
        ? `Kamera nicht verfuegbar: ${err.message}`
        : 'Kamera nicht verfuegbar'
  }
}

function stopScanner(): void {
  controls?.stop()
  controls = null
  reader = null
}

async function onDetected(barcode: string): Promise<void> {
  if (!authStore.token) {
    return
  }
  loading.value = true
  error.value = null
  try {
    product.value = await pantryStore.lookupByBarcode(barcode)
    unit.value = product.value.suggestedUnit
    stage.value = 'confirm'
  } catch (err: unknown) {
    error.value =
      err instanceof Error ? err.message : 'Produkt konnte nicht geladen werden'
    stage.value = 'manual'
  } finally {
    loading.value = false
  }
}

function onManualSubmit(): void {
  const code = manualCode.value.trim()
  if (code.length === 0) {
    return
  }
  void onDetected(code)
}

async function onConfirm(): Promise<void> {
  if (!authStore.token || !product.value) {
    return
  }
  loading.value = true
  error.value = null
  try {
    // Server-State-Zugriff laeuft ueber den Store; der pflegt die Liste selbst.
    const item = await pantryStore.addByBarcode({
      barcode: product.value.barcode,
      amount: amount.value,
      unit: unit.value,
      bestBefore: bestBefore.value || null,
    })
    emit('added', item)
  } catch (err: unknown) {
    error.value =
      err instanceof Error ? err.message : 'Hinzufuegen fehlgeschlagen'
  } finally {
    loading.value = false
  }
}

function switchToManual(): void {
  stopScanner()
  stage.value = 'manual'
  error.value = null
}

watch(
  () => props.open,
  async (open) => {
    if (open) {
      stage.value = 'scan'
      error.value = null
      await nextTick()
      void startScanner()
    } else {
      stopScanner()
    }
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  stopScanner()
})
</script>

<template>
  <BaseModal :open="open" @close="emit('close')">
    <template #header>
      <h2 class="text-base font-semibold text-ink-900">Barcode scannen</h2>
    </template>

    <div class="space-y-4">
      <ErrorMessage :message="error ?? ''" />

      <!-- SCAN -->
      <template v-if="stage === 'scan'">
        <div class="relative overflow-hidden rounded border border-cream-300 bg-black">
          <video ref="video" class="aspect-video w-full" autoplay muted playsinline />
        </div>
        <p class="text-xs text-ink-500">
          Halte den Strichcode in die Kamera. Beleuchtung hilft.
        </p>
        <button
          type="button"
          class="ee-btn-secondary w-full"
          @click="switchToManual"
        >
          Stattdessen manuell eingeben
        </button>
      </template>

      <!-- MANUAL -->
      <template v-else-if="stage === 'manual'">
        <form class="space-y-2" @submit.prevent="onManualSubmit">
          <label for="manual-code" class="block text-sm font-medium text-ink-700">
            Barcode (EAN/UPC)
          </label>
          <input
            id="manual-code"
            v-model="manualCode"
            type="text"
            placeholder="z. B. 3017620422003"
            autocomplete="off"
            class="ee-input w-full"
          />
          <button
            type="submit"
            :disabled="loading"
            class="ee-btn-primary ee-btn-lg w-full"
          >
            {{ loading ? 'Lade ...' : 'Produkt suchen' }}
          </button>
        </form>
      </template>

      <!-- CONFIRM -->
      <template v-else-if="stage === 'confirm' && product">
        <div class="rounded border border-cream-200 bg-cream-50 px-3 py-2">
          <p class="text-xs uppercase tracking-wide text-ink-500">Gefunden</p>
          <p class="text-sm font-medium text-ink-900">{{ product.name }}</p>
          <p class="text-xs text-ink-500">Barcode: {{ product.barcode }}</p>
        </div>

        <form class="space-y-3" @submit.prevent="onConfirm">
          <div class="grid grid-cols-2 gap-2">
            <div>
              <label for="bc-amount" class="block text-xs font-medium text-ink-500">
                Menge
              </label>
              <input
                id="bc-amount"
                v-model.number="amount"
                type="number"
                step="0.01"
                min="0.01"
                required
                class="ee-input mt-1 w-full"
              />
            </div>
            <div>
              <label for="bc-unit" class="block text-xs font-medium text-ink-500">
                Einheit
              </label>
              <select
                id="bc-unit"
                v-model="unit"
                class="ee-input mt-1 w-full"
              >
                <option v-for="u in UNITS" :key="u" :value="u">{{ UNIT_ABBREV[u] }}</option>
              </select>
            </div>
          </div>

          <div>
            <label for="bc-bb" class="block text-xs font-medium text-ink-500">
              Mindesthaltbarkeit (optional)
            </label>
            <input
              id="bc-bb"
              v-model="bestBefore"
              type="date"
              class="ee-input mt-1 w-full"
            />
          </div>

          <button
            type="submit"
            :disabled="loading"
            class="ee-btn-primary ee-btn-lg w-full"
          >
            {{ loading ? 'Speichere ...' : 'In Vorrat uebernehmen' }}
          </button>
        </form>
      </template>
    </div>
  </BaseModal>
</template>
