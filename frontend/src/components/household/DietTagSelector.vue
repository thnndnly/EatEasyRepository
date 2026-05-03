<script setup lang="ts">
import { computed } from 'vue'
import { DIET_TAGS, DIET_TAG_LABELS, type DietTag } from '@/types/dietTags'

interface Props {
  modelValue: DietTag[]
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), { disabled: false })
const emit = defineEmits<{ 'update:modelValue': [value: DietTag[]] }>()

const selected = computed(() => new Set(props.modelValue))

function toggle(tag: DietTag): void {
  if (props.disabled) {
    return
  }
  const next = new Set(selected.value)
  if (next.has(tag)) {
    next.delete(tag)
  } else {
    next.add(tag)
  }
  emit('update:modelValue', DIET_TAGS.filter((t) => next.has(t)))
}
</script>

<template>
  <div class="flex flex-wrap gap-2">
    <label
      v-for="tag in DIET_TAGS"
      :key="tag"
      class="inline-flex cursor-pointer items-center gap-2 rounded border px-3 py-1.5 text-sm"
      :class="
        selected.has(tag)
          ? 'border-emerald-500 bg-emerald-50 text-emerald-800'
          : 'border-slate-300 bg-white text-slate-600 hover:border-slate-400'
      "
    >
      <input
        type="checkbox"
        class="sr-only"
        :checked="selected.has(tag)"
        :disabled="disabled"
        @change="toggle(tag)"
      />
      {{ DIET_TAG_LABELS[tag] }}
    </label>
  </div>
</template>
