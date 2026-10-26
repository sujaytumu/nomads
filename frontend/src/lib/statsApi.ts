import { api } from "./api";

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

export async function fetchTravelStats(): Promise<TravelStats> {
  const { data } = await api.get("/api/me/stats");
  return data as TravelStats;
}
