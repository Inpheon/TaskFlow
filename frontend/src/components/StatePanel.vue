<script setup lang="ts">
withDefaults(defineProps<{
  title?: string;
  message: string;
  variant?: "loading" | "empty" | "error";
}>(), {
  title: undefined,
  variant: "empty"
});
</script>

<template>
  <div
    class="state-panel"
    :class="`${variant}-state`"
    :aria-live="variant === 'loading' ? 'polite' : undefined"
  >
    <span v-if="variant === 'loading'" class="spinner" aria-hidden="true" />
    <slot name="icon" />
    <h2 v-if="title">{{ title }}</h2>
    <p>{{ message }}</p>
    <slot name="actions" />
  </div>
</template>
