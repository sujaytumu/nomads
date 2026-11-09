import { getCached } from "./api";

export type Badge = { code: string; title: string; description: string; icon: string; current: number; target: number; earned: boolean };
export type Level = { level: number; title: string; xp: number; xpIntoLevel: number; xpForNextLevel: number; percentToNext: number };

export type TravelStats = {
  bookedTrips: number;
  cancelledTrips: number;
  pendingTrips: number;
  placesVisited: number;
  citiesVisited: number;
  kmTravelled: number;
  totalSpent: number;
  groupTrips: number;
  reviewsWritten: number;
  favouriteInterest: string | null;
  tripsByInterest: Record<string, number>;
  level: Level;
  badgesEarned: number;
  badgesTotal: number;
  badges: Badge[];
  nextBadge: Badge | null;
};

export const STATS_URL = "/api/me/stats";

export async function fetchTravelStats(): Promise<TravelStats> {
  return getCached<TravelStats>(STATS_URL, { ttl: 30000, persist: true });
}
