<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { BrowserMultiFormatReader, type IScannerControls } from '@zxing/browser'
import * as barcodeService from '@/services/barcodeService'
import { useAuthStore } from '@/stores/authStore'
import { usePantryStore } from '@/stores/pantryStore'
import type { BarcodeProductDto } from '@/types/barcode'
import type { Unit } from '@/types/units'
import type { PantryItemDto } from '@/types/pantry'

const emit = defineEmits<{
  added: [item: PantryItemDto]
  close: []
}>()

const props = defineProps<{
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
    product.value = await barcodeService.lookupBarcode(authStore.token, barcode)
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
    const item = await barcodeService.addPantryItemByBarcode(
      authStore.token,
      props.householdId,
      {
        barcode: product.value.barcode,
        amount: amount.value,
        unit: unit.value,
        bestBefore: bestBefore.value || null,
      },
    )
    // Pantry-Store aktualisieren, damit die Liste den neuen Eintrag sofort zeigt.
    await pantryStore.load(props.householdId)
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

onMounted(() => {
  void startScanner()
})

onBeforeUnmount(() => {
  stopScanner()
})
</script>

<template>
  <div
    class="fixed inset-0 z-30 flex items-center justify-center bg-ink-900/40 px-4"
    @click.self="emit('close')"
  >
    <div class="w-full max-w-md rounded-2xl bg-white shadow-[0_20px_60px_-15px_rgba(45,42,50,0.3)]">
      <header class="flex items-center justify-between border-b border-cream-200 px-5 py-4">
        <h2 class="text-base font-semibold text-ink-900">Barcode scannen</h2>
        <button
          type="button"
          class="rounded text-ink-400 hover:text-ink-700"
          @click="emit('close')"
        >
          ✕
        </button>
      </header>

      <div class="space-y-4 px-5 py-4">
        <p
          v-if="error"
          class="rounded-2xl border border-rose-200 bg-rose-100 px-3 py-2 text-sm font-medium text-rose-700"
        >
          {{ error }}
        </p>

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
              class="w-full rounded border border-cream-300 px-3 py-2 focus:border-peach-400 focus:outline-none"
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
                  class="mt-1 w-full rounded border border-cream-300 px-3 py-2 focus:border-peach-400 focus:outline-none"
                />
              </div>
              <div>
                <label for="bc-unit" class="block text-xs font-medium text-ink-500">
                  Einheit
                </label>
                <select
                  id="bc-unit"
                  v-model="unit"
                  class="mt-1 w-full rounded border border-cream-300 px-3 py-2 focus:border-peach-400 focus:outline-none"
                >
                  <option value="GRAM">g</option>
                  <option value="ML">ml</option>
                  <option value="PIECE">Stueck</option>
                  <option value="TBSP">EL</option>
                  <option value="TSP">TL</option>
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
                class="mt-1 w-full rounded border border-cream-300 px-3 py-2 focus:border-peach-400 focus:outline-none"
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
    </div>
  </div>
</template>
