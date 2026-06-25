<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ArrowLeft, FolderKanban, Plus, RefreshCw, SquarePen, Trash2 } from "@lucide/vue";
import { RouterLink, useRoute } from "vue-router";
import { getProject } from "@/api/projects";
import { createNote, createTask, deleteNote, deleteTask, getNotes, getProjectReport, getProjectSummary,
  getSuggestedTask, getTask, listTasks, moveTask, projectBoard, updateTask } from "@/api/tasks";
import Knob from "primevue/knob";
import DatePicker from "primevue/datepicker";
import { ApiClientError } from "@/api/http";
import BaseButton from "@/components/BaseButton.vue";
import BaseDialog from "@/components/BaseDialog.vue";
import FormField from "@/components/FormField.vue";
import PageHeader from "@/components/PageHeader.vue";
import StatePanel from "@/components/StatePanel.vue";
import type { BoardResponse, CreateTaskRequest, Project, ProjectReportResponse, ProjectStatsResponse,
  SuggestedTaskReason, SuggestedTaskResponse, TaskNoteResponse, TaskPriority, TaskResponse, TaskStatus } from "@/types/api";

const STATUSES: TaskStatus[] = ["TODO", "IN_PROGRESS", "DONE"];
const STATUS_LABELS: Record<TaskStatus, string> = {
  TODO: "To Do",
  IN_PROGRESS: "In Progress",
  DONE: "Done",
};
const PRIORITIES: TaskPriority[] = ["LOW", "MEDIUM", "HIGH"];
const PRIORITY_LABELS: Record<TaskPriority, string> = {
  LOW: "Low",
  MEDIUM: "Medium",
  HIGH: "High",
};
const REASON_LABELS: Record<SuggestedTaskReason, string> = {
  OVERDUE: "Overdue",
  NEAREST_DUE_DATE: "Nearest due date",
  HIGH_PRIORITY: "High priority",
  OLDEST_OPEN_TASK: "Oldest open task",
};

interface DragState { task: TaskResponse; fromStatus: TaskStatus }
interface DropTarget { status: TaskStatus; index: number }

const route = useRoute();
const project = ref<Project | null>(null);
const board = ref<BoardResponse | null>(null);
const tasks = ref<TaskResponse[]>([]);
const projectStats = ref<ProjectStatsResponse | null>(null);
const viewingReport = ref<ProjectReportResponse | null>(null);
const reportLoading = ref(false);
const reportError = ref<string | null>(null);
const suggestedTask = ref<SuggestedTaskResponse | null>(null);
const suggestionLoading = ref(false);
const suggestionError = ref<string | null>(null);
const loading = ref(true);
const formLoading = ref(false);
const pendingDelete = ref<{ id: string; index: number; name: string } | null>(null);
const pendingAdd = ref(false);
const pendingEdit = ref<{ id: string; index: number } | null>(null);
const error = ref<string | null>(null);
const dialogError = ref<string | null>(null);
const dragging = ref<DragState | null>(null);
const dropAt = ref<DropTarget | null>(null);
const viewingTask = ref<TaskResponse | null>(null);
const viewingTaskNotes = ref<TaskNoteResponse[]>([]);
const notesLoading = ref(false);
const notesError = ref<string | null>(null);
const newNoteContent = ref("");

const TASK_NAME_MAX_LENGTH : string = "200"
const TASK_DESCR_MAX_LENGTH : string = "5000"
const TASK_NOTE_MAX_LENGTH : string = "5000"

const taskCount = computed(() => {
  const n = tasks.value.length;
  return `${n} ${n === 1 ? "task" : "tasks"}`;
});

const form = reactive({
  title: "",
  description: "",
  priority: "" as TaskPriority | "",
  dueDate: "",
});

// Bridges form.dueDate (YYYY-MM-DD string) with DatePicker's Date object
const dueDatePicker = computed<Date | null>({
  get() {
    if (!form.dueDate) return null;
    const [y, m, d] = form.dueDate.split("-").map(Number);
    return new Date(y, m - 1, d);
  },
  set(date: Date | null) {
    if (!date) { form.dueDate = ""; return; }
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, "0");
    const d = String(date.getDate()).padStart(2, "0");
    form.dueDate = `${y}-${m}-${d}`;
  },
});

