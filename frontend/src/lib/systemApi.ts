import { api } from "./api";

export type SystemHealth = {
  checkedAt: string;
  uptimeSeconds: number;
  memoryUsedMb: number;
  memoryMaxMb: number;
  requests: { total: number; clientErrors: number; serverErrors: number; p50Ms: number; p95Ms: number; windowSize: number; slowest: { route: string; count: number; errors: number; p95Ms: number }[] };
  cache: { hits: number; misses: number; hitRatePercent: number; entries: number };
  push: { enabled: boolean; pending: number; failed: number };
  paymentEvents: number;
  database: { ok: boolean; pingMs: number };
};

export async function fetchSystemHealth(): Promise<SystemHealth> {
  const { data } = await api.get("/api/admin/system");
  return data;
}
