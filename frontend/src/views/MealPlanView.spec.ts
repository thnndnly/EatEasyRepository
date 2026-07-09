import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import type { DayOfWeek, MealPlanEntryDto, MealType } from '@/types/mealplan'
import { TEST_RECIPE_MINI } from '@/test/fixtures'

/**
 * Gemockte Store-/Router-Abhängigkeiten. Über vi.hoisted gebaut, damit die
 * vi.mock-Factories (die vor den Imports laufen) auf dieselben Spy-Instanzen
 * zugreifen wie der Test-Body.
 */
const mocks = vi.hoisted(() => ({
  setEntry: vi.fn<
    (request: {
      dayOfWeek: DayOfWeek
      mealType: MealType
      recipeId: string
      servings: number
    }) => Promise<MealPlanEntryDto>
  >(),
  entryAt: vi.fn<(day: DayOfWeek, mealType: MealType) => MealPlanEntryDto | null>(),
  load: vi.fn<(householdId: string) => Promise<void>>(),
  reset: vi.fn<() => void>(),
  gotoWeek: vi.fn<(weekStart: string) => void>(),
  gotoToday: vi.fn<() => void>(),
  householdLoad: vi.fn<(householdId: string) => Promise<void>>(),
  routerPush: vi.fn<() => void>(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mocks.routerPush }),
}))

vi.mock('@/stores/householdStore', () => ({
  useHouseholdStore: () => ({
    selected: { id: 'hh-1', name: 'Test-Haushalt', defaultDietTags: [] },
    load: mocks.householdLoad,
  }),
}))

vi.mock('@/stores/mealPlanStore', () => ({
  useMealPlanStore: () => ({
    plan: { id: 'plan-1', householdId: 'hh-1', weekStart: '2026-05-25', entries: [] },
    loading: false,
    error: null,
    weekRangeLabel: '2026-05-25 – 2026-05-31',
    load: mocks.load,
    reset: mocks.reset,
    gotoWeek: mocks.gotoWeek,
    gotoToday: mocks.gotoToday,
    setEntry: mocks.setEntry,
    entryAt: mocks.entryAt,
  }),
}))

import MealPlanView from './MealPlanView.vue'
import MealPlanGrid from '@/components/mealplan/MealPlanGrid.vue'

// Zwei Slots mit Rezept — bewusst in derselben Mahlzeit-Zeile, damit die
// Spalten-Reihenfolge (Mo vor Di) deterministisch ist.
const SLOT_A: MealPlanEntryDto = {
  id: 'entry-a',
  dayOfWeek: 'MONDAY',
  mealType: 'DINNER',
  servings: 4,
  recipe: TEST_RECIPE_MINI,
}
const SLOT_B: MealPlanEntryDto = {
  id: 'entry-b',
  dayOfWeek: 'TUESDAY',
  mealType: 'DINNER',
  servings: 4,
  recipe: TEST_RECIPE_MINI,
}

function entryFor(day: DayOfWeek, mealType: MealType): MealPlanEntryDto | null {
  if (day === 'MONDAY' && mealType === 'DINNER') return SLOT_A
  if (day === 'TUESDAY' && mealType === 'DINNER') return SLOT_B
  return null
}

async function mountView() {
  const wrapper = mount(MealPlanView, {
    // RecipePickerModal hängt am (hier nicht gemockten) recipeStore und ist
    // für den Stepper-Guard irrelevant — deshalb ausstubben.
    global: { stubs: { RecipePickerModal: true } },
  })
  await flushPromises() // onMounted → ensureLoaded abwarten
  return wrapper
}

