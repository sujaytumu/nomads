export type Mode = "CAB" | "LOCAL_BUS" | "BUS" | "TRAIN" | "FLIGHT";

export type Option = { mode: Mode; label: string; hours: number; costPerPerson: number; note: string; cheapest?: boolean; fastest?: boolean };

export type Journey = {
  roadKm: number;
  options: Option[];
  /** One-way time of the quickest option. */
  quickestHours: number;
  fitsWeekend: boolean;
  advice: string;
};

export const ROAD_FACTOR = 1.3;
const LOCAL_KM = 40; // up to here, it is a city hop
const INTERCITY_KM = 60; // from here, buses and trains make sense
const FLIGHT_KM = 400;

export const MODE_LABEL: Record<Mode, string> = { CAB: "Cab", LOCAL_BUS: "City bus", BUS: "Bus", TRAIN: "Train", FLIGHT: "Flight" };

const round = (n: number, step: number) => Math.max(step, Math.round(n / step) * step);

/**
 * Rough one-way time and cost from the start to the first stop. These are planning estimates, not fares:
 * cabs are split between up to 4 people, everything else is per person.
 */
export function gettingThere(straightKm: number, people: number, twoDay: boolean): Journey | null {
  if (!Number.isFinite(straightKm) || straightKm <= 0) return null;
  const road = straightKm * ROAD_FACTOR;
  const pax = Math.max(1, Math.floor(people));
  const cabShare = Math.ceil(pax / 4);
  const options: Option[] = [];

  if (road <= LOCAL_KM) {
    const speed = 25; // city traffic
    options.push({ mode: "CAB", label: "Cab", hours: road / speed, costPerPerson: round((50 + road * 14) * cabShare / pax, 10), note: "Door to door" });
    options.push({ mode: "LOCAL_BUS", label: "City bus / metro", hours: road / 15 + 0.2, costPerPerson: round(10 + road * 1.5, 5), note: "Cheapest, with walking at both ends" });
  } else {
    options.push({ mode: "CAB", label: "Cab", hours: road / 55, costPerPerson: round((road * 12) * cabShare / pax, 50), note: "Door to door, stop when you like" });
    if (road >= INTERCITY_KM) {
      options.push({ mode: "BUS", label: "Bus", hours: road / 45 + 0.3, costPerPerson: round(road * 2.2, 50), note: "Overnight sleepers are common on long routes" });
      options.push({ mode: "TRAIN", label: "Train", hours: road / 55 + 0.4, costPerPerson: round(road * 1.8, 50), note: "Book early; sleeper to AC prices vary" });
    }
    if (road >= FLIGHT_KM) {
      options.push({ mode: "FLIGHT", label: "Flight", hours: 3.2 + road / 700, costPerPerson: round(2500 + road * 3.5, 100), note: "Includes airport time both ends" });
    }
  }

  const fastest = options.reduce((a, b) => (b.hours < a.hours ? b : a));
  const cheapest = options.reduce((a, b) => (b.costPerPerson < a.costPerPerson ? b : a));
  fastest.fastest = true;
  cheapest.cheapest = true;
  options.sort((a, b) => a.hours - b.hours);

  const limit = twoDay ? 4 : 2.5;
  const fits = fastest.hours <= limit;
  const advice = fits
    ? "Short enough to enjoy the whole weekend."
    : `About ${fastest.hours.toFixed(1)} hours each way is a lot for ${twoDay ? "a two-day" : "a one-day"} trip. Consider a closer city or leaving the night before.`;
  return { roadKm: road, options, quickestHours: fastest.hours, fitsWeekend: fits, advice };
}

export function formatHours(h: number): string {
  const mins = Math.round(h * 60);
  if (mins < 60) return `${mins} min`;
  const hh = Math.floor(mins / 60);
  const mm = mins % 60;
  return mm === 0 ? `${hh} h` : `${hh} h ${mm} min`;
}
