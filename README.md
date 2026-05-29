# EatEasy EE

Komponentenbasierte Web-Anwendung fuer haushaltsweite Mahlzeitenplanung,
Rezeptverwaltung und automatische Einkaufslisten. Studienprojekt, Gruppe 5
(Dimitrios Tsakos, Kardeena Kameran).

> Tiefer Einstieg: `CLAUDE.md` (Konventionen) und `IMPLEMENTATION.md`
> (Phasen-Plan, Datenmodell, API-Spec).

---

## Stack

| Layer    | Technologie                                                                  |
| -------- | ---------------------------------------------------------------------------- |
| Frontend | Vue 3 (Composition API), Vite, Pinia, Vue Router, TypeScript, Tailwind CSS 4 |
| Backend  | Quarkus 3.34 (Java 21), RESTEasy, Hibernate ORM Panache, SmallRye JWT, Mailer |
| Datenbank | PostgreSQL 16                                                               |
| LLM      | Ollama (Llama 3 / Mistral)                                                   |
| Mailer   | Maildev (lokal) / SMTP (Produktion)                                          |
| Build    | Maven (Backend), npm + Vite (Frontend)                                       |
| Tests    | JUnit 5 + REST Assured (Backend), Vitest + Vue Test Utils (Frontend)         |

---

## Voraussetzungen

- **Java 21** (z. B. Temurin/OpenJDK)
- **Maven 3.9+** _oder_ der mitgelieferte `./mvnw`-Wrapper
- **Node 20.19+ / 22.12+**
- **Docker Desktop** (oder Docker Engine + Compose v2)
- **OpenSSL** (fuer JWT-Keys)

---

## Setup von Null

```bash
git clone <repo-url> compprojekt
cd compprojekt

# 1) JWT-Keys für dev/prod lokal erzeugen (diese Keys werden NICHT eingecheckt).
#    Tests brauchen das NICHT: ein dediziertes Test-Keypaar liegt bereits unter
#    backend/src/test/resources/ im Repo, damit `mvnw test` und das CI ohne Setup laufen.
bash scripts/gen-jwt-keys.sh

# 2) Optional: lokale Konfig anlegen
cp .env.example .env

# 3) Infrastruktur hochfahren (Postgres, Ollama, Maildev)
docker compose -f docker-compose.dev.yml up -d

# 4) Backend starten (Hot-Reload)
cd backend
./mvnw quarkus:dev
# Backend laeuft auf http://localhost:8080

# 5) Frontend starten (zweites Terminal)
cd frontend
npm install        # nur beim ersten Mal
npm run dev
# Frontend laeuft auf http://localhost:5173
```

Health-Check:

```bash
curl http://localhost:8080/api/v1/health
# {"status":"ok"}
```

Im Browser oeffnet `http://localhost:5173` die Home-Seite und zeigt
„Backend status: ok", sobald das Backend antwortet.

### API-Dokumentation (OpenAPI / Swagger UI)

Quarkus generiert die OpenAPI-Spec automatisch aus den REST-Resources:

- **Spec (JSON/YAML):** `http://localhost:8080/q/openapi`
- **Swagger UI (interaktiv):** `http://localhost:8080/q/swagger-ui`

Authentifizierung in Swagger UI: über den `Authorize`-Knopf den JWT eintragen,
den `/api/v1/auth/login` zurückgibt.

### Projekt-Doku (VitePress)

Komplette Projektdoku (Setup, Architektur, API, Tech-Stack) liegt unter `docs/`:

```bash
cd docs
npm install        # nur beim ersten Mal
npm run docs:dev   # → http://localhost:5173 (VitePress-Dev-Server)
npm run docs:build # statische Site nach docs/.vitepress/dist
```

---

## Tests

