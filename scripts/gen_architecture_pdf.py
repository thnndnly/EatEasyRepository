#!/usr/bin/env python3
"""Erzeugt das Architektur-Deck (EatEasy EE) als PDF im Folien-Format (16:9).

Style spiegelt das Projektidee-Dokument (Hausstil): dunkelgruene Titelfolie,
gruene Section-Linien, Helvetica, gedaempfte Footer. Direkt via reportlab, da
die LibreOffice-PPTX->PDF-Konvertierung in dieser Umgebung nicht verfuegbar ist.
"""
from reportlab.lib.units import inch
from reportlab.lib.colors import HexColor
from reportlab.pdfgen import canvas

PAGE = (13.333 * inch, 7.5 * inch)
W, H = PAGE

# EatEasy Hausstil-Palette (aus Projektidee.pdf extrahiert)
PRIMARY = HexColor("#1E3A2F")   # dunkles Waldgruen (Titel-BG, Nummern)
SECONDARY = HexColor("#2D5A47")  # Sekundaergruen (Linien, Header-Streifen)
TINT = HexColor("#E8F0EB")       # heller Gruen-Tint
TEXT = HexColor("#1A1A1A")       # Fliesstext
SUBTEXT = HexColor("#333333")
MUTED = HexColor("#777777")
KICKER = HexColor("#9FB6A9")     # gedaempftes Hellgruen auf dunklem BG
SUBLIGHT = HexColor("#C9D8CF")   # Untertitel auf dunklem BG
WHITE = HexColor("#FFFFFF")

TOTAL = 7  # Folienanzahl fuer Footer

c = canvas.Canvas("/home/user/EatEasyRepository/EatEasy-Architektur.pdf", pagesize=PAGE)


def title_slide(kicker, title, subtitle_lines, meta):
    c.setFillColor(PRIMARY)
    c.rect(0, 0, W, H, fill=1, stroke=0)
    # oberer, etwas hellerer Header-Streifen
    c.setFillColor(SECONDARY)
    c.rect(0, H - 0.32 * inch, W, 0.32 * inch, fill=1, stroke=0)
    # Kicker: gesperrte Grossbuchstaben
    c.setFillColor(KICKER)
    c.setFont("Helvetica", 13)
    c.drawString(0.9 * inch, H - 2.5 * inch, " ".join(kicker.upper()))
    # Titel
    c.setFillColor(WHITE)
    c.setFont("Helvetica-Bold", 52)
    c.drawString(0.88 * inch, H - 3.5 * inch, title)
    # Untertitel
    c.setFillColor(SUBLIGHT)
    c.setFont("Helvetica", 20)
    y = H - 4.15 * inch
    for ln in subtitle_lines:
        c.drawString(0.9 * inch, y, ln)
        y -= 0.36 * inch
    # untere Trennlinie + Meta-Block
    c.setStrokeColor(SECONDARY)
    c.setLineWidth(0.8)
    c.line(0.9 * inch, 1.55 * inch, W - 0.9 * inch, 1.55 * inch)
    my = 1.15 * inch
    for label, value in meta:
        c.setFillColor(KICKER)
        c.setFont("Helvetica", 12)
        c.drawString(0.9 * inch, my, label)
        c.setFillColor(WHITE)
        c.setFont("Helvetica", 12)
        c.drawString(2.9 * inch, my, value)
        my -= 0.34 * inch
    c.showPage()


def footer(idx):
    c.setFillColor(MUTED)
    c.setFont("Helvetica", 9)
    c.drawString(0.7 * inch, 0.45 * inch, "EatEasy EE — Architektur")
    c.drawRightString(W - 0.7 * inch, 0.45 * inch, f"Folie {idx} / {TOTAL}")


def content_slide(idx, number, title, bullets):
    c.setFillColor(WHITE)
    c.rect(0, 0, W, H, fill=1, stroke=0)
    # Heading: gruene Nummer + schwarzer Bold-Titel
    base_y = H - 1.0 * inch
    c.setFont("Helvetica-Bold", 28)
    c.setFillColor(SECONDARY)
    c.drawString(0.7 * inch, base_y, number)
    num_w = c.stringWidth(number, "Helvetica-Bold", 28)
    c.setFillColor(TEXT)
    c.drawString(0.7 * inch + num_w + 0.18 * inch, base_y, title)
    # durchgehende gruene Linie unter dem Heading
    c.setStrokeColor(SECONDARY)
    c.setLineWidth(1.4)
    c.line(0.72 * inch, H - 1.22 * inch, W - 0.7 * inch, H - 1.22 * inch)
    # Body
    y = H - 1.85 * inch
    for level, text, bold in bullets:
        size = 18 - level * 2
        if level == 0:
            x = 0.85 * inch
            c.setFillColor(SECONDARY)
            c.setFont("Helvetica-Bold", size)
            c.drawString(x, y, "▪")
            c.setFillColor(TEXT if bold else SUBTEXT)
            c.setFont("Helvetica-Bold" if bold else "Helvetica", size)
            c.drawString(x + 0.3 * inch, y, text)
        else:
            x = 1.4 * inch
            c.setFillColor(SUBTEXT)
            c.setFont("Helvetica", size)
            c.drawString(x, y, "–  " + text)
        y -= (0.46 if level == 0 else 0.4) * inch
    footer(idx)
    c.showPage()


