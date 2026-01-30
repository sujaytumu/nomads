import { api } from "./api";

export async function fetchSharedTrip(token: string) {
  const { data } = await api.get(`/api/share/${token}`);
  return data;
}
