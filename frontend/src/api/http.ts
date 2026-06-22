import type { ApiError } from "@/types/api";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

let accessTokenProvider: () => string | null = () => null;
let unauthorizedHandler: () => void = () => undefined;

export class ApiClientError extends Error {
  readonly status: number;
  readonly apiError: ApiError | null;

  constructor(status: number, message: string, apiError: ApiError | null = null) {
    super(message);
    this.name = "ApiClientError";
    this.status = status;
    this.apiError = apiError;
  }
}

export function configureHttpAuth(
  tokenProvider: () => string | null,
  onUnauthorized: () => void
) {
  accessTokenProvider = tokenProvider;
  unauthorizedHandler = onUnauthorized;
}

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set("Accept", "application/json");

  if (options.body !== undefined && options.body !== null) {
    headers.set("Content-Type", "application/json");
  }

  const accessToken = accessTokenProvider();
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers
    });
  } catch {
    throw new ApiClientError(0, "Unable to reach the server");
  }

  if (response.status === 401) {
    unauthorizedHandler();
  }

  if (!response.ok) {
    const apiError = await readApiError(response);
    throw new ApiClientError(
      response.status,
      apiError?.message ?? `Request failed with status ${response.status}`,
      apiError
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export function getJson<T>(path: string): Promise<T> {
  return request<T>(path);
}

async function readApiError(response: Response): Promise<ApiError | null> {
  const contentType = response.headers.get("Content-Type");
  if (!contentType?.includes("application/json")) {
    return null;
  }

  try {
    return await response.json() as ApiError;
  } catch {
    return null;
  }
}
