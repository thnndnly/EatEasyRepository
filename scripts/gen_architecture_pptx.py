#!/usr/bin/env python3
"""Erzeugt eine einfache Architektur-Praesentation (EatEasy EE) als .pptx."""
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN

NAVY = RGBColor(0x1F, 0x2D, 0x3D)
GREEN = RGBColor(0x2E, 0x7D, 0x32)
GREY = RGBColor(0x55, 0x55, 0x55)
ACCENT = RGBColor(0x2E, 0x7D, 0x32)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)

prs = Presentation()
prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5)
BLANK = prs.slide_layouts[6]


def add_title_slide(title, subtitle):
    s = prs.slides.add_slide(BLANK)
    bg = s.background.fill
    bg.solid()
    bg.fore_color.rgb = NAVY
    tb = s.shapes.add_textbox(Inches(0.8), Inches(2.4), Inches(11.7), Inches(2.5))
    tf = tb.text_frame
    tf.word_wrap = True
    p = tf.paragraphs[0]
    r = p.add_run(); r.text = title
    r.font.size = Pt(44); r.font.bold = True; r.font.color.rgb = WHITE
    p2 = tf.add_paragraph()
    r2 = p2.add_run(); r2.text = subtitle
    r2.font.size = Pt(22); r2.font.color.rgb = RGBColor(0xBB, 0xD0, 0xC2)
    return s


def add_content_slide(title, bullets):
    """bullets: list of (level, text, bold) tuples."""
    s = prs.slides.add_slide(BLANK)
    # title bar
    bar = s.shapes.add_textbox(Inches(0.6), Inches(0.35), Inches(12.1), Inches(0.9))
    tf = bar.text_frame
    p = tf.paragraphs[0]
    r = p.add_run(); r.text = title
    r.font.size = Pt(30); r.font.bold = True; r.font.color.rgb = NAVY
    # underline accent
    line = s.shapes.add_shape(1, Inches(0.65), Inches(1.25), Inches(2.6), Pt(3))
    line.fill.solid(); line.fill.fore_color.rgb = ACCENT
    line.line.fill.background()
    # body
    body = s.shapes.add_textbox(Inches(0.7), Inches(1.5), Inches(12.0), Inches(5.5))
    tfb = body.text_frame
    tfb.word_wrap = True
    first = True
    for level, text, bold in bullets:
        p = tfb.paragraphs[0] if first else tfb.add_paragraph()
        first = False
        p.level = level
        p.space_after = Pt(6)
        r = p.add_run()
        prefix = "" if bold and level == 0 else ("•  " if level == 0 else "–  ")
        r.text = prefix + text
        r.font.size = Pt(20 - level * 2)
        r.font.bold = bold
        r.font.color.rgb = NAVY if bold else GREY
    return s


add_title_slide(
    "EatEasy EE — Architektur",
    "Modularer Monolith · Vue 3 + Quarkus · Studienprojekt Gruppe 5",
)

add_content_slide("Grundsatzentscheidungen", [
    (0, "Architekturstil: Modularer Monolith", True),
    (1, "Fachliche Komponenten statt technischer Layer", False),
    (1, "Komponenten kommunizieren nur ueber Service-Interfaces (CDI), nie via REST", False),
    (0, "Strikte DTO-Trennung", True),
    (1, "Entities lecken nie aus REST-Endpunkten; DTOs sind Java-Records mit Validation", False),
    (0, "Auth: Stateless JWT (8 h)", True),
    (1, "@RolesAllowed plus Authorization-Checks im Service-Layer", False),
    (0, "Daten: UUID v4 ueberall, Flyway-Migrationen, Hibernate = validate", True),
])

add_content_slide("Komponenten & Abhaengigkeiten", [
    (0, "Backend (de.eateasy.*)", True),
    (1, "auth · household · recipe · ingredient · mealplan", False),
    (1, "pantry · shoppinglist · suggestion · integration · notification · common", False),
    (0, "Schichten je Komponente", True),
    (1, "entity -> repository -> service (Interface+Impl) -> resource -> dto", False),
    (0, "Abhaengigkeitsfluss", True),
    (1, "auth -> household -> (recipe, pantry) -> mealplan -> shoppinglist -> suggestion", False),
    (1, "notification an household (Einladungen); integration speist recipe & pantry", False),
])

add_content_slide("Genutzte Technologien", [
    (0, "Frontend: Vue 3, Vite, Pinia, Vue Router, TypeScript (strict), Tailwind, VueUse", True),
    (0, "Backend: Quarkus (Java 21), RESTEasy Reactive, Hibernate Panache, SmallRye JWT, Mailer", True),
    (0, "Datenbank: PostgreSQL 16 + Flyway", True),
    (0, "LLM: Ollama self-hosted (Llama 3 / Mistral)", True),
    (0, "Externe APIs: TheMealDB (Rezepte), OpenFoodFacts (Barcode)", True),
    (0, "Testing: JUnit5 + REST Assured, Vitest + MSW, Playwright (E2E)", True),
    (0, "Qualitaet/CI: Checkstyle, ESLint + Prettier, GitHub Actions", True),
    (0, "Deployment: Render (Backend + DB), Vercel (Frontend, geplant)", True),
])

add_content_slide("Resilienz / Backup-Plan", [
    (0, "Smart-Suggestion: Ollama-Ausfall -> Coverage-Heuristik (App bleibt nutzbar)", True),
    (0, "Barcode-Scan: keine Kamera -> manuelle Eingabe (Feature optional)", True),
    (0, "Rezept-Import: nur TheMealDB (kostenlos, kein API-Key noetig)", True),
    (0, "E-Mail: Maildev/SMTP; Invitation wird auch bei Mail-Fehler angelegt", True),
])

add_content_slide("Was wir noch planen (Issues #3-#8)", [
    (0, "#3 Auto-Haushalt bei Registrierung", True),
    (1, "AuthService injiziert HouseholdService, eine @Transactional-Einheit (kein Schema)", False),
    (0, "#4 Rezept-Rating (1-5 Sterne)", True),
    (1, "Neuer Slice: Migration V8, Entity, Service, REST, UI — einziger Schema-Eingriff", False),
    (0, "#5 Einkaufsliste explizit gegen Vorrat pruefen vor Ausstellen", True),
    (0, "#6 LLM-Modell verifizieren / leichteres Modell fuer Rechenkapazitaet", True),
    (0, "#7 Vercel-Deployment fuers Frontend", True),
    (0, "#8 Backup-Plan, Abhaengigkeiten & Scope dokumentieren", True),
])

add_content_slide("Kernaussage", [
    (0, "Architektur bleibt unveraendert stabil:", True),
    (1, "Modularer Monolith, Service-Interfaces, strikte DTO-Trennung", False),
    (1, "JWT + Service-Layer-Authz, Flyway, UUID", False),
    (0, "Von den geplanten Aenderungen beruehrt nur #4 (Rating) das Datenmodell", True),
    (0, "Alles andere ist Service-Logik, Config oder Deployment/Doku", True),
])

out = "/home/user/EatEasyRepository/EatEasy-Architektur.pptx"
prs.save(out)
print("saved", out)
