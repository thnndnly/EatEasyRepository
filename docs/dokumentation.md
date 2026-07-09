# Projektdokumentation

Diese Seite fasst EatEasy als **zusammenhängende Projektdokumentation** zusammen:
von der fachlichen Einführung über die Komponentenarchitektur und den
Technologiestack bis zu Setup, Funktionsumfang und bekannten Einschränkungen.
Sie ist so aufgebaut, dass sie **von oben nach unten** gelesen werden kann und
für sich verständlich ist; für Detailtiefe verlinkt jeder Abschnitt auf die
jeweilige Spezialseite dieser Doku bzw. auf `IMPLEMENTATION.md` im Repo-Root.

---

## 1. Einführung in das Projekt

**EatEasy EE** ist eine komponentenbasierte Web-Anwendung für die
**haushaltsweite Mahlzeitenplanung**. Ein Haushalt verwaltet gemeinsam seine
Rezepte, plant Mahlzeiten für die Woche, pflegt seinen Vorrat und bekommt daraus
**automatisch eine Einkaufsliste** sowie **KI-gestützte Rezeptvorschläge**.

Entstanden ist das Projekt im Modul „Web Frameworks" als Studienprojekt der
**Gruppe 5** (Dimitrios Tsakos, Kardeena Kameran). Technisch ist es ein
**modularer Monolith**: ein Quarkus-Backend mit klar getrennten fachlichen
Komponenten und eine Vue-3-Single-Page-Application als Frontend, die über eine
REST-API kommunizieren.

Der Funktionsumfang ist in **vertikalen Slices** (Phasen 0–16) entstanden — jede
Phase liefert eine vollständige End-to-End-Funktionalität von der Datenbank bis
ins UI. Die Pflichtabgabe umfasst die Phasen 0–10; die Phasen 11–16 sind
umgesetzte Stretch-Goals (Beleg-Scanner, Google-Login, MHD-Tracking,
Auto-Nachbuchen, Favoriten/Portionen/PDF-Export, Kategorie-Sortierung).

> Vollständiger Phasen-Plan, Datenmodell und API-Spezifikation: `IMPLEMENTATION.md`.

---

## 2. Zielsetzung und Problemstellung

**Problem.** Mahlzeitenplanung in einem Haushalt ist verteiltes, fehleranfälliges
Handarbeiten: Rezepte liegen in Köpfen, Apps und auf Zetteln; wer kocht was wann;
was ist noch im Vorrat; und was muss eingekauft werden. Die Einkaufsliste wird
typischerweise manuell aus dem Wochenplan abgeleitet und dabei der vorhandene
Vorrat vergessen — man kauft doppelt oder es fehlt am Ende doch etwas. Diätwünsche
(vegetarisch, glutenfrei, …) mehrerer Haushaltsmitglieder machen die Rezeptauswahl
zusätzlich mühsam.

**Ziel.** EatEasy automatisiert genau diese Kette:

1. **Gemeinsame Datenbasis pro Haushalt** — Rezepte, Wochenplan und Vorrat sind
   für alle Mitglieder eines Haushalts sichtbar und pflegbar.
2. **Automatische Einkaufsliste** — aus *Wochenplan minus Vorrat* wird die
   benötigte Einkaufsmenge berechnet (inkl. Portionsskalierung, Mengen-Aggregation
   über mehrere Rezepte und Einheiten-Umrechnung).
3. **Reduzierung von Reibung** — Zutaten werden per Autocomplete gefunden/angelegt,
   Rezepte lassen sich aus externen Quellen importieren, Vorratseinträge per
   Barcode oder Kassenbon-Scan befüllen.
4. **Entscheidungshilfe** — „Was kann ich kochen?" schlägt aus dem aktuellen
   Vorrat passende Rezepte vor (LLM-gestützt, mit deterministischem Fallback).

