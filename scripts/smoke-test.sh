#!/usr/bin/env bash
# End-to-End Smoke-Test fuer EatEasy.
# Voraussetzungen: docker-compose.dev.yml laeuft (Postgres, Maildev, Ollama),
# Backend laeuft auf :8080. Ollama-Modell ist optional — bei Fehler greift
# der Coverage-Fallback im Suggestion-Service.
#
# Usage: bash scripts/smoke-test.sh

set -euo pipefail

BASE="${BASE:-http://localhost:8080/api/v1}"
MAILDEV="${MAILDEV:-http://localhost:1080}"

# Eindeutige Email pro Run, damit wiederholte Aufrufe nicht an Email-UNIQUE
# scheitern. Genauigkeit von date(1) reicht — kein Parallel-Run vorgesehen.
SUFFIX="$(date +%s)"
ALICE_EMAIL="alice-${SUFFIX}@example.com"
BOB_EMAIL="bob-${SUFFIX}@example.com"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

pass() { printf "${GREEN}PASS${NC} %s\n" "$1"; }
fail() { printf "${RED}FAIL${NC} %s\n" "$1"; exit 1; }
info() { printf "${YELLOW}····${NC} %s\n" "$1"; }

# --- Phase 0: Health -------------------------------------------------------

info "Phase 0 — Health-Check"
HEALTH=$(curl -fsS "${BASE}/health")
[[ "$HEALTH" == *'"status":"ok"'* ]] || fail "Health-Endpoint antwortet nicht 'ok': $HEALTH"
pass "Backend health-check ok"

# --- Phase 1: Auth ---------------------------------------------------------

info "Phase 1 — Register Alice"
ALICE_RESP=$(curl -fsS -X POST "${BASE}/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${ALICE_EMAIL}\",\"password\":\"secret123\",\"displayName\":\"Alice Smoke\"}")
ALICE_TOKEN=$(echo "$ALICE_RESP" | python -c "import sys,json; print(json.load(sys.stdin)['token'])")
[[ -n "$ALICE_TOKEN" ]] || fail "Kein Token von /auth/register"
pass "Alice registriert + JWT erhalten"

info "Phase 1 — Login Alice (Re-Test)"
LOGIN_RESP=$(curl -fsS -X POST "${BASE}/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${ALICE_EMAIL}\",\"password\":\"secret123\"}")
echo "$LOGIN_RESP" | grep -q '"token"' || fail "Login lieferte keinen Token"
pass "Login Alice ok"

info "Phase 1 — GET /auth/me mit Token"
ME=$(curl -fsS "${BASE}/auth/me" -H "Authorization: Bearer ${ALICE_TOKEN}")
[[ "$ME" == *"${ALICE_EMAIL}"* ]] || fail "/auth/me lieferte falsche User-Daten"
pass "/auth/me liefert eingeloggten User"

# Bob auch anlegen, fuer Invitation-Test
BOB_RESP=$(curl -fsS -X POST "${BASE}/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${BOB_EMAIL}\",\"password\":\"secret123\",\"displayName\":\"Bob Smoke\"}")
BOB_TOKEN=$(echo "$BOB_RESP" | python -c "import sys,json; print(json.load(sys.stdin)['token'])")

# --- Phase 2: Haushalt -----------------------------------------------------

