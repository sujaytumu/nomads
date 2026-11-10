import { api } from "./api";

export type SharedPosition = { userId: number; name: string; me: boolean; lat: number; lng: number; accuracy: number | null; secondsAgo: number };

export async function fetchLive(groupId: number): Promise<SharedPosition[]> {
  const { data } = await api.get(`/api/groups/${groupId}/live`);
  return data;
}

export async function shareLive(groupId: number, lat: number, lng: number, accuracy?: number) {
  await api.put(`/api/groups/${groupId}/live`, { lat, lng, accuracy: accuracy ?? null });
}

export async function stopLive(groupId: number) {
  await api.delete(`/api/groups/${groupId}/live`);
}
