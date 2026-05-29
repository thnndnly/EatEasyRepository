# Kollaboration & Git-Workflow

Wie wir im Team zusammenarbeiten: Branches, Commits, Pull Requests und der Weg
nach `main`. Gilt für **macOS, Linux und Windows** — Git-Befehle sind überall
identisch; wo nötig sind plattformspezifische Hinweise ausgewiesen.

> Neu im Projekt? Richte zuerst deine [lokale Entwicklungsumgebung](/lokale-entwicklung)
> ein, bevor du den ersten Branch anlegst.

---

## 1. Git einmalig einrichten

::: code-group
```bash [macOS / Linux]
git config --global user.name "Vorname Nachname"
git config --global user.email "deine@mail.de"

# Zeilenenden: auf macOS/Linux LF beibehalten
git config --global core.autocrlf input
```
```powershell [Windows · PowerShell]
git config --global user.name "Vorname Nachname"
git config --global user.email "deine@mail.de"

# Zeilenenden: Checkout CRLF, Commit LF
git config --global core.autocrlf true
```
:::

> **Zeilenenden:** Damit Dateien nicht bei jedem Commit „komplett geändert"
> erscheinen, nutzt macOS/Linux `input` und Windows `true`. So landet im Repo
> immer LF, im Arbeitsverzeichnis das plattformübliche Format.

### SSH-Key für GitHub (empfohlen)

::: code-group
```bash [macOS / Linux]
ssh-keygen -t ed25519 -C "deine@mail.de"
cat ~/.ssh/id_ed25519.pub          # Inhalt in GitHub → Settings → SSH Keys einfügen
```
```powershell [Windows · PowerShell]
ssh-keygen -t ed25519 -C "deine@mail.de"
Get-Content $HOME\.ssh\id_ed25519.pub   # Inhalt in GitHub → Settings → SSH Keys einfügen
```
:::

---

## 2. Branch-Modell

`main` ist **immer lauffähig** und geschützt: kein direkter Push, Änderungen
nur über Pull Requests.

| Branch | Zweck |
| --- | --- |
| `main` | Stabiler Stand, deploybar. Nur über PR + Approval. |
| `feature/<slice-nr>-<kurzname>` | Neues Feature, z. B. `feature/04-wochenplan` |
| `fix/<kurzname>` | Bugfix, z. B. `fix/login-token-expiry` |
| `chore/<kurzname>` / `docs/<kurzname>` | Wartung, Doku, Build |

Branch anlegen (immer von aktuellem `main` aus):

```bash
git switch main
git pull
git switch -c feature/04-wochenplan
```

---

## 3. Commits

Wir nutzen **Conventional Commits**:

```
<type>: <kurze Beschreibung im Imperativ>

<optionaler Body: was/warum>
```

**Typen:** `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `ci`

**Beispiele:**

```
feat: Wochenplan-Endpunkt für Mahlzeiten-Slots
fix: JWT-Lifespan auf 8h korrigiert
docs: Onboarding-Anleitung für lokale Entwicklung
test: Edge-Case für leeren Vorrat in ShoppingListService
```

Richtlinien:

- Kleine, thematisch fokussierte Commits — keine „Sammel-Commits".
- Imperativ („füge hinzu", nicht „hinzugefügt").
- **Nichts ohne Tests committen** — mind. ein Test pro neuem Service-Method,
  mind. ein E2E-/REST-Test pro neuer Route (siehe `CLAUDE.md`).

---

## 4. Vor dem Push: lokal grün machen

Damit die CI nicht rot wird, lokal dasselbe prüfen, was die Pipeline prüft:

::: code-group
```bash [macOS / Linux]
# Backend
cd backend && ./mvnw test

# Frontend
cd frontend
npm run type-check
npm run lint
npm run test:unit -- --run
npm run build
```
```powershell [Windows · PowerShell]
# Backend
cd backend; .\mvnw.cmd test

