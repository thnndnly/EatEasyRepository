<script setup lang="ts">
// Wrapper für leere Zustände (Listen ohne Einträge, etc.).
// Default-Slot enthält den Inhalt (Text, Icon, Buttons).
</script>

<template>
  <div
    class="rounded-2xl border border-dashed border-cream-300 bg-cream-50 p-8 text-center text-ink-500"
  >
    <slot />
  </div>
</template>
