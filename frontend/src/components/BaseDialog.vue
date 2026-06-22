<script setup lang="ts">
import { onBeforeUnmount, onMounted, useId } from "vue";
import { X } from "@lucide/vue";

defineProps<{
  title: string;
  description?: string;
}>();

const emit = defineEmits<{
  close: [];
}>();
const titleId = useId();

function close() {
  emit("close");
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === "Escape") {
    close();
  }
}

onMounted(() => {
  document.addEventListener("keydown", handleKeydown);
});

onBeforeUnmount(() => {
  document.removeEventListener("keydown", handleKeydown);
});
</script>

<template>
  <div class="dialog-backdrop" @mousedown.self="close">
    <section
      class="dialog"
      role="dialog"
      aria-modal="true"
      :aria-labelledby="titleId"
    >
      <header class="dialog-header">
        <div>
          <h2 :id="titleId">{{ title }}</h2>
          <p v-if="description">{{ description }}</p>
        </div>
        <button
          class="icon-button"
          type="button"
          title="Close"
          aria-label="Close dialog"
          @click="close"
        >
          <X :size="18" aria-hidden="true" />
        </button>
      </header>
      <slot />
    </section>
  </div>
</template>