info "Phase 2 — Haushalt anlegen"
HOUSE_RESP=$(curl -fsS -X POST "${BASE}/households" \
  -H "Authorization: Bearer ${ALICE_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"name":"Smoke-WG","defaultDietTags":["vegetarian"]}')
HOUSE_ID=$(echo "$HOUSE_RESP" | python -c "import sys,json; print(json.load(sys.stdin)['id'])")
[[ -n "$HOUSE_ID" ]] || fail "Haushalt hat keine ID"
pass "Haushalt angelegt: ${HOUSE_ID}"

info "Phase 2 — Bob einladen (triggert Mail)"
INVITE_RESP=$(curl -fsS -X POST "${BASE}/households/${HOUSE_ID}/invitations" \
  -H "Authorization: Bearer ${ALICE_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${BOB_EMAIL}\"}")
INVITE_TOKEN=$(echo "$INVITE_RESP" | python -c "import sys,json; print(json.load(sys.stdin)['token'])")
[[ -n "$INVITE_TOKEN" ]] || fail "Keine Invitation-Token"
pass "Einladung an Bob: ${INVITE_TOKEN:0:12}..."

info "Phase 2 — Bob nimmt Einladung an"
curl -fsS -X POST "${BASE}/invitations/accept" \
  -H "Authorization: Bearer ${BOB_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"token\":\"${INVITE_TOKEN}\"}" > /dev/null
pass "Bob ist Mitglied"

# --- Phase 10 (Vorab): Maildev-Pruefung -----------------------------------

info "Phase 10 — Maildev-Postfach pruefen"
sleep 1  # Mail-Versand ist async im Vert.x-Stack
MAILS=$(curl -fsS "${MAILDEV}/email" || echo "[]")
FOUND_MAIL=$(echo "$MAILS" | python -c "
import sys, json
mails = json.load(sys.stdin)
match = [m for m in mails if any('${BOB_EMAIL}' in t.get('address','') for t in m.get('to',[]))]
print(len(match))
")
if [[ "$FOUND_MAIL" -ge 1 ]]; then
  pass "Maildev hat ${FOUND_MAIL} Mail(s) an ${BOB_EMAIL}"
else
  fail "Keine Mail an ${BOB_EMAIL} im Maildev gefunden"
fi

# --- Phase 3: Rezept -------------------------------------------------------

info "Phase 3 — Rezept mit 2 Zutaten anlegen"
RECIPE_RESP=$(curl -fsS -X POST "${BASE}/recipes" \
  -H "Authorization: Bearer ${ALICE_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\":\"Smoke-Tomatensalat\",
    \"instructions\":\"Tomaten schneiden, salzen, fertig.\",
    \"servings\":2,
    \"dietTags\":[\"vegetarian\"],
    \"householdId\":\"${HOUSE_ID}\",
    \"ingredients\":[
      {\"ingredientName\":\"Tomate\",\"amount\":4,\"unit\":\"PIECE\"},
      {\"ingredientName\":\"Salz\",\"amount\":5,\"unit\":\"GRAM\"}
    ]
  }")
RECIPE_ID=$(echo "$RECIPE_RESP" | python -c "import sys,json; print(json.load(sys.stdin)['id'])")
pass "Rezept angelegt: ${RECIPE_ID}"

# --- Phase 4: Wochenplan ---------------------------------------------------

info "Phase 4 — MealPlan fuer aktuelle Woche holen"
PLAN_RESP=$(curl -fsS "${BASE}/households/${HOUSE_ID}/mealplans" \
  -H "Authorization: Bearer ${ALICE_TOKEN}")
PLAN_ID=$(echo "$PLAN_RESP" | python -c "import sys,json; print(json.load(sys.stdin)['id'])")
pass "MealPlan ID: ${PLAN_ID}"

info "Phase 4 — Slot Monday/Lunch setzen"
curl -fsS -X PUT "${BASE}/mealplans/${PLAN_ID}/entries" \
  -H "Authorization: Bearer ${ALICE_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"dayOfWeek\":\"MONDAY\",\"mealType\":\"LUNCH\",\"recipeId\":\"${RECIPE_ID}\",\"servings\":2}" > /dev/null
pass "Slot Mo/Lunch gesetzt"

# --- Phase 5: Vorrat -------------------------------------------------------

info "Phase 5 — Pantry: nur 2 Tomaten anlegen (Salz fehlt)"
curl -fsS -X POST "${BASE}/households/${HOUSE_ID}/pantry" \
  -H "Authorization: Bearer ${ALICE_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"ingredientName":"Tomate","amount":2,"unit":"PIECE"}' > /dev/null
pass "Pantry: 2x Tomate"

# --- Phase 6: Einkaufsliste ------------------------------------------------

info "Phase 6 — Shoppinglist berechnen (sollte 2 Tomaten + 5g Salz fordern)"
SHOPPING=$(curl -fsS "${BASE}/mealplans/${PLAN_ID}/shoppinglist" \
  -H "Authorization: Bearer ${ALICE_TOKEN}")
ITEM_COUNT=$(echo "$SHOPPING" | python -c "import sys,json; print(len(json.load(sys.stdin)['items']))")
[[ "$ITEM_COUNT" -ge 2 ]] || fail "Erwarte mind. 2 Items, bekomme: $ITEM_COUNT"
pass "Shoppinglist hat ${ITEM_COUNT} Items"

# --- Phase 7: TheMealDB-Import ---------------------------------------------

info "Phase 7 — TheMealDB-Suche nach 'pasta' (Live-Call)"
SEARCH=$(curl -fsS "${BASE}/integration/recipes/search?source=themealdb&q=pasta" \
  -H "Authorization: Bearer ${ALICE_TOKEN}")
PREVIEW_COUNT=$(echo "$SEARCH" | python -c "import sys,json; print(len(json.load(sys.stdin)))")
if [[ "$PREVIEW_COUNT" -ge 1 ]]; then
  pass "TheMealDB liefert ${PREVIEW_COUNT} Treffer"
  FIRST_EXT=$(echo "$SEARCH" | python -c "import sys,json; print(json.load(sys.stdin)[0]['externalId'])")
  info "Phase 7 — Import erstes Resultat (externalId=${FIRST_EXT})"
  IMPORT=$(curl -fsS -X POST "${BASE}/recipes/import" \
    -H "Authorization: Bearer ${ALICE_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{\"source\":\"themealdb\",\"externalId\":\"${FIRST_EXT}\"}")
  echo "$IMPORT" | grep -q '"externalSource":"themealdb"' || fail "Import: externalSource fehlt"
  pass "Rezept aus TheMealDB importiert"
else
  fail "TheMealDB lieferte 0 Treffer — Internet-Connectivity pruefen"
fi

# --- Phase 8: OpenFoodFacts-Barcode ----------------------------------------

info "Phase 8 — OpenFoodFacts Lookup (Nutella: 3017620422003)"
PRODUCT=$(curl -fsS "${BASE}/integration/products/3017620422003" \
  -H "Authorization: Bearer ${ALICE_TOKEN}" || echo "")
if echo "$PRODUCT" | grep -q '"name"'; then
  NAME=$(echo "$PRODUCT" | python -c "import sys,json; print(json.load(sys.stdin)['name'])")
  pass "OpenFoodFacts: ${NAME}"

  info "Phase 8 — Barcode in Pantry uebernehmen"
  curl -fsS -X POST "${BASE}/households/${HOUSE_ID}/pantry/barcode" \
    -H "Authorization: Bearer ${ALICE_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{"barcode":"3017620422003","amount":400,"unit":"GRAM"}' > /dev/null
  pass "Nutella in Pantry uebernommen"
else
  printf "${YELLOW}SKIP${NC} OpenFoodFacts unerreichbar oder Produkt unbekannt\n"
fi

# --- Phase 9: Smart-Suggestion ---------------------------------------------

info "Phase 9 — Smart-Suggestion (Ollama oder Coverage-Fallback)"
SUGGEST=$(curl -fsS -X POST "${BASE}/households/${HOUSE_ID}/suggestions" \
  -H "Authorization: Bearer ${ALICE_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"numSuggestions":3}')
SUG_COUNT=$(echo "$SUGGEST" | python -c "import sys,json; print(len(json.load(sys.stdin)))")
HAS_REASON=$(echo "$SUGGEST" | python -c "
import sys, json
s = json.load(sys.stdin)
print('yes' if any(x.get('reason') for x in s) else 'no')
")
pass "Suggestion: ${SUG_COUNT} Vorschlaege, Ollama-Reason: ${HAS_REASON}"

# --- Zusammenfassung -------------------------------------------------------

printf "\n${GREEN}=== Smoke-Test abgeschlossen ===${NC}\n"
echo "Alice: ${ALICE_EMAIL}"
echo "Haushalt: ${HOUSE_ID}"
echo "Rezept (manuell): ${RECIPE_ID}"
echo "Maildev: ${MAILDEV}"
