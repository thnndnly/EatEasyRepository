# Tech-Stack

Im Modul „Web Frameworks" sollten mehrere der vorgegebenen Technologien im Projekt eingesetzt werden. Dieser Abschnitt listet, **welche** wir einsetzen und **warum**.

## Verwendet im Projekt

| Tech | Schicht | Was es liefert |
| --- | --- | --- |
| **Vue 3** + **Composition API** | Frontend | Reaktives UI-Framework, `<script setup>` überall |
| **Vite 8** | Frontend-Build | Dev-Server mit Hot-Reload, Build-Tooling |
| **Pinia 3** | Frontend-State | Serverseitiger State pro Komponente in eigenem Store |
| **TypeScript (strict)** | Frontend | Voll typisiert, kein `any` |
| **Tailwind CSS 4** | Frontend-Styles | Utility-First, eigenes Designsystem (`ee-btn-*`) |
| **VueUse** | Frontend-Composables | `useStorage` für Auth-Persistenz, `useEventListener` + `useScrollLock` in `BaseModal` |
| **Vitest 4** | Frontend-Tests | Unit-Test-Runner, jsdom-Environment |
| **MSW (Mock Service Worker)** | Frontend-Tests | Backend-Mocks für deterministische Store-Tests |
| **Playwright** | Frontend-E2E | Smoke-Flows in Chromium, Backend-Calls per `page.route()` gemockt |
| **ESLint + oxlint + Prettier** | Frontend-Quality | Linting + Auto-Format |
| **Quarkus 3.34** | Backend | Java-Framework mit Hot-Reload, optimiert für JVM- und Native-Builds |
| **Java 21** | Backend | Records, Pattern Matching, Sealed Types |
| **Hibernate ORM Panache** | Backend | JPA mit kürzerer Boilerplate |
| **SmallRye JWT** | Backend | JWT-Auth |
| **SmallRye OpenAPI** | Backend | Automatisch generierte OpenAPI-Spec + Swagger UI |
| **JUnit 5 + REST Assured** | Backend-Tests | Klassische Test-Pyramide + Endpoint-Tests |
| **Testcontainers** | Backend-Tests | Echter Postgres im Test (via Quarkus Dev Services) |
| **Checkstyle** | Backend-Quality | Java-Linter, lenient konfiguriert (UnusedImports, LineLength etc.) |
| **PostgreSQL 16** | DB | Relationale Datenbank |
| **Flyway** | DB-Migrations | Versionierte Schema-Änderungen |
| **Ollama** | LLM | Lokales LLM für Smart-Suggestions |
| **TheMealDB / OpenFoodFacts** | Externe APIs | Rezept-Import, Barcode-Lookup |
| **Docker Compose** | Infrastruktur | Postgres, Ollama, Maildev für Dev |
| **VitePress** | Doku | Diese Seite |

## Bewusst nicht aufgenommen

| Tech | Grund |
| --- | --- |
| **Nuxt** | Migration von Vite-SPA → Nuxt zu invasiv für den Nutzen im Modulkontext |
| **PrimeVue** | UI-System ist mit Tailwind-Utility-Klassen bereits konsistent, eine zweite Komponentenbibliothek bringt nur Reibung |
| **Figma** | Aufwand außerhalb des Code-Anteils des Moduls |
| **Stylelint** | Tailwind-First macht eigenes CSS marginal; ESLint deckt JS/TS, Checkstyle deckt Java |
| **Cypress** | Playwright bevorzugt (Team-Erfahrung, robustere Auto-Wait-Semantik) |
