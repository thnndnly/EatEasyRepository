# EatEasy EE — Implementation Specification

Dieses Dokument ist der vollständige Bauplan für die Implementierung von EatEasy EE durch Claude Code. Es ist nach **vertikalen Slices** organisiert: Jede Phase liefert eine vollständige End-to-End-Funktionalität (Datenbank → Backend → REST-API → Frontend → Test) und ist demobereit.

> Lies vorher `CLAUDE.md` für Stack, Konventionen und Projektstruktur.

---

## Inhaltsverzeichnis

1. [Übersicht & Phasen-Plan](#1-übersicht--phasen-plan)
2. [Datenmodell (gesamt)](#2-datenmodell-gesamt)
3. [API-Spec (Übersicht)](#3-api-spec-übersicht)
4. [Phase 0: Projekt-Setup](#phase-0-projekt-setup)
5. [Phase 1: Auth — Registrieren, Login, JWT](#phase-1-auth--registrieren-login-jwt)
6. [Phase 2: Haushalt — Anlegen, Einladen, Mitgliedschaft](#phase-2-haushalt--anlegen-einladen-mitgliedschaft)
7. [Phase 3: Zutaten & Rezepte — CRUD mit Diät-Tags](#phase-3-zutaten--rezepte--crud-mit-diät-tags)
8. [Phase 4: Wochenplan](#phase-4-wochenplan)
9. [Phase 5: Vorratskammer](#phase-5-vorratskammer)
10. [Phase 6: Einkaufsliste (Aggregation)](#phase-6-einkaufsliste-aggregation)
11. [Phase 7: Externe Rezept-API (Spoonacular/TheMealDB)](#phase-7-externe-rezept-api)
12. [Phase 8: Barcode-Scan via OpenFoodFacts](#phase-8-barcode-scan-via-openfoodfacts)
13. [Phase 9: Smart-Suggestion (Ollama)](#phase-9-smart-suggestion-ollama)
14. [Phase 10: E-Mail-Notifications](#phase-10-e-mail-notifications)
15. [Stretch Goals (Phase 11+)](#stretch-goals-phase-11)
16. [Definition of Done](#definition-of-done)

---

## 1. Übersicht & Phasen-Plan

| # | Slice | Komponenten neu | Demo-Zustand danach |
|---|---|---|---|
| 0 | Projekt-Setup | — | Backend startet, Frontend lädt, DB verbunden |
| 1 | Auth | auth | User kann sich registrieren und einloggen |
| 2 | Haushalt | household | User kann Haushalt anlegen und Mitglieder verwalten |
| 3 | Rezepte + Zutaten | recipe, ingredient | User kann Rezepte mit Zutaten und Diät-Tags pflegen |
| 4 | Wochenplan | mealplan | User kann Rezepte Wochentagen zuweisen |
| 5 | Vorrat | pantry | User kann Vorratskammer pflegen |
| 6 | Einkaufsliste | shoppinglist | Liste wird aus Plan minus Vorrat berechnet |
| 7 | Rezept-Import | integration | Externe Rezepte können importiert werden |
| 8 | Barcode-Scan | (integration erweitert) | Vorrat befüllen via Barcode |
| 9 | KI-Vorschläge | suggestion | Vorschlag passender Rezepte zum Vorrat |
| 10 | E-Mail | notification | Haushaltseinladungen per Mail |

Jede Phase endet mit funktionierender Demo, grünen Tests und dokumentiertem README.

---

## 2. Datenmodell (gesamt)

```
User
  id: UUID (PK)
  email: VARCHAR(255) UNIQUE NOT NULL
  password_hash: VARCHAR(255) NOT NULL
  display_name: VARCHAR(100) NOT NULL
  created_at, updated_at

Household
  id: UUID (PK)
  name: VARCHAR(100) NOT NULL
  default_diet_tags: VARCHAR(50)[] (z. B. ['vegetarian'])
  created_at, updated_at

HouseholdMembership
  id: UUID (PK)
  user_id: UUID (FK → User)
  household_id: UUID (FK → Household)
  role: ENUM('OWNER', 'MEMBER')
  joined_at: TIMESTAMP
  UNIQUE(user_id, household_id)

HouseholdInvitation
  id: UUID (PK)
  household_id: UUID (FK)
  email: VARCHAR(255) NOT NULL
  token: VARCHAR(255) UNIQUE NOT NULL
  expires_at: TIMESTAMP NOT NULL
  accepted_at: TIMESTAMP NULL
  created_at

Ingredient
  id: UUID (PK)
  name: VARCHAR(100) NOT NULL
  default_unit: ENUM('GRAM', 'ML', 'PIECE', 'TBSP', 'TSP')
  created_at, updated_at
  -- global gepflegt, normalisiert (z. B. "Tomate" für "Tomaten", "Tomate frisch")

Recipe
  id: UUID (PK)
  owner_id: UUID (FK → User)
  household_id: UUID (FK → Household, NULL = privat)
  title: VARCHAR(200) NOT NULL
  description: TEXT
  instructions: TEXT NOT NULL
  servings: INT NOT NULL DEFAULT 2
  prep_minutes: INT
  diet_tags: VARCHAR(50)[] -- ['vegan', 'vegetarian', 'gluten_free', 'halal', 'low_carb']
  source_url: VARCHAR(500) NULL  -- bei Import gefüllt
  external_source: VARCHAR(50) NULL -- 'spoonacular', 'themealdb', 'manual'
  created_at, updated_at

RecipeIngredient
  id: UUID (PK)
  recipe_id: UUID (FK → Recipe)
  ingredient_id: UUID (FK → Ingredient)
  amount: DECIMAL(10,2) NOT NULL
  unit: ENUM(...) NOT NULL
  note: VARCHAR(200) NULL  -- z. B. "fein gehackt"

MealPlan
  id: UUID (PK)
  household_id: UUID (FK → Household)
  week_start: DATE NOT NULL  -- immer Montag
  UNIQUE(household_id, week_start)
  created_at, updated_at

MealPlanEntry
  id: UUID (PK)
  meal_plan_id: UUID (FK → MealPlan)
  day_of_week: ENUM('MONDAY' .. 'SUNDAY')
  meal_type: ENUM('BREAKFAST', 'LUNCH', 'DINNER')
  recipe_id: UUID (FK → Recipe)
  servings: INT NOT NULL
  UNIQUE(meal_plan_id, day_of_week, meal_type)

PantryItem
  id: UUID (PK)
  household_id: UUID (FK → Household)
  ingredient_id: UUID (FK → Ingredient)
  amount: DECIMAL(10,2) NOT NULL
  unit: ENUM(...) NOT NULL
  best_before: DATE NULL
  created_at, updated_at

ShoppingList
  id: UUID (PK)
  household_id: UUID (FK → Household)
  meal_plan_id: UUID (FK → MealPlan)
  created_at, updated_at
  UNIQUE(household_id, meal_plan_id)

ShoppingListItem
  id: UUID (PK)
  shopping_list_id: UUID (FK → ShoppingList)
  ingredient_id: UUID (FK → Ingredient)
  amount: DECIMAL(10,2) NOT NULL
  unit: ENUM(...) NOT NULL
  checked: BOOLEAN DEFAULT FALSE
```

**Beziehungen:**
- User n:m Household via HouseholdMembership
- Recipe 1:n RecipeIngredient n:1 Ingredient
- MealPlan 1:n MealPlanEntry, MealPlanEntry n:1 Recipe
- ShoppingList 1:n ShoppingListItem n:1 Ingredient

---

## 3. API-Spec (Übersicht)

Base: `/api/v1`. Alle Endpunkte außer `/auth/*` brauchen Bearer-Token im `Authorization`-Header.

```
POST   /auth/register              → 201 + JWT
POST   /auth/login                 → 200 + JWT
GET    /auth/me                    → 200 + User-Info

POST   /households                 → 201
GET    /households                 → 200 (Liste eigener Haushalte)
GET    /households/{id}            → 200
PATCH  /households/{id}            → 200 (Name, default_diet_tags)
POST   /households/{id}/invitations → 201 (verschickt Mail)
POST   /invitations/accept         → 200 (mit token im Body)
GET    /households/{id}/members    → 200
DELETE /households/{id}/members/{userId} → 204

GET    /ingredients                → 200 (Suche/Liste)
POST   /ingredients                → 201
GET    /recipes                    → 200 (eigene + Haushalt, Filter via Query)
POST   /recipes                    → 201
GET    /recipes/{id}               → 200
PATCH  /recipes/{id}               → 200
DELETE /recipes/{id}               → 204
POST   /recipes/import             → 201 (extern, body: {source, externalId})

GET    /households/{id}/mealplans?weekStart=YYYY-MM-DD → 200
POST   /households/{id}/mealplans  → 201 (anlegen für Woche)
PUT    /mealplans/{id}/entries     → 200 (Slot setzen)
DELETE /mealplans/{id}/entries/{day}/{mealType} → 204

GET    /households/{id}/pantry     → 200
POST   /households/{id}/pantry     → 201
PATCH  /pantry/{id}                → 200
DELETE /pantry/{id}                → 204
POST   /households/{id}/pantry/barcode → 201 (body: {barcode})

GET    /mealplans/{id}/shoppinglist → 200 (lazy, wird berechnet wenn nicht da)
PATCH  /shoppinglist/items/{id}    → 200 (checked toggle)

POST   /households/{id}/suggestions → 200 (KI-Vorschlag, body: {numSuggestions})
```

Detail-Specs (Request/Response-Bodies) stehen in den jeweiligen Phasen.

---

## Phase 0: Projekt-Setup

**Ziel:** Backend + Frontend + DB + Ollama laufen lokal, Hello-World-Endpunkt funktioniert.

### Schritte

1. **Repo-Init**
   - `git init`, `.gitignore` für Java, Node, IDE-Files
   - `README.md` mit Projektname, Setup-Anleitung
   - `CLAUDE.md` und `IMPLEMENTATION.md` ins Repo

2. **Backend-Skeleton (Quarkus)**
   - Generieren: https://code.quarkus.io mit Extensions:
     - `resteasy-reactive`, `resteasy-reactive-jackson`
     - `hibernate-orm-panache`, `jdbc-postgresql`, `flyway`
     - `smallrye-jwt`, `smallrye-jwt-build`
     - `mailer`
     - `rest-client-reactive-jackson`
     - `hibernate-validator`
     - `arc` (CDI)
   - GroupId `de.eateasy`, ArtifactId `eateasy-backend`, Java 21
   - Hello-Endpunkt unter `/api/v1/health` → `{"status":"ok"}`

3. **Frontend-Skeleton (Vue 3)**
   - `npm create vue@latest frontend` mit: TypeScript, Router, Pinia, Vitest, ESLint, Prettier
   - Tailwind CSS hinzufügen für Styling: `npm install -D tailwindcss postcss autoprefixer`
   - Erste View „Home" zeigt Health-Status vom Backend an

4. **docker-compose.dev.yml**
   ```yaml
   services:
     postgres:
       image: postgres:16
       environment:
         POSTGRES_DB: eateasy
         POSTGRES_USER: eateasy
         POSTGRES_PASSWORD: eateasy
       ports: ["5432:5432"]
       volumes: ["postgres_data:/var/lib/postgresql/data"]

     ollama:
       image: ollama/ollama:latest
       ports: ["11434:11434"]
       volumes: ["ollama_data:/root/.ollama"]

     maildev:
       image: maildev/maildev
       ports: ["1080:1080", "1025:1025"]

   volumes:
     postgres_data:
     ollama_data:
   ```

5. **application.properties (Backend)**
   ```properties
   quarkus.datasource.db-kind=postgresql
   quarkus.datasource.username=eateasy
   quarkus.datasource.password=${DB_PASSWORD:eateasy}
   quarkus.datasource.jdbc.url=jdbc:postgresql://${DB_HOST:localhost}:5432/eateasy

   quarkus.hibernate-orm.database.generation=validate
   quarkus.flyway.migrate-at-start=true
   quarkus.flyway.locations=classpath:db/migration

   quarkus.http.cors=true
   quarkus.http.cors.origins=http://localhost:5173
   quarkus.http.port=8080

   mp.jwt.verify.publickey.location=publicKey.pem
   mp.jwt.verify.issuer=https://eateasy.local
   smallrye.jwt.sign.key.location=privateKey.pem

   quarkus.mailer.host=${MAIL_HOST:localhost}
   quarkus.mailer.port=${MAIL_PORT:1025}
   quarkus.mailer.from=noreply@eateasy.local

   ollama.url=${OLLAMA_URL:http://localhost:11434}
   ollama.model=${OLLAMA_MODEL:llama3}

   integration.spoonacular.api-key=${SPOONACULAR_API_KEY:}
   integration.openfoodfacts.url=https://world.openfoodfacts.org
   ```

6. **Erste Flyway-Migration** `V1__init.sql` — leere Tabelle nur als Test, oder direkt User-Tabelle (siehe Phase 1).

7. **Verifikation**
   - `docker-compose -f docker-compose.dev.yml up -d` läuft fehlerfrei
   - `cd backend && ./mvnw quarkus:dev` startet, Health-Endpunkt erreichbar
   - `cd frontend && npm run dev` startet, Health-Status wird im Browser angezeigt
   - `curl http://localhost:8080/api/v1/health` → 200

### DoD Phase 0
- [ ] Repository auf GitHub
- [ ] `docker-compose up` startet alles ohne Fehler
- [ ] Backend antwortet auf `/api/v1/health`
- [ ] Frontend zeigt „Backend status: ok"
- [ ] README erklärt Setup von Null

---

## Phase 1: Auth — Registrieren, Login, JWT

**Ziel:** User kann sich registrieren, einloggen, JWT erhalten und einen geschützten Endpunkt aufrufen.

### Backend

**Migration `V2__user.sql`**
```sql
CREATE TABLE app_user (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(100) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_user_email ON app_user(email);
```

**Komponente `auth/`:**
- Entity `User` (Tabelle `app_user`)
- Repository `UserRepository extends PanacheRepositoryBase<User, UUID>`
- Service-Interface `AuthService` mit Methoden `register`, `login`, `getCurrentUser`
- Implementierung `AuthServiceImpl`:
  - Passwort-Hash via BCrypt (`org.mindrot:jbcrypt`)
  - Token-Generierung via `Jwt.issuer(...).upn(email).groups("user").claim("uid", userId).expiresIn(Duration.ofHours(8)).sign()`
- REST-Resource `AuthResource`:
  - `POST /auth/register` (public) — Body `{email, password, displayName}`, Response `{token, user}`
  - `POST /auth/login` (public) — Body `{email, password}`, Response `{token, user}`
  - `GET /auth/me` (`@RolesAllowed("user")`) — Response `{user}`
- DTOs: `RegisterRequest`, `LoginRequest`, `AuthResponse`, `UserDto`

**JWT-Keys generieren:**
```bash
openssl genrsa -out privateKey.pem 2048
openssl rsa -in privateKey.pem -pubout -out publicKey.pem
mv publicKey.pem privateKey.pem backend/src/main/resources/
```

**Tests:**
- `AuthServiceImplTest`: register-happy, register-duplicate-email, login-happy, login-wrong-password
- `AuthResourceTest` (REST Assured): alle drei Endpunkte E2E

### Frontend

- `services/authService.ts`: `register`, `login`, `getMe`, mit fetch + Bearer-Header-Helper
- `stores/authStore.ts` (Pinia): hält `user`, `token` (in `localStorage`), Aktionen `login`, `register`, `logout`, `restoreSession`
- Views: `LoginView.vue`, `RegisterView.vue`
- Router-Guard: nicht-eingeloggte User auf `/login` umleiten, eingeloggte vom `/login` auf Dashboard
- Layout: `MainLayout.vue` mit Topbar (Username, Logout-Button)
- Dashboard `HomeView.vue` (geschützt) zeigt „Hallo, <displayName>"

### DoD Phase 1
- [ ] `POST /auth/register` legt User an, gibt JWT zurück
- [ ] `POST /auth/login` mit korrektem Passwort gibt JWT zurück
- [ ] `GET /auth/me` mit gültigem JWT gibt User zurück
- [ ] Frontend speichert Token, leitet nach Login auf Dashboard
- [ ] Backend-Tests grün
- [ ] Reload behält Login-Status (token aus localStorage)

---

## Phase 2: Haushalt — Anlegen, Einladen, Mitgliedschaft

**Ziel:** User kann Haushalt anlegen, andere User per E-Mail einladen, Einladung annehmen.

> Mail-Versand ist in dieser Phase **simuliert** (Token-Link wird im UI angezeigt). Echtes Mailing kommt in Phase 10. Alternative: schon hier Maildev nutzen, dann Phase 10 nur Templates aufpolieren.

### Backend

**Migration `V3__household.sql`**
```sql
CREATE TABLE household (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(100) NOT NULL,
  default_diet_tags VARCHAR(50)[] DEFAULT '{}',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE household_membership (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  household_id UUID NOT NULL REFERENCES household(id) ON DELETE CASCADE,
  role VARCHAR(20) NOT NULL,
  joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(user_id, household_id)
);

CREATE TABLE household_invitation (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  household_id UUID NOT NULL REFERENCES household(id) ON DELETE CASCADE,
  email VARCHAR(255) NOT NULL,
  token VARCHAR(255) NOT NULL UNIQUE,
  expires_at TIMESTAMP NOT NULL,
  accepted_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_invitation_token ON household_invitation(token);
```

**Komponente `household/`:**
- Entities: `Household`, `HouseholdMembership`, `HouseholdInvitation`
- Repos für alle drei
- Service-Interface `HouseholdService`:
  - `create(userId, name): HouseholdDto`
  - `listForUser(userId): List<HouseholdDto>`
  - `get(userId, householdId): HouseholdDto` (mit Auth-Check)
  - `update(userId, householdId, request): HouseholdDto` (Name, default_diet_tags; nur OWNER)
  - `invite(userId, householdId, email): InvitationDto` (nur OWNER)
  - `acceptInvitation(userId, token): HouseholdDto`
  - `listMembers(userId, householdId): List<MemberDto>`
  - `removeMember(userId, householdId, memberId): void` (nur OWNER, nicht sich selbst)
- Helper-Method `assertMembership(userId, householdId)` und `assertOwner(userId, householdId)` werfen `ForbiddenException`
- REST-Resource `HouseholdResource` mit allen Endpunkten

**Wichtig:** Beim Anlegen wird der Creator automatisch als `OWNER` Mitglied. Tokens für Invitations 32 Zeichen random Base64URL, 7 Tage gültig.

**Tests:** Service-Tests für jede Methode, REST-Tests für Happy-Path und Auth-Verstoß (z. B. fremder User versucht Haushalt zu lesen → 403).

### Frontend

- `services/householdService.ts`
- `stores/householdStore.ts`: hält Liste der Haushalte des Users + currently selected household ID
- Views:
  - `HouseholdListView.vue` — Übersicht eigener Haushalte, „Neuer Haushalt"-Dialog
  - `HouseholdDetailView.vue` — Mitgliederliste, Einladungen-Sektion (nur Owner sichtbar), Edit-Form für Name/Diäten
  - `InvitationAcceptView.vue` — Routenparameter `?token=...`, zeigt Haushalt-Info, Button „Annehmen"
- `components/household/MemberList.vue`, `InviteForm.vue`
- Topbar: Dropdown zur Haushalt-Auswahl

### DoD Phase 2
- [ ] User kann Haushalt anlegen → wird automatisch Owner
- [ ] Owner kann andere User einladen → Token wird generiert
- [ ] Eingeladener User kann Token einlösen und tritt bei
- [ ] Owner kann Mitglieder entfernen
- [ ] Nicht-Mitglieder bekommen 403 bei Zugriffsversuch
- [ ] Frontend: Haushalt-Switcher in Topbar, Haushalt-Detail-Seite, Invitation-Annahme-Seite
- [ ] Default-Diet-Tags können auf Haushalt gesetzt werden

---

## Phase 3: Zutaten & Rezepte — CRUD mit Diät-Tags

**Ziel:** User kann globale Zutaten anlegen/finden und Rezepte mit Zutaten + Diät-Tags pflegen.

### Backend

**Migration `V4__ingredient_recipe.sql`**
```sql
CREATE TABLE ingredient (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(100) NOT NULL UNIQUE,
  default_unit VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ingredient_name ON ingredient(LOWER(name));

CREATE TABLE recipe (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id UUID NOT NULL REFERENCES app_user(id),
  household_id UUID REFERENCES household(id) ON DELETE SET NULL,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  instructions TEXT NOT NULL,
  servings INT NOT NULL DEFAULT 2,
  prep_minutes INT,
  diet_tags VARCHAR(50)[] DEFAULT '{}',
  source_url VARCHAR(500),
  external_source VARCHAR(50),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_recipe_owner ON recipe(owner_id);
CREATE INDEX idx_recipe_household ON recipe(household_id);

CREATE TABLE recipe_ingredient (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  recipe_id UUID NOT NULL REFERENCES recipe(id) ON DELETE CASCADE,
  ingredient_id UUID NOT NULL REFERENCES ingredient(id),
  amount DECIMAL(10,2) NOT NULL,
  unit VARCHAR(20) NOT NULL,
  note VARCHAR(200)
);
CREATE INDEX idx_recipe_ingredient_recipe ON recipe_ingredient(recipe_id);
```

**Komponente `ingredient/`:**
- Service `IngredientService`: `findOrCreate(name, defaultUnit)`, `search(query)`, `getById(id)`
- Idempotente Anlage: Wenn der Name (case-insensitive) schon existiert, gib bestehende zurück.

**Komponente `recipe/`:**
- Entities: `Recipe`, `RecipeIngredient`
- `RecipeService`:
  - `list(userId, filter)` — gibt Rezepte zurück, die dem User selbst oder einem seiner Haushalte gehören. Filter-Parameter: `dietTags` (alle müssen matchen), `q` (Volltextsuche im Titel), `householdId` (optional)
  - `create(userId, request): RecipeDto` — Diät-Tags validieren gegen Whitelist, Zutaten via `IngredientService.findOrCreate` resolven
  - `get(userId, id)`, `update(userId, id, request)`, `delete(userId, id)` — alle mit Auth-Check (Owner oder Haushalt-Mitglied bei `household_id != null`)
- DTOs: `RecipeDto` (mit eingebetteten Zutaten), `RecipeCreateRequest`, `RecipeUpdateRequest`, `RecipeIngredientDto`, `RecipeFilterDto`
- **Diät-Tag-Whitelist** im Code als Enum oder Konstante: `vegan`, `vegetarian`, `gluten_free`, `halal`, `low_carb`, `dairy_free`. Whitelist im Frontend gespiegelt.

**Tests:** CRUD happy-path, Filter-Kombinationen, Auth-Verstöße.

### Frontend

- `services/recipeService.ts`, `services/ingredientService.ts`
- `stores/recipeStore.ts`: List + currentRecipe
- Views:
  - `RecipeListView.vue` — Grid/Liste, Filter-Sidebar (Tags + Suche), „Neues Rezept"-Button
  - `RecipeDetailView.vue` — Read-Only-Anzeige mit Zutatenliste, Edit-Button
  - `RecipeFormView.vue` — Anlegen/Bearbeiten in einem Form (Routen `/recipes/new` und `/recipes/:id/edit`)
- `components/recipe/`:
  - `RecipeCard.vue` — Kachel mit Titel, Tags, Zubereitungszeit
  - `IngredientPicker.vue` — Autocomplete-Input mit Lazy-Search auf Backend, fällt auf „neu anlegen" zurück
  - `DietTagSelector.vue` — Multi-Checkbox mit Whitelist-Tags
  - `RecipeIngredientRow.vue` — Eine Zeile im Form (Zutat, Menge, Einheit, Note, Löschen-Button)

### DoD Phase 3
- [ ] User kann Rezept mit ≥1 Zutat und ≥0 Diät-Tags anlegen
- [ ] Rezeptliste zeigt eigene + Haushaltsrezepte, filterbar nach Tags und Suche
- [ ] Bearbeiten und Löschen funktioniert mit Auth-Check
- [ ] Zutaten werden per Autocomplete gefunden, neue Zutaten werden auto-angelegt
- [ ] Backend-Validation: Diät-Tags nur aus Whitelist, Servings > 0, Title nicht leer

---

## Phase 4: Wochenplan

**Ziel:** Haushalt hat Wochenplan, Rezepte können Slots (Tag × Mahlzeit) zugewiesen werden.

### Backend

**Migration `V5__mealplan.sql`**
```sql
CREATE TABLE meal_plan (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  household_id UUID NOT NULL REFERENCES household(id) ON DELETE CASCADE,
  week_start DATE NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(household_id, week_start)
);

CREATE TABLE meal_plan_entry (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  meal_plan_id UUID NOT NULL REFERENCES meal_plan(id) ON DELETE CASCADE,
  day_of_week VARCHAR(10) NOT NULL,
  meal_type VARCHAR(20) NOT NULL,
  recipe_id UUID NOT NULL REFERENCES recipe(id),
  servings INT NOT NULL,
  UNIQUE(meal_plan_id, day_of_week, meal_type)
);
```

**Komponente `mealplan/`:**
- `MealPlanService`:
  - `getOrCreate(userId, householdId, weekStart): MealPlanDto` — `weekStart` wird auf Montag normalisiert. Plan wird lazy angelegt.
  - `setEntry(userId, mealPlanId, day, mealType, recipeId, servings): MealPlanEntryDto`
  - `removeEntry(userId, mealPlanId, day, mealType): void`
  - `getCurrent(userId, householdId): MealPlanDto` — aktuelle Woche
- Auth-Check: User muss Mitglied des Haushalts sein
- DTOs: `MealPlanDto` (mit eingebetteten Entries inkl. Recipe-Mini-DTO), `MealPlanEntryDto`, `SetEntryRequest`

**Cross-Component-Aufruf:** `MealPlanService` injiziert `RecipeService` für Lookup beim Setzen einer Entry (Existenz prüfen, Auth-Check für Recipe).

**Tests:** Plan auto-create, Entry setzen + überschreiben, Entry löschen, Auth-Verstöße.

### Frontend

- `services/mealPlanService.ts`
- `stores/mealPlanStore.ts`: hält aktuellen Plan + week-navigation
- Views:
  - `MealPlanView.vue` — 7-Tages-Grid mit 3 Mahlzeiten-Slots pro Tag, Wochen-Navigation („Vorherige", „Nächste", „Heute"), Drag-and-Drop optional
- `components/mealplan/`:
  - `MealPlanGrid.vue`
  - `MealSlot.vue` — eine Zelle, zeigt Rezept oder „+"-Placeholder
  - `RecipePickerModal.vue` — Dialog mit Rezept-Filter (nutzt `RecipeService.list` mit Tag-Filter; berücksichtigt `household.default_diet_tags` als Vorfilter)

### DoD Phase 4
- [ ] User kann zur aktuellen Woche springen, vor/zurück navigieren
- [ ] Klick auf leeren Slot öffnet Rezept-Picker
- [ ] Rezept-Picker filtert vor mit Haushalts-Standard-Diäten
- [ ] Slot kann überschrieben oder geleert werden
- [ ] Plan wird beim ersten Aufruf auto-angelegt

---

## Phase 5: Vorratskammer

**Ziel:** Haushalt pflegt Liste vorhandener Zutaten mit Mengen.

### Backend

**Migration `V6__pantry.sql`**
```sql
CREATE TABLE pantry_item (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  household_id UUID NOT NULL REFERENCES household(id) ON DELETE CASCADE,
  ingredient_id UUID NOT NULL REFERENCES ingredient(id),
  amount DECIMAL(10,2) NOT NULL,
  unit VARCHAR(20) NOT NULL,
  best_before DATE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_pantry_household ON pantry_item(household_id);
```

**Komponente `pantry/`:**
- `PantryService`:
  - `list(userId, householdId): List<PantryItemDto>`
  - `add(userId, householdId, request): PantryItemDto` — wenn Zutat schon im Vorrat, addiert Menge stattdessen (mit Unit-Konvertierung wenn möglich, sonst als neuer Eintrag)
  - `update(userId, itemId, request)`, `delete(userId, itemId)`
  - `getCurrentInventory(householdId): Map<UUID, BigDecimal>` (in Basis-Einheit) — für Phase 6 und 9
- DTOs

**Tests:** CRUD, Mengen-Aggregation bei Duplikaten, Auth.

### Frontend

- `stores/pantryStore.ts`
- `views/PantryView.vue` — Tabelle mit Zutat, Menge, Einheit, MHD; Inline-Edit; „+"-Button für neuen Eintrag
- `components/pantry/PantryRow.vue`, `AddItemForm.vue`

### DoD Phase 5
- [ ] User kann Pantry-Items anlegen/ändern/löschen
- [ ] Bei doppelter Zutat: Mengen werden addiert (gleiche Einheit) oder als neue Zeile gehandhabt
- [ ] Liste zeigt MHD-Spalte (sortiert ascending)

---

## Phase 6: Einkaufsliste (Aggregation)

**Ziel:** Aus Wochenplan minus Vorrat wird automatisch eine Einkaufsliste berechnet.

### Backend

**Migration `V7__shoppinglist.sql`**
```sql
CREATE TABLE shopping_list (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  household_id UUID NOT NULL REFERENCES household(id) ON DELETE CASCADE,
  meal_plan_id UUID NOT NULL REFERENCES meal_plan(id) ON DELETE CASCADE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(household_id, meal_plan_id)
);

CREATE TABLE shopping_list_item (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  shopping_list_id UUID NOT NULL REFERENCES shopping_list(id) ON DELETE CASCADE,
  ingredient_id UUID NOT NULL REFERENCES ingredient(id),
  amount DECIMAL(10,2) NOT NULL,
  unit VARCHAR(20) NOT NULL,
  checked BOOLEAN NOT NULL DEFAULT FALSE
);
```

**Komponente `shoppinglist/`:**
- `ShoppingListService`:
  - `getOrGenerate(userId, mealPlanId): ShoppingListDto`
    - Wenn Liste schon existiert und Plan unverändert → zurückgeben
    - Sonst: aggregiere alle Zutaten aller `MealPlanEntry`s × `servings`-Skalierung, ziehe `PantryService.getCurrentInventory(householdId)` ab, schreibe Diff in Liste
  - `regenerate(userId, mealPlanId)` — explizites Neuberechnen (löscht und legt neu an, behält `checked`-Status soweit möglich nach Ingredient-ID)
  - `toggleChecked(userId, itemId, checked): ShoppingListItemDto`
- **Cross-Component:** Service injiziert `MealPlanService`, `RecipeService`, `PantryService`, `IngredientService`
- **Aggregations-Logik:**
  - Pro Recipe×Entry: Skaliere Zutaten = `recipeIngredient.amount * (entry.servings / recipe.servings)`
  - Aggregiere über alle Entries: gruppiere nach `(ingredient_id, unit)`, summiere amounts
  - Subtrahiere Pantry: pro Ingredient-ID, in passender Unit (Unit-Konvertierung über Helper-Class `UnitConverter`, einfache Tabelle für GRAM↔KG, ML↔L; PIECE/TBSP/TSP nicht konvertiert)
  - Wenn `pantry >= benötigt`: Item kommt nicht in Liste
  - Wenn `pantry < benötigt`: Item in Liste mit `benötigt - pantry`

**Tests:**
- `ShoppingListServiceTest`: 
  - Plan ohne Pantry → Liste = Summe aller Recipe-Zutaten
  - Plan mit teilweisem Pantry → Liste = Diff
  - Plan mit komplettem Pantry → leere Liste
  - Servings-Skalierung
  - Edge-Case: gleiche Zutat, gleiche Unit über mehrere Rezepte → ein Listen-Item

### Frontend

- `stores/shoppingListStore.ts`
- `views/ShoppingListView.vue` — Liste mit Checkboxen, gruppiert nach optional Kategorie (für später) oder Alphabet, „Neu berechnen"-Button
- `components/shoppinglist/ShoppingListItem.vue`

### DoD Phase 6
- [ ] Liste wird korrekt aus Wochenplan minus Vorrat berechnet
- [ ] Mengen werden über mehrere Rezepte richtig aggregiert
- [ ] Servings-Skalierung pro Wochenplan-Entry funktioniert
- [ ] Items abhakbar, abgehakte sichtbar abgesetzt
- [ ] „Neu berechnen" wirft Liste neu auf, behält checked-Status nach Ingredient-ID

---

## Phase 7: Externe Rezept-API

**Ziel:** User kann Rezepte aus externer Quelle (Spoonacular oder TheMealDB) importieren.

> **Empfehlung:** Beide unterstützen, mit `external_source`-Feld. TheMealDB ist gratis und braucht keinen API-Key — gute Default-Wahl für Demo.

### Backend

**Komponente `integration/`:**
- `RecipeImportService`:
  - `searchExternal(source, query): List<ExternalRecipePreviewDto>`
  - `importRecipe(userId, source, externalId, householdId): RecipeDto`
- `SpoonacularClient` (MicroProfile REST Client) — falls API-Key vorhanden
- `TheMealDbClient` (MicroProfile REST Client) — als Default
- **Mapping:** Externe Daten → interne `Recipe` + `RecipeIngredient`. Zutaten via `IngredientService.findOrCreate`. Diät-Tags aus externen Tags ableiten (mapping Tabelle).
- `external_source` und `source_url` werden auf dem importierten Recipe gesetzt
- REST-Endpoints:
  - `GET /integration/recipes/search?source=themealdb&q=pasta` → Liste Previews
  - `POST /recipes/import` Body `{source, externalId, householdId?}` → erstellt Recipe und gibt zurück

**Tests:**
- Mit `@QuarkusTest` und Mock-WireMock-Server: Suche, Import-Roundtrip, Fehlerfälle (404, Timeout)

### Frontend

- Neue Tab/Modal in `RecipeListView`: „Aus Quelle importieren"
- `components/recipe/ExternalRecipeSearch.vue` — Source-Auswahl (Dropdown), Suche, Liste mit Preview-Cards, „Importieren"-Button pro Card

### DoD Phase 7
- [ ] User kann TheMealDB durchsuchen und ein Rezept importieren
- [ ] Importiertes Rezept hat Zutaten und ist genauso bearbeitbar wie eigene
- [ ] `source_url` wird auf Detail-Seite verlinkt

---

## Phase 8: Barcode-Scan via OpenFoodFacts

**Ziel:** User scannt einen Produktbarcode mit dem Handy/Webcam, OpenFoodFacts liefert Produktdaten, Item landet im Vorrat.

### Backend

**Komponente `integration/` erweitert:**
- `OpenFoodFactsClient` (MicroProfile REST Client gegen `https://world.openfoodfacts.org/api/v2/product/{barcode}.json`)
- `BarcodeService.lookup(barcode): ProductInfoDto` — gibt Name, vorgeschlagene Einheit zurück
- Endpoint: `POST /households/{id}/pantry/barcode` Body `{barcode, amount, unit}` → erstellt PantryItem direkt (Lookup + IngredientService.findOrCreate inline)

**Edge-Cases:**
- Produkt nicht gefunden → 404 mit Hinweis
- Produktname auf Deutsch wenn verfügbar (`product_name_de`), sonst `product_name`

### Frontend

- `components/pantry/BarcodeScanner.vue` mit `@zxing/browser`:
  - Aktiviert Kamera, scannt EAN-13/UPC
  - On-Detect: Aufruf an Backend, zeigt Produkt-Bestätigungsdialog mit Mengen-Eingabe
- Integration in `PantryView.vue` als „Barcode scannen"-Button
- Fallback bei Geräten ohne Kamera: manuelles Barcode-Eingabefeld

### DoD Phase 8
- [ ] Kamera-Scan erkennt EAN-Codes im Browser
- [ ] Bekannter Barcode → Produkt-Daten werden angezeigt, User bestätigt + setzt Menge
- [ ] Item landet korrekt im Vorrat, Zutat wird automatisch angelegt wenn neu

---

## Phase 9: Smart-Suggestion (Ollama)

**Ziel:** User klickt „Was kann ich kochen?" → System schlägt 3 Rezepte vor, basierend auf aktuellem Vorrat.

> **Empfehlung:** **Hybrid-Ansatz** — zuerst die Rezeptliste filtern auf Rezepte, deren Zutaten zu großen Teilen im Vorrat sind, dann Ollama nur als Reranker / Begründer einsetzen. Reines LLM-Rezeptegenerieren ist toll für Kreativität, aber unzuverlässig für die Demo.

### Backend

**Komponente `suggestion/`:**
- `OllamaClient` (MicroProfile REST Client gegen `http://ollama:11434/api/generate`)
- `SmartSuggestionService`:
  - `suggest(userId, householdId, numSuggestions): List<SuggestionDto>`
  - **Algorithmus:**
    1. Hole alle Rezepte des Users + Haushalts (`recipeService.list`)
    2. Hole Vorrat (`pantryService.getCurrentInventory`)
    3. Berechne pro Rezept: `coverage = Anzahl der Zutaten, die im Vorrat sind / Gesamtanzahl der Zutaten`
    4. Filter Rezepte mit `coverage >= 0.5`, sortiere descending
    5. Top 10 nehmen
    6. Ollama-Prompt: „Hier sind 10 Rezepte und ein Vorrat. Wähle die 3 besten Empfehlungen und begründe jeweils kurz, warum (nutze nur die genannten Daten)." → JSON-Antwort mit `[{recipeId, reason}]`
    7. Wenn Ollama-Call fehlschlägt → Fallback auf Top 3 nach `coverage` ohne Begründung
- DTOs: `SuggestionDto` (recipe + reason + coverage)
- Endpoint: `POST /households/{id}/suggestions` Body `{numSuggestions: 3}`

**Prompt-Template:**
```
Du hilfst bei der Rezeptauswahl. Vorrat: {pantry-list}.
Verfügbare Rezepte (nur aus dieser Liste auswählen):
{recipe-list mit ID, Titel, Zutaten}

Wähle die {n} besten Vorschläge. Antworte AUSSCHLIESSLICH mit JSON:
[{"recipeId":"<id>","reason":"<kurzer Grund>"}]
```

**Tests:**
- Mock OllamaClient: Happy-Path, JSON-Parse-Fehler → Fallback, Timeout → Fallback
- Coverage-Berechnung isoliert testen

### Frontend

- Neue Sektion auf Dashboard: „Was kann ich kochen?"-Button
- Auf Klick: Loader, dann Liste mit 3 Rezept-Cards + Begründung pro Card + „Zum Wochenplan hinzufügen"-Button
- Bei Fallback (kein Reason): Cards trotzdem anzeigen mit „aus Vorratsabdeckung berechnet"-Hinweis

### DoD Phase 9
- [ ] Bei vollem Vorrat: Vorschläge passend, mit Begründung
- [ ] Bei leerem Vorrat: leere Liste oder Hinweis „kein passendes Rezept gefunden"
- [ ] Ollama-Call timeoutet nach 30s, Fallback funktioniert
- [ ] Vorschlag kann mit einem Klick in Wochenplan übernommen werden (öffnet Picker für Slot-Auswahl)

---

## Phase 10: E-Mail-Notifications

**Ziel:** Haushaltseinladungen werden per E-Mail verschickt (lokal: Maildev unter `http://localhost:1080`).

### Backend

**Komponente `notification/`:**
- `NotificationService`:
  - `sendInvitation(invitation, householdName, inviterName)` — verschickt Mail mit Token-Link
  - `sendCookingReminder(...)` — optional, für Stretch
- Templates in `src/main/resources/templates/`:
  - `invitation.html` (Quarkus Qute oder einfaches String-Template)
- Wird in `HouseholdService.invite()` aufgerufen statt nur Token zu generieren
- Auch als REST-Endpoint testbar: `POST /test/email` (nur in dev profile)

**Mailer-Config in `application.properties`:**
```properties
%dev.quarkus.mailer.mock=false
%dev.quarkus.mailer.host=localhost
%dev.quarkus.mailer.port=1025
quarkus.mailer.from=noreply@eateasy.local
```

**Tests:**
- `@QuarkusTest` mit `MockMailbox` (Quarkus-Helper) — verifiziert dass Mails versendet werden mit korrektem Inhalt

### Frontend

- Frontend muss nichts Neues — die Einladung über Email kommt jetzt extern an. UI in Phase 2 hatte den Link noch in der App angezeigt; jetzt verschwindet das, weil der User die Mail bekommt.
- Test-UI: Link zur Maildev-Webview im Footer (nur in Dev-Mode)

### DoD Phase 10
- [ ] Invitation-Endpoint verschickt echte Mail an Maildev
- [ ] Mail enthält gültigen Token-Link
- [ ] Mail-Template ist lesbar (HTML), zeigt Haushalt-Name + Inviter-Name
- [ ] Bei Mail-Versand-Fehler: Invitation wird trotzdem angelegt (Fallback wie in Phase 2)

---

## Stretch Goals (Phase 11+)

Nach Phase 10 ist die Pflichtabgabe **fertig**. Weitere Slices nach Kapazität:

### Phase 11: Beleg-Scanner (höchste Priorität unter Stretch)
- Neue Komponente `receipt/`
- Tesseract als 5. Container im docker-compose
- Pipeline: Foto-Upload → Tesseract OCR → Ollama-Strukturierung → Vorschau-Liste → Bestätigung → PantryItems
- Realistisch: 3–5 Tage Entwicklung, fragile, gut für Demo wenn's klappt

### Phase 12: Google OAuth
- `quarkus-oidc` Extension
- Neuer Login-Button im Frontend
- Account-Linking-Logik (User mit Email + User über Google → was passiert wenn beide gleiche Email haben?)
- Realistisch: 2–3 Tage

### Phase 13: MHD-Tracking
- Pantry-Items mit MHD < 7 Tage werden hervorgehoben
- Dashboard-Widget „Demnächst ablaufend"

### Phase 14: Auto-Nachbuchen
- Beim Abhaken eines ShoppingList-Items: Item landet automatisch im Pantry
- Toggle in Settings

### Phase 15: Polish
- Portionen-Slider auf Wochenplan-Entry
- Favoritenliste
- PDF-Export der Einkaufsliste (Quarkus-Erweiterung oder browserseitig)

### Phase 16: Sortierung Einkaufsliste
- Neues Feld `category` auf Ingredient (Obst/Gemüse, Milch, TK, etc.)
- Einkaufsliste gruppiert + sortiert für effizienten Gang durch den Supermarkt

---

## Definition of Done

Eine Phase gilt als **fertig**, wenn alle Punkte erfüllt sind:

- [ ] **Code:** Alle Komponenten/Schichten implementiert, Konventionen aus `CLAUDE.md` eingehalten
- [ ] **Tests:** Mindestens ein Service-Test und ein REST-Test pro neuer Funktion. Tests grün lokal und in CI.
- [ ] **Migration:** Schema-Änderungen als nummerierte Flyway-Migration, lokal getestet
- [ ] **Demo:** Feature ist End-to-End klickbar, ohne manuellen DB-Eingriff
- [ ] **Auth:** Alle neuen Endpunkte sind authentifiziert, Authorization-Checks implementiert
- [ ] **README:** Neue Setup-Schritte / Env-Vars dokumentiert
- [ ] **Branch gemerged:** PR mit Review-Beschreibung, sauberer Commit-Historie

---

## Architektur-Rücksprache mit der Lehrperson

**Vor Phase 1 sollten folgende Punkte mit dem Prof geklärt werden:**

1. **Modularer Monolith vs. Microservices:** Aktuelle Annahme = modularer Monolith mit klarer Komponenten-Trennung. Falls separate Container/Services pro Komponente verlangt werden, wären Auth + Core + Smart-Suggestion die natürlichen Splits.
2. **CI/CD-Anspruch:** SonarCloud + GitHub Actions Pipeline ja/nein?
3. **Dokumentations-Tiefe:** Reichen JavaDoc + README, oder Architektur-Dokument zusätzlich erforderlich?

Falls einer dieser Punkte anders entschieden wird, werden Phase 0 und ggf. Phase 1 entsprechend angepasst.

---

**Stand:** 27.04.2026 · Version 1.0
