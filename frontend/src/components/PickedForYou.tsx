"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { api } from "@/lib/api";
import { fetchMe } from "@/lib/authApi";

type Pick = { place: any; why: string; score: number };

const label = (s?: string) => (s ? s.charAt(0) + s.slice(1).toLowerCase() : "");

/**
 * Suggestions ranked for this person: places that match their interest score highest,
 * then rating and closeness. Places already in one of their trips are left out so the
 * list is always something new.
 */
export default function PickedForYou() {
  const [picks, setPicks] = useState<Pick[] | null>(null);

  useEffect(() => {
    let live = true;
    (async () => {
      try {
        const me = await fetchMe();
        if (!me.latitude || !me.longitude) return live && setPicks([]);
        const [places, trips] = await Promise.all([
          api.get("/api/places/nearby", { params: { city: me.city, latitude: me.latitude, longitude: me.longitude, radiusKm: 50, limit: 60 } }),
          api.get("/api/trips/me").catch(() => ({ data: [] as any[] })),
        ]);
        const seen = new Set<string>();
        (trips.data || [])
          .filter((t: any) => t.status !== "CANCELLED")
          .forEach((t: any) => (t.plans || []).forEach((p: any) => seen.add(String(p.placeName).toLowerCase())));

        const ranked: Pick[] = (places.data || [])
          .filter((p: any) => !seen.has(String(p.name).toLowerCase()))
          .map((p: any) => {
            const match = !!me.interestType && p.category === me.interestType;
            const rating = Number(p.rating) || 0;
            const near = typeof p.distanceKm === "number" && p.distanceKm <= 5;
            const score = (match ? 3 : 0) + rating + (near ? 0.5 : 0) - (Number(p.distanceKm) || 0) * 0.02;
            const why = match
              ? `Matches your love of ${label(me.interestType).toLowerCase()}`
              : rating >= 4.5
                ? "Top rated nearby"
                : near
                  ? "Close to you"
                  : "Popular nearby";
            return { place: p, why, score };
          })
          .sort((a: Pick, b: Pick) => b.score - a.score)
          .slice(0, 6);
        if (live) setPicks(ranked);
      } catch {
        if (live) setPicks([]);
      }
    })();
    return () => {
      live = false;
    };
  }, []);

  if (picks === null) return <div className="section"><div className="card h-40 animate-pulse" /></div>;
  if (picks.length === 0) return null;

  return (
    <section className="section space-y-3 pb-8">
      <div>
        <h2 className="text-xl font-semibold">Picked for you</h2>
        <p className="text-sm text-slate-400">New places, ranked by your interests, rating and distance.</p>
      </div>
      <div className="-mx-6 flex snap-x gap-3 overflow-x-auto px-6 pb-2 sm:mx-0 sm:grid sm:grid-cols-2 sm:overflow-visible sm:px-0 lg:grid-cols-3">
        {picks.map(({ place, why }) => (
          <Link key={place.id} href={`/place/${place.id}`} className="card block w-64 shrink-0 snap-start overflow-hidden transition-colors hover:border-brand-500/50 sm:w-auto">
            <img src={place.imageUrl || "/images/place-placeholder.svg"} alt="" className="h-28 w-full object-cover" />
            <div className="space-y-1 p-3">
              <p className="truncate font-semibold">{place.name}</p>
              <p className="text-xs text-slate-400">
                {label(place.category)}
                {place.rating ? ` · ★ ${Number(place.rating).toFixed(1)}` : ""}
                {typeof place.distanceKm === "number" ? ` · ${place.distanceKm.toFixed(1)} km` : ""}
              </p>
              <p className="text-xs font-medium text-brand-400">{why}</p>
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}
