import { api } from "./api";

export type Stay = { area: string; tier: "BUDGET" | "COMFORT" | "PREMIUM"; pricePerNight: number; avgKmToStops: number; why: string };
export type Scenic = { name: string; bestTime: string; kmFromTrip: number; why: string };
export type Medical = { name: string; type: string; kmFromTrip: number; note: string };
export type TripEssentials = { city: string; stay: Stay[]; scenic: Scenic[]; medical: Medical[] };

export async function fetchEssentials(tripId: number | string): Promise<TripEssentials> {
  const { data } = await api.get(`/api/trips/${tripId}/essentials`);
  return data;
}

const q = (s: string) => encodeURIComponent(s);
export const mapsSearch = (query: string) => `https://www.google.com/maps/search/?api=1&query=${q(query)}`;
export const hotelsIn = (area: string, city: string) => mapsSearch(`hotels in ${area}, ${city}`);
