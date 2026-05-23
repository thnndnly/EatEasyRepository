---
layout: home

hero:
  name: EatEasy
  text: Mahlzeiten planen, gemeinsam.
  tagline: Haushaltsweite Rezeptverwaltung, Wochenplan und automatische Einkaufslisten — modular, selbst gehostet, mit Smart-Suggestions per LLM.
  actions:
    - theme: brand
      text: Setup starten
      link: /setup
    - theme: alt
      text: Architektur
      link: /architektur

features:
  - icon: 🧱
    title: Komponentenbasiert
    details: Modularer Quarkus-Monolith mit fachlichen Komponenten (auth, household, recipe, pantry, mealplan, shoppinglist, suggestion).
  - icon: ⚡
    title: Vue 3 + Vite
    details: Composition API, Pinia für State, voll typisiert mit TypeScript, lazy-loaded Routen.
  - icon: 🔐
    title: JWT-Auth
    details: SmallRye JWT mit kurzem Token-Lifecycle, Bean-Validation auf allen DTOs, Authorization-Checks im Service-Layer.
  - icon: 🧪
    title: Test-getrieben
    details: JUnit 5 + REST Assured im Backend, Vitest + MSW im Frontend, Playwright für E2E-Smoke-Tests.
  - icon: 🤖
    title: Smart-Suggestions
    details: Lokale Ollama-Instanz analysiert Vorrat + Diät-Tags und schlägt passende Rezepte vor — kein Cloud-Dienst notwendig.
  - icon: 📜
    title: Live-API-Doku
    details: OpenAPI 3 + Swagger UI direkt aus dem Backend, integriert via quarkus-smallrye-openapi.
---

## Was ist EatEasy?

EatEasy ist ein Studienprojekt der Gruppe 5 (Dimitrios Tsakos, Kardeena Kameran) im Modul „Web Frameworks". Ziel: ein Haushalt verwaltet seine Rezepte, plant Mahlzeiten für die Woche, sieht automatisch, welche Zutaten fehlen, und bekommt aus dem aktuellen Vorrat passende Rezeptvorschläge.

## Diese Doku abdeckt

- **[Setup & Commands](/setup)** — Voraussetzungen, Bootstrap, Standard-Workflows
- **[Architektur](/architektur)** — Komponenten, Layer-Regeln, Datenmodell
- **[API](/api)** — OpenAPI-Spec und Swagger UI
- **[Tech-Stack](/tech-stack)** — alle eingesetzten Technologien mit kurzem Steckbrief

Für tiefere Implementierungsdetails: `IMPLEMENTATION.md` im Repo-Root listet sämtliche Phasen, das Datenmodell und die vollständige API-Spezifikation.
