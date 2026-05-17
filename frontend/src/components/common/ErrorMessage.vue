<script setup lang="ts">
interface Props {
  message: string
}

defineProps<Props>()
</script>

<template>
  <p
    v-if="message"
    class="rounded-2xl border border-rose-200 bg-rose-100 px-3 py-2 text-sm font-medium text-rose-700"
  >
    {{ message }}
  </p>
</template>
