#!/usr/bin/env python3
"""EatEasy EE — Architektur-Deck (16:9) als grafisches PDF im Hausstil.

Fokus: wenig Text, viel Grafik — Struktur, Datenmodell, Technologien.
Direkt via reportlab gezeichnet (LibreOffice-Konvertierung hier nicht verfuegbar).
"""
from reportlab.lib.units import inch
from reportlab.lib.colors import HexColor
from reportlab.pdfgen import canvas

W, H = 13.333 * inch, 7.5 * inch  # 960 x 540 pt

# ---- EatEasy Hausstil-Palette (aus Projektidee.pdf) ----
PRIMARY = HexColor("#1E3A2F")
SECONDARY = HexColor("#2D5A47")
GREEN3 = HexColor("#3E7A5E")
TINT = HexColor("#E8F0EB")
CARD = HexColor("#F4F8F5")
TEXT = HexColor("#1A1A1A")
SUBTEXT = HexColor("#333333")
MUTED = HexColor("#777777")
KICKER = HexColor("#9FB6A9")
SUBLIGHT = HexColor("#C9D8CF")
WHITE = HexColor("#FFFFFF")
BORDER = HexColor("#CBD9CF")
TOTAL = 4

c = canvas.Canvas("/home/user/EatEasyRepository/EatEasy-Architektur.pdf", pagesize=(W, H))

F = "Helvetica"
FB = "Helvetica-Bold"


# ---------------- Primitive Helfer ----------------
def text(x, y, s, size=11, font=F, color=TEXT, align="l"):
    c.setFillColor(color); c.setFont(font, size)
    if align == "l":
        c.drawString(x, y, s)
    elif align == "c":
        c.drawCentredString(x, y, s)
    else:
        c.drawRightString(x, y, s)


def box(x, y, w, h, fill=CARD, stroke=BORDER, r=8, lw=1):
    if fill is not None:
        c.setFillColor(fill)
    c.setStrokeColor(stroke if stroke else fill); c.setLineWidth(lw)
    c.roundRect(x, y, w, h, r, stroke=1 if stroke else 0, fill=1 if fill else 0)


def chip(x, y, s, size=10, fill=TINT, fg=PRIMARY, pad=8, h=20):
    w = c.stringWidth(s, F, size) + pad * 2
    c.setFillColor(fill); c.setStrokeColor(fill)
    c.roundRect(x, y, w, h, h / 2, stroke=0, fill=1)
    text(x + pad, y + (h - size) / 2 + 1, s, size, F, fg)
    return w


def arrow(x1, y1, x2, y2, color=SECONDARY, lw=1.6, head=6):
    import math
    c.setStrokeColor(color); c.setFillColor(color); c.setLineWidth(lw)
    c.line(x1, y1, x2, y2)
    ang = math.atan2(y2 - y1, x2 - x1)
    for da in (math.radians(150), math.radians(-150)):
        c.line(x2, y2, x2 + head * math.cos(ang + da), y2 + head * math.sin(ang + da))


def cylinder(x, y, w, h, fill, stroke, label, sub=None):
    ry = h * 0.13
    c.setFillColor(fill); c.setStrokeColor(stroke); c.setLineWidth(1.2)
    c.rect(x, y + ry, w, h - 2 * ry, stroke=0, fill=1)
    c.ellipse(x, y + h - 2 * ry, x + w, y + h, stroke=1, fill=1)
    c.ellipse(x, y, x + w, y + 2 * ry, stroke=1, fill=1)
    c.setStrokeColor(stroke)
    c.line(x, y + ry, x, y + h - ry); c.line(x + w, y + ry, x + w, y + h - ry)
    text(x + w / 2, y + h / 2 - 2, label, 11, FB, WHITE, "c")
    if sub:
        text(x + w / 2, y + h / 2 - 16, sub, 8, F, SUBLIGHT, "c")


# ---------------- Folien-Rahmen ----------------
def header(number, title):
    c.setFillColor(WHITE); c.rect(0, 0, W, H, fill=1, stroke=0)
    text(50, H - 52, number, 26, FB, SECONDARY)
    nx = 50 + c.stringWidth(number, FB, 26) + 12
    text(nx, H - 52, title, 26, FB, TEXT)
    c.setStrokeColor(SECONDARY); c.setLineWidth(1.4)
    c.line(52, H - 64, W - 50, H - 64)


