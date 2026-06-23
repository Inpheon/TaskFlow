<script setup lang="ts">
import {computed, onMounted, reactive, ref} from "vue";
import { ArrowRight, FolderKanban, RefreshCw, Trash2, Plus, SquarePen } from "@lucide/vue";
import { RouterLink } from "vue-router";
import { listProjects, deleteProject, createProject, getProject, updateProject } from "@/api/projects";
import { ApiClientError } from "@/api/http";
import BaseButton from "@/components/BaseButton.vue";
import BaseDialog from "@/components/BaseDialog.vue";
import PageHeader from "@/components/PageHeader.vue";
import StatePanel from "@/components/StatePanel.vue";
import type { Project } from "@/types/api";
import FormField from "@/components/FormField.vue";

const projects = ref<Project[]>([]);
const loading = ref(true);
const loadError = ref<string | null>(null);
const pendingDelete = ref<{ id: string; index: number; name: string } | null>(null);
const pendingAdd = ref(false);
const pendingEdit = ref<{ id: string; index: number } | null>(null);
const form = reactive({
  name: "",
  description: "",
});
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

function promptAdd() {
  form.name = "";
  form.description = "";
  loadError.value = null;
  pendingAdd.value = true;
}

async function promptEdit(projectId: string, index: number) {
  loadError.value = null;
  try {
    const project = await getProject(projectId);
    form.name = project.name;
    form.description = project.description ?? "";
    pendingEdit.value = { id: projectId, index };
  } catch (error) {
    loadError.value = error instanceof ApiClientError
      ? error.message
      : "Unable to load project";
  }
}

function promptDelete(projectId: string, index: number, name: string) {
  pendingDelete.value = { id: projectId, index, name };
}

async function removeProject(projectId: string, index: number) {
  loading.value = true;
  loadError.value = null;
  try {
    await deleteProject(projectId);
    projects.value.splice(index, 1)
  } catch (error) {
    loadError.value = error instanceof ApiClientError
      ? error.message
      : "Unable to delete projects";
  } finally {
    loading.value = false;
  }
}

async function confirmDelete() {
  if (!pendingDelete.value) return;
  const { id, index } = pendingDelete.value;
  pendingDelete.value = null;
  await removeProject(id, index);
}

async function submitEdit() {
  if (!pendingEdit.value) return;
  loading.value = true;
  try {
    const updated = await updateProject(pendingEdit.value.id, {
      name: form.name.trim(),
      description: form.description || null
    });
    projects.value[pendingEdit.value.index] = updated;
  } catch (error) {
    loadError.value = error instanceof ApiClientError
      ? error.message
      : "Unable to update project";
  } finally {
    loading.value = false;
    pendingEdit.value = null;
  }
}

async function submit() {
  loading.value = true;
  try {
    const newProject = await createProject({
      name: form.name.trim(),
      description: form.description
    });
    projects.value.push(newProject);
  } catch (error) {
    loadError.value = error instanceof ApiClientError
      ? error.message
      : "Unable to add the project";
  } finally {
    loading.value = false;
    pendingAdd.value = false;
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

    <div class="project-section">
      <div class="project-list-toolbar">
        <strong>All projects</strong>
        <span>{{ projectCount }}</span>
        <button
          class="icon-button"
          title="Create a new project"
          aria-label="Create a new project"
          @click="promptAdd()"
        >
          <Plus :size="18" aria-hidden="true" />
        </button>
      </div>
      <div class="project-list">
        <article v-for="(project, index) in projects" :key="project.id" class="project-row">
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
          <button
            class="icon-button"
            title="Edit project"
            :aria-label="`Edit ${project.name}`"
            @click="promptEdit(project.id, index)"
          >
            <SquarePen :size="18" aria-hidden="true" />
          </button>
          <button
            class="icon-button"
            title="Delete project"
            :aria-label="`Delete ${project.name}`"
            @click="promptDelete(project.id, index, project.name)"
          >
            <Trash2 :size="18" aria-hidden="true" />
          </button>
        </article>
      </div>

      <StatePanel
        v-if="projects.length === 0"
        title="No projects yet"
        message="Projects created for this account will appear here."
      >
        <template #icon>
          <FolderKanban :size="34" aria-hidden="true" />
        </template>
      </StatePanel>

    </div>

    <BaseDialog
      v-if="pendingAdd || pendingEdit"
      :title="pendingEdit ? 'Edit project' : 'Add a new project'"
      @close="pendingAdd = false; pendingEdit = null"
    >
      <form @submit.prevent="pendingEdit ? submitEdit() : submit()">
        <div class="dialog-body">
          <FormField label="Name">
            <input
              v-model="form.name"
              name="name"
              type="text"
              required
              autofocus
            />
          </FormField>
          <FormField label="Description">
            <input
              v-model="form.description"
              name="description"
              type="text"
            />
          </FormField>
          <p v-if="loadError" class="form-error" role="alert">{{ loadError }}</p>
        </div>
        <div class="dialog-footer">
          <BaseButton @click="pendingAdd = false; pendingEdit = null">Cancel</BaseButton>
          <BaseButton type="submit" variant="primary" :disabled="loading">
            {{ pendingEdit ? (loading ? "Saving..." : "Save changes") : (loading ? "Adding..." : "Add") }}
          </BaseButton>
        </div>
      </form>
    </BaseDialog>

    <BaseDialog
      v-if="pendingDelete"
      title="Delete project"
      :description="`Are you sure you want to delete &quot;${pendingDelete.name}&quot;? This action cannot be undone.`"
      @close="pendingDelete = null"
    >
      <div class="dialog-footer">
        <BaseButton @click="pendingDelete = null">Cancel</BaseButton>
        <BaseButton variant="danger" @click="confirmDelete">Delete</BaseButton>
      </div>
    </BaseDialog>

  </section>
</template>
