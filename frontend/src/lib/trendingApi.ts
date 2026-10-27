import { api } from "./api";
import type { SearchItem } from "./placeSearchApi";

export type TrendingPlace = { place: SearchItem; trips: number };

export async function fetchTrending(limit = 8): Promise<TrendingPlace[]> {
  const { data } = await api.get("/api/places/trending", { params: { limit } });
  return data as TrendingPlace[];
}
