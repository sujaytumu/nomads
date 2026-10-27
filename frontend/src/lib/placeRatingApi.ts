import { api } from "./api";

export type PlaceReview = { stars: number; comment: string; reviewer: string; when: string };
export type PlaceRatings = { placeId: number; count: number; average: number; blended: number; distribution: number[]; reviews: PlaceReview[] };
export type MyRating = { canRate: boolean; stars: number | null; comment: string | null };

export async function fetchPlaceRatings(placeId: number | string): Promise<PlaceRatings> {
  const { data } = await api.get(`/api/places/${placeId}/ratings`);
  return data as PlaceRatings;
}

export async function fetchMyRating(placeId: number | string): Promise<MyRating> {
  const { data } = await api.get(`/api/places/${placeId}/ratings/me`);
  return data as MyRating;
}

export async function savePlaceRating(placeId: number | string, stars: number, comment: string): Promise<MyRating> {
  const { data } = await api.put(`/api/places/${placeId}/ratings`, { stars, comment });
  return data as MyRating;
}

export async function deletePlaceRating(placeId: number | string) {
  await api.delete(`/api/places/${placeId}/ratings`);
}
