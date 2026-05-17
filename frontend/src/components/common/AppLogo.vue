<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  size?: number
}

const props = withDefaults(defineProps<Props>(), {
  size: 40,
})

const sizeStyle = computed(() => {
  const px = `${props.size}px`
  const fontSize = `${Math.round(props.size * 0.5)}px`
  return {
    width: px,
    height: px,
    fontSize,
    background: 'linear-gradient(135deg, #ffb5a7 0%, #ffd47a 100%)',
  }
})
</script>

<template>
  <span
    class="flex items-center justify-center rounded-2xl shadow-sm"
    :style="sizeStyle"
    aria-hidden="true"
  >
    🍅
  </span>
</template>
