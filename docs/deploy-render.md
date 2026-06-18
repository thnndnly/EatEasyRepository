# Gratis-Demo auf Render

Minimal-Deployment, um EatEasy oeffentlich vorzeigen zu koennen. Bewusst
**out of scope fuer die Pflichtabgabe** (siehe `CLAUDE.md`) — nur fuer Demos.

Alles ist in `render.yaml` (Repo-Root) beschrieben: ein Blueprint, der drei
Dinge auf dem Render-Free-Tier anlegt:

| Service       | Was                              | Verhalten im Free-Tier                        |
|---------------|----------------------------------|-----------------------------------------------|
| `eateasy-db`  | PostgreSQL 16                    | 1 GB, **laeuft nach 30 Tagen ab**, keine Backups |
| `eateasy-api` | Quarkus-Backend (Docker)         | schlaeft nach 15 Min, ~1 Min Kaltstart        |
| `eateasy-web` | statisches Vue-Build (CDN)       | immer sofort verfuegbar                        |

Single-Origin: `eateasy-web` proxyt `/api/*` und `/q/*` ans Backend, daher kein
CORS und keine Code-Aenderung am Frontend noetig.

## Einrichten (einmalig, ~5 Min)

1. Branch mit `render.yaml` nach GitHub pushen (oder nach `main` mergen).
2. Auf [render.com](https://render.com) einloggen (GitHub-Login).
3. **New → Blueprint** → dieses Repo auswaehlen → den Branch mit `render.yaml`.
4. **Apply**. Render baut Datenbank, Backend-Image und Frontend.

Beim ersten Boot legt Flyway (`migrate-at-start=true`) das Schema in der frischen
DB an — kein manueller Migrationsschritt noetig.

## Nach dem ersten Deploy pruefen

- **Backend-URL kontrollieren:** Wenn Render dem Backend einen Namenssuffix gibt
  (z. B. `eateasy-api-xy12.onrender.com`), die beiden `destination:`-Zeilen unter
  `eateasy-web` in `render.yaml` darauf anpassen und neu deployen. Das ist der
  einzige Stolperstein des Setups.
- **Swagger UI:** `https://<web-url>/q/swagger-ui` (laeuft ueber den Proxy).

## Bewusste Einschraenkungen der Demo

- **Smart-Suggestion (Ollama) ist nicht dabei** — das LLM braucht mehrere GB RAM
  und passt nicht in den Free-Tier. Der Vorschlags-Endpoint laeuft ins Timeout,
  falls er aufgerufen wird. Alle anderen Features funktionieren.
- **Keine echten E-Mails** (`QUARKUS_MAILER_MOCK=true`): Haushalts-Einladungen
  werden serverseitig "verschickt", aber nicht zugestellt.
- **JWT-Signierschluessel** ist das im Repo committete Demo-Keypaar. Fuer eine
  Wegwerf-Demo ok; fuer echten Betrieb ein eigenes Paar mit
  `scripts/gen-jwt-keys.sh` erzeugen und als Secret einspielen.
