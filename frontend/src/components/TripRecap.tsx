"use client";

import Link from "next/link";
import { useMemo } from "react";

import { buildRecap, isTripOver, type RecapPlan } from "@/lib/tripRecap";
import { toast } from "@/lib/toast";

/** Shown once a booked trip is over: the numbers from the trip, a share button, and the next steps (rate it, plan again). */
export default function TripRecap({ tripId, city, status, tripDate, days, plans }: { tripId: number | string; city?: string; status?: string; tripDate?: string | null; days: number; plans: RecapPlan[] }) {
  const recap = useMemo(() => (status === "CONFIRMED" && isTripOver(tripDate, days, new Date()) ? buildRecap(city, plans, days) : null), [status, tripDate, days, city, plans]);
  if (!recap) return null;

  async function share() {
    if (!recap) return;
    try {
      if (navigator.share) {
        await navigator.share({ title: `My ${city ?? ""} trip`.trim(), text: recap.text });
        return;
      }
      await navigator.clipboard.writeText(recap.text);
      toast("Recap copied", "success");
    } catch (e: any) {
      if (e?.name !== "AbortError") toast("Couldn't share the recap.", "error");
    }
  }

  return (
    <section className="card space-y-4 p-6" aria-label="Trip recap">
      <div>
        <h3 className="text-lg font-semibold">Your trip in numbers</h3>
        <p className="text-sm text-slate-400">{city ? `${city} · ` : ""}{tripDate}</p>
      </div>
      <dl className="grid grid-cols-3 gap-3 text-center">
        <div className="rounded-2xl bg-slate-800/60 p-3"><dt className="text-xs uppercase text-slate-400">Days</dt><dd className="font-display text-2xl font-semibold">{recap.days}</dd></div>
        <div className="rounded-2xl bg-slate-800/60 p-3"><dt className="text-xs uppercase text-slate-400">Stops</dt><dd className="font-display text-2xl font-semibold">{recap.stops}</dd></div>
        <div className="rounded-2xl bg-slate-800/60 p-3"><dt className="text-xs uppercase text-slate-400">Driven</dt><dd className="font-display text-2xl font-semibold">{recap.roadKm} km</dd></div>
      </dl>
      <p className="text-sm text-slate-300">{recap.places.slice(0, 6).join(" · ")}{recap.places.length > 6 ? " · …" : ""}</p>
      <div className="flex flex-wrap gap-2">
        <button type="button" className="btn btn-primary btn-sm" onClick={share}>Share recap</button>
        <Link href={`/review?tripId=${tripId}`} className="btn btn-secondary btn-sm">Rate this trip</Link>
        <Link href="/trip-planner" className="btn btn-secondary btn-sm">Plan the next one</Link>
      </div>
    </section>
  );
}
