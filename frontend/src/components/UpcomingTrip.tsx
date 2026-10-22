"use client";

import Link from "next/link";

type Plan = { dayNumber: number; placeName: string; startTime?: string | null; endTime?: string | null };
type Trip = { tripRequestId: number; city?: string | null; status: string; tripDate?: string | null; plans?: Plan[] | null };

const DAY = 86_400_000;
const hm = (t?: string | null) => (t ? t.slice(0, 5) : "");

/** Countdown to the next booked trip; on the day itself it becomes today's plan with directions. */
export default function UpcomingTrip({ trips }: { trips: Trip[] }) {
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
  const { t, start, daysTo } = upcoming;

  const dayNumber = Math.max(1, 1 - daysTo);
  const underway = daysTo <= 0;
  const stops = (t.plans || []).filter((p) => (p.dayNumber || 1) === dayNumber).sort((a, b) => hm(a.startTime).localeCompare(hm(b.startTime)));
  const label = underway ? (daysTo === 0 ? "Today" : `Day ${dayNumber}`) : daysTo === 1 ? "Tomorrow" : `In ${daysTo} days`;
  const nowHm = new Date().toTimeString().slice(0, 5);
  const nextIdx = underway ? stops.findIndex((s) => hm(s.endTime || s.startTime) >= nowHm) : -1;

  return (
    <div className="relative overflow-hidden rounded-3xl border border-brand-500/30 bg-gradient-to-br from-brand-900/60 via-slate-900 to-slate-900 p-5 shadow-soft sm:p-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-ocean-300">{underway ? "Your trip is on" : "Coming up"}</p>
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

      {underway && stops.length > 0 && (
        <ol className="mt-4 space-y-2">
          {stops.map((s, i) => {
            const maps = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${s.placeName} ${t.city || ""}`.trim())}`;
            const isNext = i === (nextIdx === -1 ? -1 : nextIdx);
            return (
              <li key={`${s.placeName}-${i}`} className={`flex items-center gap-3 rounded-2xl border px-3 py-2.5 ${isNext ? "border-brand-500/60 bg-brand-600/15 shadow-glow" : "border-slate-800 bg-slate-900/60"}`}>
                <span className="w-24 shrink-0 text-xs font-semibold text-slate-400">
                  {hm(s.startTime)}{s.endTime ? `–${hm(s.endTime)}` : ""}
                </span>
                <span className="min-w-0 flex-1 truncate text-sm font-semibold">
                  {s.placeName}
                  {isNext && <span className="ml-2 rounded-full bg-brand-600 px-2 py-0.5 text-[10px] font-bold uppercase text-white">Next</span>}
                </span>
                <a href={maps} target="_blank" rel="noopener noreferrer" className="btn btn-secondary btn-sm shrink-0">Directions</a>
              </li>
            );
          })}
        </ol>
      )}
    </div>
  );
}
