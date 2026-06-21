<script setup lang="ts">
import { computed } from "vue";
import { FolderKanban, LogOut } from "@lucide/vue";
import { RouterLink, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const router = useRouter();
const initials = computed(() => {
  const name = auth.user?.displayName.trim();
  if (!name) {
    return "U";
  }
  return name
    .split(/\s+/)
    .slice(0, 2)
    .map(part => part.charAt(0).toUpperCase())
    .join("");
});

function logout() {
  auth.clearSession();
  void router.push({ name: "login" });
}
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <RouterLink class="brand" :to="{ name: 'projects' }">
        <span class="brand-mark">TF</span>
        <span>TaskFlow</span>
      </RouterLink>

      <nav class="primary-nav" aria-label="Primary navigation">
        <RouterLink :to="{ name: 'projects' }">
          <FolderKanban :size="18" aria-hidden="true" />
          Projects
        </RouterLink>
      </nav>

      <div class="account">
        <span class="account-avatar" aria-hidden="true">{{ initials }}</span>
        <div class="account-copy">
          <strong>{{ auth.user?.displayName }}</strong>
          <span>{{ auth.user?.email }}</span>
        </div>
        <button
          class="icon-button"
          type="button"
          title="Log out"
          aria-label="Log out"
          @click="logout"
        >
          <LogOut :size="18" aria-hidden="true" />
        </button>
      </div>
    </header>

    <main class="app-content">
      <slot />
    </main>
  </div>
</template>