**Nicht-Ziele (bewusst).** EatEasy ist ein Lernprojekt und bleibt ein modularer
Monolith; Produktions-Härtung, Microservice-Splitting, Mehrsprachigkeit und
mobile Apps sind ausdrücklich *out of scope* (siehe Abschnitt 9, „Bekannte
Einschränkungen").

---

## 3. Komponentenarchitektur

EatEasy ist ein **modularer Monolith**. Frontend und Backend sprechen
ausschließlich über die REST-API (`/api/v1/...`); **innerhalb** des Backends
kommunizieren Komponenten über **Service-Interfaces**, niemals über REST und
niemals am Interface einer anderen Komponente vorbei.

```
        Browser
          │  HTTPS / REST  (/api/v1/...)
          ▼
   ┌──────────────────────────────────────────────┐
   │  Frontend — Vue 3 SPA (Vite, Pinia, Router)    │
   │  views → stores → services (fetch)             │
   └──────────────────┬─────────────────────────────┘
                      │  JSON + JWT (Bearer)
   ┌──────────────────▼─────────────────────────────┐
   │  Backend — Quarkus (modularer Monolith)         │
   │                                                 │
   │  resource → service (Interface) → repository    │
   │  Komponenten rufen sich über Service-Interfaces │
   │  (z. B. ShoppingListService → PantryService)    │
   └───┬───────────┬───────────┬───────────┬─────────┘
       │           │           │           │
   PostgreSQL  Ollama/Groq  TheMealDB   Maildev/SMTP
    (Daten)    (LLM)        OpenFoodFacts  Tesseract (OCR)
```

### Schichten innerhalb einer fachlichen Komponente

```
de.eateasy.<komponente>
├── entity/       — JPA-Entitäten (Hibernate Panache)
├── repository/   — Panache-Repositories
├── service/      — Business-Logik (Interface + Impl)
├── resource/     — REST-Endpunkte unter /api/v1/...
└── dto/          — Request/Response-Objekte (kein Entity-Leak nach außen)
```

**Architektur-Regeln (verbindlich, siehe `CLAUDE.md`):**

- **Komponenten sind fachliche Module**, keine technischen Layer.
- Eine Komponente exponiert ihr **`Service`-Interface**; andere Komponenten
  injizieren das Interface, nie das Repository.
- **DTOs sind strikt getrennt von Entities** — Entities lecken nie aus
  REST-Endpunkten heraus. DTOs sind Java-`record`s mit Bean-Validation.
- **Authorization-Checks im Service-Layer** — die Annotation `@RolesAllowed("user")`
  reicht nicht; jede Operation prüft zusätzlich, ob der eingeloggte User Zugriff
  auf die konkrete Ressource hat (z. B. „gehört dieses Rezept zu einem Haushalt,
  in dem ich Mitglied bin?").
- **Transaktionen** auf Service-Ebene (`@Transactional`), nicht auf Resource-Ebene.

Das **Frontend spiegelt** die Backend-Komponenten in Pinia-Stores 1:1
(`useAuthStore`, `useHouseholdStore`, `useRecipeStore`, …). Views nutzen nur die
Stores, die sie brauchen; HTTP-Calls laufen ausschließlich über
`services/<komponente>Service.ts`.

> Ausführlicher inkl. Datenmodell-Highlights: **[Architektur](/architektur)**.

---

## 4. Beschreibung der wichtigsten Komponenten

### Backend (`backend/src/main/java/de/eateasy/`)

| Komponente | Aufgabe | Schichten |
| --- | --- | --- |
| **auth** | Registrierung, Login, JWT-Ausstellung, `/auth/me`; optionaler Google-Login (Token-Verify) | entity · repo · service · resource · dto |
| **household** | Haushalte, Mitgliedschaften (OWNER/MEMBER), Einladungs-Tokens, Auto-Nachbuchen-Flag | entity · repo · service · resource · dto |
| **ingredient** | Zutaten-Stammdaten (global, normalisiert), Kategorien für die Einkaufslisten-Sortierung | entity · repo · service · resource · dto |
| **recipe** | Rezepte inkl. Zutaten, Diät-Tags, Favoriten, Soft-Delete | entity · repo · service · resource · dto |
| **mealplan** | Wochenplan (pro Haushalt/Woche) mit Einträgen je Slot (Tag × Mahlzeit) + Portionen | entity · repo · service · resource · dto |
| **pantry** | Vorrat eines Haushalts inkl. Mengen-Aggregation und MHD-Tracking | entity · repo · service · resource · dto |
| **shoppinglist** | Auto-generierte Einkaufsliste aus *Mealplan − Pantry*, Abhaken, Auto-Nachbuchen | entity · repo · service · resource · dto |
| **suggestion** | Smart-Suggestions: Coverage-Ranking + LLM-Reranking (Ollama/Groq), deterministischer Fallback | service + `client/` (kein Entity) |
| **integration** | Adapter für **TheMealDB** (Rezept-Import) und **OpenFoodFacts** (Barcode-Lookup) | service + `client/` (kein Entity) |
| **receipt** | Beleg-Scanner: Tesseract-OCR → LLM-Strukturierung → Vorschau (persistiert selbst nichts) | service + `client/` (kein Entity) |
| **notification** | Versand transaktionaler E-Mails (Einladungs-Links) über Quarkus Mailer | service (kein Entity) |
| **common** | Querschnitt: Exceptions + Mapper, `security/CurrentUser`, `units/` (Unit/Converter/Parser), `diet/DietTag`, `HealthResource` | cross-cutting |

> **Hinweis:** `suggestion`, `integration`, `receipt` und `notification` besitzen
> **keine eigene JPA-Entität** — sie kapseln externe Systeme (LLM, Web-APIs, OCR,
> SMTP) hinter Service-/Client-Klassen. `common` ist ebenfalls keine fachliche
> Komponente, sondern gebündelte Querschnittslogik.

### Frontend (`frontend/src/`)

| Verzeichnis | Inhalt |
| --- | --- |
| `views/` | 13 Routen-Komponenten (Login/Register, Home-Dashboard, Haushalte, Rezepte, Wochenplan, Vorrat, Einkaufsliste, Einladung-Annahme) — lazy-loaded |
| `stores/` | 11 Pinia-Stores (`auth`, `household`, `ingredient`, `mealPlan`, `pantry`, `recipe`, `shoppingList`, `suggestion`, `integration`, `receipt`, `toast`) — einzige Quelle für Server-State |
| `services/` | 13 fetch-Wrapper pro Komponente; zentraler `apiClient.ts` injiziert das JWT als `Authorization: Bearer` |
| `components/` | Wiederverwendbare Bausteine, nach Feature gruppiert (`common/`, `auth/`, `household/`, `mealplan/`, `pantry/`, `recipe/`, `shoppinglist/`, `suggestion/`) |
| `composables/` | `useConfirmDialog`, `useRequireToken` |
| `router/` | Routen + Guards (`requiresAuth`, `guestOnly`) |
| `types/`, `utils/`, `config/` | TS-Interfaces (DTOs), Helper (`utils/mhd.ts` Ampel-Logik), Feature-Flags (`config/features.ts`) |

---

## 5. Verwendeter Technologiestack

| Layer | Technologie |
| --- | --- |
| **Frontend** | Vue 3.5 (Composition API, `<script setup>`), Vite 8, Pinia 3, Vue Router, **TypeScript (strict)**, Tailwind CSS 4, VueUse, `@zxing/browser` (Barcode) |
| **Backend** | **Quarkus 3.34** (Java 21), RESTEasy Reactive (`quarkus-rest`), Hibernate ORM Panache, Hibernate Validator, SmallRye JWT, SmallRye OpenAPI, Quarkus Mailer, REST Client |
| **Datenbank** | PostgreSQL 16, Schema-Verwaltung über **Flyway** (Migrationen V1–V12), Hibernate-Strategy `validate` |
| **LLM** | **Ollama** (self-hosted, Default `llama3`) — alternativ **Groq** (gehostete OpenAI-kompatible API) via `AI_PROVIDER` |
| **OCR** | Tesseract (HTTP-Wrapper-Container) für den Beleg-Scanner |
| **Externe APIs** | **TheMealDB** (Rezept-Import, kein Key), **OpenFoodFacts** (Barcode-Lookup) |
| **Mailer** | Maildev (lokal) / SMTP (Produktion) |
| **Build** | Maven (Backend, via `./mvnw`-Wrapper), npm + Vite (Frontend) |
| **Tests** | JUnit 5 + REST Assured + **Testcontainers** (Backend), **Vitest** + Vue Test Utils + **MSW** (Frontend-Unit), **Playwright** (E2E-Smoke) |
| **Quality** | Checkstyle (lenient, Backend), ESLint + oxlint + Prettier (Frontend) |
| **API-Doku** | OpenAPI 3 + Swagger UI, automatisch aus den REST-Resources generiert |
| **Infrastruktur** | Docker Compose (Postgres, Ollama, Maildev, Tesseract), GitHub Actions (CI), VitePress (diese Doku) |

> Auswahl-Begründungen und bewusst *nicht* eingesetzte Technologien (Nuxt,
> PrimeVue, Cypress …): **[Tech-Stack](/tech-stack)**.

---

## 6. Setup- und Installationsanleitung

**Voraussetzungen:** Java 21 · Node.js 20.19+ / 22.12+ · Docker Desktop ·
OpenSSL · (Maven wird über den mitgelieferten `./mvnw`-Wrapper bereitgestellt).

```bash
git clone <repo-url> compprojekt
cd compprojekt

# 1) JWT-Keys lokal erzeugen (werden NICHT eingecheckt).
#    Tests brauchen das nicht — ein Test-Keypaar liegt bereits im Repo.
bash scripts/gen-jwt-keys.sh

# 2) Optionale lokale Konfiguration
cp .env.example .env

# 3) Infrastruktur hochfahren (Postgres, Ollama, Maildev, Tesseract)
docker compose -f docker-compose.dev.yml up -d

# 4) Backend starten (Hot-Reload) — Terminal 1
cd backend && ./mvnw quarkus:dev          # → http://localhost:8080

# 5) Frontend starten — Terminal 2
cd frontend && npm install && npm run dev  # → http://localhost:5173
```

**Health-Check:** `curl http://localhost:8080/api/v1/health` → `{"status":"ok"}`.
Die Startseite unter <http://localhost:5173> zeigt „Backend status: ok", sobald
das Backend antwortet — damit ist die Kette Frontend → Backend → DB verifiziert.

**Für Smart-Suggestions** einmalig das LLM-Modell ziehen:
`docker exec eateasy-ollama ollama pull llama3`.

**Live-API-Doku (bei laufendem Backend):** Swagger UI unter
<http://localhost:8080/q/swagger-ui>, Spec unter `/q/openapi`.

**Tests:**

```bash
cd backend && ./mvnw test                       # JUnit 5 + REST Assured + Testcontainers (Docker nötig)
cd frontend && npm run type-check && npm run lint && npm run test:unit -- --run
cd frontend && npm run test:e2e                 # Playwright-Smoke (Backend gemockt)
```

> Ausführliches Onboarding (Windows PowerShell **und** macOS/Linux, JWT-Keys,
> Troubleshooting): **[Onboarding · Lokale Entwicklung](/lokale-entwicklung)**.
> Kurzreferenz aller Commands: **[Setup & Commands](/setup)**.
> Öffentliche Gratis-Demo auf Render: **[Deploy auf Render](/deploy-render)**.

---

## 7. Beschreibung der wichtigsten Funktionalitäten

| Funktion | Beschreibung |
| --- | --- |
| **Konten & Auth** | Registrierung/Login mit E-Mail + Passwort (BCrypt), JWT (8 h). Optional „Mit Google anmelden" (feature-geflaggt, Token-Verify + Account-Linking per E-Mail). |
| **Haushalte** | Haushalt anlegen (Ersteller wird OWNER), Mitglieder per E-Mail einladen (Token-Link, 7 Tage gültig), Einladung annehmen, Mitglieder entfernen. Standard-Diät-Tags pro Haushalt. |
| **Rezepte & Zutaten** | CRUD für Rezepte mit Zutatenliste, Portionen, Diät-Tags (Whitelist). Zutaten per Autocomplete finden/anlegen. Filter nach Tags/Suche/Haushalt. **Favoriten** (Herz-Button, per User). Soft-Delete. |
| **Rezept-Import** | Rezepte aus **TheMealDB** durchsuchen und importieren; importierte Rezepte sind voll bearbeitbar, `source_url` wird verlinkt. |
| **Wochenplan** | 7-Tage-Grid × 3 Mahlzeiten. Slots mit Rezepten belegen/leeren, Wochen-Navigation, **Portionen-Stepper** pro Slot. Rezept-Picker filtert mit den Haushalts-Standard-Diäten vor. Plan wird beim ersten Aufruf lazy angelegt. |
| **Vorrat (Pantry)** | Zutaten mit Menge, Einheit und MHD pflegen. Doppelte Zutaten werden mengenmäßig aggregiert. **MHD-Tracking**: bald ablaufende Items hervorgehoben, Dashboard-Widget „Demnächst ablaufend". |
| **Vorrat befüllen** | **Barcode-Scan** (Webcam via `@zxing/browser` → OpenFoodFacts-Lookup) und **Beleg-Scanner** (Kassenbon-Foto → Tesseract-OCR → LLM-Strukturierung → Vorschau bestätigen). |
| **Einkaufsliste** | Automatisch aus *Wochenplan − Vorrat* berechnet: Portionsskalierung, Mengen-Aggregation über Rezepte, Einheiten-Umrechnung. Abhaken, „Neu berechnen" (behält Haken nach Zutat). **Kategorie-Sortierung** für den effizienten Supermarkt-Gang, **Auto-Nachbuchen** (Abhaken legt den Posten in den Vorrat), **PDF-Export** via Druckansicht. |
| **Smart-Suggestions** | „Was kann ich kochen?" — Rezepte werden nach Vorrats-Abdeckung gerankt, das LLM (Ollama/Groq) wählt die besten aus und begründet sie. Fällt bei LLM-Ausfall deterministisch auf das Coverage-Ranking zurück (`aiAvailable=false`). |
| **Benachrichtigungen** | Haushalts-Einladungen gehen als echte E-Mail raus (lokal an Maildev). Bei Mail-Fehler wird die Einladung trotzdem angelegt. |

> Endpunkt-Übersicht und Aufruf-Konventionen: **[API-Dokumentation](/api)**.

---

## 8. Hinweise zur Projektstruktur

```
compprojekt/
├── backend/                    # Quarkus-Anwendung (Java 21)
│   ├── src/main/java/de/eateasy/
│   │   ├── auth/ household/ recipe/ ingredient/ mealplan/
│   │   ├── pantry/ shoppinglist/ suggestion/ integration/
│   │   ├── receipt/ notification/ common/
│   │   └── <komponente>/{entity,repository,service,resource,dto}/
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── db/migration/        # Flyway-Migrationen V1..V12
│   ├── src/test/java/...        # JUnit 5 + REST Assured
│   ├── src/main/docker/         # von Quarkus generierte Dockerfiles
│   └── pom.xml
├── frontend/                    # Vue-3-App (Vite + TypeScript)
│   └── src/{views,components,stores,services,composables,router,types,utils,config}/
├── docs/                        # diese VitePress-Doku
├── scripts/                     # Helper (gen-jwt-keys.sh, smoke-test.sh)
├── docker-compose.dev.yml       # lokale Infrastruktur
├── render.yaml                  # optionale Render-Demo (out of scope f. Abgabe)
├── .env.example                 # Environment-Variablen-Vorlage
├── CLAUDE.md                    # verbindliche Code-Konventionen
├── IMPLEMENTATION.md            # Phasen-Plan, Datenmodell, API-Spec
└── README.md                    # Schnellreferenz
```

**Orientierung:**

- **Backend-Komponente finden:** `backend/src/main/java/de/eateasy/<komponente>/`
  — darin immer dieselben Schichten (`entity/`, `repository/`, `service/`,
  `resource/`, `dto/`).
- **Frontend-Feature finden:** View unter `views/`, zugehöriger State im
  gleichnamigen `stores/<name>Store.ts`, HTTP im `services/<name>Service.ts`,
  UI-Bausteine unter `components/<feature>/`.
- **Schema-Änderung:** neue Flyway-Migration `V<N>__<name>.sql` unter
  `backend/src/main/resources/db/migration/` (nur additiv, wird beim Start
  automatisch eingespielt).
- **Konfiguration:** `application.properties` + Env-Vars; Defaults für lokale
  Entwicklung sind hinterlegt, Secrets/Keys **nicht** im Repo (`.pem`, `.env`
  sind ge-`.gitignore`-t).

**Git-Konventionen:** Branches `feature/<slice-nr>-<kurzname>`, Conventional
Commits, PR + mindestens ein Approval vor `main`, grüne CI (siehe
**[Kollaboration](/kollaboration)**).

---

## 9. Bekannte Einschränkungen

Bewusst getroffene Grenzen (Lernprojekt / *out of scope* der Pflichtabgabe) sowie
technische Limits des aktuellen Standes:

**Bewusst out of scope (siehe `CLAUDE.md`):**

- **Modularer Monolith**, kein Microservice-Splitting.
- **Nur Deutsch** — keine Mehrsprachigkeit/i18n im Frontend.
- **Keine Produktions-Härtung** — z. B. kein Rate-Limiting auf Endpunkt-Ebene,
  keine CSRF-Token-Rotation, kein Refresh-Token (nach 8 h Ablauf neu einloggen).
- **Kein produktives Deployment-Setup** — für die Demo laufen Dev-Server + Docker
  Compose; ein produktiver JVM-Build ist über das Quarkus-`Dockerfile.jvm` möglich.

**Feature-Verfügbarkeit / Abhängigkeiten:**

- **Google-Login** ist implementiert, aber **standardmäßig deaktiviert**
  (`EATEASY_GOOGLE_OAUTH_ENABLED=false`) — Aktivierung erfordert eine
  Google-Cloud-OAuth-Client-ID.
- **Smart-Suggestions** brauchen ein LLM: entweder ein gezogenes Ollama-Modell
  (mehrere GB RAM) oder einen Groq-API-Key. Fehlt beides, greift der
  Coverage-Fallback ohne Begründung.
- **Beleg-Scanner** braucht den Tesseract-Container **und** ein LLM; per Flag
  abschaltbar (`EATEASY_RECEIPT_ENABLED`).
- **Rezept-Import / Barcode-Lookup** hängen von der Verfügbarkeit der externen
  APIs (TheMealDB, OpenFoodFacts) ab. **Spoonacular** ist bewusst *nicht*
  implementiert (nur TheMealDB).

**Fachlich-technische Limits:**

- **Einheiten-Umrechnung** in der Einkaufslisten-Aggregation nur für
  GRAM ↔ KG und ML ↔ L; PIECE/TBSP/TSP werden nicht umgerechnet, sondern als
  eigene Position geführt.
- **Keine Pagination** — Listen (Rezepte, Vorrat, …) werden aktuell vollständig
  geladen.

**Render-Gratis-Demo (falls genutzt, nicht Teil der Abgabe):** Smart-Suggestion
(Ollama) und Beleg-Scanner sind dort deaktiviert (kein RAM im Free-Tier); die
Datenbank läuft nach 30 Tagen ab, das Backend hat nach Inaktivität einen
Kaltstart (~1 Min). Details: **[Deploy auf Render](/deploy-render)**.
