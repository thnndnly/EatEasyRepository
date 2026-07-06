import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { daysUntil, mhdStatusFor } from './mhd'

describe('mhd utils', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-07-01T12:00:00Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('daysUntil rechnet in ganzen Kalendertagen', () => {
    expect(daysUntil('2026-07-01')).toBe(0)
    expect(daysUntil('2026-07-02')).toBe(1)
    expect(daysUntil('2026-06-30')).toBe(-1)
    expect(daysUntil('2026-07-08')).toBe(7)
  })

  it.each([
    ['2026-06-28', 'expired', 'vor 3 Tagen abgelaufen'],
    ['2026-06-30', 'expired', 'gestern abgelaufen'],
    ['2026-07-01', 'urgent', 'heute'],
    ['2026-07-02', 'urgent', 'morgen'],
    ['2026-07-04', 'urgent', 'in 3 Tagen'],
    ['2026-07-08', 'soon', 'in 7 Tagen'],
    ['2026-07-20', 'ok', 'in 19 Tagen'],
  ])('mhdStatusFor(%s) → level %s, label "%s"', (date, level, label) => {
    const status = mhdStatusFor(date)
    expect(status.level).toBe(level)
    expect(status.label).toBe(label)
  })
})

describe('daysUntil ist zeitzonen-robust', () => {
  const originalTz = process.env.TZ

  afterEach(() => {
    vi.useRealTimers()
    // Node ruft tzset() bei Zuweisung von process.env.TZ auf.
    process.env.TZ = originalTz
  })

  // Das heutige *lokale* Datum muss immer 0 Tage ergeben — unabhaengig von der
  // Systemzeitzone. Die fruehere Implementierung (new Date(isoDate) parst als
  // UTC-Mitternacht, dann setHours in lokaler TZ) lieferte in Zonen westlich
  // von UTC faelschlich -1 und verschob damit den expiringSoon-Filter.
  it.each(['UTC', 'America/Los_Angeles', 'Pacific/Kiritimati'])(
    'liefert 0 fuer das heutige lokale Datum in TZ %s',
    (tz) => {
      process.env.TZ = tz
      vi.useFakeTimers()
      vi.setSystemTime(new Date('2026-07-01T12:00:00Z'))

      const now = new Date()
      const localToday = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(
        now.getDate(),
      ).padStart(2, '0')}`

      expect(daysUntil(localToday)).toBe(0)
    },
  )
})
