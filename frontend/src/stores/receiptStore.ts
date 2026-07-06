import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as receiptService from '@/services/receiptService'
import { useRequireToken } from '@/composables/useRequireToken'
import type { ReceiptScanResponse } from '@/types/receipt'

export const useReceiptStore = defineStore('receipt', () => {
  const result = ref<ReceiptScanResponse | null>(null)
  const scanning = ref(false)
  const error = ref<string | null>(null)

  const requireToken = useRequireToken()

  async function scan(householdId: string, file: File): Promise<void> {
    scanning.value = true
    error.value = null
    result.value = null
    try {
      result.value = await receiptService.scanReceipt(requireToken(), householdId, file)
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Scan fehlgeschlagen'
    } finally {
      scanning.value = false
    }
  }

  function reset(): void {
    result.value = null
    error.value = null
    scanning.value = false
  }

  return { result, scanning, error, scan, reset }
})
