export type RecapPlan = { dayNumber: number; placeName?: string; distanceFromPrevious?: number | null };

export type Recap = { stops: number; days: number; roadKm: number; places: string[]; text: string };

const ROAD_FACTOR = 1.3;

/** A trip is over once its last day is before today. Needs a date: an undated trip is never "over". */
export function isTripOver(tripDate: string | null | undefined, days: number, today: Date): boolean {
  const m = tripDate ? /^(\d{4})-(\d{2})-(\d{2})/.exec(tripDate) : null;
  if (!m) return false;
  const last = Date.UTC(+m[1], +m[2] - 1, +m[3] + Math.max(1, days) - 1);
  const now = Date.UTC(today.getFullYear(), today.getMonth(), today.getDate());
  return last < now;
}

/** Straight-line legs scaled to road distance, plus the drive back from the last stop of each day. */
export function buildRecap(city: string | undefined, plans: RecapPlan[], days: number): Recap | null {
  if (plans.length === 0) return null;
  const byDay = new Map<number, number[]>();
  for (const p of plans) byDay.set(p.dayNumber, [...(byDay.get(p.dayNumber) ?? []), Number(p.distanceFromPrevious) || 0]);
  let km = 0;
  byDay.forEach((legs) => {
    km += legs.reduce((a, b) => a + b, 0) + (legs[0] || 0);
  });
  const roadKm = Math.round(km * ROAD_FACTOR);
  const places = plans.map((p) => p.placeName).filter((n): n is string => !!n);
  const d = Math.max(1, days);
  const text = `${d} ${d === 1 ? "day" : "days"} in ${city || "the city"} with Nomads: ${plans.length} stops, about ${roadKm} km. ${places.slice(0, 5).join(", ")}${places.length > 5 ? " and more" : ""}.`;
  return { stops: plans.length, days: d, roadKm, places, text };
}