onMounted(loadProject);

async function loadProject() {
  loading.value = true;
  error.value = null;
  const id = String(route.params.projectId);
  try {
    [project.value, board.value, tasks.value, projectStats.value] = await Promise.all([
      getProject(id),
      projectBoard(id),
      listTasks(id),
      getProjectSummary(id),
    ]);
  } catch (caught) {
    error.value = caught instanceof ApiClientError
      ? caught.message
      : "Unable to load the project";
  } finally {
    loading.value = false;
  }
}

async function refreshWorkspace() {
  try {
    if (project.value) board.value = await projectBoard(project.value.id);
  } catch (caught) {
    error.value = caught instanceof ApiClientError
      ? caught.message
      : "Unable to refresh the project workspace";
  }
}

async function refreshStats() {
  try {
    if (project.value) projectStats.value = await getProjectSummary(project.value.id);
  } catch {
    // stats are non-critical, fail silently
  }
}

async function openReport() {
  if (!project.value) return;
  reportLoading.value = true;
  reportError.value = null;
  viewingReport.value = null;
  try {
    viewingReport.value = await getProjectReport(project.value.id);
  } catch (caught) {
    reportError.value = caught instanceof ApiClientError
      ? caught.message
      : "Unable to load the report";
  } finally {
    reportLoading.value = false;
  }
}

async function openSuggestion() {
  if (!project.value) return;
  suggestionLoading.value = true;
  suggestionError.value = null;
  suggestedTask.value = null;
  try {
    suggestedTask.value = await getSuggestedTask(project.value.id);
  } catch (caught) {
    suggestionError.value = caught instanceof ApiClientError
      ? caught.message
      : "Unable to load suggestion";
  } finally {
    suggestionLoading.value = false;
  }
}

async function refreshTaskList() {
  try {
    if (project.value) tasks.value = await listTasks(project.value.id);
  } catch (caught) {
    error.value = caught instanceof ApiClientError
      ? caught.message
      : "Unable to refresh the task list";
  }
}

function promptAdd() {
  form.title = "";
  form.description = "";
  form.priority = "";
  form.dueDate = "";
  dialogError.value = null;
  pendingAdd.value = true;
}

async function promptEdit(taskId: string, index: number) {
  dialogError.value = null;
  try {
    const task = await getTask(taskId);
    form.title = task.title;
    form.description = task.description;
    form.priority = task.priority;
    form.dueDate = task.dueDate;
    pendingEdit.value = { id: taskId, index };
  } catch (caught) {
    error.value = caught instanceof ApiClientError
      ? caught.message
      : "Unable to load task";
  }
}

async function submit() {
  if (!project.value) return;
  formLoading.value = true;
  dialogError.value = null;
  try {
    const payload: CreateTaskRequest = {
      title: form.title.trim(),
      description: form.description,
      priority: form.priority as TaskPriority,
      dueDate: form.dueDate,
    };
    const task = await createTask(project.value.id, payload);
    tasks.value.push(task);
    await Promise.all([refreshWorkspace(), refreshStats()]);
    pendingAdd.value = false;
  } catch (caught) {
    dialogError.value = caught instanceof ApiClientError
      ? caught.message
      : "Unable to create task";
  } finally {
    formLoading.value = false;
  }
}

async function submitEdit() {
  if (!pendingEdit.value) return;
  formLoading.value = true;
  dialogError.value = null;
  try {
    const payload: CreateTaskRequest = {
      title: form.title.trim(),
      description: form.description,
      priority: form.priority as TaskPriority,
      dueDate: form.dueDate,
    };
    tasks.value[pendingEdit.value.index] = await updateTask(pendingEdit.value.id, payload);
    await Promise.all([refreshWorkspace(), refreshStats()]);
    pendingEdit.value = null;
  } catch (caught) {
    dialogError.value = caught instanceof ApiClientError
      ? caught.message
      : "Unable to update task";
  } finally {
    formLoading.value = false;
  }
}

function promptDelete(taskId: string, index: number, name: string) {
  pendingDelete.value = { id: taskId, index, name };
}

