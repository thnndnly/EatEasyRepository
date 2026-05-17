<script setup lang="ts">
// Wrapper fuer leere Zustaende (Listen ohne Eintraege, etc.).
// Default-Slot enthaelt den Inhalt (Text, Icon, Buttons).
</script>

<template>
  <div
    class="rounded-2xl border border-dashed border-cream-300 bg-cream-50 p-8 text-center text-ink-500"
  >
    <slot />
  </div>
</template>
