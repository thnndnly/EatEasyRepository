import { afterAll, afterEach, beforeAll } from 'vitest'
import { server } from './mocks/server'

// MSW: vor allen Tests starten, nach jedem Test Handler zuruecksetzen
// (damit ein server.use(...)-Override im Test nicht in den naechsten Test
// leakt), nach allen Tests stoppen.
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
