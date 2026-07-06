import type { Unit } from './units'

export const INGREDIENT_CATEGORIES = [
  'OBST_GEMUESE',
  'BACKWAREN',
  'MILCHPRODUKTE',
  'FLEISCH_FISCH',
  'VORRAT',
  'GEWUERZE_SAUCEN',
  'TIEFKUEHL',
  'GETRAENKE',
  'SONSTIGES',
] as const

export type IngredientCategory = (typeof INGREDIENT_CATEGORIES)[number]

/** Anzeige-Labels; Reihenfolge von INGREDIENT_CATEGORIES = Gang durch den Supermarkt. */
export const CATEGORY_LABELS: Record<IngredientCategory, string> = {
  OBST_GEMUESE: 'Obst & Gemuese',
  BACKWAREN: 'Backwaren',
  MILCHPRODUKTE: 'Milchprodukte & Eier',
  FLEISCH_FISCH: 'Fleisch & Fisch',
  VORRAT: 'Vorrat & Trockenwaren',
  GEWUERZE_SAUCEN: 'Gewuerze & Saucen',
  TIEFKUEHL: 'Tiefkuehl',
  GETRAENKE: 'Getraenke',
  SONSTIGES: 'Sonstiges',
}

export const CATEGORY_ICONS: Record<IngredientCategory, string> = {
  OBST_GEMUESE: '🥦',
  BACKWAREN: '🥖',
  MILCHPRODUKTE: '🧀',
  FLEISCH_FISCH: '🍗',
  VORRAT: '🍚',
  GEWUERZE_SAUCEN: '🧂',
  TIEFKUEHL: '🧊',
  GETRAENKE: '🥤',
  SONSTIGES: '🛒',
}

export interface IngredientDto {
  id: string
  name: string
  defaultUnit: Unit
  category: IngredientCategory
}

export interface IngredientCreateRequest {
  name: string
  defaultUnit: Unit
}

export interface IngredientUpdateRequest {
  category: IngredientCategory
}
