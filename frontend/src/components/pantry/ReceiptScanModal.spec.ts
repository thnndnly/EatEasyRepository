import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ReceiptScanModal from './ReceiptScanModal.vue'
import { usePantryStore } from '@/stores/pantryStore'
import { useReceiptStore } from '@/stores/receiptStore'
import { TEST_HOUSEHOLD } from '@/test/fixtures'
import type { PantryItemDto } from '@/types/pantry'

const SCAN_RESULT = {
  rawText: 'REWE\nMilch 1,19\nBanane 0,89',
  items: [
    {
      name: 'Milch',
      amount: 1000,
      unit: 'ML' as const,
      ingredientId: 'a1b2c3d4-0000-0000-0000-000000000001',
    },
    { name: 'Banane', amount: 3, unit: 'PIECE' as const, ingredientId: null },
  ],
}

function mountModal() {
  const receiptStore = useReceiptStore()
  const pantryStore = usePantryStore()
  const wrapper = mount(ReceiptScanModal, {
    props: { open: true, householdId: TEST_HOUSEHOLD.id },
  })
  return { wrapper, receiptStore, pantryStore }
}

async function showPreview(receiptStore: ReturnType<typeof useReceiptStore>) {
  receiptStore.result = SCAN_RESULT
  await flushPromises()
}

describe('ReceiptScanModal', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('Namensaenderung an gematchter Zutat entfernt die ingredientId (Chip wird "neu")', async () => {
    const { wrapper, receiptStore, pantryStore } = mountModal()
    await showPreview(receiptStore)
    const addItem = vi
      .spyOn(pantryStore, 'addItem')
      .mockResolvedValue({} as PantryItemDto)

    // Erste Zeile ist "Milch" mit Match — Name editieren.
    await wrapper.find('input[type="text"]').setValue('Hafermilch')
    expect(wrapper.html()).not.toContain('bekannt')

    // Nur die erste Zeile uebernehmen.
    const checkboxes = wrapper.findAll('input[type="checkbox"]')
    await checkboxes[1]!.setValue(false)
    await wrapper.find('button.ee-btn-primary').trigger('click')
    await flushPromises()

    expect(addItem).toHaveBeenCalledWith(
      expect.objectContaining({ ingredientId: null, ingredientName: 'Hafermilch' }),
    )
  })

  it('Rueckaenderung auf den Original-Namen stellt den Match wieder her', async () => {
    const { wrapper, receiptStore } = mountModal()
    await showPreview(receiptStore)

    const nameInput = wrapper.find('input[type="text"]')
    await nameInput.setValue('Hafermilch')
    expect(wrapper.html()).not.toContain('bekannt')

    await nameInput.setValue('milch')
    expect(wrapper.html()).toContain('bekannt')
  })

  it('Teilfehler: Fehler erscheint im Modal, uebernommene Posten verschwinden, kein added-Emit', async () => {
    const { wrapper, receiptStore, pantryStore } = mountModal()
    await showPreview(receiptStore)
    vi.spyOn(pantryStore, 'addItem')
      .mockResolvedValueOnce({} as PantryItemDto)
      .mockRejectedValueOnce(new Error('Netzwerkfehler'))

    await wrapper.find('button.ee-btn-primary').trigger('click')
    await flushPromises()

    // Posten 1 (Milch) ist durch, Posten 2 (Banane) blieb stehen.
    const rows = wrapper.findAll('li')
    expect(rows).toHaveLength(1)
    expect(rows[0]!.html()).toContain('Banane')
    expect(wrapper.emitted('added')).toBeUndefined()
    expect(wrapper.html()).toContain('fehlgeschlagen')
  })

  it('Erfolg: alle Posten uebernommen, added-Emit mit Anzahl', async () => {
    const { wrapper, receiptStore, pantryStore } = mountModal()
    await showPreview(receiptStore)
    const addItem = vi
      .spyOn(pantryStore, 'addItem')
      .mockResolvedValue({} as PantryItemDto)

    await wrapper.find('button.ee-btn-primary').trigger('click')
    await flushPromises()

    expect(addItem).toHaveBeenCalledTimes(2)
    expect(wrapper.emitted('added')).toEqual([[2]])
  })
})
