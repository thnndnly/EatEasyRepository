import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { http, HttpResponse } from 'msw'
import { server } from '@/test/mocks/server'
import { TEST_HOUSEHOLD, TEST_TOKEN } from '@/test/fixtures'
import type { ReceiptScanResponse } from '@/types/receipt'
import { useAuthStore } from './authStore'
import { useReceiptStore } from './receiptStore'

const SCAN_URL = `/api/v1/households/${TEST_HOUSEHOLD.id}/receipts/scan`

const SCAN_RESPONSE: ReceiptScanResponse = {
  rawText: 'REWE\nTomaten 0,99\nMilch 1,19',
  items: [
    { name: 'Tomaten', amount: 500, unit: 'GRAM', ingredientId: null },
    { name: 'Milch', amount: 1000, unit: 'ML', ingredientId: 'a1b2c3d4-0000-0000-0000-000000000001' },
  ],
}

function testFile(): File {
  return new File(['fake-image-bytes'], 'bon.jpg', { type: 'image/jpeg' })
}

describe('receiptStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    useAuthStore().$patch({ token: TEST_TOKEN })
  })

  it('scan setzt result mit erkannten Items', async () => {
    server.use(http.post(SCAN_URL, () => HttpResponse.json(SCAN_RESPONSE)))
    const store = useReceiptStore()

    await store.scan(TEST_HOUSEHOLD.id, testFile())

    expect(store.result).toEqual(SCAN_RESPONSE)
    expect(store.error).toBeNull()
    expect(store.scanning).toBe(false)
  })

  it('scan setzt bei Fehler error und lässt result leer', async () => {
    server.use(
      http.post(SCAN_URL, () =>
        HttpResponse.json({ error: 'OCR-Dienst nicht erreichbar' }, { status: 503 }),
      ),
    )
    const store = useReceiptStore()

    await store.scan(TEST_HOUSEHOLD.id, testFile())

    expect(store.result).toBeNull()
    expect(store.error).toBe('OCR-Dienst nicht erreichbar')
    expect(store.scanning).toBe(false)
  })

  it('scan verwirft altes result vor erneutem Scan', async () => {
    server.use(http.post(SCAN_URL, () => HttpResponse.json(SCAN_RESPONSE)))
    const store = useReceiptStore()
    await store.scan(TEST_HOUSEHOLD.id, testFile())

    server.use(
      http.post(SCAN_URL, () => HttpResponse.json({ error: 'kaputt' }, { status: 500 })),
    )
    await store.scan(TEST_HOUSEHOLD.id, testFile())

    expect(store.result).toBeNull()
    expect(store.error).toBe('kaputt')
  })

  it('reset leert result, error und scanning', async () => {
    server.use(http.post(SCAN_URL, () => HttpResponse.json(SCAN_RESPONSE)))
    const store = useReceiptStore()
    await store.scan(TEST_HOUSEHOLD.id, testFile())

    store.reset()

    expect(store.result).toBeNull()
    expect(store.error).toBeNull()
    expect(store.scanning).toBe(false)
  })
})
