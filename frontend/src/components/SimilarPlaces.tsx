"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import Cover from "@/components/Cover";
import { searchPlaces, type SearchItem } from "@/lib/placeSearchApi";

/** Other places of the same kind in the same city, best rated first. Hidden when there are none. */
export default function SimilarPlaces({ placeId, city, category }: { placeId: number; city?: string; category?: string }) {
  const [items, setItems] = useState<SearchItem[]>([]);

  useEffect(() => {
    if (!city) return;
    let live = true;
    searchPlaces({ city, category, sort: "rating", size: 7 })
      .then((r) => live && setItems(r.items.filter((p) => p.id !== placeId).slice(0, 6)))
      .catch(() => undefined);
    return () => { live = false; };
  }, [placeId, city, category]);

  if (items.length === 0) return null;

  return (
    <section aria-label="Similar places" className="mt-8 space-y-2">
      <h2 className="font-display text-lg font-semibold">More in {city}</h2>
      <div className="-mx-1 flex gap-3 overflow-x-auto px-1 pb-1">
        {items.map((p) => (
          <Link key={p.id} href={`/place/${p.id}`} className="card group block w-40 shrink-0 overflow-hidden sm:w-44">
            <Cover src={p.imageUrl} alt={p.name} kind={p.category} className="h-24" />
            <div className="p-2">
              <p className="truncate text-sm font-semibold">{p.name}</p>
              <p className="truncate text-xs text-slate-400">★ {p.rating.toFixed(1)}</p>
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}
