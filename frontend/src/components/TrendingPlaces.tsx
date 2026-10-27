"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import Cover from "@/components/Cover";
import { fetchTrending, type TrendingPlace } from "@/lib/trendingApi";

/** Places that recent bookings are heading to. Hidden when there is nothing to show (or the server is older). */
export default function TrendingPlaces() {
  const [items, setItems] = useState<TrendingPlace[]>([]);

  useEffect(() => {
    let live = true;
    fetchTrending(8).then((d) => live && Array.isArray(d) && setItems(d)).catch(() => undefined);
    return () => { live = false; };
  }, []);

  if (items.length === 0) return null;

  return (
    <section aria-label="Trending places" className="space-y-2">
      <h2 className="font-display text-lg font-semibold">Trending now</h2>
      <div className="-mx-1 flex gap-3 overflow-x-auto px-1 pb-1">
        {items.map(({ place: p, trips }, i) => (
          <Link key={p.id} href={`/place/${p.id}`} className="card group block w-40 shrink-0 overflow-hidden sm:w-44">
            <div className="relative">
              <Cover src={p.imageUrl} alt={p.name} kind={p.category} className="h-28" />
              <span className="absolute left-2 top-2 rounded-full bg-black/60 px-2 py-0.5 text-xs font-bold text-white ring-1 ring-white/10">#{i + 1}</span>
            </div>
            <div className="p-2">
              <p className="truncate text-sm font-semibold">{p.name}</p>
              <p className="truncate text-xs text-slate-400">{p.city} · {trips} {trips === 1 ? "trip" : "trips"}</p>
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}
