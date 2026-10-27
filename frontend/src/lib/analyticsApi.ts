import { api } from "./api";

export type Totals = { users: number; trips: number; bookedTrips: number; cancelledTrips: number; bookingRate: number; netRevenue: number; refunded: number; averageBookingValue: number };
export type FunnelStep = { label: string; count: number; percentOfFirst: number };
export type Ranked = { label: string; count: number };
export type DayPoint = { date: string; signups: number; tripsPlanned: number; bookings: number; revenue: number };
export type AnalyticsSnapshot = {
  from: string;
  to: string;
  days: number;
  totals: Totals;
  funnel: FunnelStep[];
  tripsByStatus: Record<string, number>;
  topCities: Ranked[];
  interestMix: Ranked[];
  daily: DayPoint[];
};

export async function fetchAnalytics(days: number): Promise<AnalyticsSnapshot> {
  const { data } = await api.get("/api/admin/analytics", { params: { days } });
  return data as AnalyticsSnapshot;
}
