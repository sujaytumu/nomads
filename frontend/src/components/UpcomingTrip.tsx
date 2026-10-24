"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import Countdown from "@/components/Countdown";
import TripProgress from "@/components/TripProgress";
import LiveTrip from "@/components/LiveTrip";
import TripWeather from "@/components/WeatherStrip";

type Plan = { dayNumber: number; placeId?: number | null; placeName: string; startTime?: string | null; endTime?: string | null };
type Trip = { tripRequestId: number; city?: string | null; status: string; tripDate?: string | null; plans?: Plan[] | null };

const DAY = 86_400_000;
const hm = (t?: string | null) => (t ? t.slice(0, 5) : "");

/** Countdown to the next booked trip; on the day itself it becomes today's plan with directions. */
export default function UpcomingTrip({ trips }: { trips: Trip[] }) {
  // Re-checks the date every minute, so the card flips to "Today" at midnight without a reload.
  const [, setTick] = useState(0);
  useEffect(() => {
    const t = setInterval(() => setTick((n) => n + 1), 60000);
    return () => clearInterval(t);
  }, []);
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const upcoming = trips
    .filter((t) => t.status === "CONFIRMED" && t.tripDate)
    .map((t) => {
      const start = new Date(`${t.tripDate}T00:00:00`);
      const lastDay = Math.max(1, ...(t.plans || []).map((p) => p.dayNumber || 1));
      return { t, start, daysTo: Math.round((start.getTime() - today.getTime()) / DAY), lastDay };
    })
    // Still relevant until the last day of the trip is over.
    .filter((x) => x.daysTo + x.lastDay - 1 >= 0)
    .sort((a, b) => a.daysTo - b.daysTo)[0];

  if (!upcoming) return null;
  const { t, start, daysTo, lastDay } = upcoming;

  const dayNumber = Math.max(1, 1 - daysTo);
  const underway = daysTo <= 0;
  const stops = (t.plans || []).filter((p) => (p.dayNumber || 1) === dayNumber).sort((a, b) => hm(a.startTime).localeCompare(hm(b.startTime)));
  const label = underway ? (daysTo === 0 ? "Today" : `Day ${dayNumber}`) : daysTo === 1 ? "Tomorrow" : `In ${daysTo} days`;

  // The moment the trip starts: the first stop's time on day one (or midnight if no times are set).
  const firstStop = (t.plans || []).filter((p) => (p.dayNumber || 1) === 1 && p.startTime).sort((a, b) => hm(a.startTime).localeCompare(hm(b.startTime)))[0];
  const departs = new Date(start);
  if (firstStop?.startTime) {
    const [hh, mm] = hm(firstStop.startTime).split(":").map(Number);
    departs.setHours(hh || 0, mm || 0, 0, 0);
  }
  const beforeStart = departs.getTime() > Date.now();

  return (
    <div className="pop-in relative overflow-hidden rounded-3xl border border-brand-500/30 bg-gradient-to-br from-brand-900/60 via-slate-900 to-slate-900 p-5 shadow-soft sm:p-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.18em] text-ocean-300">
            {underway && !beforeStart && <span className="live-dot" aria-hidden="true" />}
            {underway && !beforeStart ? "Your trip is live" : underway ? "Starts soon" : "Coming up"}
          </p>
          <p className="mt-1 font-display text-3xl font-semibold sm:text-4xl">{label}</p>
          <p className="mt-0.5 text-sm text-slate-300">
            {t.city ? `${t.city} weekend` : "Weekend trip"} · {start.toLocaleDateString("en-IN", { weekday: "long", day: "numeric", month: "long" })}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link href={`/trip-summary?tripId=${t.tripRequestId}`} className="btn btn-primary btn-sm">Open trip</Link>
          <Link href={`/pickup?tripId=${t.tripRequestId}`} className="btn btn-secondary btn-sm">Pickup</Link>
        </div>
      </div>

      {beforeStart && (
        <div className="mt-4">
          <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-slate-400">Departs in</p>
          <Countdown to={departs} onDone={() => setTick((n) => n + 1)} />
        </div>
      )}

      <div className="mt-4">
        <TripProgress status={t.status} tripDate={t.tripDate} plans={t.plans} tripId={t.tripRequestId} compact />
      </div>

      {daysTo <= 3 && (
        <div className="mt-4">
          <TripWeather tripId={t.tripRequestId} start={start} days={lastDay} />
        </div>
      )}

      {underway && stops.length > 0 && <LiveTrip tripId={t.tripRequestId} day={dayNumber} city={t.city} stops={stops} />}
    </div>
  );
}
