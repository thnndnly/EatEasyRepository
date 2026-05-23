# Architektur

EatEasy ist ein **modularer Monolith** mit klarer Trennung zwischen fachlichen Komponenten. Frontend (Vue) und Backend (Quarkus) sprechen ausschließlich über die REST-API; intern kommunizieren Komponenten über Service-Interfaces, niemals über REST.

## Komponenten

| Backend-Komponente | Aufgabe |
| --- | --- |
| `auth` | Registrierung, Login, JWT-Issuance, `/auth/me` |
| `household` | Haushalte, Mitglieder, Einladungs-Tokens |
| `recipe` | Rezepte inkl. Diät-Tags und Import aus externen Quellen |
| `ingredient` | Stammdaten der Zutaten |
| `mealplan` | Wochenplan, Einträge pro Slot |
| `pantry` | Vorrat eines Haushalts, MHD-Tracking |
| `shoppinglist` | Auto-generierte Einkaufslisten aus Mealplan – Pantry |
| `suggestion` | Smart-Suggestions per Ollama-LLM |
| `integration` | Adapter für TheMealDB und OpenFoodFacts |
| `notification` | Versand transaktionaler E-Mails |
| `common` | Shared Utilities, Exceptions, Security-Helper |

## Schichten innerhalb einer Komponente

```
de.eateasy.<komponente>
├── entity/       — JPA-Entitäten (Hibernate Panache)
├── repository/   — Panache-Repositories
├── service/      — Business-Logik (Interface + Impl)
├── resource/     — REST-Endpunkte unter /api/v1/...
└── dto/          — Request/Response-Objekte (kein Entity-Leak)
```

**Regel:** Eine Komponente exponiert ihr `Service`-Interface; andere Komponenten injizieren das Interface, nicht das Repository. Beispiel: `ShoppingListService` ruft `PantryService`, nicht `PantryRepository`.

## Frontend-Spiegelung

Pinia-Stores spiegeln die Backend-Komponenten 1:1: `useAuthStore`, `useHouseholdStore`, `useRecipeStore`, `usePantryStore`, `useMealPlanStore`, `useShoppingListStore`, `useSuggestionStore`. Views nutzen nur die Stores, die sie tatsächlich brauchen; HTTP-Calls laufen ausschließlich über `services/<komponente>Service.ts`.

```
frontend/src
├── views/        — Routen-Komponenten (lazy loaded)
├── components/   — wiederverwendbare UI-Bausteine, gruppiert per Feature
├── stores/       — Pinia-Stores (Server-State)
├── services/     — fetch-Wrapper pro Komponente
├── types/        — TypeScript-Interfaces (DTOs)
└── router/       — Routes + Guards (requiresAuth, guestOnly)
```

## Authentifizierung

- Login/Register → Backend gibt einen **JWT** zurück (8 h Lifetime).
- Frontend persistiert Token + User via **VueUse `useStorage`** in `localStorage` (Keys: `eateasy.auth.token`, `eateasy.auth.user`).
- `apiClient.ts` injiziert das Token als `Authorization: Bearer <jwt>` für authentifizierte Calls.
- Jede REST-Methode trägt `@RolesAllowed("user")`, plus zusätzliche Authorization-Checks im Service-Layer (gehört diese Ressource zu einem Haushalt, in dem ich Mitglied bin?).

## Datenmodell-Highlights

- IDs sind **UUID v4** auf jeder Tabelle, keine Auto-Increment-Integer.
- `created_at` / `updated_at` auf jeder Tabelle, automatisch von Hibernate gepflegt.
- **Soft-Delete** nur bei Entitäten, bei denen das fachlich nötig ist (Rezepte ja, Wochenplan-Einträge nein).
- Schema-Änderungen via **Flyway** (`V<N>__<beschreibung>.sql`), Hibernate-Strategy ist `validate`.

Detaillierte Tabellenstruktur, Beziehungen und Indizes: `IMPLEMENTATION.md` im Repo-Root.

## Externe Systeme

| System | Zweck |
| --- | --- |
| **PostgreSQL 16** | Primäre Datenbank |
| **Ollama** (lokal) | LLM für Smart-Suggestions (Llama 3 / Mistral) |
| **TheMealDB** | Rezept-Import (kostenlos, kein API-Key) |
| **OpenFoodFacts** | Barcode-Lookup für Vorratseinträge |
| **Maildev / SMTP** | E-Mails (Einladungslinks, Notifications) |

## Konventionen kurz

- **Branches:** `feature/<slice-nr>-<kurzname>`
- **Commits:** Conventional Commits (`feat:`, `fix:`, `refactor:`, `chore:`, `docs:`, `test:`)
- **Tests:** mindestens Happy-Path + ein Edge-Case pro Service-Methode; REST-Endpunkte mit REST Assured
- **DTOs sind Records** mit Bean-Validation; Entities lecken nie aus REST-Endpunkten heraus
