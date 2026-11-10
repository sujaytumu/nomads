"use client";

import { useMemo } from "react";

import { formatHours, gettingThere, MODE_LABEL } from "@/lib/gettingThere";

type Plan = { dayNumber: number; placeName?: string; distanceFromPrevious?: number | null };

const rupees = (n: number) => `₹${Math.round(n).toLocaleString("en-IN")}`;

/** How to reach the first stop: time and cost by cab, bus, train or flight, with a verdict for a weekend trip. */
export default function GettingThere({ plans, city, people, days }: { plans: Plan[]; city?: string; people: number; days: number }) {
  const first = useMemo(() => {
    const day1 = plans.filter((p) => p.dayNumber === 1);
    return day1.length > 0 ? day1[0] : plans[0];
  }, [plans]);

  const km = Number(first?.distanceFromPrevious);
  const journey = useMemo(() => gettingThere(km, people, days >= 2), [km, people, days]);
  if (!first || !journey) return null;

  const dest = encodeURIComponent([first.placeName, city].filter(Boolean).join(", "));
  const maps = `https://www.google.com/maps/dir/?api=1&destination=${dest}&travelmode=transit`;

  return (
    <section className="card space-y-3 p-6" aria-label="Getting there">
      <div>
        <h3 className="text-lg font-semibold">Getting there</h3>
        <p className="text-sm text-slate-400">
          To {first.placeName || "your first stop"}: about {Math.round(journey.roadKm)} km by road. One way, per person. These are estimates, not fares.
        </p>
      </div>
      <ul className="divide-y divide-slate-800 rounded-2xl border border-slate-800">
        {journey.options.map((o) => (
          <li key={o.mode} className="flex items-center justify-between gap-3 px-4 py-3">
            <div className="min-w-0">
              <p className="text-sm font-semibold">
                {MODE_LABEL[o.mode]}
                {o.fastest && <span className="ml-2 rounded-full bg-emerald-500/15 px-2 py-0.5 text-[10px] font-bold uppercase text-emerald-300">Fastest</span>}
                {o.cheapest && <span className="ml-2 rounded-full bg-sky-500/15 px-2 py-0.5 text-[10px] font-bold uppercase text-sky-300">Cheapest</span>}
              </p>
              <p className="text-xs text-slate-400">{o.note}</p>
            </div>
            <div className="shrink-0 text-right">
              <p className="text-sm font-semibold tabular-nums">{rupees(o.costPerPerson)}</p>
              <p className="text-xs text-slate-400">{formatHours(o.hours)}</p>
            </div>
          </li>
        ))}
      </ul>
      <p className={`text-sm ${journey.fitsWeekend ? "text-emerald-300" : "text-amber-300"}`}>{journey.advice}</p>
      <a href={maps} target="_blank" rel="noopener noreferrer" className="btn btn-secondary btn-sm">Live directions in Maps</a>
    </section>
  );
}
