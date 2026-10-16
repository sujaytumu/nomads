"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import TripProgress from "@/components/TripProgress";
import { api } from "@/lib/api";
import { fetchMe } from "@/lib/authApi";
import { formatCost, isPayable, tripTitle } from "@/lib/tripStatus";

const TILES = [
  { href: "/trip-planner", title: "Plan a trip", text: "Build a weekend itinerary" },
  { href: "/packages", title: "Packages", text: "Ready-made weekends" },
  { href: "/map", title: "Map", text: "Explore nearby places" },
  { href: "/wishlist", title: "Wishlist", text: "Places you saved" },
];

/** Signed-in home: one clear next step, then the main doors. */
export default function HomeDashboard() {
  const [name, setName] = useState("");
  const [trips, setTrips] = useState<any[] | null>(null);

  useEffect(() => {
    fetchMe().then((m) => setName((m.name || "").split(" ")[0])).catch(() => undefined);
    api.get("/api/trips/me").then((r) => setTrips(r.data || [])).catch(() => setTrips([]));
  }, []);

  const newest = (list: any[]) =>
    [...list].sort((a, b) => new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime())[0];
  const open = newest((trips || []).filter((t) => isPayable(t.status)));
  const booked = newest((trips || []).filter((t) => t.status === "CONFIRMED"));
  const focus = open || booked;

  return (
    <section className="section space-y-6 py-8">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">{name ? `Hi ${name} 👋` : "Welcome back"}</h1>
        <p className="mt-1 text-sm text-slate-400">Plan → review → pay → your itinerary lands in your inbox.</p>
      </div>

      {trips === null ? (
        <div className="card h-28 animate-pulse" />
      ) : focus ? (
        <div className="card space-y-3 p-5">
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
            {open ? "Finish booking" : "Your latest trip"}
          </p>
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-lg font-semibold">{tripTitle(focus)} · {formatCost(focus.estimatedCost)}</p>
              <TripProgress status={focus.status} />
            </div>
            <div className="flex flex-wrap gap-2">
              {open ? (
                <Link href={`/payment?tripRequestId=${open.tripRequestId}`} className="btn btn-primary">Pay &amp; book</Link>
              ) : (
                <Link href={`/pickup?tripId=${booked.tripRequestId}`} className="btn btn-primary">Arrange pickup</Link>
              )}
              <Link href={`/trip-summary?tripId=${focus.tripRequestId}`} className="btn btn-secondary">View trip</Link>
            </div>
          </div>
        </div>
      ) : (
        <div className="card flex flex-wrap items-center justify-between gap-3 p-5">
          <div>
            <p className="text-lg font-semibold">No trips yet</p>
            <p className="text-sm text-slate-400">Tell us what you like and we&apos;ll build your weekend.</p>
          </div>
          <Link href="/trip-planner" className="btn btn-primary">Plan my first trip →</Link>
        </div>
      )}

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        {TILES.map((t) => (
          <Link key={t.href} href={t.href} className="card p-4 transition-colors hover:border-brand-500/50">
            <p className="font-semibold">{t.title}</p>
            <p className="mt-0.5 text-xs text-slate-400">{t.text}</p>
          </Link>
        ))}
      </div>
    </section>
  );
}
