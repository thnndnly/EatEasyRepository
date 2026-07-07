import type { Unit } from './units'

export interface ReceiptItemDto {
  name: string
  amount: number
  unit: Unit
  ingredientId: string | null
}

export interface ReceiptScanResponse {
  rawText: string
  items: ReceiptItemDto[]
}
