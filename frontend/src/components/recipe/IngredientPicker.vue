<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useIngredientStore } from '@/stores/ingredientStore'
import type { IngredientDto } from '@/types/ingredient'

interface Props {
  /** Aktuelle Auswahl (id + name) — id ist null fuer eine geplante Neu-Anlage. */
  modelValue: { id: string | null; name: string }
  placeholder?: string
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: 'Zutat suchen oder neu anlegen ...',
})
const emit = defineEmits<{
  'update:modelValue': [value: { id: string | null; name: string }]
}>()

const ingredientStore = useIngredientStore()

const inputValue = ref(props.modelValue.name)
// Lokale Kopie der Treffer — eigenes Snapshot pro Picker, damit zwei
// Picker auf derselben Seite sich nicht gegenseitig die Liste ueberschreiben.
const results = ref<IngredientDto[]>([])
const open = ref(false)
const loading = ref(false)

let debounceTimer: ReturnType<typeof setTimeout> | null = null

watch(
  () => props.modelValue.name,
  (next) => {
    if (next !== inputValue.value) {
      inputValue.value = next
    }
  },
)

async function runSearch(): Promise<void> {
  loading.value = true
  try {
    results.value = await ingredientStore.search(inputValue.value, 10)
  } catch {
    results.value = []
  } finally {
    loading.value = false
  }
}

function onInput(event: Event): void {
  const target = event.target as HTMLInputElement
  inputValue.value = target.value
  emit('update:modelValue', { id: null, name: target.value })
  open.value = true

  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  debounceTimer = setTimeout(runSearch, 200)
}

function onFocus(): void {
  open.value = true
  if (results.value.length === 0) {
    void runSearch()
  }
}

function onBlur(): void {
  // Verzoegert, damit Klicks auf Treffer noch durchkommen.
  setTimeout(() => {
    open.value = false
  }, 150)
}

function selectIngredient(ingredient: IngredientDto): void {
  inputValue.value = ingredient.name
  emit('update:modelValue', { id: ingredient.id, name: ingredient.name })
  open.value = false
}

const showCreateHint = computed(() => {
  const trimmed = inputValue.value.trim()
  if (!trimmed) {
    return false
  }
  return !results.value.some((r) => r.name.toLowerCase() === trimmed.toLowerCase())
})
</script>

<template>
  <div class="relative">
    <input
      type="text"
      :value="inputValue"
      :placeholder="placeholder"
      autocomplete="off"
      class="ee-input w-full"
      @input="onInput"
      @focus="onFocus"
      @blur="onBlur"
    />

    <ul
      v-if="open && (results.length > 0 || showCreateHint || loading)"
      class="absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded border border-cream-200 bg-white shadow-lg"
    >
      <li v-if="loading" class="px-3 py-2 text-sm text-ink-500">Suche ...</li>

      <li
        v-for="ingredient in results"
        :key="ingredient.id"
        class="cursor-pointer px-3 py-2 text-sm hover:bg-cream-200"
        @mousedown.prevent="selectIngredient(ingredient)"
      >
        <span class="font-medium">{{ ingredient.name }}</span>
        <span class="ml-2 text-xs text-ink-500">{{ ingredient.defaultUnit }}</span>
      </li>

      <li
        v-if="showCreateHint && !loading"
        class="cursor-pointer border-t border-cream-100 px-3 py-2 text-sm text-peach-600 hover:bg-peach-50"
        @mousedown.prevent="
          emit('update:modelValue', { id: null, name: inputValue.trim() }); open = false
        "
      >
        + „{{ inputValue.trim() }}" anlegen
      </li>
    </ul>
  </div>
</template>
