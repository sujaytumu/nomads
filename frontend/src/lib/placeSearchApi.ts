import { api } from "./api";

export type SearchItem = {
  id: number;
  name: string;
  city: string;
  latitude: number;
  longitude: number;
  category: string;
  rating: number;
  imageUrl?: string | null;
  distanceKm?: number | null;
};

export type SearchResult = {
  items: SearchItem[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  facets: { categories: Record<string, number>; cities: Record<string, number> };
};

export type SearchParams = {
  q?: string;
  city?: string;
  category?: string;
  minRating?: number;
  sort?: "relevance" | "rating" | "name" | "distance";
  lat?: number;
  lon?: number;
  page?: number;
  size?: number;
};

export async function searchPlaces(p: SearchParams): Promise<SearchResult> {
  const params: Record<string, string | number> = {};
  (Object.keys(p) as (keyof SearchParams)[]).forEach((k) => {
    const v = p[k];
    if (v !== undefined && v !== "" && v !== null) params[k] = v as string | number;
  });
  const { data } = await api.get("/api/places/search", { params });
  return data as SearchResult;
}
