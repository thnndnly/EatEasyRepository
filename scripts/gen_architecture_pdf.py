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
    chip(420, 405, "SPA · Vercel", 9, TINT, PRIMARY)
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


def relv(a, b, label):
    """Vertikaler Pfeil von Box a (oben) nach Box b (unten) oder umgekehrt."""
    x = a["cx"]
    arrow(x, a["bottom"], x, b["top"], GREEN3, 1.3, 5)
    my = (a["bottom"] + b["top"]) / 2
    c.setFillColor(WHITE); c.rect(x - 11, my - 5, 22, 11, fill=1, stroke=0)
    text(x, my - 3, label, 8, FB, GREEN3, "c")


def cluster(x, w, label):
    box(x, 80, w, 372, HexColor("#F1F6F2"), HexColor("#DCE8E0"), 10, 1)
    text(x + 13, 432, label, 9.5, FB, GREEN3)


def slide_datamodel():
    header("2", "Datenmodell")
    cluster(46, 200, "Nutzer & Haushalt")
    cluster(262, 200, "Rezepte & Zutaten")
    cluster(478, 186, "Wochenplan")
    cluster(684, 232, "Vorrat & Einkauf")
    # Cluster A
    user = entity(62, 416, "User", ["email · UNIQUE", "display_name"])
    hh = entity(62, 318, "Household", ["name", "default_diet_tags[]"])
    mem = entity(62, 214, "HouseholdMembership", ["role: OWNER / MEMBER", "→ user · → household"])
    relv(user, hh, "")
    relv(hh, mem, "n:m")
    # Cluster B
    rec = entity(278, 416, "Recipe", ["title · servings", "diet_tags[] · instructions"])
    ri = entity(278, 318, "RecipeIngredient", ["amount · unit", "→ recipe · → ingredient"])
    ing = entity(278, 214, "Ingredient", ["name · UNIQUE", "default_unit"])
    relv(rec, ri, "1:n")
    relv(ri, ing, "n:1")
    # Cluster C
    mp = entity(486, 416, "MealPlan", ["week_start (Mo)", "→ household"], w=170)
    mpe = entity(486, 300, "MealPlanEntry", ["day · meal_type · servings", "→ recipe"], w=170)
    relv(mp, mpe, "1:n")
    # Cluster D
    pan = entity(700, 416, "PantryItem", ["amount · unit · best_before", "→ ingredient"], w=200)
    sl = entity(700, 300, "ShoppingList", ["→ household", "→ meal_plan"], w=200)
    sli = entity(700, 196, "ShoppingListItem", ["amount · unit · checked", "→ ingredient"], w=200)
    relv(sl, sli, "1:n")
    # Legende
    text(50, 60, "FK-Referenzen (→ Feld):  MealPlanEntry → Recipe   ·   ShoppingList → MealPlan   ·   "
                 "RecipeIngredient / PantryItem / ShoppingListItem → Ingredient", 8.5, F, SUBTEXT)
    text(50, 44, "Konventionen:  household_id auf allen Haushalts-Entitäten   ·   UUID-PK + created_at / updated_at überall   ·   "
                 "Soft-Delete nur bei Recipe", 8.5, F, MUTED)
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
        ("Build & Deployment", ["Maven", "npm", "Docker", "Render", "Vercel"]),
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
