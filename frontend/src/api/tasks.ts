import { request } from "./http";
import {BoardResponse, CreateTaskRequest, MoveTaskRequest, TaskResponse} from "@/types/api";

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

export function deleteTask(taskId: string): Promise<void> {
  return request<void>(`/api/tasks/${taskId}`, {
    method: "DELETE"
  });
}

export function moveTask(taskId: string, payload: MoveTaskRequest): Promise<TaskResponse> {
  return request<TaskResponse>(`/api/tasks/${taskId}/move`, {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}
