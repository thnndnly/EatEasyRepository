import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import MealSlot from './MealSlot.vue'
import type { MealPlanEntryDto } from '@/types/mealplan'
import { TEST_RECIPE_MINI } from '@/test/fixtures'

function entryWithServings(servings: number): MealPlanEntryDto {
  return {
    id: 'entry-1',
    dayOfWeek: 'MONDAY',
    mealType: 'LUNCH',
    servings,
    recipe: TEST_RECIPE_MINI,
  }
}

describe('MealSlot Portionen-Stepper', () => {
  it('zeigt Stepper mit aktueller Portionszahl', () => {
    const wrapper = mount(MealSlot, { props: { entry: entryWithServings(4) } })

    expect(wrapper.text()).toContain('4 Portionen')
    expect(wrapper.find('[aria-label="Mehr Portionen"]').exists()).toBe(true)
    expect(wrapper.find('[aria-label="Weniger Portionen"]').exists()).toBe(true)
  })

  it('emittet changeServings mit +1 / -1, nicht aber select', async () => {
    const wrapper = mount(MealSlot, { props: { entry: entryWithServings(4) } })

    await wrapper.find('[aria-label="Mehr Portionen"]').trigger('click')
    await wrapper.find('[aria-label="Weniger Portionen"]').trigger('click')

    expect(wrapper.emitted('changeServings')).toEqual([[5], [3]])
    expect(wrapper.emitted('select')).toBeUndefined()
  })

  it('geht nicht unter 1 Portion — Button ist echt disabled und feuert kein select', async () => {
    const wrapper = mount(MealSlot, { props: { entry: entryWithServings(1) } })
    const minus = wrapper.find('[aria-label="Weniger Portionen"]')

    expect(minus.attributes('disabled')).toBeDefined()
    await minus.trigger('click')

    expect(wrapper.emitted('changeServings')).toBeUndefined()
    // Früher fiel der Klick per pointer-events-none auf den Slot durch
    // und öffnete den Picker — darf nicht wieder passieren.
    expect(wrapper.emitted('select')).toBeUndefined()
  })

  it('erlaubt Dekrement auch oberhalb von 20 (Werte > 20 sind via Picker/API möglich)', async () => {
    const wrapper = mount(MealSlot, { props: { entry: entryWithServings(25) } })

    expect(wrapper.find('[aria-label="Mehr Portionen"]').attributes('disabled')).toBeDefined()
    await wrapper.find('[aria-label="Weniger Portionen"]').trigger('click')

    expect(wrapper.emitted('changeServings')).toEqual([[24]])
  })

  it('deaktiviert beide Stepper-Buttons während saving', async () => {
    const wrapper = mount(MealSlot, {
      props: { entry: entryWithServings(4), saving: true },
    })

    expect(wrapper.find('[aria-label="Mehr Portionen"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('[aria-label="Weniger Portionen"]').attributes('disabled')).toBeDefined()
    await wrapper.find('[aria-label="Mehr Portionen"]').trigger('click')

    expect(wrapper.emitted('changeServings')).toBeUndefined()
  })

  it('Klick auf die Slot-Fläche emittet select', async () => {
    const wrapper = mount(MealSlot, { props: { entry: entryWithServings(4) } })

    await wrapper.find('button.absolute').trigger('click')

    expect(wrapper.emitted('select')).toHaveLength(1)
    expect(wrapper.emitted('changeServings')).toBeUndefined()
  })

  it('zeigt keinen Stepper im leeren Slot', () => {
    const wrapper = mount(MealSlot, { props: { entry: null } })

    expect(wrapper.find('[aria-label="Mehr Portionen"]').exists()).toBe(false)
    expect(wrapper.find('button[aria-label="Rezept wählen"]').exists()).toBe(true)
  })
})
