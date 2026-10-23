// A small copy of your booked trips, kept on this device so the itinerary still opens with no signal.
// It is removed when you log out.

const KEY = "nomad_offline_trips";

type Plan = { dayNumber?: number | null; placeName?: string | null; startTime?: string | null; endTime?: string | null };

export function saveOfflineTrips(trips: any[]) {
  try {
    const keep = (trips || [])
      .filter((t) => t && t.status === "CONFIRMED")
      .map((t) => ({
        id: t.tripRequestId,
        city: t.city || "",
        date: t.tripDate || "",
        cost: t.estimatedCost ?? null,
        plans: ((t.plans || []) as Plan[]).map((p) => ({
          d: p.dayNumber || 1,
          n: p.placeName || "",
          s: (p.startTime || "").slice(0, 5),
          e: (p.endTime || "").slice(0, 5),
        })),
      }));
    localStorage.setItem(KEY, JSON.stringify({ savedAt: Date.now(), trips: keep }));
  } catch {
    /* storage full or blocked: offline copy is a bonus, not required */
  }
}

export function clearOfflineTrips() {
  try {
    localStorage.removeItem(KEY);
  } catch {
    /* ignore */
  }
}