def footer(idx):
    text(50, 26, "EatEasy EE — Architektur", 9, F, MUTED)
    text(W - 50, 26, f"Folie {idx} / {TOTAL}", 9, F, MUTED, "r")


# ================= Folie 1: Titel =================
def slide_title():
    c.setFillColor(PRIMARY); c.rect(0, 0, W, H, fill=1, stroke=0)
    c.setFillColor(SECONDARY); c.rect(0, H - 23, W, 23, fill=1, stroke=0)
    text(65, H - 180, " ".join("KOMPONENTENBASIERTE SOFTWAREENTWICKLUNG"),
         13, F, KICKER)
    text(63, H - 250, "EatEasy EE — Architektur", 50, FB, WHITE)
    text(65, H - 295, "Struktur · Datenmodell · Technologien", 20, F, SUBLIGHT)
    c.setStrokeColor(SECONDARY); c.setLineWidth(0.8)
    c.line(65, 112, W - 65, 112)
    rows = [("Stack", "Vue 3 · Quarkus · PostgreSQL · Docker"),
            ("Architektur", "Modularer Monolith · 11 Komponenten"),
            ("Stand", "Phasen 0–10 umgesetzt · Issues #3–#8 offen")]
    yy = 86
    for label, val in rows:
        text(65, yy, label, 12, F, KICKER)
        text(210, yy, val, 12, F, WHITE)
        yy -= 24
    c.showPage()


# ================= Folie 2: Struktur =================
def slide_structure():
    header("1", "Struktur")
    # Frontend
    box(50, 392, 560, 48, CARD, BORDER, 10)
    text(70, 421, "Frontend", 13, FB, PRIMARY)
    text(70, 403, "Vue 3 · Pinia · Vue Router · TypeScript", 9.5, F, SUBTEXT)
    chip(400, 405, "SPA · Render (static)", 9, TINT, PRIMARY)
    # REST Pfeil
    arrow(330, 392, 330, 366, SECONDARY, 1.8, 7)
    text(340, 374, "REST  /api/v1  ·  JWT", 9, FB, SECONDARY)
    # Backend-Container
    box(50, 120, 560, 240, "#FBFDFB", SECONDARY, 12, 1.4)
    text(70, 338, "Backend — Quarkus", 13, FB, PRIMARY)
    text(70, 322, "Modularer Monolith · Komponenten via Service-Interfaces (CDI)", 8.5, F, MUTED)
    comps = ["auth", "household", "recipe", "ingredient", "mealplan",
             "pantry", "shoppinglist", "suggestion", "integration",
             "notification", "common"]
    cx0, cy0, cw, ch, gx, gy = 70, 270, 168, 30, 8, 10
    for i, name in enumerate(comps):
        col, row = i % 3, i // 3
        x = cx0 + col * (cw + gx); y = cy0 - row * (ch + gy)
        box(x, y, cw, ch, TINT, SECONDARY, 6, 0.8)
        text(x + cw / 2, y + 9, name, 10, FB, PRIMARY, "c")
    # Schichten-Hinweis
    text(70, 134, "je Komponente:  entity → repository → service → resource → dto",
         9, F, SECONDARY)
    # DB
    cylinder(150, 36, 150, 60, SECONDARY, PRIMARY, "PostgreSQL 16", "Flyway-Migrationen")
    arrow(230, 120, 225, 98, SECONDARY, 1.6, 6)
    # Externe Systeme
    box(645, 120, 270, 320, CARD, BORDER, 12)
    text(665, 421, "Externe Systeme & Infra", 12, FB, PRIMARY)
    ext = [("Ollama (LLM)", "Smart-Suggestion · Llama 3 / Mistral"),
           ("TheMealDB", "Rezept-Import (gratis)"),
           ("OpenFoodFacts", "Barcode-Lookup"),
           ("Maildev / SMTP", "Einladungs-Mails"),
           ("Docker · Render", "Container & Hosting")]
    ey = 360
    for name, sub in ext:
        box(665, ey, 230, 44, WHITE, BORDER, 8)
        text(678, ey + 26, name, 10.5, FB, SECONDARY)
        text(678, ey + 10, sub, 8, F, MUTED)
        arrow(645, ey + 22, 612, ey + 22, GREEN3, 1.2, 5)
        ey -= 56
    footer(2)
    c.showPage()