function promptDeleteFromEdit() {
  if (!pendingEdit.value) return;
  const { id, index } = pendingEdit.value;
  pendingEdit.value = null;
  promptDelete(id, index, form.title);
}

async function removeTask(taskId: string, index: number) {
  loading.value = true;
  dialogError.value = null;
  try {
    await deleteTask(taskId);
    tasks.value.splice(index, 1);
    await Promise.all([refreshWorkspace(), refreshStats()]);
  } catch (caught) {
    dialogError.value = caught instanceof ApiClientError
      ? caught.message
      : "Unable to delete the task";
  } finally {
    loading.value = false;
  }
}

async function confirmDelete() {
  if (!pendingDelete.value) return;
  const { id, index } = pendingDelete.value;
  pendingDelete.value = null;
  await removeTask(id, index);
}

function onDragStart(event: DragEvent, task: TaskResponse, fromStatus: TaskStatus) {
  dragging.value = { task, fromStatus };
  event.dataTransfer!.effectAllowed = "move";
}

function onDragEnd() {
  dragging.value = null;
  dropAt.value = null;
}

function onTaskDragOver(event: DragEvent, status: TaskStatus, index: number) {
  if (!dragging.value) return;
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
  dropAt.value = {
    status,
    index: event.clientY < rect.top + rect.height / 2 ? index : index + 1,
  };
}

function onColumnDragOver(event: DragEvent, status: TaskStatus) {
  if (!dragging.value) return;
  // Fires only when the cursor is in the column body but NOT over a task card
  // (cards call .stop on dragover). Scan rendered cards to find drop position.
  const cards = Array.from(
    (event.currentTarget as HTMLElement).querySelectorAll<HTMLElement>(".task-card")
  );
  const col = board.value?.columns[status] ?? [];
  let index = col.length;
  for (let i = 0; i < cards.length; i++) {
    const rect = cards[i].getBoundingClientRect();
    if (event.clientY < rect.top + rect.height / 2) { index = i; break; }
  }
  dropAt.value = { status, index: index };
}

function onColumnDragLeave(event: DragEvent) {
  if (!(event.currentTarget as HTMLElement).contains(event.relatedTarget as Node)) {
    dropAt.value = null;
  }
}

function showDropLine(status: TaskStatus, index: number): boolean {
  if (!dropAt.value || !dragging.value) return false;
  if (dropAt.value.status !== status || dropAt.value.index !== index) return false;
  if (dragging.value.fromStatus === status) {
    const col = board.value?.columns[status] ?? [];
    const cur = col.findIndex(t => t.id === dragging.value!.task.id);
    if (cur === index || cur + 1 === index) return false;
  }
  return true;
}

async function onDrop(status: TaskStatus) {
  if (!dragging.value || !dropAt.value || dropAt.value.status !== status) return;

  const { task, fromStatus } = dragging.value;
  let position = dropAt.value.index;

  dragging.value = null;
  dropAt.value = null;

  if (fromStatus === status) {
    const col = board.value?.columns[status] ?? [];
    const cur = col.findIndex(t => t.id === task.id);
    if (cur === position || cur + 1 === position) return;
    if (cur < position) position--;
  }

  try {
    await moveTask(task.id, { targetStatus: status, position });
    await Promise.all([refreshWorkspace(), refreshTaskList(), refreshStats()]);
  } catch (caught) {
    error.value = caught instanceof ApiClientError
      ? caught.message
      : "Unable to move task";
  }
}

async function promptView(taskId: string) {
  newNoteContent.value = "";
  notesError.value = null;
  viewingTaskNotes.value = [];
  suggestedTask.value = null;
  suggestionError.value = null;
  try {
    viewingTask.value = await getTask(taskId);
  } catch (caught) {
    error.value = caught instanceof ApiClientError
      ? caught.message
      : "Unable to load task";
    return;
  }
  notesLoading.value = true;
  try {
    viewingTaskNotes.value = await getNotes(taskId);
  } catch (caught) {
    notesError.value = caught instanceof ApiClientError
      ? caught.message
      : "Unable to load notes";
  } finally {
    notesLoading.value = false;
  }
}

