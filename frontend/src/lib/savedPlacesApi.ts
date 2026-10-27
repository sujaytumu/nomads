import { api } from "./api";
import type { SearchItem } from "./placeSearchApi";

export async function fetchSavedPlaceIds(): Promise<number[]> {
  const { data } = await api.get("/api/me/saved-places/ids");
  return data as number[];
}

export async function fetchSavedPlaces(): Promise<SearchItem[]> {
  const { data } = await api.get("/api/me/saved-places");
  return data as SearchItem[];
}

export async function savePlace(id: number) {
  await api.post(`/api/me/saved-places/${id}`);
}

export async function unsavePlace(id: number) {
  await api.delete(`/api/me/saved-places/${id}`);
}