# ================= Folie 3: Datenmodell =================
def entity(x, ytop, name, fields, w=168):
    h = 24 + len(fields) * 14 + 8
    box(x, ytop - h, w, h, WHITE, BORDER, 7, 1)
    c.setFillColor(SECONDARY)
    c.roundRect(x, ytop - 24, w, 24, 7, stroke=0, fill=1)
    c.rect(x, ytop - 24, w, 12, stroke=0, fill=1)
    text(x + 9, ytop - 17, name, 10.5, FB, WHITE)
    fy = ytop - 40
    for f in fields:
        text(x + 9, fy, f, 8, F, SUBTEXT)
        fy -= 14
    return {"x": x, "w": w, "top": ytop, "bottom": ytop - h, "cx": x + w / 2}


def _mid(b):
    return (b["top"] + b["bottom"]) / 2


def _label(x, y, s, color):
    w = c.stringWidth(s, FB, 8) + 6
    c.setFillColor(WHITE); c.rect(x - w / 2, y - 5, w, 11, fill=1, stroke=0)
    text(x, y - 3, s, 8, FB, color, "c")


def relv(a, b, label, color=SECONDARY):
    """Vertikaler Pfeil zwischen gestapelten Boxen einer Spalte."""
    x = a["cx"]
    arrow(x, a["bottom"], x, b["top"], color, 1.3, 5)
    if label:
        _label(x, (a["bottom"] + b["top"]) / 2, label, color)


def relh(a, b, label, color=GREEN3):
    """Diagonaler Querpfeil von rechter Kante a zu linker Kante b (Modul→Modul)."""
    x1, y1 = a["x"] + a["w"], _mid(a)
    x2, y2 = b["x"], _mid(b)
    arrow(x1, y1, x2, y2, color, 1.4, 6)
    _label((x1 + x2) / 2, (y1 + y2) / 2, label, color)


def relh_rev(a, b, label, color=GREEN3):
    """Querpfeil von linker Kante a zu rechter Kante b (nach links zeigend)."""
    x1, y1 = a["x"], _mid(a)
    x2, y2 = b["x"] + b["w"], _mid(b)
    arrow(x1, y1, x2, y2, color, 1.4, 6)
    _label((x1 + x2) / 2, (y1 + y2) / 2, label, color)


def elbow(pts, label, color=GREEN3):
    """Orthogonaler Pfad (Liste von Punkten), Pfeilspitze am letzten Segment."""
    c.setStrokeColor(color); c.setLineWidth(1.4)
    for i in range(len(pts) - 2):
        c.line(pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1])
    arrow(pts[-2][0], pts[-2][1], pts[-1][0], pts[-1][1], color, 1.4, 6)
    m = len(pts) // 2  # Label mittig auf der Bodenspur
    _label((pts[m - 1][0] + pts[m][0]) / 2, pts[m - 1][1], label, color)


def cluster(x, w, label):
    box(x, 86, w, 366, HexColor("#F1F6F2"), HexColor("#DCE8E0"), 10, 1)
    text(x + 13, 434, label, 9.5, FB, GREEN3)


