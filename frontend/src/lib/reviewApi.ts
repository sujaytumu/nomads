import { api } from "./api";

export async function createReview(payload: { tripRequestId: number; rating: number; comment?: string }) {
  const { data } = await api.post("/api/reviews", payload);
  return data;
}

export async function fetchReviews(tripRequestId: number) {
  const { data } = await api.get(`/api/reviews/${tripRequestId}`);
  return data;
}
