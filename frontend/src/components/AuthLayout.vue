<script setup lang="ts">
defineProps<{
  title: string;
  subtitle: string;
}>();
</script>

<template>
  <main class="auth-page">
    <section class="auth-panel">
      <div class="auth-brand">
        <span class="brand-mark">TF</span>
        <span>TaskFlow</span>
      </div>
      <header>
        <h1>{{ title }}</h1>
        <p>{{ subtitle }}</p>
      </header>
      <slot />
    </section>
  </main>
</template>
