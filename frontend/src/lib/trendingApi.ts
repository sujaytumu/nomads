import { getCached } from "./api";
import type { SearchItem } from "./placeSearchApi";

export type TrendingPlace = { place: SearchItem; trips: number };

export const TRENDING_URL = "/api/places/trending?limit=8";

/** Shared and kept for a minute; the last copy is also saved on the device so the next visit paints at once. */
export async function fetchTrending(_limit = 8): Promise<TrendingPlace[]> {
  return getCached<TrendingPlace[]>(TRENDING_URL, { ttl: 60000, persist: true });
}
