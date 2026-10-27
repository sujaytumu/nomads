"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import Cover from "@/components/Cover";
import { clearRecent, readRecent, type RecentPlace } from "@/lib/recentPlaces";

/** A row of the places you opened lately, newest first. Hidden until there is something to show. */
export default function RecentlyViewed() {
  const [items, setItems] = useState<RecentPlace[]>([]);

  useEffect(() => {
    setItems(readRecent());
  }, []);

  if (items.length === 0) return null;

  return (
    <section aria-label="Recently viewed" className="space-y-2">
      <div className="flex items-center justify-between">
        <h2 className="font-display text-lg font-semibold">Recently viewed</h2>
        <button type="button" className="text-xs text-slate-400 hover:text-slate-200" onClick={() => { clearRecent(); setItems([]); }}>Clear</button>
      </div>
      <div className="-mx-1 flex gap-3 overflow-x-auto px-1 pb-1">
        {items.map((p) => (
          <Link key={p.id} href={`/place/${p.id}`} className="card group block w-36 shrink-0 overflow-hidden sm:w-40">
            <Cover src={p.imageUrl} alt={p.name} kind={p.category} className="h-24" />
            <div className="p-2">
              <p className="truncate text-sm font-semibold">{p.name}</p>
              {p.city && <p className="truncate text-xs text-slate-400">{p.city}</p>}
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}
