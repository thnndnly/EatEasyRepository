import { apiFetch } from './apiClient'
import type {
  DayOfWeek,
  MealPlanDto,
  MealPlanEntryDto,
  MealType,
  SetEntryRequest,
} from '@/types/mealplan'

const BASE = '/api/v1'

export function getMealPlan(
  token: string,
  householdId: string,
  weekStart?: string,
): Promise<MealPlanDto> {
  const qs = weekStart ? `?weekStart=${encodeURIComponent(weekStart)}` : ''
  return apiFetch<MealPlanDto>(`${BASE}/households/${householdId}/mealplans${qs}`, { token })
}

export function setEntry(
  token: string,
  mealPlanId: string,
  request: SetEntryRequest,
): Promise<MealPlanEntryDto> {
  return apiFetch<MealPlanEntryDto>(`${BASE}/mealplans/${mealPlanId}/entries`, {
    method: 'PUT',
    body: request,
    token,
  })
}

export function removeEntry(
  token: string,
  mealPlanId: string,
  day: DayOfWeek,
  mealType: MealType,
): Promise<void> {
  return apiFetch<void>(`${BASE}/mealplans/${mealPlanId}/entries/${day}/${mealType}`, {
    method: 'DELETE',
    token,
  })
}
