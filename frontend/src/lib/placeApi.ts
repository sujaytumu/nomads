import { api } from "./api";

export type PlaceNearby = {
  id: number;
  name: string;
  city: string;
  latitude: number;
  longitude: number;
  category: string;
  rating: number;
  distanceKm: number;
};

export async function fetchNearbyPlaces(params: {
  city: string;
  latitude: number;
  longitude: number;
  interest?: string;
  radiusKm?: number;
  limit?: number;
}) {
  const { data } = await api.get<PlaceNearby[]>("/api/places/nearby", { params });
  return data;
}
