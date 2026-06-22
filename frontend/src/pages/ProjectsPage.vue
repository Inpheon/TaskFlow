<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ArrowRight, FolderKanban, RefreshCw } from "@lucide/vue";
import { RouterLink } from "vue-router";
import { listProjects } from "@/api/projects";
import { ApiClientError } from "@/api/http";
import BaseButton from "@/components/BaseButton.vue";
import PageHeader from "@/components/PageHeader.vue";
import StatePanel from "@/components/StatePanel.vue";
import type { Project } from "@/types/api";

const projects = ref<Project[]>([]);
const loading = ref(true);
const loadError = ref<string | null>(null);
const projectCount = computed(() => {
  const count = projects.value.length;
  return `${count} ${count === 1 ? "project" : "projects"}`;
});

onMounted(loadProjects);

async function loadProjects() {
  loading.value = true;
  loadError.value = null;
  try {
    projects.value = await listProjects();
  } catch (error) {
    loadError.value = error instanceof ApiClientError
      ? error.message
      : "Unable to load projects";
  } finally {
    loading.value = false;
  }
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium"
  }).format(new Date(value));
}
</script>

<template>
  <section class="page">
    <PageHeader
      kicker="Workspace"
      title="Projects"
      description="Organize related tasks and follow their progress."
    />

    <StatePanel
      v-if="loading"
      variant="loading"
      message="Loading projects..."
    />

    <StatePanel
      v-else-if="loadError"
      variant="error"
      :message="loadError"
    >
      <template #actions>
        <BaseButton @click="loadProjects">
          <RefreshCw :size="17" aria-hidden="true" />
          Try again
        </BaseButton>
      </template>
    </StatePanel>

    <StatePanel
      v-else-if="projects.length === 0"
      title="No projects yet"
      message="Projects created for this account will appear here."
    >
      <template #icon>
        <FolderKanban :size="34" aria-hidden="true" />
      </template>
    </StatePanel>

    <div v-else class="project-section">
      <div class="project-list-toolbar">
        <strong>All projects</strong>
        <span>{{ projectCount }}</span>
      </div>
      <div class="project-list">
        <article v-for="project in projects" :key="project.id" class="project-row">
          <div class="project-main">
            <span class="project-icon">
              <FolderKanban :size="20" aria-hidden="true" />
            </span>
            <div>
              <RouterLink
                class="project-name"
                :to="{ name: 'project-detail', params: { projectId: project.id } }"
              >
                {{ project.name }}
              </RouterLink>
              <p>{{ project.description || "No description" }}</p>
            </div>
          </div>
          <span class="project-date">Updated {{ formatDate(project.updatedAt) }}</span>
          <RouterLink
            class="icon-button"
            title="Open project"
            :aria-label="`Open ${project.name}`"
            :to="{ name: 'project-detail', params: { projectId: project.id } }"
          >
            <ArrowRight :size="18" aria-hidden="true" />
          </RouterLink>
        </article>
      </div>
    </div>
  </section>
</template>
