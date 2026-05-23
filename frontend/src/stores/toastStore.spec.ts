import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useToastStore } from './toastStore'

describe('toastStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('success pusht einen Toast mit level=success', () => {
    const store = useToastStore()
    store.success('Gespeichert')

    expect(store.toasts).toHaveLength(1)
    expect(store.toasts[0]).toMatchObject({ level: 'success', message: 'Gespeichert' })
  })

  it('error und info nutzen den passenden Level', () => {
    const store = useToastStore()
    store.error('Fehler')
    store.info('Hinweis')

    expect(store.toasts.map((t) => t.level)).toEqual(['error', 'info'])
  })

  it('jeder Toast bekommt eine eindeutige ID', () => {
    const store = useToastStore()
    store.success('A')
    store.success('B')
    store.success('C')

    const ids = store.toasts.map((t) => t.id)
    expect(new Set(ids).size).toBe(3)
  })

  it('dismiss entfernt nur den Toast mit der gegebenen ID', () => {
    const store = useToastStore()
    store.success('A')
    store.success('B')
    const idOfB = store.toasts[1]!.id

    store.dismiss(idOfB)

    expect(store.toasts).toHaveLength(1)
    expect(store.toasts[0]!.message).toBe('A')
  })

  it('Toast wird nach Default-Dauer automatisch entfernt', () => {
    const store = useToastStore()
    store.success('Auto-Dismiss')
    expect(store.toasts).toHaveLength(1)

    vi.advanceTimersByTime(4000)

    expect(store.toasts).toHaveLength(0)
  })

  it('show mit durationMs=0 disabled den Auto-Dismiss', () => {
    const store = useToastStore()
    store.show('info', 'Sticky', 0)

    vi.advanceTimersByTime(60_000)

    expect(store.toasts).toHaveLength(1)
  })
})