# Frontend
cd frontend
npm run type-check
npm run lint
npm run test:unit -- --run
npm run build
```
:::

> **Docker muss für die Backend-Tests laufen** (Testcontainers startet einen
> ephemeren Postgres). Siehe [Onboarding](/lokale-entwicklung).

---

## 5. Pull Request erstellen

```bash
git push -u origin feature/04-wochenplan
```

Dann auf GitHub einen PR gegen `main` öffnen. Die Beschreibung folgt dem
Schema **Was / Warum / Wie getestet**:

```markdown
## Was
Kurzbeschreibung der Änderung.

## Warum
Kontext / Motivation / verlinktes Issue.

## Wie getestet
- [ ] Backend-Tests grün (./mvnw test)
- [ ] Frontend type-check + lint + build grün
- [ ] Manuell durchgeklickt: <Flow>
```

**Regeln:**

- Mindestens **ein Approval** vor dem Merge auf `main`.
- CI muss grün sein (siehe unten).
- Reviewer adressieren: CRITICAL/HIGH-Findings beheben, MEDIUM nach Möglichkeit.

---

## 6. Was die CI prüft

Bei jedem Push/PR auf `main` laufen zwei Jobs **parallel**
(`.github/workflows/ci.yml`):

| Job | Schritte |
| --- | --- |
| **Backend** (Quarkus/Maven, JDK 21) | Checkstyle (non-blocking) → `./mvnw test` (mit Testcontainers-Postgres) |
| **Frontend** (Vue/Vite, Node 22) | `npm ci` → `type-check` → `lint:ci` (Check-Modus, kein Auto-Fix) → `test:unit --run` (Vitest + MSW) → `build` → Playwright-E2E (Chromium) |

> Externe APIs (TheMealDB, OpenFoodFacts, Ollama) sind in den Tests gemockt —
> die CI braucht **kein** Internet zu diesen Diensten und **keine** Secrets.

---

## 7. Review-Etikette

**Als Autor:in:**

- PR klein halten (idealerweise < ~400 geänderte Zeilen).
- Selbst-Review vor dem Anfragen: Diff durchsehen, Debug-Code/`console.log`
  entfernen.
- Auf Kommentare antworten, nicht stillschweigend force-pushen ohne Notiz.

**Als Reviewer:in:**

- Zeitnah reviewen (idealerweise am selben Tag).
- Konkret und freundlich: Vorschlag statt nur Kritik.
- Architektur-Grenzen prüfen (siehe `CLAUDE.md`): keine Cross-Component-Calls
  am Service-Interface vorbei, keine Entities aus REST-Endpunkten, Auth-Checks
  im Service-Layer.

---

## 8. Mergen & aufräumen

- **Squash-Merge** bevorzugt, damit `main` eine saubere Historie behält.
- Nach dem Merge den Feature-Branch löschen (GitHub bietet den Button an).
- Lokal aufräumen:

```bash
git switch main
git pull
git branch -d feature/04-wochenplan
```

---

## 9. Konflikte & aktuell bleiben

`main`-Stand regelmäßig in den Feature-Branch holen, um große Konflikte zu
vermeiden:

```bash
git switch feature/04-wochenplan
git fetch origin
git merge origin/main          # oder: git rebase origin/main
```

Bei Merge-Konflikten: betroffene Dateien auflösen, dann

```bash
git add <datei>
git commit                     # bei merge
# bzw. git rebase --continue   # bei rebase
```

> **`.pem`-Keys & `.env`** sind in `.gitignore` und gehören **nie** in einen
> Commit. Jede:r erzeugt die JWT-Keys lokal (siehe Onboarding, Schritt 4).
> Wenn `git status` solche Dateien zeigt, **nicht** stagen.

---

## 10. Goldene Regeln

1. `main` ist heilig — nur über PR + Approval + grüne CI.
2. Branch pro Feature/Fix, kurze fokussierte Commits.
3. Keine Secrets, keine `*.pem`, keine `.env` im Repo.
4. Nichts ohne Tests, lokal grün vor dem Push.
5. Bei Unklarheit: `IMPLEMENTATION.md` lesen oder im Team fragen — nicht raten.
