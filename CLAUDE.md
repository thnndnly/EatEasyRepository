# EatEasy EE — Claude Code Briefing

Komponentenbasierte Web-Anwendung für haushaltsweite Mahlzeitenplanung, Rezeptverwaltung und automatische Einkaufslisten. Studienprojekt, Gruppe 5 (Dimitrios Tsakos, Kardeena Kameran).

> **Vor jeder Änderung:** Lies `IMPLEMENTATION.md` für den aktuellen Phasen-Plan, das Datenmodell und die API-Spec. Diese Datei hier ist nur die Schnellreferenz.

---

## Stack

| Layer | Technologie |
|---|---|
| Frontend | Vue.js 3 (Composition API), Vite, Pinia, Vue Router, TypeScript |
| Backend | Quarkus (Java 21), RESTEasy Reactive, Hibernate ORM mit Panache, MicroProfile, SmallRye JWT, Quarkus Mailer |
| Datenbank | PostgreSQL 16 |
| LLM (self-hosted) | Ollama mit Llama 3 oder Mistral |
| Externe APIs | TheMealDB (Rezept-Import), OpenFoodFacts (Barcode) |
| Build | Maven (Backend), npm + Vite (Frontend) |
| Testing | JUnit 5 + REST Assured (Backend), Vitest + Vue Test Utils (Frontend) |
| Container | Docker, docker-compose |
| Versionsverwaltung | Git / GitHub |

---

## Projekt-Layout

```
eateasy-ee/
├── backend/                    # Quarkus-Anwendung
│   ├── src/main/java/de/eateasy/
│   │   ├── auth/               # Auth-Komponente
│   │   ├── household/          # Haushalt-Komponente
│   │   ├── recipe/             # Rezept-Komponente (inkl. Diät-Tags)
│   │   ├── mealplan/           # Wochenplan-Komponente
│   │   ├── pantry/             # Vorrat-Komponente
│   │   ├── shoppinglist/       # Einkaufslisten-Komponente
│   │   ├── ingredient/         # Zutaten-Komponente
│   │   ├── suggestion/         # Smart-Suggestion-Komponente (Ollama/Groq)
│   │   ├── receipt/            # Beleg-Scanner (OCR + LLM-Strukturierung, Stretch)
│   │   ├── integration/        # Externe-API-Adapter
│   │   ├── notification/       # E-Mail-Versand
│   │   └── common/             # Shared Utilities, Exceptions
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── db/migration/       # Flyway-Migrations
│   ├── src/test/java/...
│   ├── pom.xml
│   └── src/main/docker/        # von Quarkus generierte Dockerfiles (Dockerfile.jvm)
├── frontend/                   # Vue-Anwendung
│   ├── src/
│   │   ├── views/              # Routen-Komponenten
│   │   ├── components/         # Wiederverwendbare UI-Komponenten
│   │   ├── stores/             # Pinia-Stores
│   │   ├── services/           # API-Clients
│   │   ├── types/              # TypeScript-Interfaces
│   │   └── router/
│   ├── public/
│   ├── package.json
│   └── vite.config.ts
├── docker-compose.dev.yml      # Lokale Infrastruktur (Postgres, Ollama, Maildev)
├── scripts/                    # Helper (gen-jwt-keys.sh, smoke-test.sh)
├── docs/                       # VitePress-Projektdoku
├── .env.example
├── README.md
├── CLAUDE.md                   # diese Datei
└── IMPLEMENTATION.md           # detaillierter Phasen-Plan
```

---

## Architektur-Prinzipien

**Komponenten sind fachliche Module**, keine reinen technischen Layer. Jede Backend-Komponente hat:

- `entity/` — JPA-Entitäten (Hibernate Panache)
- `repository/` — Repository-Interfaces
- `service/` — Business-Logik
- `resource/` — REST-Endpunkte (`/api/v1/...`)
- `dto/` — Request/Response-Objekte (kein Entity-Leak nach außen)

**Komponenten kommunizieren über Service-Interfaces**, nicht über REST. Cross-Component-Calls laufen via CDI-Injection des Service-Interfaces einer anderen Komponente. Beispiel: `ShoppingListService` injiziert `PantryService`, nicht das Repository.

**Das Frontend spiegelt die Backend-Komponenten** in Pinia-Stores: `useRecipeStore`, `usePantryStore`, etc. Jede View-Komponente nutzt nur die Stores, die sie braucht.

---

## Konventionen

### Backend (Java)

- **Package-Naming:** `de.eateasy.<komponente>.<schicht>`, z. B. `de.eateasy.recipe.service`
- **Entity-Naming:** Singular, ohne Suffix (`Recipe`, nicht `RecipeEntity`)
- **DTO-Naming:** `<Name>Dto` für Response, `<Name>CreateRequest` / `<Name>UpdateRequest` für Eingaben
- **Service-Interface:** `<Name>Service` (Interface) + `<Name>ServiceImpl` (Implementierung) — Interface in der eigenen Komponente, Impl auch dort. Andere Komponenten injizieren das Interface.
- **REST-Pfade:** `/api/v1/recipes`, `/api/v1/households/{id}/members`, etc.
- **Jede REST-Methode wird mit `@RolesAllowed("user")`** abgesichert, außer explizit public (Login, Register).
- **Transaktionen:** `@Transactional` auf Service-Ebene, nicht auf Resource-Ebene.
- **Tests:** Pro Service mindestens Happy-Path + ein Edge-Case. REST-Endpunkte mit REST Assured.

