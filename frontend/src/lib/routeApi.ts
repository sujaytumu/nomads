import { api } from "./api";

export async function fetchRoute(
  tripRequestId: number,
  dayNumber: number,
  mode: "driving" | "walking" = "driving"
) {
  const { data } = await api.get(`/api/routes/${tripRequestId}`, {
    params: { mode, dayNumber },
  });
  return data as { tripRequestId: number; mode: string; geoJson: string };
}
