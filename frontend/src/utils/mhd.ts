const MS_PER_DAY = 1000 * 60 * 60 * 24

/** Kalendertage bis zum ISO-Datum (negativ = in der Vergangenheit). */
export function daysUntil(isoDate: string): number {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  // Datumsteile direkt parsen statt `new Date(isoDate)`: ein date-only
  // ISO-String wird sonst als UTC-Mitternacht interpretiert, und ein
  // anschliessendes setHours(0,0,0,0) in lokaler Zeitzone (westlich von UTC)
  // verschiebt das Datum um einen Tag — was den expiringSoon-Filter verfaelscht.
  const [year, month, day] = isoDate.slice(0, 10).split('-').map(Number)
  const target = new Date(year, month - 1, day)
  return Math.round((target.getTime() - today.getTime()) / MS_PER_DAY)
}

export interface MhdStatus {
  level: 'expired' | 'urgent' | 'soon' | 'ok'
  label: string
  rowClass: string
  chipClass: string
}

/**
 * Ampel-Status fuer ein MHD: abgelaufen (rose), ≤3 Tage (rose),
 * ≤7 Tage (butter), sonst neutral. Wird von PantryRow und dem
 * Dashboard-Widget „Demnaechst ablaufend" geteilt.
 */
export function mhdStatusFor(bestBefore: string): MhdStatus {
  const days = daysUntil(bestBefore)
  if (days < 0) {
    return {
      level: 'expired',
      label: days === -1 ? 'gestern abgelaufen' : `vor ${-days} Tagen abgelaufen`,
      rowClass: 'bg-rose-50',
      chipClass: 'ee-chip-rose',
    }
  }
  if (days <= 3) {
    return {
      level: 'urgent',
      label: days === 0 ? 'heute' : days === 1 ? 'morgen' : `in ${days} Tagen`,
      rowClass: 'bg-rose-50',
      chipClass: 'ee-chip-rose',
    }
  }
  if (days <= 7) {
    return {
      level: 'soon',
      label: `in ${days} Tagen`,
      rowClass: 'bg-butter-100/60',
      chipClass: 'ee-chip-butter',
    }
  }
  return {
    level: 'ok',
    label: `in ${days} Tagen`,
    rowClass: '',
    chipClass: 'ee-chip-neutral',
  }
}
