<script setup lang="ts">
defineProps<{
  label: string;
  error?: string | null;
  hint?: string | null;
  optional?: boolean;
}>();
</script>

<template>
  <label class="field">
    <span>
      {{ label }}
      <small v-if="optional">Optional</small>
    </span>
    <slot />
    <small v-if="error" class="field-error">{{ error }}</small>
    <small v-else-if="hint" class="field-hint">{{ hint }}</small>
  </label>
</template>