### Frontend (Vue/TypeScript)

- **Strict TypeScript:** kein `any`, alle Stores und API-Clients voll typisiert.
- **Composition API + `<script setup>`** überall, keine Options API.
- **Pinia-Stores** sind die einzige Quelle für serverseitigen State. Keine Daten in Component-State, die auch andere Components brauchen.
- **API-Calls** ausschließlich über `services/<komponente>Service.ts`, nie direkt aus Components.
- **Routing:** Lazy-loaded Views (`() => import('...')`).
- **Komponenten-Naming:** PascalCase (`RecipeCard.vue`), in Verzeichnissen nach Feature gruppiert (`components/recipe/`).
- **Forms:** Validation clientseitig mit einfachen Helper-Functions, keine schwere Lib (Vuelidate o. ä.) ohne Bedarf.

### Git

- **Branches:** `feature/<slice-nr>-<kurzname>`, z. B. `feature/02-recipes`
- **Commits:** Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `chore:`)
- **PRs:** Beschreibung mit „Was/Warum/Wie getestet", mindestens ein Approval vor Merge auf `main`.

### Datenbank

- **Migrations:** Flyway, jede Schema-Änderung als neue Migration (`V<N>__<beschreibung>.sql`).
- **IDs:** UUID v4 für alle Entitäten. Keine Auto-Increment-Integer.
- **Soft-Delete** nur wenn fachlich nötig (Rezepte ja, Wochenplan-Einträge nein).
- **Timestamps:** `created_at`, `updated_at` auf jeder Tabelle, automatisch von Hibernate gepflegt.

---

## Commands

### Lokal entwickeln

```bash
# Alles hochfahren (Postgres, Ollama, Maildev)
docker-compose -f docker-compose.dev.yml up -d

# Backend (Quarkus Dev Mode mit Hot-Reload)
cd backend && ./mvnw quarkus:dev

# Frontend (Vite Dev Server)
cd frontend && npm run dev

# Tests
cd backend && ./mvnw test
cd frontend && npm run test:unit -- --run
```

### Produktion

> Ein dediziertes Produktions-Setup (eigene `docker-compose.yml`, gebautes
> Frontend-Image) ist bewusst **out of scope** für die Pflichtabgabe (siehe
> Abschnitt „Out of Scope"). Für die Demo läuft alles über `docker-compose.dev.yml`
> plus die auf dem Host gestarteten Dev-Server. Ein produktiver JVM-Build des
> Backends ist über das von Quarkus generierte `backend/src/main/docker/Dockerfile.jvm`
> möglich:
>
> ```bash
> cd backend && ./mvnw package
> docker build -f src/main/docker/Dockerfile.jvm -t eateasy/backend .
> ```

### Datenbank

```bash
# Migration neu anlegen
touch backend/src/main/resources/db/migration/V<N>__<name>.sql

# DB-Reset (lokale Dev-DB)
docker-compose -f docker-compose.dev.yml down -v
docker-compose -f docker-compose.dev.yml up -d postgres
```

### Ollama

```bash
# Modell ziehen (einmalig)
docker exec eateasy-ollama ollama pull llama3

# Test-Aufruf
curl http://localhost:11434/api/generate -d '{"model":"llama3","prompt":"Hallo"}'
```

---

## Was Claude Code beachten soll

1. **Nicht über Komponenten-Grenzen springen.** Wenn ein Feature Pantry und Recipe braucht, geht der Aufruf über `RecipeService` → `PantryService` (Interface), nie direkt auf `PantryRepository`.

2. **DTOs strikt trennen von Entities.** Niemals JPA-Entities aus REST-Endpunkten zurückgeben.

3. **Authorization-Checks im Service-Layer.** Jede Operation prüft, ob der eingeloggte User Zugriff auf die Ressource hat (z. B. „gehört dieses Rezept zu einem Haushalt, in dem ich Mitglied bin?"). Annotation-Level reicht nicht.

4. **Konfiguration über `application.properties` + Env-Vars.** Keine hardcoded Werte, keine Secrets im Repo. Lokale Defaults dürfen drinstehen.

5. **Nichts ohne Tests committen.** Mindestens ein Test pro neuem Service-Method, mindestens ein E2E-Test pro neuer REST-Route.

6. **README aktuell halten.** Wenn ein neuer Service / eine neue Env-Var dazukommt, README anpassen.

7. **IMPLEMENTATION.md ist das Briefing.** Bei Unklarheit dort nachschauen, nicht raten. Wenn etwas in IMPLEMENTATION.md unklar ist, den User fragen, nicht improvisieren.

---

## Out of Scope (für die Pflichtabgabe)

- Microservice-Splitting — wir bleiben modularer Monolith
- Production-grade Security-Härtung (CSRF-Token-Rotation, Rate Limiting auf Endpunktebene)
- Kubernetes-Deployment
- Mobile Apps
- Mehrsprachigkeit über DE hinaus (Frontend-Texte sind erstmal Deutsch)

Stretch Goals (siehe Optional-Liste in der Projektidee, IMPLEMENTATION.md Phase 11+): Beleg-Scanner, Google OAuth, MHD-Tracking, Filter-Erweiterungen, etc.
