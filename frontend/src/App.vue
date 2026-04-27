<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div class="min-h-screen bg-slate-50 text-slate-900">
    <header class="border-b border-slate-200 bg-white">
      <div class="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
        <RouterLink to="/" class="text-lg font-semibold text-emerald-700">
          EatEasy EE
        </RouterLink>
        <nav class="flex gap-4 text-sm">
          <RouterLink
            to="/"
            class="text-slate-600 hover:text-emerald-700"
            active-class="text-emerald-700 font-medium"
          >
            Home
          </RouterLink>
          <RouterLink
            to="/about"
            class="text-slate-600 hover:text-emerald-700"
            active-class="text-emerald-700 font-medium"
          >
            About
          </RouterLink>
        </nav>
      </div>
    </header>

    <main class="mx-auto max-w-5xl px-6 py-10">
      <RouterView />
    </main>
  </div>
</template>
