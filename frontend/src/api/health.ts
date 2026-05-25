import { getJson } from "./http";
import type { HealthResponse } from "@/types/api";

export function fetchHealth(): Promise<HealthResponse> {
  return getJson<HealthResponse>("/api/health");
}

