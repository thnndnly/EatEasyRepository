# Lokale Entwicklungsumgebung — Onboarding

Diese Anleitung bringt dich vom frischen Rechner zur **vollständig laufenden
EatEasy-Dev-Umgebung**. Sie ist für neue Mitarbeiter:innen gedacht und setzt
kein Vorwissen über das Projekt voraus. Wer es kurz mag, findet die
Kommando-Übersicht unter [Setup & Commands](/setup).

> **Plattform:** Befehle gibt es jeweils für **Windows (PowerShell)** und
> **macOS/Linux (bash)**. Unter Windows empfehlen wir zusätzlich **Git Bash**
> (kommt mit „Git for Windows"), weil zwei Helper-Skripte (`scripts/*.sh`)
> bash voraussetzen.

---

## 1. Wie EatEasy lokal läuft

Die App besteht aus zwei selbst gestarteten Prozessen plus drei
Infrastruktur-Diensten in Docker:

```
                       ┌──────────────────────────────────────┐
   Browser ──────────► │  Frontend  (Vite Dev)  :5173          │
                       └───────────────┬──────────────────────┘
                                       │  REST /api/v1/...
                       ┌───────────────▼──────────────────────┐
                       │  Backend   (Quarkus Dev) :8080        │
                       └───┬──────────┬──────────┬─────────────┘
                           │          │          │
                ┌──────────▼──┐ ┌─────▼─────┐ ┌──▼──────────┐
                │ PostgreSQL  │ │  Ollama   │ │  Maildev    │
                │   :5432     │ │  :11434   │ │ :1025/:1080 │
                └─────────────┘ └───────────┘ └─────────────┘
                       └──────── Docker Compose ──────────┘
```

- **Frontend** und **Backend** startest du selbst auf dem Host (Hot-Reload).
- **Postgres, Ollama, Maildev** laufen als Container über
  `docker-compose.dev.yml`.

| Dienst | Port | Zweck | Schnelltest im Browser |
| --- | --- | --- | --- |
| Frontend (Vite) | `5173` | Vue-3-App | <http://localhost:5173> |
| Backend (Quarkus) | `8080` | REST-API | <http://localhost:8080/api/v1/health> |
| Swagger UI | `8080` | Interaktive API-Doku | <http://localhost:8080/q/swagger-ui> |
| PostgreSQL | `5432` | Datenbank | — (Client/IDE) |
| Ollama | `11434` | Lokales LLM für Suggestions | <http://localhost:11434> |
| Maildev Webview | `1080` | Versendete E-Mails ansehen | <http://localhost:1080> |
| Maildev SMTP | `1025` | E-Mail-Empfang | — (intern) |

---

## 2. Voraussetzungen installieren

| Tool | Version | Windows | macOS/Linux |
| --- | --- | --- | --- |
| **Java JDK** | **21** | [Temurin 21](https://adoptium.net/temurin/releases/?version=21) oder `winget install EclipseAdoptium.Temurin.21.JDK` | `brew install temurin@21` / Paketmanager |
| **Node.js** | **20.19+** oder **22.12+** | [nodejs.org](https://nodejs.org/) oder `winget install OpenJS.NodeJS.LTS` | `brew install node@22` / nvm |
| **Docker** | aktuell | [Docker Desktop](https://www.docker.com/products/docker-desktop/) | Docker Desktop / Docker Engine + Compose v2 |
| **Git** | aktuell | [Git for Windows](https://git-scm.com/download/win) (bringt Git Bash + OpenSSL mit) | meist vorinstalliert |
| **OpenSSL** | aktuell | über Git Bash vorhanden | meist vorinstalliert |

Maven wird **nicht** separat benötigt — das Repo liefert den Wrapper
(`mvnw` / `mvnw.cmd`) mit.

### Installation prüfen

::: code-group
```powershell [Windows · PowerShell]
java -version       # -> openjdk 21.x
node -v             # -> v20.19+ oder v22.12+
npm -v
docker version      # Client + Server müssen antworten
git --version
```
```bash [macOS/Linux · bash]
java -version       # -> openjdk 21.x
node -v             # -> v20.19+ oder v22.12+
npm -v
docker version      # Client + Server müssen antworten
git --version
```
:::

> **Docker Desktop muss laufen**, bevor du weitermachst (Wal-Symbol in der
> Taskleiste / Menüleiste sichtbar). Ohne laufenden Docker-Daemon scheitern
> sowohl die Infrastruktur als auch die Backend-Tests.

---

## 3. Repository klonen

::: code-group
```powershell [Windows · PowerShell]
git clone <repo-url> compprojekt
cd compprojekt
```
```bash [macOS/Linux · bash]
git clone <repo-url> compprojekt
cd compprojekt
```
:::

---

## 4. JWT-Keys erzeugen (einmalig pro Rechner)

Das Backend signiert seine Login-Tokens mit einem RSA-Keypaar. Die Keys werden
**bewusst nicht eingecheckt** (`.gitignore: *.pem`) und musst du lokal einmal
erzeugen. Sie landen in `backend/src/main/resources/`.

::: code-group
```bash [Git Bash / macOS / Linux]
bash scripts/gen-jwt-keys.sh
# -> JWT keys written to: .../backend/src/main/resources
```
```powershell [Windows · PowerShell (ohne Git Bash)]
$dest = "backend/src/main/resources"
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$dest/privateKey.pem"
openssl rsa -in "$dest/privateKey.pem" -pubout -out "$dest/publicKey.pem"
```
:::

**Verifikation** — beide Dateien müssen existieren:

```
backend/src/main/resources/privateKey.pem
backend/src/main/resources/publicKey.pem
```

> Fehlen die Keys, startet Quarkus zwar, aber Login/Registrierung schlägt mit
> JWT-Fehlern fehl.

---

## 5. Konfiguration (optional)

Für die Standard-Dev-Umgebung brauchst du **keine** eigene Konfiguration — alle
Defaults (`localhost`, Ports, DB-Credentials `eateasy/eateasy`) sind im Code und
in `docker-compose.dev.yml` hinterlegt.

Eine `.env` brauchst du nur, wenn du Defaults überschreiben willst (z. B. Ports)
oder einen **Spoonacular-API-Key** für den externen Rezept-Import setzen möchtest:

::: code-group
```powershell [Windows · PowerShell]
Copy-Item .env.example .env
```
```bash [macOS/Linux · bash]
cp .env.example .env
```
:::

> **Wichtig:** Die `.env` wird von **Docker Compose** gelesen (Postgres/Ollama/
> Maildev). Das auf dem Host laufende Backend liest die `.env` **nicht**
> automatisch. Für lokale Standardwerte ist das egal. Brauchst du z. B. den
> Spoonacular-Key im Backend, setze ihn als echte Umgebungsvariable, bevor du
> `quarkus:dev` startest:
>
> ```powershell
> $env:SPOONACULAR_API_KEY = "dein-key"   # PowerShell
> ```
> ```bash
> export SPOONACULAR_API_KEY="dein-key"   # bash
> ```
> Ohne Key funktioniert alles außer dem Spoonacular-Import (TheMealDB läuft
> keyfrei).

---

## 6. Infrastruktur hochfahren

::: code-group
```powershell [Windows · PowerShell]
docker compose -f docker-compose.dev.yml up -d
docker compose -f docker-compose.dev.yml ps   # Status prüfen
```
```bash [macOS/Linux · bash]
docker compose -f docker-compose.dev.yml up -d
docker compose -f docker-compose.dev.yml ps   # Status prüfen
```
:::

Postgres ist „healthy", sobald der Healthcheck greift (ein paar Sekunden).
Maildev erreichst du sofort unter <http://localhost:1080>.

---

## 7. Ollama-Modell ziehen (einmalig)

Ollama startet ohne Modell. Für die Smart-Suggestions musst du das konfigurierte
Modell einmal herunterladen (mehrere GB, dauert je nach Verbindung):

```bash
docker exec eateasy-ollama ollama pull llama3
```

> **Modellname:** Das Backend liest den Modellnamen aus `OLLAMA_MODEL`
> (Default in `application.properties`: `llama3.2`). Ziehe entweder das Modell,
> das du in der `.env` setzt, oder setze `OLLAMA_MODEL=llama3`, wenn du `llama3`
> gezogen hast. Wichtig ist: **gezogenes Modell == konfiguriertes Modell.**

Test:

```bash
curl http://localhost:11434/api/generate -d '{"model":"llama3","prompt":"Hallo"}'
```

> Ollama ist nur für das Smart-Suggestion-Feature (Phase 9) nötig. Der Rest der
> App läuft auch ohne gezogenes Modell.

---

## 8. Backend starten (Terminal 1)

::: code-group
```powershell [Windows · PowerShell]
cd backend
.\mvnw.cmd quarkus:dev
```
```bash [macOS/Linux · bash]
cd backend
./mvnw quarkus:dev
```
:::

- Beim ersten Start lädt Maven alle Dependencies (kann dauern).
- Flyway spielt die DB-Migrationen automatisch ein (`migrate-at-start=true`).
- Quarkus läuft im **Hot-Reload** — Code-Änderungen werden beim nächsten
  Request neu kompiliert.

**Verifikation:**

```bash
curl http://localhost:8080/api/v1/health
# {"status":"ok"}
```

Swagger UI: <http://localhost:8080/q/swagger-ui>

> Der Quarkus-Dev-Modus bietet eine interaktive Konsole: `d` öffnet die
> Dev-UI, `q` beendet, `r` startet Tests im Watch-Modus.

---

## 9. Frontend starten (Terminal 2)

::: code-group
```powershell [Windows · PowerShell]
cd frontend
npm install      # nur beim ersten Mal
npm run dev
```
```bash [macOS/Linux · bash]
cd frontend
npm install      # nur beim ersten Mal
npm run dev
```
:::

Öffne <http://localhost:5173>. Die Startseite zeigt **„Backend status: ok"**,
sobald das Backend antwortet — damit ist die Kette Frontend → Backend → DB
verifiziert.

---

## 10. End-to-End verifizieren (optional, empfohlen)

Wenn Backend, Maildev und Ollama laufen, prüft ein Skript die komplette Kette
über alle Features (Auth, Haushalt, Rezepte, Wochenplan, Vorrat, Einkaufsliste,
externe APIs, Ollama-Suggestion, Mail-Versand):

```bash
bash scripts/smoke-test.sh
```

Das Skript legt pro Lauf eindeutige Test-E-Mails an (`alice-<timestamp>@example.com`)
und ist beliebig wiederholbar. Versendete Mails siehst du im Maildev-Webview
(<http://localhost:1080>).

---

## 11. Täglicher Workflow

Einmal eingerichtet, brauchst du im Alltag nur noch:

::: code-group
```powershell [Windows · PowerShell]
# 1. Infrastruktur (falls nicht mehr laufend)
docker compose -f docker-compose.dev.yml up -d

# 2. Backend (Terminal 1)
cd backend; .\mvnw.cmd quarkus:dev

# 3. Frontend (Terminal 2)
cd frontend; npm run dev

# Feierabend: Container stoppen (Daten bleiben erhalten)
docker compose -f docker-compose.dev.yml stop
```
```bash [macOS/Linux · bash]
# 1. Infrastruktur (falls nicht mehr laufend)
docker compose -f docker-compose.dev.yml up -d

# 2. Backend (Terminal 1)
cd backend && ./mvnw quarkus:dev

# 3. Frontend (Terminal 2)
cd frontend && npm run dev

# Feierabend: Container stoppen (Daten bleiben erhalten)
docker compose -f docker-compose.dev.yml stop
```
:::

JWT-Keys, `npm install` und der Ollama-Pull sind **einmalige** Schritte und
entfallen danach.

---

## 12. Tests laufen lassen

| Bereich | Command (im jeweiligen Ordner) |
| --- | --- |
| Backend Unit + Integration | `./mvnw test` (Windows: `.\mvnw.cmd test`) |
| Frontend Unit (Vitest + MSW) | `npm run test:unit -- --run` |
| Frontend Coverage | `npm run test:coverage` → Report unter `frontend/coverage/index.html` |
| Frontend E2E (Playwright) | `npm run test:e2e` |
| TypeScript-Type-Check | `npm run type-check` |
| Lint (ESLint + oxlint) | `npm run lint` |

> Die Backend-Tests starten über **Quarkus Dev Services** automatisch einen
> Postgres-Testcontainer — **Docker muss dafür laufen**. Eine eigene DB ist
> nicht nötig.

---

## 13. Datenbank zurücksetzen

Wenn die lokale DB in einen kaputten Zustand gerät, kannst du sie komplett
neu aufsetzen (Flyway baut sie beim nächsten Backend-Start neu auf):

```bash
docker compose -f docker-compose.dev.yml down -v   # -v löscht das Volume!
docker compose -f docker-compose.dev.yml up -d postgres
```

> `-v` löscht **alle** lokalen DB-Daten unwiderruflich. Ohne `-v` bleiben die
> Daten beim Stoppen/Neustarten erhalten.

---

## 14. Troubleshooting

| Symptom | Ursache & Lösung |
| --- | --- |
| `Cannot connect to the Docker daemon` | Docker Desktop ist nicht gestartet. Starten und warten, bis das Wal-Symbol „running" zeigt. |
| Backend-Login wirft JWT-Fehler | `privateKey.pem` / `publicKey.pem` fehlen. Schritt 4 ausführen. |
| `Port 5432 already in use` | Lokale Postgres-Installation belegt den Port. Entweder stoppen oder in `.env` `DB_PORT` ändern und Container neu starten. |
| `Port 8080 already in use` | Anderer Prozess auf 8080. Beenden, oder Quarkus-Port via `quarkus.http.port` setzen. |
| Frontend zeigt „Backend status: error" | Backend läuft nicht / noch nicht hochgefahren. `curl http://localhost:8080/api/v1/health` prüfen. |
| Smart-Suggestions liefern nichts / Timeout | Ollama-Modell nicht gezogen oder Name passt nicht zu `OLLAMA_MODEL`. Siehe Schritt 7. |
| `npm install` schlägt mit Node-Version-Fehler fehl | Node zu alt. Auf 20.19+ oder 22.12+ aktualisieren. |
| Backend findet Java nicht / falsche Version | `JAVA_HOME` zeigt nicht auf JDK 21. Setzen und Terminal neu öffnen. |
| VitePress-Doku startet nicht auf 5173 | Der Vite-Frontend-Dev-Server belegt 5173 bereits. Frontend stoppen **oder** Doku auf anderem Port starten: `npm run docs:dev -- --port 5174`. |
| `scripts/*.sh` läuft unter Windows nicht | In **Git Bash** ausführen, nicht in PowerShell/CMD — oder die PowerShell-Variante aus Schritt 4 nutzen. |

Logs der Container ansehen:

```bash
docker compose -f docker-compose.dev.yml logs -f postgres   # bzw. ollama / maildev
```

---

## 15. Nächste Schritte

- **[Architektur](/architektur)** — Komponenten, Layer-Regeln, Datenmodell
- **[API](/api)** — OpenAPI-Spec & Swagger UI
- **[Tech-Stack](/tech-stack)** — eingesetzte Technologien im Überblick
- **`CLAUDE.md`** (Repo-Root) — verbindliche Code-Konventionen
- **`IMPLEMENTATION.md`** (Repo-Root) — Phasen-Plan, Datenmodell, vollständige API-Spec
