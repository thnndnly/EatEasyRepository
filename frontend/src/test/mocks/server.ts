import { setupServer } from 'msw/node'
import { handlers } from './handlers'

/**
 * Node-Server fuer Vitest. Wird vom Test-Setup (`src/test/setup.ts`)
 * mit beforeAll/afterEach/afterAll gestartet, zurueckgesetzt und gestoppt.
 */
export const server = setupServer(...handlers)