```bash
# Backend (Quarkus startet einen Postgres-Testcontainer ueber Dev Services)
cd backend
./mvnw test

# Frontend
cd frontend
npm run lint
npm run type-check
npm run test:unit -- --run     # Vitest + MSW (Stores, Components)
npm run test:coverage          # mit Coverage-Report unter coverage/
npm run test:e2e               # Playwright Smoke (mocked Backend)
```

Aktueller Test-Stand:
- Backend: 203 Tests (JUnit 5 + REST Assured + Testcontainers)
- Frontend Unit: 75 Tests, 80%+ Coverage auf Stores + Services
- Frontend E2E: 3 Smoke-Tests (Login + Redirect)

**End-to-End-Smoke-Test** (Backend + Maildev + Ollama muessen laufen):

```bash
bash scripts/smoke-test.sh
```

Geht alle Phasen 0–10 live durch (Auth, Haushalt, Rezepte, Wochenplan,
Vorrat, Einkaufsliste, TheMealDB-Import, OpenFoodFacts-Lookup,
Ollama-Suggestion, Mail-Versand an Maildev). Setzt eindeutige
Test-Emails pro Run (`alice-<timestamp>@example.com`), kann beliebig
wiederholt werden.

**CI:** `.github/workflows/ci.yml` faehrt Backend-Tests (mit
Testcontainers/Postgres) und Frontend-Build parallel bei jedem
push/PR auf `main`.

---

## Datenbank

- Flyway-Migrationen liegen in `backend/src/main/resources/db/migration/`.
- Beim Start (`migrate-at-start=true`) werden offene Migrationen automatisch
  eingespielt.
- Neue Migration anlegen:
  ```bash
  touch backend/src/main/resources/db/migration/V<N>__<name>.sql
  ```
- Lokale Dev-DB zuruecksetzen:
  ```bash
  docker compose -f docker-compose.dev.yml down -v
  docker compose -f docker-compose.dev.yml up -d postgres
  ```

---

## Maildev (lokaler SMTP-Server)

- SMTP: `localhost:1025`
- Webview: <http://localhost:1080>

---

## Ollama (lokales LLM)

```bash
# Modell einmalig ziehen
docker exec eateasy-ollama ollama pull llama3

# Test-Aufruf
curl http://localhost:11434/api/generate \
  -d '{"model":"llama3","prompt":"Hallo"}'
```

Standard-Modell ist ueber `OLLAMA_MODEL` (Default: `llama3`) konfigurierbar.

---

## Projekt-Layout

```
compprojekt/
├── backend/                # Quarkus-Anwendung (Java 21)
├── frontend/               # Vue-3-Anwendung (Vite + TS)
├── scripts/                # Helper-Skripte (z. B. JWT-Keys)
├── docker-compose.dev.yml  # Lokale Infrastruktur (Postgres, Ollama, Maildev)
├── .env.example            # Environment-Variablen-Vorlage
├── CLAUDE.md               # Konventionen + Quick-Reference
├── IMPLEMENTATION.md       # Phasen-Plan, Datenmodell, API-Spec
└── README.md               # diese Datei
```

Die Backend-Komponenten leben unter `backend/src/main/java/de/eateasy/<komponente>/`
mit den Schichten `entity/`, `repository/`, `service/`, `resource/`, `dto/`.

---

## Phasen-Status

| Phase | Slice                          | Status |
| ----- | ------------------------------ | ------ |
| 0     | Projekt-Setup                  | done   |
| 1     | Auth                           | done   |
| 2     | Haushalt                       | done   |
| 3     | Zutaten + Rezepte              | done   |
| 4     | Wochenplan                     | done   |
| 5     | Vorrat                         | done   |
| 6     | Einkaufsliste                  | done   |
| 7     | Externe Rezept-API             | done   |
| 8     | Barcode-Scan                   | done   |
| 9     | Smart-Suggestion (Ollama)      | done   |
| 10    | E-Mail-Notifications           | done   |

Definitions of Done je Phase und Stretch-Goals stehen in `IMPLEMENTATION.md`.
