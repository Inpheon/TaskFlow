import { request } from "./http";
import {
  BoardResponse,
  CreateTaskRequest,
  MoveTaskRequest, ProjectReportResponse, ProjectStatsResponse,
  TaskNoteResponse,
  TaskResponse
} from "@/types/api";

export function projectBoard(projectId: string): Promise<BoardResponse> {
  return request<BoardResponse>(`/api/projects/${projectId}/board`);
}

export function listTasks(projectId: string): Promise<TaskResponse[]> {
  return request<TaskResponse[]>(`/api/projects/${projectId}/tasks`);
}

export function createTask(projectId: string, payload: CreateTaskRequest): Promise<TaskResponse> {
  return request<TaskResponse>(`/api/projects/${projectId}/tasks`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function getTask(taskId: string): Promise<TaskResponse> {
  return request<TaskResponse>(`/api/tasks/${taskId}`);
}

export function updateTask(taskId: string, payload: CreateTaskRequest): Promise<TaskResponse> {
  return request<TaskResponse>(`/api/tasks/${taskId}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export function moveTask(taskId: string, payload: MoveTaskRequest): Promise<TaskResponse> {
  return request<TaskResponse>(`/api/tasks/${taskId}/move`, {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}

export function deleteTask(taskId: string): Promise<void> {
  return request<void>(`/api/tasks/${taskId}`, {
    method: "DELETE"
  });
}

export function createNote(taskId: string, payload: string): Promise<TaskNoteResponse> {
  return request<TaskNoteResponse>(`/api/tasks/${taskId}/notes`, {
    method: "POST",
    body: JSON.stringify({ content: payload }),
  });
}

export function getNotes(taskId: string): Promise<TaskNoteResponse[]> {
  return request<TaskNoteResponse[]>(`/api/tasks/${taskId}/notes`);
}

export function deleteNote(noteId: string): Promise<void> {
  return request<void>(`/api/notes/${noteId}`, {
    method: "DELETE",
  });
}

export function getProjectSummary(projectId: string): Promise<ProjectStatsResponse> {
  return request<ProjectStatsResponse>(`/api/projects/${projectId}/stats`);
}

export function getProjectReport(projectId: string): Promise<ProjectReportResponse> {
  return request<ProjectReportResponse>(`/api/projects/${projectId}/report`);
}