title_slide(
    "Komponentenbasierte Softwareentwicklung",
    "EatEasy EE — Architektur",
    ["Modularer Monolith · Vue 3 + Quarkus",
     "Studienprojekt Gruppe 5"],
    [("Thema", "Architekturüberblick & Planung"),
     ("Stack", "Vue.js · Quarkus · PostgreSQL · Docker"),
     ("Stand", "Phasen 0–10 umgesetzt, Issues #3–#8 offen")],
)

content_slide(2, "1", "Grundsatzentscheidungen", [
    (0, "Architekturstil: Modularer Monolith", True),
    (1, "Fachliche Komponenten statt technischer Layer", False),
    (1, "Kommunikation nur ueber Service-Interfaces (CDI), nie via REST", False),
    (0, "Strikte DTO-Trennung", True),
    (1, "Entities lecken nie aus REST-Endpunkten; DTOs sind Java-Records", False),
    (0, "Auth: Stateless JWT (8 h)", True),
    (1, "@RolesAllowed plus Authorization-Checks im Service-Layer", False),
    (0, "Daten: UUID v4, Flyway-Migrationen, Hibernate = validate", True),
])

content_slide(3, "2", "Komponenten & Abhaengigkeiten", [
    (0, "Backend (de.eateasy.*)", True),
    (1, "auth · household · recipe · ingredient · mealplan", False),
    (1, "pantry · shoppinglist · suggestion · integration · notification · common", False),
    (0, "Schichten je Komponente", True),
    (1, "entity -> repository -> service (Interface+Impl) -> resource -> dto", False),
    (0, "Abhaengigkeitsfluss", True),
    (1, "auth -> household -> (recipe, pantry) -> mealplan -> shoppinglist -> suggestion", False),
    (1, "notification an household; integration speist recipe & pantry", False),
])

content_slide(4, "3", "Genutzte Technologien", [
    (0, "Frontend: Vue 3, Vite, Pinia, Vue Router, TypeScript, Tailwind, VueUse", True),
    (0, "Backend: Quarkus (Java 21), RESTEasy Reactive, Hibernate Panache, SmallRye JWT", True),
    (0, "Datenbank: PostgreSQL 16 + Flyway", True),
    (0, "LLM: Ollama self-hosted (Llama 3 / Mistral)", True),
    (0, "Externe APIs: TheMealDB (Rezepte), OpenFoodFacts (Barcode)", True),
    (0, "Testing: JUnit5 + REST Assured, Vitest + MSW, Playwright (E2E)", True),
    (0, "Qualitaet/CI: Checkstyle, ESLint + Prettier, GitHub Actions", True),
    (0, "Deployment: Render (Backend + DB), Vercel (Frontend, geplant)", True),
])

content_slide(5, "4", "Resilienz / Backup-Plan", [
    (0, "Smart-Suggestion: Ollama-Ausfall -> Coverage-Heuristik", True),
    (0, "Barcode-Scan: keine Kamera -> manuelle Eingabe (optional)", True),
    (0, "Rezept-Import: nur TheMealDB (kostenlos, kein API-Key)", True),
    (0, "E-Mail: Invitation wird auch bei Mail-Fehler angelegt", True),
])

content_slide(6, "5", "Was wir noch planen (Issues #3–#8)", [
    (0, "#3 Auto-Haushalt bei Registrierung", True),
    (1, "AuthService injiziert HouseholdService, eine @Transactional-Einheit", False),
    (0, "#4 Rezept-Rating (1-5 Sterne) — einziger Schema-Eingriff (V8)", True),
    (0, "#5 Einkaufsliste explizit gegen Vorrat pruefen vor Ausstellen", True),
    (0, "#6 LLM-Modell verifizieren / leichteres Modell", True),
    (0, "#7 Vercel-Deployment fuers Frontend", True),
    (0, "#8 Backup-Plan, Abhaengigkeiten & Scope dokumentieren", True),
])

content_slide(7, "6", "Kernaussage", [
    (0, "Architektur bleibt unveraendert stabil:", True),
    (1, "Modularer Monolith, Service-Interfaces, strikte DTO-Trennung", False),
    (1, "JWT + Service-Layer-Authz, Flyway, UUID", False),
    (0, "Nur #4 (Rating) beruehrt das Datenmodell", True),
    (0, "Alles andere ist Service-Logik, Config oder Deployment/Doku", True),
])

c.save()
print("saved EatEasy-Architektur.pdf")
