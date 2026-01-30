import { api } from "./api";

export async function assignTravel(tripRequestId: number) {
  const { data } = await api.post("/api/travel/assign", { tripRequestId });
  return data;
}

export async function fetchTravel(tripRequestId: number) {
  const { data } = await api.get(`/api/travel/${tripRequestId}`);
  return data;
}
