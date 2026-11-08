import { api } from "./api";

export type HealthFactor = { key: string; label: string; score: number; weightPercent: number; detail: string };
export type TripHealth = { score: number; label: string; factors: HealthFactor[]; tips: string[] };

export async function fetchTripHealth(tripId: number | string): Promise<TripHealth> {
  const { data } = await api.get(`/api/trips/${tripId}/health`);
  return data;
}
