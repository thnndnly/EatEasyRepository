import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import RecipeCard from './RecipeCard.vue'
import type { RecipeDto } from '@/types/recipe'

/**
 * Component-Spec fuer RecipeCard. Deckt die Kern-Interaktion des Herz-Buttons ab:
 *  - Klick emittiert `toggleFavorite` mit der korrekten Rezept-ID
 *  - `@click.stop` verhindert, dass der Karten-Klick (Navigation) mitfeuert
 *  - Button ist waehrend eines laufenden Toggle-Requests deaktiviert (Doppelklick-Schutz)
 */
function makeRecipe(overrides: Partial<RecipeDto> = {}): RecipeDto {
  return {
    id: 'recipe-1',
    ownerId: 'user-1',
    householdId: null,
    title: 'Testrezept',
    description: null,
    instructions: 'Kochen.',
    servings: 2,
    prepMinutes: 20,
    dietTags: [],
    sourceUrl: null,
    externalSource: null,
    favorite: false,
    ingredients: [],
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides,
  }
}

describe('RecipeCard', () => {
  it('emittiert toggleFavorite mit der Rezept-ID beim Klick auf den Herz-Button', async () => {
    const recipe = makeRecipe({ id: 'recipe-42' })
    const wrapper = mount(RecipeCard, { props: { recipe } })

    await wrapper.find('button[aria-label="Favorit umschalten"]').trigger('click')

    expect(wrapper.emitted('toggleFavorite')).toHaveLength(1)
    expect(wrapper.emitted('toggleFavorite')?.[0]).toEqual(['recipe-42'])
  })

  it('stoppt die Klick-Propagation, damit der Karten-Klick (Navigation) nicht mitfeuert', async () => {
    const recipe = makeRecipe()
    let cardClicked = false
    const wrapper = mount(RecipeCard, {
      props: { recipe },
      attrs: {
        onClick: () => {
          cardClicked = true
        },
      },
    })

    await wrapper.find('button[aria-label="Favorit umschalten"]').trigger('click')

    expect(wrapper.emitted('toggleFavorite')).toHaveLength(1)
    expect(cardClicked).toBe(false)
  })

  it('deaktiviert den Herz-Button nach dem Klick und verhindert einen zweiten Emit', async () => {
    const recipe = makeRecipe()
    const wrapper = mount(RecipeCard, { props: { recipe } })
    const button = wrapper.find('button[aria-label="Favorit umschalten"]')

    await button.trigger('click')
    expect(button.attributes('disabled')).toBeDefined()

    // Zweiter Klick waehrend "in flight" darf keinen weiteren Emit ausloesen.
    await button.trigger('click')
    expect(wrapper.emitted('toggleFavorite')).toHaveLength(1)
  })

  it('gibt den Button wieder frei, sobald der Store recipe.favorite umschaltet', async () => {
    const recipe = makeRecipe({ favorite: false })
    const wrapper = mount(RecipeCard, { props: { recipe } })
    const button = wrapper.find('button[aria-label="Favorit umschalten"]')

    await button.trigger('click')
    expect(button.attributes('disabled')).toBeDefined()

    // Store hat den Toggle abgeschlossen -> Prop flippt -> Button wieder aktiv.
    await wrapper.setProps({ recipe: makeRecipe({ favorite: true }) })
    expect(button.attributes('disabled')).toBeUndefined()

    await button.trigger('click')
    expect(wrapper.emitted('toggleFavorite')).toHaveLength(2)
  })
})
