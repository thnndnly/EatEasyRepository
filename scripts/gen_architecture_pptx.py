#!/usr/bin/env python3
"""Erzeugt das Architektur-Deck (EatEasy EE) als .pptx im EatEasy-Hausstil.

Style aus Projektidee.pdf: dunkelgruene Titelfolie mit gesperrtem Kicker,
gruene Section-Linien unter den Headings, gedaempfte Footer, Helvetica/Arial.
"""
from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN

# EatEasy Hausstil-Palette (aus Projektidee.pdf)
PRIMARY = RGBColor(0x1E, 0x3A, 0x2F)
SECONDARY = RGBColor(0x2D, 0x5A, 0x47)
TEXT = RGBColor(0x1A, 0x1A, 0x1A)
SUBTEXT = RGBColor(0x33, 0x33, 0x33)
MUTED = RGBColor(0x77, 0x77, 0x77)
KICKER = RGBColor(0x9F, 0xB6, 0xA9)
SUBLIGHT = RGBColor(0xC9, 0xD8, 0xCF)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
FONT = "Arial"  # Helvetica-Aequivalent (TeX-Gyre-Heros im Original)
TOTAL = 7

prs = Presentation()
prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5)
BLANK = prs.slide_layouts[6]
SW, SH = prs.slide_width, prs.slide_height


def _fill(shape, rgb):
    shape.fill.solid()
    shape.fill.fore_color.rgb = rgb
    shape.line.fill.background()


def title_slide(kicker, title, subtitle_lines, meta):
    s = prs.slides.add_slide(BLANK)
    bg = s.background.fill
    bg.solid(); bg.fore_color.rgb = PRIMARY
    # heller Header-Streifen oben
    strip = s.shapes.add_shape(1, 0, 0, SW, Inches(0.32))
    _fill(strip, SECONDARY)
    # Kicker (gesperrt simuliert ueber Spaces)
    kb = s.shapes.add_textbox(Inches(0.9), Inches(2.1), Inches(11.5), Inches(0.5))
    p = kb.text_frame.paragraphs[0]
    r = p.add_run(); r.text = " ".join(kicker.upper())
    r.font.size = Pt(13); r.font.name = FONT; r.font.color.rgb = KICKER
    # Titel
    tb = s.shapes.add_textbox(Inches(0.85), Inches(2.6), Inches(11.6), Inches(1.3))
    p = tb.text_frame.paragraphs[0]
    r = p.add_run(); r.text = title
    r.font.size = Pt(50); r.font.bold = True; r.font.name = FONT; r.font.color.rgb = WHITE
    # Untertitel
    sb = s.shapes.add_textbox(Inches(0.9), Inches(3.9), Inches(11.5), Inches(1.2))
    tf = sb.text_frame; tf.word_wrap = True
    for i, ln in enumerate(subtitle_lines):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        r = p.add_run(); r.text = ln
        r.font.size = Pt(20); r.font.name = FONT; r.font.color.rgb = SUBLIGHT
    # Trennlinie
    line = s.shapes.add_shape(1, Inches(0.9), Inches(5.95), SW - Inches(1.8), Pt(1))
    _fill(line, SECONDARY)
    # Meta-Block
    mb = s.shapes.add_textbox(Inches(0.9), Inches(6.15), Inches(11.5), Inches(1.2))
    tf = mb.text_frame; tf.word_wrap = True
    for i, (label, value) in enumerate(meta):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        rl = p.add_run(); rl.text = f"{label:<14}"
        rl.font.size = Pt(12); rl.font.name = FONT; rl.font.color.rgb = KICKER
        rv = p.add_run(); rv.text = "    " + value
        rv.font.size = Pt(12); rv.font.name = FONT; rv.font.color.rgb = WHITE
    return s


def content_slide(idx, number, title, bullets):
    s = prs.slides.add_slide(BLANK)
    # Heading: gruene Nummer + schwarzer Titel
    hb = s.shapes.add_textbox(Inches(0.7), Inches(0.4), Inches(12.0), Inches(0.9))
    p = hb.text_frame.paragraphs[0]
    rn = p.add_run(); rn.text = number + "  "
    rn.font.size = Pt(28); rn.font.bold = True; rn.font.name = FONT; rn.font.color.rgb = SECONDARY
    rt = p.add_run(); rt.text = title
    rt.font.size = Pt(28); rt.font.bold = True; rt.font.name = FONT; rt.font.color.rgb = TEXT
    # gruene Linie unter Heading
    line = s.shapes.add_shape(1, Inches(0.72), Inches(1.28), SW - Inches(1.42), Pt(2))
    _fill(line, SECONDARY)
    # Body
    body = s.shapes.add_textbox(Inches(0.75), Inches(1.55), Inches(12.0), Inches(5.2))
    tf = body.text_frame; tf.word_wrap = True
    first = True
    for level, text, bold in bullets:
        p = tf.paragraphs[0] if first else tf.add_paragraph()
        first = False
        p.level = level
        p.space_after = Pt(7)
        r = p.add_run()
        r.text = ("▪  " + text) if level == 0 else ("–  " + text)
        r.font.size = Pt(18 - level * 2)
        r.font.bold = bold
        r.font.name = FONT
        r.font.color.rgb = TEXT if (bold and level == 0) else SUBTEXT
    # Footer
    fl = s.shapes.add_textbox(Inches(0.7), Inches(7.0), Inches(6), Inches(0.4))
    rp = fl.text_frame.paragraphs[0]; rr = rp.add_run(); rr.text = "EatEasy EE — Architektur"
    rr.font.size = Pt(9); rr.font.name = FONT; rr.font.color.rgb = MUTED
    fr = s.shapes.add_textbox(Inches(7.0), Inches(7.0), Inches(5.6), Inches(0.4))
    rp = fr.text_frame.paragraphs[0]; rp.alignment = PP_ALIGN.RIGHT
    rr = rp.add_run(); rr.text = f"Folie {idx} / {TOTAL}"
    rr.font.size = Pt(9); rr.font.name = FONT; rr.font.color.rgb = MUTED
    return s


title_slide(
    "Komponentenbasierte Softwareentwicklung",
    "EatEasy EE — Architektur",
    ["Modularer Monolith · Vue 3 + Quarkus", "Studienprojekt Gruppe 5"],
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

out = "/home/user/EatEasyRepository/EatEasy-Architektur.pptx"
prs.save(out)
print("saved", out)