describe('MealPlanView onChangeServings — Cross-Slot-Guard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.entryAt.mockImplementation(entryFor)
  })

  it('reicht changeServings des Grids an den Store weiter', async () => {
    mocks.setEntry.mockResolvedValue({ ...SLOT_A, servings: 5 })
    const wrapper = await mountView()

    wrapper.findComponent(MealPlanGrid).vm.$emit('changeServings', 'MONDAY', 'DINNER', 5)
    await flushPromises()

    expect(mocks.setEntry).toHaveBeenCalledTimes(1)
    expect(mocks.setEntry).toHaveBeenNthCalledWith(
      1,
      expect.objectContaining({
        dayOfWeek: 'MONDAY',
        mealType: 'DINNER',
        recipeId: TEST_RECIPE_MINI.id,
        servings: 5,
      }),
    )
  })

  it('blockt einen zweiten Klick auf denselben Slot während der Request läuft', async () => {
    // Request bleibt in-flight (Promise löst nie auf) → savingSlot bleibt gesetzt.
    mocks.setEntry.mockReturnValue(new Promise<MealPlanEntryDto>(() => {}))
    const wrapper = await mountView()
    const grid = wrapper.findComponent(MealPlanGrid)

    grid.vm.$emit('changeServings', 'MONDAY', 'DINNER', 5)
    await flushPromises()
    grid.vm.$emit('changeServings', 'MONDAY', 'DINNER', 6)
    await flushPromises()

    // Zweiter Klick auf Slot A wird verschluckt — nur der erste Aufruf zählt.
    expect(mocks.setEntry).toHaveBeenCalledTimes(1)
    expect(mocks.setEntry).toHaveBeenNthCalledWith(
      1,
      expect.objectContaining({ dayOfWeek: 'MONDAY', servings: 5 }),
    )
  })

  it('erlaubt parallele Edits UNTERSCHIEDLICHER Slots (Klick auf B bleibt wirksam)', async () => {
    // Slot A läuft noch, wenn B geklickt wird.
    mocks.setEntry.mockReturnValue(new Promise<MealPlanEntryDto>(() => {}))
    const wrapper = await mountView()
    const grid = wrapper.findComponent(MealPlanGrid)

    grid.vm.$emit('changeServings', 'MONDAY', 'DINNER', 5) // Slot A → in-flight
    await flushPromises()
    grid.vm.$emit('changeServings', 'TUESDAY', 'DINNER', 7) // Slot B → darf NICHT blockiert werden
    await flushPromises()

    expect(mocks.setEntry).toHaveBeenCalledTimes(2)
    expect(mocks.setEntry).toHaveBeenNthCalledWith(
      1,
      expect.objectContaining({ dayOfWeek: 'MONDAY', servings: 5 }),
    )
    expect(mocks.setEntry).toHaveBeenNthCalledWith(
      2,
      expect.objectContaining({ dayOfWeek: 'TUESDAY', servings: 7 }),
    )
  })

  it('gibt den Slot nach Abschluss des Requests wieder frei', async () => {
    let resolveFirst: (value: MealPlanEntryDto) => void = () => {}
    mocks.setEntry.mockImplementationOnce(
      () => new Promise<MealPlanEntryDto>((resolve) => (resolveFirst = resolve)),
    )
    const wrapper = await mountView()
    const grid = wrapper.findComponent(MealPlanGrid)

    grid.vm.$emit('changeServings', 'MONDAY', 'DINNER', 5)
    await flushPromises()

    // Request abschliessen → finally setzt savingSlot zurück.
    mocks.setEntry.mockResolvedValue({ ...SLOT_A, servings: 6 })
    resolveFirst({ ...SLOT_A, servings: 5 })
    await flushPromises()

    grid.vm.$emit('changeServings', 'MONDAY', 'DINNER', 6)
    await flushPromises()

    expect(mocks.setEntry).toHaveBeenCalledTimes(2)
    expect(mocks.setEntry).toHaveBeenNthCalledWith(
      2,
      expect.objectContaining({ dayOfWeek: 'MONDAY', servings: 6 }),
    )
  })

  it('ignoriert changeServings für einen leeren Slot ohne Rezept', async () => {
    const wrapper = await mountView()

    wrapper.findComponent(MealPlanGrid).vm.$emit('changeServings', 'FRIDAY', 'LUNCH', 3)
    await flushPromises()

    expect(mocks.setEntry).not.toHaveBeenCalled()
  })
})
