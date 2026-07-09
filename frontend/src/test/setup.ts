import { afterAll, afterEach, beforeAll } from 'vitest'
import { server } from './mocks/server'

// MSW: vor allen Tests starten, nach jedem Test Handler zurücksetzen
// (damit ein server.use(...)-Override im Test nicht in den nächsten Test
// leakt), nach allen Tests stoppen.
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
