import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import BaseModal from './BaseModal.vue'

/**
 * Component-Spec als Vorlage. Demonstriert:
 *  - prop-getriebenes v-if Rendering
 *  - DOM-Events ueber Vue Test Utils
 *  - Esc-Key via dispatchEvent (echtes KeyboardEvent, nicht trigger)
 *  - Body-Scroll-Lock-Side-Effect via useScrollLock
 *  - Emit-Verifikation
 */
describe('BaseModal', () => {
  afterEach(() => {
    document.body.style.overflow = ''
  })

  it('rendert nichts, wenn open=false', () => {
    const wrapper = mount(BaseModal, {
      props: { open: false },
      slots: { default: '<p>Inhalt</p>' },
    })

    expect(wrapper.find('[role="dialog"]').exists()).toBe(false)
  })

  it('rendert Slot-Inhalt und Header, wenn open=true', () => {
    const wrapper = mount(BaseModal, {
      props: { open: true },
      slots: {
        header: '<h2>Mein Header</h2>',
        default: '<p>Mein Inhalt</p>',
      },
    })

    expect(wrapper.find('[role="dialog"]').exists()).toBe(true)
    expect(wrapper.html()).toContain('Mein Header')
    expect(wrapper.html()).toContain('Mein Inhalt')
  })

  it('emit close, wenn Esc gedrueckt wird', async () => {
    const wrapper = mount(BaseModal, { props: { open: true } })

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('close')).toHaveLength(1)
  })

  it('ignoriert Esc, wenn open=false', async () => {
    const wrapper = mount(BaseModal, { props: { open: false } })

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('close')).toBeUndefined()
  })

  it('emit close beim Klick auf den Backdrop', async () => {
    const wrapper = mount(BaseModal, { props: { open: true } })

    await wrapper.find('[role="dialog"]').trigger('click')

    expect(wrapper.emitted('close')).toHaveLength(1)
  })

  it('emit close ueber den X-Button im Header', async () => {
    const wrapper = mount(BaseModal, { props: { open: true } })

    await wrapper.find('button[aria-label="Schliessen"]').trigger('click')

    expect(wrapper.emitted('close')).toHaveLength(1)
  })

  it('sperrt den Body-Scroll bei open=true und gibt ihn bei open=false frei', async () => {
    const wrapper = mount(BaseModal, { props: { open: true } })
    expect(document.body.style.overflow).toBe('hidden')

    await wrapper.setProps({ open: false })

    expect(document.body.style.overflow).toBe('')
  })
})
