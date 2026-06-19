#!/usr/bin/env python3
"""Erzeugt das Architektur-Deck (EatEasy EE) direkt als PDF im Folien-Format.

Spiegelt 1:1 die Slides aus gen_architecture_pptx.py — wird genutzt, weil die
LibreOffice-PPTX->PDF-Konvertierung in dieser Umgebung nicht verfuegbar ist.
"""
from reportlab.lib.pagesizes import landscape
from reportlab.lib.units import inch
from reportlab.lib.colors import HexColor
from reportlab.pdfgen import canvas

PAGE = (13.333 * inch, 7.5 * inch)
W, H = PAGE
NAVY = HexColor("#1F2D3D")
GREEN = HexColor("#2E7D32")
GREY = HexColor("#555555")
SUB = HexColor("#BBD0C2")
WHITE = HexColor("#FFFFFF")

c = canvas.Canvas("/home/user/EatEasyRepository/EatEasy-Architektur.pdf", pagesize=PAGE)


def title_slide(title, subtitle):
    c.setFillColor(NAVY)
    c.rect(0, 0, W, H, fill=1, stroke=0)
    c.setFillColor(WHITE)
    c.setFont("Helvetica-Bold", 40)
    c.drawString(0.8 * inch, H - 3.1 * inch, title)
    c.setFillColor(SUB)
    c.setFont("Helvetica", 20)
    c.drawString(0.8 * inch, H - 3.7 * inch, subtitle)
    c.showPage()


def content_slide(title, bullets):
    c.setFillColor(WHITE)
    c.rect(0, 0, W, H, fill=1, stroke=0)
    # title
    c.setFillColor(NAVY)
    c.setFont("Helvetica-Bold", 28)
    c.drawString(0.7 * inch, H - 1.0 * inch, title)
    # accent underline
    c.setFillColor(GREEN)
    c.rect(0.72 * inch, H - 1.18 * inch, 2.4 * inch, 3, fill=1, stroke=0)
    # body
    y = H - 1.75 * inch
    for level, text, bold in bullets:
        size = 18 - level * 2
        if level == 0:
            c.setFillColor(NAVY if bold else GREY)
            c.setFont("Helvetica-Bold" if bold else "Helvetica", size)
            x = 0.85 * inch
            c.setFillColor(GREEN)
            c.drawString(x, y, "▪")
            c.setFillColor(NAVY if bold else GREY)
            c.drawString(x + 0.28 * inch, y, text)
        else:
            c.setFont("Helvetica", size)
            c.setFillColor(GREY)
            x = 1.35 * inch
            c.drawString(x, y, "–  " + text)
        y -= (0.46 if level == 0 else 0.4) * inch
    c.showPage()


title_slide(
    "EatEasy EE — Architektur",
    "Modularer Monolith · Vue 3 + Quarkus · Studienprojekt Gruppe 5",
)

content_slide("Grundsatzentscheidungen", [
    (0, "Architekturstil: Modularer Monolith", True),
    (1, "Fachliche Komponenten statt technischer Layer", False),
    (1, "Kommunikation nur ueber Service-Interfaces (CDI), nie via REST", False),
    (0, "Strikte DTO-Trennung", True),
    (1, "Entities lecken nie aus REST-Endpunkten; DTOs sind Java-Records", False),
    (0, "Auth: Stateless JWT (8 h)", True),
    (1, "@RolesAllowed plus Authorization-Checks im Service-Layer", False),
    (0, "Daten: UUID v4, Flyway-Migrationen, Hibernate = validate", True),
])

content_slide("Komponenten & Abhaengigkeiten", [
    (0, "Backend (de.eateasy.*)", True),
    (1, "auth · household · recipe · ingredient · mealplan", False),
    (1, "pantry · shoppinglist · suggestion · integration · notification · common", False),
    (0, "Schichten je Komponente", True),
    (1, "entity -> repository -> service (Interface+Impl) -> resource -> dto", False),
    (0, "Abhaengigkeitsfluss", True),
    (1, "auth -> household -> (recipe, pantry) -> mealplan -> shoppinglist -> suggestion", False),
    (1, "notification an household; integration speist recipe & pantry", False),
])

content_slide("Genutzte Technologien", [
    (0, "Frontend: Vue 3, Vite, Pinia, Vue Router, TypeScript, Tailwind, VueUse", True),
    (0, "Backend: Quarkus (Java 21), RESTEasy Reactive, Hibernate Panache, SmallRye JWT", True),
    (0, "Datenbank: PostgreSQL 16 + Flyway", True),
    (0, "LLM: Ollama self-hosted (Llama 3 / Mistral)", True),
    (0, "Externe APIs: TheMealDB (Rezepte), OpenFoodFacts (Barcode)", True),
    (0, "Testing: JUnit5 + REST Assured, Vitest + MSW, Playwright (E2E)", True),
    (0, "Qualitaet/CI: Checkstyle, ESLint + Prettier, GitHub Actions", True),
    (0, "Deployment: Render (Backend + DB), Vercel (Frontend, geplant)", True),
])

content_slide("Resilienz / Backup-Plan", [
    (0, "Smart-Suggestion: Ollama-Ausfall -> Coverage-Heuristik", True),
    (0, "Barcode-Scan: keine Kamera -> manuelle Eingabe (optional)", True),
    (0, "Rezept-Import: nur TheMealDB (kostenlos, kein API-Key)", True),
    (0, "E-Mail: Invitation wird auch bei Mail-Fehler angelegt", True),
])

content_slide("Was wir noch planen (Issues #3-#8)", [
    (0, "#3 Auto-Haushalt bei Registrierung", True),
    (1, "AuthService injiziert HouseholdService, eine @Transactional-Einheit", False),
    (0, "#4 Rezept-Rating (1-5 Sterne) — einziger Schema-Eingriff (V8)", True),
    (0, "#5 Einkaufsliste explizit gegen Vorrat pruefen vor Ausstellen", True),
    (0, "#6 LLM-Modell verifizieren / leichteres Modell", True),
    (0, "#7 Vercel-Deployment fuers Frontend", True),
    (0, "#8 Backup-Plan, Abhaengigkeiten & Scope dokumentieren", True),
])

content_slide("Kernaussage", [
    (0, "Architektur bleibt unveraendert stabil:", True),
    (1, "Modularer Monolith, Service-Interfaces, strikte DTO-Trennung", False),
    (1, "JWT + Service-Layer-Authz, Flyway, UUID", False),
    (0, "Nur #4 (Rating) beruehrt das Datenmodell", True),
    (0, "Alles andere ist Service-Logik, Config oder Deployment/Doku", True),
])

c.save()
print("saved EatEasy-Architektur.pdf")
