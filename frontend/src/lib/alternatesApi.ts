import { api } from "./api";

export type AltOption = { placeId: number; name: string; category: string; rating: number; distanceKm: number; why: string };
export type StopAlternates = { day: number; placeId: number; name: string; category: string; rainChance: number | null; needsBackup: boolean; headline: string; options: AltOption[] };
export type Alternates = { rainyDays: number[]; forecastKnown?: boolean; stops: StopAlternates[] };

export async function fetchAlternates(tripId: number | string): Promise<Alternates> {
  const { data } = await api.get(`/api/trips/${tripId}/alternates`);
  return data as Alternates;
}
