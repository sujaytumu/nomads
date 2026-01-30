import { api } from "./api";

export async function updatePickup(tripRequestId: number, payload: {
  pickupTime?: string;
  status?: string;
  routeMapUrl?: string;
  vehicleId?: number;
}) {
  const { data } = await api.put(`/api/travel/${tripRequestId}`, payload);
  return data;
}

export async function confirmPickup(tripRequestId: number, payload: {
  pickupTime: string;
}) {
  const { data } = await api.put(`/api/travel/${tripRequestId}/confirm`, payload);
  return data;
}
