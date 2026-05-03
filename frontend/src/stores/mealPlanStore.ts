import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as mealPlanService from '@/services/mealPlanService'
import { useAuthStore } from '@/stores/authStore'
import type {
  DayOfWeek,
  MealPlanDto,
  MealPlanEntryDto,
  MealType,
  SetEntryRequest,
} from '@/types/mealplan'

/**
 * Liefert das ISO-Datum (YYYY-MM-DD) des Montags der Woche, in der das
 * uebergebene Datum liegt. Sonntag → vorhergehender Montag, Mittwoch →
 * Montag derselben Woche.
 */
export function mondayOf(date: Date): string {
  const d = new Date(date)
  const day = d.getUTCDay() // 0=Sonntag .. 6=Samstag
  const diff = day === 0 ? -6 : 1 - day
  d.setUTCDate(d.getUTCDate() + diff)
  return d.toISOString().slice(0, 10)
}

export function addDays(isoDate: string, days: number): string {
  const d = new Date(isoDate + 'T00:00:00Z')
  d.setUTCDate(d.getUTCDate() + days)
  return d.toISOString().slice(0, 10)
}

export const useMealPlanStore = defineStore('mealPlan', () => {
  const plan = ref<MealPlanDto | null>(null)
  const weekStart = ref<string>(mondayOf(new Date()))
  const householdId = ref<string | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  function requireToken(): string {
    const authStore = useAuthStore()
    if (!authStore.token) {
      throw new Error('Nicht eingeloggt')
    }
    return authStore.token
  }

  async function load(targetHouseholdId: string, targetWeek?: string): Promise<void> {
    householdId.value = targetHouseholdId
    if (targetWeek) {
      weekStart.value = targetWeek
    }
    loading.value = true
    error.value = null
    try {
      plan.value = await mealPlanService.getMealPlan(
        requireToken(),
        targetHouseholdId,
        weekStart.value,
      )
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : 'Laden fehlgeschlagen'
      plan.value = null
    } finally {
      loading.value = false
    }
  }

  async function gotoWeek(deltaDays: number): Promise<void> {
    weekStart.value = addDays(weekStart.value, deltaDays)
    if (householdId.value) {
      await load(householdId.value)
    }
  }

  async function gotoToday(): Promise<void> {
    weekStart.value = mondayOf(new Date())
    if (householdId.value) {
      await load(householdId.value)
    }
  }

  async function setEntry(request: SetEntryRequest): Promise<MealPlanEntryDto> {
    if (!plan.value) {
      throw new Error('Kein Wochenplan geladen')
    }
    const updated = await mealPlanService.setEntry(requireToken(), plan.value.id, request)

    const next = plan.value.entries.filter(
      (e) => !(e.dayOfWeek === request.dayOfWeek && e.mealType === request.mealType),
    )
    next.push(updated)
    plan.value = { ...plan.value, entries: next }
    return updated
  }

  async function removeEntry(day: DayOfWeek, mealType: MealType): Promise<void> {
    if (!plan.value) {
      return
    }
    await mealPlanService.removeEntry(requireToken(), plan.value.id, day, mealType)
    plan.value = {
      ...plan.value,
      entries: plan.value.entries.filter(
        (e) => !(e.dayOfWeek === day && e.mealType === mealType),
      ),
    }
  }

  function reset(): void {
    plan.value = null
    householdId.value = null
    error.value = null
    weekStart.value = mondayOf(new Date())
  }

  function entryAt(day: DayOfWeek, mealType: MealType): MealPlanEntryDto | null {
    if (!plan.value) {
      return null
    }
    return (
      plan.value.entries.find(
        (e) => e.dayOfWeek === day && e.mealType === mealType,
      ) ?? null
    )
  }

  const weekRangeLabel = computed<string>(() => {
    const start = weekStart.value
    const end = addDays(start, 6)
    return `${start} – ${end}`
  })

  return {
    plan,
    weekStart,
    householdId,
    loading,
    error,
    weekRangeLabel,
    load,
    gotoWeek,
    gotoToday,
    setEntry,
    removeEntry,
    reset,
    entryAt,
  }
})
