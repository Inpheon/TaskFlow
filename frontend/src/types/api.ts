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
