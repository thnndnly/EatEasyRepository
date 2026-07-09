import { setupServer } from 'msw/node'
import { handlers } from './handlers'

/**
 * Node-Server für Vitest. Wird vom Test-Setup (`src/test/setup.ts`)
 * mit beforeAll/afterEach/afterAll gestartet, zurückgesetzt und gestoppt.
 */
export const server = setupServer(...handlers)
