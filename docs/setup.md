# Setup & Commands

## Voraussetzungen

- **Java 21** (Temurin/OpenJDK)
- **Maven 3.9+** oder der mitgelieferte `./mvnw`-Wrapper
- **Node.js 20.19+ / 22.12+**
- **Docker Desktop** (für Postgres, Ollama, Maildev)
- **OpenSSL** (für JWT-Key-Generierung)

## Erster Start

```bash
git clone <repo-url> compprojekt
cd compprojekt

# 1) JWT-Keys lokal erzeugen (NICHT im Repo)
bash scripts/gen-jwt-keys.sh

# 2) Lokale Config (optional)
cp .env.example .env

# 3) Infrastruktur hochfahren (Postgres, Ollama, Maildev)
docker compose -f docker-compose.dev.yml up -d

# 4) Backend starten (Hot-Reload)
cd backend
./mvnw quarkus:dev
# → http://localhost:8080

# 5) Frontend starten (zweites Terminal)
cd frontend
npm install
npm run dev
# → http://localhost:5173
```

## Health-Check

```bash
curl http://localhost:8080/api/v1/health
# {"status":"ok"}
```

## Tests

| Bereich | Command | Hinweis |
| --- | --- | --- |
| Backend Unit + Integration | `cd backend && ./mvnw test` | Quarkus Dev Services startet einen Postgres-Testcontainer (Docker erforderlich) |
| Frontend Unit (Vitest + MSW) | `cd frontend && npm run test:unit` | Mock Service Worker liefert deterministische Backend-Antworten |
| Frontend Coverage | `cd frontend && npm run test:coverage` | HTML-Report unter `frontend/coverage/index.html` |
| Frontend E2E (Playwright) | `cd frontend && npm run test:e2e` | Vite-Dev-Server wird automatisch gestartet, Backend-Calls werden gemockt |
| Java-Lint | `cd backend && ./mvnw checkstyle:check` | Lenient, blockiert den Build nicht |
| TypeScript-Type-Check | `cd frontend && npm run type-check` | |
| ESLint + oxlint | `cd frontend && npm run lint` | Auto-Fix aktiv |

## Dev-Tools

- **Swagger UI:** `http://localhost:8080/q/swagger-ui`
- **OpenAPI-Spec:** `http://localhost:8080/q/openapi`
- **Maildev (E-Mails ansehen):** `http://localhost:1080`
- **Diese Doku-Site (VitePress):** `cd docs && npm run docs:dev` → `http://localhost:5173`

## Datenbank-Operationen

```bash
# Migration anlegen
touch backend/src/main/resources/db/migration/V<N>__<name>.sql

# Lokale DB komplett zurücksetzen
docker compose -f docker-compose.dev.yml down -v
docker compose -f docker-compose.dev.yml up -d postgres
```

## Ollama

```bash
# Modell einmalig ziehen
docker exec eateasy-ollama ollama pull llama3

# Test-Aufruf
curl http://localhost:11434/api/generate \
  -d '{"model":"llama3","prompt":"Hallo"}'
```
