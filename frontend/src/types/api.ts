export interface ApiError {
  status: number;
  error: string;
  message: string;
  path: string;
  timestamp: string;
}

export interface CurrentUser {
  id: string;
  email: string;
  displayName: string;
}

export interface AuthSession {
  accessToken: string;
  tokenType: "Bearer";
  user: CurrentUser;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
}

export interface Project {
  id: string;
  name: string;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectRequest {
  name: string;
  description: string | null;
}

export interface HealthResponse {
  status: string;
  service: string;
  timestamp: string;
}

export type TaskStatus = "TODO" | "IN_PROGRESS" | "DONE";
export type TaskPriority = "LOW" | "MEDIUM" | "HIGH";

export interface TaskResponse {
  id: string;
  projectId: string;
  title: string;
  description: string;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate: string; // date
  position: number;
  createdAt: string; // datetime
  updatedAt: string; // datetime
  completedAt: string | null; // datetime
}

export interface BoardResponse {
  project: {
    id: string;
    name: string;
  }
  columns : Record<TaskStatus, TaskResponse[]>
}

export interface CreateTaskRequest {
  title: string;
  description: string;
  priority: TaskPriority;
  dueDate: string;
}

export interface MoveTaskRequest {
  targetStatus: TaskStatus;
  position: number;
}

export interface TaskNoteResponse {
  id: string;
  taskId: string;
  authorId: string;
  authorDisplayName: string;
  content: string;
  createdAt: string; // datetime
}

export interface DashboardSummaryResponse {
  projectsCount: number;
  openTasksCount: number;
  doneTasksCount: number;
  overdueTasksCount: number;
  highPriorityOpenTasksCount: number;
}
