<script setup lang="ts">
defineProps<{
  kicker?: string;
  title: string;
  description?: string;
}>();
</script>

<template>
  <header class="page-header">
    <div>
      <p v-if="kicker" class="page-kicker">{{ kicker }}</p>
      <h1>{{ title }}</h1>
      <p v-if="description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="header-actions">
      <slot name="actions" />
    </div>
  </header>
</template>
