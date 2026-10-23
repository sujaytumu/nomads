"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import Cover from "@/components/Cover";
import { api, getCached } from "@/lib/api";
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
          getCached<any[]>("/api/trips/me", { ttl: 15000 }).catch(() => [] as any[]),
        ]);
        const seen = new Set<string>();
        (trips || [])
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
    <section className="section space-y-3 pb-6">
      <div>
        <h2 className="font-display text-xl font-semibold sm:text-2xl">Picked for you</h2>
      </div>
      <div className="-mx-5 flex snap-x scroll-pl-5 gap-3 overflow-x-auto px-5 pb-2 sm:mx-0 sm:grid sm:grid-cols-2 sm:overflow-visible sm:px-0 lg:grid-cols-3">
        {picks.map(({ place, why }) => (
          <Link key={place.id} href={`/place/${place.id}`} className="card group block w-64 shrink-0 snap-start overflow-hidden transition-all hover:-translate-y-0.5 hover:shadow-lift sm:w-auto">
            <div className="relative">
              <Cover src={place.imageUrl} kind={place.category} className="h-36" />
              <span className="absolute left-3 top-3 rounded-full bg-brand-600 px-3 py-1 text-[11px] font-semibold text-white shadow">{why}</span>
              {place.rating ? <span className="absolute right-3 top-3 rounded-full bg-slate-950/75 backdrop-blur ring-1 ring-white/10 px-2.5 py-1 text-xs font-bold text-white">★ {Number(place.rating).toFixed(1)}</span> : null}
            </div>
            <div className="p-3">
              <p className="truncate font-display text-base font-semibold">{place.name}</p>
              <p className="text-xs text-slate-400">
                {label(place.category)}
                {typeof place.distanceKm === "number" ? ` · ${place.distanceKm.toFixed(1)} km` : ""}
              </p>
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}
