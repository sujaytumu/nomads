"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import HeroScene, { useGreeting } from "@/components/HeroScene";
import StoryChips from "@/components/StoryChips";
import TripProgress from "@/components/TripProgress";
import UpcomingTrip from "@/components/UpcomingTrip";
import { getCached } from "@/lib/api";
import { fetchMe } from "@/lib/authApi";
import { formatCost, isPayable, tripTitle } from "@/lib/tripStatus";

const TILES = [
  { href: "/trip-planner", title: "Plan a trip", text: "Build a weekend itinerary", icon: "🧭", tint: "bg-brand-600/20" },
  { href: "/packages", title: "Packages", text: "Ready-made weekends", icon: "🎒", tint: "bg-ocean-500/20" },
  { href: "/map", title: "Map", text: "Explore nearby places", icon: "🗺️", tint: "bg-violet-500/20" },
  { href: "/wishlist", title: "Wishlist", text: "Places you saved", icon: "💛", tint: "bg-brand-600/20" },
];

/** Signed-in home: one clear next step, then the main doors. */
export default function HomeDashboard() {
  const [name, setName] = useState("");
  const [trips, setTrips] = useState<any[] | null>(null);

  useEffect(() => {
    fetchMe().then((m) => setName((m.name || "").split(" ")[0])).catch(() => undefined);
    getCached<any[]>("/api/trips/me", { ttl: 15000 }).then((list) => setTrips(list || [])).catch(() => setTrips([]));
  }, []);

  const { phase, greeting } = useGreeting(name);

  const newest = (list: any[]) =>
    [...list].sort((a, b) => new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime())[0];
  const open = newest((trips || []).filter((t) => isPayable(t.status)));
  const booked = newest((trips || []).filter((t) => t.status === "CONFIRMED"));
  const focus = open || booked;

  return (
    <section className="section space-y-4 py-5 sm:space-y-6 sm:py-8">
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-[#1B2559] via-[#26379a] to-[#4f6cff] border border-brand-500/20 p-5 text-white shadow-lift sm:p-10">
        <HeroScene phase={phase} />
        <p className="relative text-xs font-semibold uppercase tracking-[0.18em] text-white/70">{greeting}</p>
        <h1 className="relative mt-1.5 max-w-xl font-display text-2xl font-semibold leading-tight sm:text-5xl">
          Where to this weekend?
        </h1>
      </div>

      <StoryChips />

      {trips && <UpcomingTrip trips={trips} />}

      {trips === null ? (
        <div className="card h-28 animate-pulse" />
      ) : focus ? (
        <div className="card space-y-3 p-5">
          <p className="eyebrow">
            {open ? "Finish booking" : "Your latest trip"}
          </p>
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-lg font-semibold">{tripTitle(focus)} · {formatCost(focus.estimatedCost)}</p>
              <TripProgress status={focus.status} tripDate={focus.tripDate} plans={focus.plans} tripId={focus.tripRequestId} />
            </div>
            <div className="flex flex-wrap gap-2">
              {open ? (
                <Link href={`/payment?tripRequestId=${open.tripRequestId}`} className="btn btn-primary btn-pulse">Pay &amp; book</Link>
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
            <div className="mt-3 max-w-md"><TripProgress status="PLANNED" stage={0} compact /></div>
          </div>
          <Link href="/trip-planner" className="btn btn-primary">Plan my first trip →</Link>
        </div>
      )}

      <div className="grid grid-cols-2 gap-2.5 sm:gap-3 lg:grid-cols-4">
        {TILES.map((t) => (
          <Link key={t.href} href={t.href} className="card flex items-center gap-3 rounded-2xl p-3 transition-all sm:p-4 hover:-translate-y-0.5 hover:shadow-lift">
            <span aria-hidden="true" className={`grid h-11 w-11 shrink-0 place-items-center rounded-2xl text-xl ${t.tint}`}>{t.icon}</span>
            <span>
              <span className="block text-sm font-semibold">{t.title}</span>
              <span className="block text-[11px] leading-tight text-slate-400">{t.text}</span>
            </span>
          </Link>
        ))}
      </div>
    </section>
  );
}