async function addNote() {
  if (!viewingTask.value || !newNoteContent.value.trim()) return;
  notesError.value = null;
  try {
    const note = await createNote(viewingTask.value.id, newNoteContent.value.trim());
    viewingTaskNotes.value.push(note);
    newNoteContent.value = "";
  } catch (caught) {
    notesError.value = caught instanceof ApiClientError
      ? caught.message
      : "Unable to add note";
  }
}

async function removeNote(noteId: string) {
  notesError.value = null;
  try {
    await deleteNote(noteId);
    viewingTaskNotes.value = viewingTaskNotes.value.filter(n => n.id !== noteId);
  } catch (caught) {
    notesError.value = caught instanceof ApiClientError
      ? caught.message
      : "Unable to delete note";
  }
}

function openEditFromView() {
  if (!viewingTask.value) return;
  const task = viewingTask.value;
  viewingTask.value = null;
  form.title = task.title;
  form.description = task.description;
  form.priority = task.priority;
  form.dueDate = task.dueDate;
  dialogError.value = null;
  pendingEdit.value = {
    id: task.id,
    index: tasks.value.findIndex(t => t.id === task.id),
  };
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("en", { dateStyle: "medium" }).format(new Date(value));
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

      <p class="project-updated">Updated {{ formatDate(project.updatedAt) }}</p>

      <section v-if="projectStats" class="stats-section">
        <div class="stats-section-header">
          <h2 class="stats-heading">Statistics</h2>
          <BaseButton @click="openReport" :disabled="reportLoading">
            {{ reportLoading ? "Loading…" : "See Detailed Report" }}
          </BaseButton>
        </div>
        <div class="project-stats-layout">
          <div class="project-stats-cards">
            <div class="stat-card">
              <span class="stat-value">{{ projectStats.totalTasks }}</span>
              <span class="stat-label">Total tasks</span>
            </div>
            <div class="stat-card">
              <span class="stat-value">{{ projectStats.todo }}</span>
              <span class="stat-label">To Do</span>
            </div>
            <div class="stat-card">
              <span class="stat-value">{{ projectStats.inProgress }}</span>
              <span class="stat-label">In Progress</span>
            </div>
            <div class="stat-card" :class="{ 'stat-card--completed': projectStats.done > 0 }">
              <span class="stat-value">{{ projectStats.done }}</span>
              <span class="stat-label">Done</span>
            </div>
          </div>
          <div class="project-stats-knob">
            <Knob
              :model-value="projectStats.completionPercentage"
              :min="0"
              :max="100"
              :size="150"
              :stroke-width="10"
              value-template="{value}%"
              readonly
            />
            <span class="stat-label">Completed</span>
          </div>
        </div>
      </section>

      <section class="project-workspace">
        <header>
          <div class="project-workspace-header-left">
            <span class="project-workspace-icon">
              <FolderKanban :size="22" aria-hidden="true" />
            </span>
            <div>
              <h2>Project workspace</h2>
              <p class="project-updated">Drag tasks between categories to change their status</p>
            </div>
          </div>
          <BaseButton :disabled="suggestionLoading" @click="openSuggestion">
            {{ suggestionLoading ? "Loading…" : "Suggest Next Task" }}
          </BaseButton>
        </header>

        <div v-if="board?.columns" class="board">
          <div v-for="status in STATUSES" :key="status" class="board-column">
            <div class="board-column-header">
              <span>{{ STATUS_LABELS[status] }}</span>
              <span class="board-column-count">{{ (board.columns[status] ?? []).length }}</span>
            </div>
            <div
              class="board-column-body"
              @dragover.prevent="onColumnDragOver($event, status)"
              @dragleave="onColumnDragLeave"
              @drop.prevent="onDrop(status)"
            >
              <template v-for="(task, index) in (board.columns[status] ?? [])" :key="task.id">
                <div v-if="showDropLine(status, index)" class="drop-line">
                  <Plus :size="12" aria-hidden="true" />
                </div>
                <div
                  class="task-card"
                  :class="[
                    `priority-${task.priority.toLowerCase()}`,
                    { 'task-card--dragging': dragging?.task.id === task.id }
                  ]"
                  draggable="true"
                  @click="promptView(task.id)"
                  @dragstart="onDragStart($event, task, status)"
                  @dragend="onDragEnd"
                  @dragover.prevent.stop="onTaskDragOver($event, status, index)"
                >
                  <span class="task-priority">{{ task.priority }}</span>
                  <p class="task-title">{{ task.title }}</p>
                  <span class="task-due">Due {{ formatDate(task.dueDate) }}</span>
                </div>
              </template>
              <div v-if="showDropLine(status, (board.columns[status] ?? []).length)" class="drop-line">
                <Plus :size="12" aria-hidden="true" />
              </div>
              <p v-if="!(board.columns[status] ?? []).length" class="board-column-empty">
                No tasks
              </p>
            </div>
          </div>
        </div>
      </section>

      <div class="task-section">
        <div class="task-list-toolbar">
          <strong>All tasks</strong>
          <span>{{ taskCount }}</span>
          <button
            class="icon-button"
            title="Create a new task"
            aria-label="Create a new task"
            @click="promptAdd()"
          >
            <Plus :size="18" aria-hidden="true" />
          </button>
        </div>

        <StatePanel
          v-if="tasks.length === 0"
          title="No tasks yet"
          message="Tasks created for this project will appear here."
        />

        <div v-else class="task-list">
          <article
            v-for="(task, index) in tasks"
            :key="task.id"
            class="task-row"
            :class="`priority-${task.priority.toLowerCase()}`"
            @click="promptView(task.id)"
          >
            <div class="task-row-main">
              <span class="task-row-title">{{ task.title }}</span>
              <p class="task-row-description">{{ task.description || "No description" }}</p>
            </div>

            <div class="task-tags">
              <span class="task-tag" :class="`status-${task.status.toLowerCase()}`">
                {{ STATUS_LABELS[task.status] }}
              </span>
              <span class="task-tag" :class="`priority-${task.priority.toLowerCase()}`">
                {{ PRIORITY_LABELS[task.priority] }}
              </span>
            </div>

            <div class="task-dates">
              <span><b>Due</b> {{ formatDate(task.dueDate) }}</span>
              <span><b>Created</b> {{ formatDate(task.createdAt) }}</span>
              <span><b>Updated</b> {{ formatDate(task.updatedAt) }}</span>
              <span v-if="task.completedAt"><b>Completed</b> {{ formatDate(task.completedAt) }}</span>
            </div>

            <button
              class="icon-button"
              title="Edit task"
              :aria-label="`Edit ${task.title}`"
              @click.stop="promptEdit(task.id, index)"
            >
              <SquarePen :size="18" aria-hidden="true" />
            </button>

            <button
              class="icon-button"
              title="Delete task"
              :aria-label="`Delete ${task.title}`"
              @click.stop="promptDelete(task.id, index, task.title)"
            >
              <Trash2 :size="18" aria-hidden="true" />
            </button>
          </article>
        </div>
      </div>
    </template>

    <BaseDialog
      v-if="viewingTask"
      :title="viewingTask.title"
      :description="viewingTask.description"
      @close="viewingTask = null"
    >
      <div class="dialog-body">
        <div class="task-view-data">
          <dl class="task-view-dates-col">
            <div><dt>Due</dt><dd>{{ formatDate(viewingTask.dueDate) }}</dd></div>
            <div><dt>Created</dt><dd>{{ formatDate(viewingTask.createdAt) }}</dd></div>
            <div><dt>Updated</dt><dd>{{ formatDate(viewingTask.updatedAt) }}</dd></div>
            <div v-if="viewingTask.completedAt">
              <dt>Completed</dt><dd>{{ formatDate(viewingTask.completedAt) }}</dd>
            </div>
          </dl>
          <dl class="task-view-badges-col">
            <div>
              <dt>Priority</dt>
              <dd>
                <span class="task-tag" :class="`priority-${viewingTask.priority.toLowerCase()}`">
                  {{ PRIORITY_LABELS[viewingTask.priority] }}
                </span>
              </dd>
            </div>
            <div>
              <dt>Status</dt>
              <dd>
                <span class="task-tag" :class="`status-${viewingTask.status.toLowerCase()}`">
                  {{ STATUS_LABELS[viewingTask.status] }}
                </span>
              </dd>
            </div>
          </dl>
        </div>

        <section class="notes-section">
          <h3 class="notes-heading">Notes</h3>

          <div v-if="notesLoading" class="notes-loading">
            <div class="spinner" />
          </div>
          <template v-else>
            <ul v-if="viewingTaskNotes.length" class="note-list">
              <li v-for="note in viewingTaskNotes" :key="note.id" class="note-item">
                <div class="note-body">
                  <span class="note-meta">
                    <span class="note-author">{{ note.authorDisplayName }}</span>
                    · {{ formatDate(note.createdAt) }}
                  </span>
                  <span class="note-content">{{ note.content }}</span>
                </div>
                <button
                  class="icon-button"
                  title="Delete note"
                  aria-label="Delete note"
                  @click="removeNote(note.id)"
                >
                  <Trash2 :size="14" aria-hidden="true" />
                </button>
              </li>
            </ul>
            <p v-else class="notes-empty">No notes yet.</p>

            <p v-if="notesError" class="form-error" role="alert">{{ notesError }}</p>

            <form class="note-form" @submit.prevent="addNote">
              <FormField label="New note" :hint="`Up to ${TASK_NOTE_MAX_LENGTH} characters`">
                <textarea
                  v-model="newNoteContent"
                  placeholder="Write a note…"
                  :maxlength="TASK_NAME_MAX_LENGTH"
                  rows="2"
                />
              </FormField>
              <BaseButton type="submit" variant="primary" :disabled="!newNoteContent.trim()">
                Add note
              </BaseButton>
            </form>
          </template>
        </section>
      </div>
      <div class="dialog-footer">
        <BaseButton @click="viewingTask = null">Cancel</BaseButton>
        <BaseButton variant="primary" @click="openEditFromView">Edit</BaseButton>
      </div>
    </BaseDialog>

    <BaseDialog
      v-if="pendingAdd || pendingEdit"
      :title="pendingEdit ? 'Edit task' : 'Add a new task'"
      @close="pendingAdd = false; pendingEdit = null"
    >
      <form @submit.prevent="pendingEdit ? submitEdit() : submit()">
        <div class="dialog-body">
          <FormField label="Title" :hint="`Up to ${TASK_NAME_MAX_LENGTH} characters`">
            <input
              v-model="form.title"
              name="title"
              type="text"
              :maxlength="TASK_NAME_MAX_LENGTH"
              required
              autofocus
            />
          </FormField>
          <FormField label="Description" :hint="`Up to ${TASK_DESCR_MAX_LENGTH} characters`">
            <input
              v-model="form.description"
              name="description"
              type="text"
              :maxlength="TASK_DESCR_MAX_LENGTH"
            />
          </FormField>
          <FormField label="Priority">
            <select v-model="form.priority" name="priority" required>
              <option value="" disabled>Select priority</option>
              <option v-for="p in PRIORITIES" :key="p" :value="p">
                {{ PRIORITY_LABELS[p] }}
              </option>
            </select>
          </FormField>
          <FormField label="Due date">
            <DatePicker
              v-model="dueDatePicker"
              date-format="dd/mm/yy"
              :manual-input="false"
              show-button-bar
              fluid
            />
          </FormField>
          <p v-if="dialogError" class="form-error" role="alert">{{ dialogError }}</p>
        </div>
        <div class="dialog-footer">
          <BaseButton @click="pendingAdd = false; pendingEdit = null">Cancel</BaseButton>
          <BaseButton
            v-if="pendingEdit"
            variant="danger"
            :disabled="formLoading"
            @click="promptDeleteFromEdit"
          >
            Delete
          </BaseButton>
          <BaseButton type="submit" variant="primary" :disabled="formLoading || !form.dueDate">
            {{ pendingEdit ? (formLoading ? "Saving..." : "Save changes") : (formLoading ? "Adding..." : "Add") }}
          </BaseButton>
        </div>
      </form>
    </BaseDialog>

    <BaseDialog
      v-if="pendingDelete"
      title="Delete task"
      :description="`Are you sure you want to delete &quot;${pendingDelete.name}&quot;? This action cannot be undone.`"
      @close="pendingDelete = null"
    >
      <div class="dialog-footer">
        <BaseButton @click="pendingDelete = null">Cancel</BaseButton>
        <BaseButton variant="danger" @click="confirmDelete">Delete</BaseButton>
      </div>
    </BaseDialog>

    <BaseDialog
      v-if="suggestedTask || suggestionLoading || suggestionError"
      title="Suggested Next Task"
      @close="suggestedTask = null; suggestionError = null"
    >
      <div class="dialog-body">
        <div v-if="suggestionLoading" class="report-loading">
          <div class="spinner" />
        </div>
        <p v-else-if="suggestionError" class="form-error" role="alert">{{ suggestionError }}</p>
        <div v-else-if="suggestedTask">
          <dl class="suggestion-reason">
            <div>
              <dt>Reason</dt>
              <dd>{{ REASON_LABELS[suggestedTask.reason] }}</dd>
            </div>
          </dl>
          <div
            class="task-card"
            :class="`priority-${suggestedTask.task.priority.toLowerCase()}`"
            @click="promptView(suggestedTask.task.id)"
          >
            <span class="task-priority">{{ PRIORITY_LABELS[suggestedTask.task.priority] }}</span>
            <p class="task-title">{{ suggestedTask.task.title }}</p>
            <div class="task-card-meta">
              <span class="task-tag" :class="`status-${suggestedTask.task.status.toLowerCase()}`">
                {{ STATUS_LABELS[suggestedTask.task.status] }}
              </span>
              <span class="task-due">Due {{ formatDate(suggestedTask.task.dueDate) }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="dialog-footer">
        <BaseButton @click="suggestedTask = null; suggestionError = null">Close</BaseButton>
      </div>
    </BaseDialog>

    <BaseDialog
      v-if="viewingReport || reportLoading || reportError"
      title="Project Report"
      @close="viewingReport = null; reportError = null"
    >
      <div class="dialog-body">
        <div v-if="reportLoading" class="report-loading">
          <div class="spinner" />
        </div>
        <p v-else-if="reportError" class="form-error" role="alert">{{ reportError }}</p>
        <template v-else-if="viewingReport">
          <p class="report-meta">
            Generated {{ formatDate(viewingReport.generatedAt) }}
          </p>
          <div class="stats-grid report-stats-grid">
            <div class="stat-card">
              <span class="stat-value">{{ viewingReport.totalTasks }}</span>
              <span class="stat-label">Total tasks</span>
            </div>
            <div class="stat-card">
              <span class="stat-value">{{ viewingReport.todoTasks }}</span>
              <span class="stat-label">To Do</span>
            </div>
            <div class="stat-card">
              <span class="stat-value">{{ viewingReport.inProgressTasks }}</span>
              <span class="stat-label">In Progress</span>
            </div>
            <div class="stat-card" :class="{ 'stat-card--completed': viewingReport.doneTasks > 0 }">
              <span class="stat-value">{{ viewingReport.doneTasks }}</span>
              <span class="stat-label">Done</span>
            </div>
            <div class="stat-card" :class="{ 'stat-card--completed': viewingReport.completionPercentage > 0 }">
              <span class="stat-value">{{ viewingReport.completionPercentage }}%</span>
              <span class="stat-label">Completed</span>
            </div>
            <div class="stat-card" :class="{ 'stat-card--danger': viewingReport.overdueTasks > 0 }">
              <span class="stat-value">{{ viewingReport.overdueTasks }}</span>
              <span class="stat-label">Overdue</span>
            </div>
            <div class="stat-card" :class="{ 'stat-card--danger': viewingReport.highPriorityOpenTasks > 0 }">
              <span class="stat-value">{{ viewingReport.highPriorityOpenTasks }}</span>
              <span class="stat-label">High priority</span>
            </div>
          </div>
        </template>
      </div>
      <div class="dialog-footer">
        <BaseButton @click="viewingReport = null; reportError = null">Close</BaseButton>
      </div>
    </BaseDialog>
  </section>
</template>
