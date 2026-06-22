import { request } from "./http";
import type {
  AuthSession,
  CurrentUser,
  LoginRequest,
  RegisterRequest
} from "@/types/api";

export function login(payload: LoginRequest): Promise<AuthSession> {
  return request<AuthSession>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function register(payload: RegisterRequest): Promise<AuthSession> {
  return request<AuthSession>("/api/auth/register", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function fetchCurrentUser(): Promise<CurrentUser> {
  return request<CurrentUser>("/api/auth/me");
}
