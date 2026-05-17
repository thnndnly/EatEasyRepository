import type { Unit } from './units'

export interface BarcodeProductDto {
  barcode: string
  name: string
  suggestedUnit: Unit
}

export interface BarcodePantryRequest {
  barcode: string
  amount: number
  unit: Unit
  bestBefore?: string | null
}
