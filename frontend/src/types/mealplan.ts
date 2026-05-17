import type { RecipeMiniDto } from './recipe'

export type { RecipeMiniDto } from './recipe'

export const DAYS_OF_WEEK = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
] as const

export type DayOfWeek = (typeof DAYS_OF_WEEK)[number]

export const DAY_LABELS: Record<DayOfWeek, string> = {
  MONDAY: 'Mo',
  TUESDAY: 'Di',
  WEDNESDAY: 'Mi',
  THURSDAY: 'Do',
  FRIDAY: 'Fr',
  SATURDAY: 'Sa',
  SUNDAY: 'So',
}

export const DAY_LONG_LABELS: Record<DayOfWeek, string> = {
  MONDAY: 'Montag',
  TUESDAY: 'Dienstag',
  WEDNESDAY: 'Mittwoch',
  THURSDAY: 'Donnerstag',
  FRIDAY: 'Freitag',
  SATURDAY: 'Samstag',
  SUNDAY: 'Sonntag',
}

export const MEAL_TYPES = ['BREAKFAST', 'LUNCH', 'DINNER'] as const

export type MealType = (typeof MEAL_TYPES)[number]

export const MEAL_TYPE_LABELS: Record<MealType, string> = {
  BREAKFAST: 'Fruehstueck',
  LUNCH: 'Mittag',
  DINNER: 'Abend',
}

export interface MealPlanEntryDto {
  id: string
  dayOfWeek: DayOfWeek
  mealType: MealType
  servings: number
  recipe: RecipeMiniDto | null
}

export interface MealPlanDto {
  id: string
  householdId: string
  weekStart: string
  entries: MealPlanEntryDto[]
}

export interface SetEntryRequest {
  dayOfWeek: DayOfWeek
  mealType: MealType
  recipeId: string
  servings: number
}