def slide_datamodel():
    header("2", "Datenmodell")
    # Cluster in Fluss-Reihenfolge: Haushalt → Wochenplan → Rezepte → Vorrat
    cluster(46, 200, "Nutzer & Haushalt")
    cluster(260, 176, "Wochenplan")
    cluster(450, 200, "Rezepte & Zutaten")
    cluster(664, 236, "Vorrat & Einkauf")
    # Cluster 1 — Nutzer & Haushalt
    user = entity(62, 416, "User", ["email · UNIQUE", "display_name"])
    hh = entity(62, 320, "Household", ["name", "default_diet_tags[]"])
    mem = entity(62, 216, "HouseholdMembership", ["role: OWNER / MEMBER", "→ user · → household"])
    relv(user, hh, "")
    relv(hh, mem, "n:m")
    # Cluster 2 — Wochenplan
    mp = entity(272, 416, "MealPlan", ["week_start (Mo)", "→ household"], w=152)
    mpe = entity(272, 300, "MealPlanEntry", ["day · meal_type", "servings · → recipe"], w=152)
    relv(mp, mpe, "1:n")
    # Cluster 3 — Rezepte & Zutaten
    rec = entity(466, 416, "Recipe", ["title · servings", "diet_tags[] · instructions"])
    ri = entity(466, 320, "RecipeIngredient", ["amount · unit", "→ recipe · → ingredient"])
    ing = entity(466, 216, "Ingredient", ["name · UNIQUE", "default_unit"])
    relv(rec, ri, "1:n")
    relv(ri, ing, "n:1")
    # Cluster 4 — Vorrat & Einkauf
    pan = entity(680, 416, "PantryItem", ["amount · unit · best_before", "→ ingredient"], w=204)
    sl = entity(680, 300, "ShoppingList", ["→ household", "→ meal_plan"], w=204)
    sli = entity(680, 196, "ShoppingListItem", ["amount · unit · checked", "→ ingredient"], w=204)
    relv(sl, sli, "1:n")
    # --- Modul-uebergreifende Pfeile (das war der fehlende Teil) ---
    relh(hh, mp, "1:n")          # Haushalt → Wochenplan
    relh(mpe, rec, "n:1")        # Wochenplan-Eintrag → Rezept
    relh_rev(sli, ing, "n:1")    # Einkaufsposten → Zutat
    # Vorratsposten → Zutat (laengerer Querpfeil)
    arrow(pan["x"], _mid(pan), ing["x"] + ing["w"], _mid(ing), GREEN3, 1.3, 6)
    _label((pan["x"] + ing["x"] + ing["w"]) / 2, (_mid(pan) + _mid(ing)) / 2, "n:1", GREEN3)
    # Einkaufsliste → Wochenplan (orthogonal ueber die untere Spur, an den
    # Boxen vorbei: rechts runter, unten quer, links wieder hoch in MealPlan)
    elbow([(sl["x"], _mid(sl)), (654, _mid(sl)), (654, 112), (256, 112),
           (256, _mid(mp)), (mp["x"], _mid(mp))], "1:1")
    # Legende
    text(50, 60, "→ household_id auf allen Haushalts-Entitäten (Haushalt besitzt Rezepte, Plan, Vorrat, Liste)   ·   "
                 "UUID-PK + created_at / updated_at überall   ·   Soft-Delete nur bei Recipe", 8.5, F, MUTED)
    footer(3)
    c.showPage()


# ================= Folie 4: Technologien =================
def tech_card(x, y, w, title, chips):
    # Hoehe dynamisch nach Chip-Umbruch berechnen
    inner = x + 14
    maxx = x + w - 14
    cx, cy = inner, 0
    lines = 1
    for s in chips:
        cw = c.stringWidth(s, F, 9.5) + 16 + 8
        if cx + cw > maxx:
            cx = inner; lines += 1
        cx += cw
    h = 30 + lines * 27
    box(x, y - h, w, h, CARD, BORDER, 10)
    c.setFillColor(SECONDARY)
    c.roundRect(x, y - 24, w, 24, 10, stroke=0, fill=1)
    c.rect(x, y - 24, w, 12, stroke=0, fill=1)
    text(x + 14, y - 17, title, 11, FB, WHITE)
    cx, cy = inner, y - 24 - 24
    for s in chips:
        cw = c.stringWidth(s, F, 9.5) + 16
        if cx + cw + 8 > maxx:
            cx = inner; cy -= 27
        chip(cx, cy, s, 9.5, TINT, PRIMARY, pad=8, h=20)
        cx += cw + 8
    return h


def slide_tech():
    header("3", "Technologien")
    cats = [
        ("Frontend", ["Vue 3", "Vite", "Pinia", "Vue Router", "TypeScript", "Tailwind", "VueUse"]),
        ("Backend", ["Quarkus", "Java 21", "RESTEasy Reactive", "Hibernate Panache", "SmallRye JWT", "Mailer", "REST Client"]),
        ("Datenbank", ["PostgreSQL 16", "Flyway", "UUID v4"]),
        ("LLM (self-hosted)", ["Ollama", "Llama 3", "Mistral"]),
        ("Externe APIs", ["TheMealDB", "OpenFoodFacts"]),
        ("Testing", ["JUnit 5", "REST Assured", "Vitest", "MSW", "Playwright E2E"]),
        ("Qualität / CI", ["Checkstyle", "ESLint", "Prettier", "GitHub Actions"]),
        ("Build & Deployment", ["Maven", "npm", "Docker", "Render: DB + API + Web"]),
    ]
    colw = 410
    xL, xR = 50, 50 + colw + 40
    yL = yR = H - 95
    for i, (title, chips) in enumerate(cats):
        if i % 2 == 0:
            h = tech_card(xL, yL, colw, title, chips); yL -= h + 16
        else:
            h = tech_card(xR, yR, colw, title, chips); yR -= h + 16
    footer(4)
    c.showPage()


slide_title()
slide_structure()
slide_datamodel()
slide_tech()
c.save()
print("saved EatEasy-Architektur.pdf")
