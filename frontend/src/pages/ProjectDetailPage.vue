<script setup lang="ts">
import { onMounted, ref } from "vue";
import { ArrowLeft, FolderKanban, RefreshCw } from "@lucide/vue";
import { RouterLink, useRoute } from "vue-router";
import { getProject } from "@/api/projects";
import { ApiClientError } from "@/api/http";
import BaseButton from "@/components/BaseButton.vue";
import PageHeader from "@/components/PageHeader.vue";
import StatePanel from "@/components/StatePanel.vue";
import type { Project } from "@/types/api";

const route = useRoute();
const project = ref<Project | null>(null);
const loading = ref(true);
const error = ref<string | null>(null);

onMounted(loadProject);

async function loadProject() {
  loading.value = true;
  error.value = null;
  try {
    project.value = await getProject(String(route.params.projectId));
  } catch (caught) {
    error.value = caught instanceof ApiClientError
      ? caught.message
      : "Unable to load the project";
  } finally {
    loading.value = false;
  }
}

function formatUpdatedAt(value: string) {
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium"
  }).format(new Date(value));
}
</script>

<template>
  <section class="page">
    <RouterLink class="back-link" :to="{ name: 'projects' }">
      <ArrowLeft :size="17" aria-hidden="true" />
      Back to projects
    </RouterLink>

    <StatePanel
      v-if="loading"
      variant="loading"
      message="Loading project..."
    />

    <StatePanel
      v-else-if="!project"
      variant="error"
      :message="error || 'Unable to load the project'"
    >
      <template #actions>
        <BaseButton @click="loadProject">
          <RefreshCw :size="17" aria-hidden="true" />
          Try again
        </BaseButton>
      </template>
    </StatePanel>

    <template v-else>
      <PageHeader
        kicker="Project"
        :title="project.name"
        :description="project.description || 'No description provided.'"
      />

      <p class="project-updated">Updated {{ formatUpdatedAt(project.updatedAt) }}</p>

      <section class="project-workspace">
        <header>
          <span class="project-workspace-icon">
            <FolderKanban :size="22" aria-hidden="true" />
          </span>
          <div>
            <h2>Project workspace</h2>
            <p>Tasks and board views can be added here.</p>
          </div>
        </header>
      </section>
    </template>
  </section>
</template>
