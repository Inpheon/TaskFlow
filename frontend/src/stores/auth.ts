import { defineStore } from "pinia";
import { fetchCurrentUser, login, register } from "@/api/auth";
import { ApiClientError } from "@/api/http";
import type {
  AuthSession,
  CurrentUser,
  LoginRequest,
  RegisterRequest
} from "@/types/api";

const STORAGE_KEY = "taskflow.session";

export type AuthStatus = "initializing" | "authenticated" | "anonymous";

interface AuthState {
  accessToken: string | null;
  user: CurrentUser | null;
  status: AuthStatus;
}

export const useAuthStore = defineStore("auth", {
  state: (): AuthState => ({
    accessToken: null,
    user: null,
    status: "initializing"
  }),
  getters: {
    isAuthenticated: state => state.status === "authenticated"
  },
  actions: {
    async initialize() {
      const session = readStoredSession();
      if (!session) {
        this.status = "anonymous";
        return;
      }

      this.accessToken = session.accessToken;
      this.user = session.user;

      try {
        this.user = await fetchCurrentUser();
        this.status = "authenticated";
        this.persist();
      } catch (error) {
        if (error instanceof ApiClientError && error.status === 401) {
          this.clearSession();
          return;
        }
        this.status = "authenticated";
      }
    },
    async login(payload: LoginRequest) {
      this.setSession(await login(payload));
    },
    async register(payload: RegisterRequest) {
      this.setSession(await register(payload));
    },
    setSession(session: AuthSession) {
      this.accessToken = session.accessToken;
      this.user = session.user;
      this.status = "authenticated";
      this.persist();
    },
    clearSession() {
      this.accessToken = null;
      this.user = null;
      this.status = "anonymous";
      localStorage.removeItem(STORAGE_KEY);
    },
    persist() {
      if (!this.accessToken || !this.user) {
        return;
      }
      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        accessToken: this.accessToken,
        tokenType: "Bearer",
        user: this.user
      } satisfies AuthSession));
    }
  }
});

function readStoredSession(): AuthSession | null {
  const value = localStorage.getItem(STORAGE_KEY);
  if (!value) {
    return null;
  }

  try {
    const session = JSON.parse(value) as Partial<AuthSession>;
    if (
      typeof session.accessToken !== "string"
      || session.tokenType !== "Bearer"
      || !session.user
      || typeof session.user.id !== "string"
      || typeof session.user.email !== "string"
      || typeof session.user.displayName !== "string"
    ) {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
    return session as AuthSession;
  } catch {
    localStorage.removeItem(STORAGE_KEY);
    return null;
  }
}
