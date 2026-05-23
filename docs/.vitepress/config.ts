import { defineConfig } from 'vitepress'

export default defineConfig({
  lang: 'de-DE',
  title: 'EatEasy',
  description: 'Komponentenbasierte Web-Anwendung für haushaltsweite Mahlzeitenplanung.',
  cleanUrls: true,
  lastUpdated: true,
  // localhost-Links zur Swagger UI sind keine "echten" Dead Links —
  // sie zeigen auf den Backend-Endpunkt, der zur Laufzeit erreichbar ist.
  ignoreDeadLinks: [/^https?:\/\/localhost(:\d+)?\//],
  themeConfig: {
    nav: [
      { text: 'Start', link: '/' },
      { text: 'Setup', link: '/setup' },
      { text: 'Architektur', link: '/architektur' },
      { text: 'API', link: '/api' },
      { text: 'Tech-Stack', link: '/tech-stack' },
    ],
    sidebar: [
      {
        text: 'Überblick',
        items: [
          { text: 'Startseite', link: '/' },
          { text: 'Setup & Commands', link: '/setup' },
          { text: 'Tech-Stack', link: '/tech-stack' },
        ],
      },
      {
        text: 'Konzept',
        items: [
          { text: 'Architektur', link: '/architektur' },
          { text: 'API-Dokumentation', link: '/api' },
        ],
      },
    ],
    footer: {
      message: 'Studienprojekt — Gruppe 5',
      copyright: 'Dimitrios Tsakos · Kardeena Kameran',
    },
    search: {
      provider: 'local',
    },
  },
})
