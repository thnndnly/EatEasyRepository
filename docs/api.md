# API-Dokumentation

Quarkus generiert die vollständige OpenAPI-3-Spezifikation automatisch aus den REST-Resources. Während ein Backend läuft, sind beide Endpunkte direkt erreichbar:

- **Swagger UI (interaktiv):** [http://localhost:8080/q/swagger-ui](http://localhost:8080/q/swagger-ui)
- **OpenAPI-Spec (YAML/JSON):** [http://localhost:8080/q/openapi](http://localhost:8080/q/openapi)

## Authentifizieren in Swagger UI

1. Backend starten: `cd backend && ./mvnw quarkus:dev`
2. `http://localhost:8080/q/swagger-ui` öffnen
3. Per `POST /api/v1/auth/login` ein Token holen
4. Oben rechts auf **Authorize** klicken und das Token (ohne `Bearer `-Prefix) eintragen
5. Geschützte Endpunkte sind freigeschaltet

## Endpunkt-Übersicht

| Prefix | Komponente | Hauptoperationen |
| --- | --- | --- |
| `/api/v1/auth` | Auth | `POST /register`, `POST /login`, `GET /me` |
| `/api/v1/households` | Household | CRUD + Mitglieder, Einladungen |
| `/api/v1/recipes` | Recipe | CRUD, Filter, Import (TheMealDB) |
| `/api/v1/ingredients` | Ingredient | Stammdaten |
| `/api/v1/mealplans` | Mealplan | Wochenplan-Einträge |
| `/api/v1/pantry/*` | Pantry | Vorrats-Items, Barcode-Import (OpenFoodFacts) |
| `/api/v1/shopping-lists/*` | Shopping List | Auto-Berechnung, manuelle Anpassungen |
| `/api/v1/integration/*` | Integration | Externe Quellen anzapfen |
| `/api/v1/health` | – | Health-Probe, öffentlich |

Detaillierte Specs (Request-Schema, Response-Codes, Beispiele) erzeugt OpenAPI selbst aus den Annotationen und Bean-Validation-Constraints auf den DTOs.

## Konventionen

- **Auth:** `Authorization: Bearer <jwt>` für alles außer `/auth/{register,login}` und `/health`
- **Status-Codes:** `201 Created` bei Anlage, `204 No Content` bei Delete, `400` bei Validierungsfehlern, `401` ohne/abgelaufenes Token, `403` ohne Recht auf Ressource, `404` für nicht gefunden
- **Fehlerformat:** `{ "error": "Menschlich lesbare Nachricht" }`
- **IDs:** UUID v4 in allen Pfaden und Bodies
- **Datumsformate:** ISO-8601 in UTC (`2026-05-23T15:00:00Z`)
- **Pagination:** noch nicht implementiert; aktuell volle Listen
