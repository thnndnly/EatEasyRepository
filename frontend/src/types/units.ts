export const UNITS = ['GRAM', 'ML', 'PIECE', 'TBSP', 'TSP'] as const

export type Unit = (typeof UNITS)[number]

export const UNIT_LABELS: Record<Unit, string> = {
  GRAM: 'Gramm',
  ML: 'Milliliter',
  PIECE: 'Stueck',
  TBSP: 'Essloeffel',
  TSP: 'Teeloeffel',
}

export const UNIT_ABBREV: Record<Unit, string> = {
  GRAM: 'g',
  ML: 'ml',
  PIECE: 'Stk',
  TBSP: 'EL',
  TSP: 'TL',
}
