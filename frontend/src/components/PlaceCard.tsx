"use client";

import React from "react";
import Link from "next/link";
import Cover from "./Cover";

const label = (s?: string) => (s ? s.charAt(0) + s.slice(1).toLowerCase() : "");

export default function PlaceCard({ place, onAdd }: { place: any; onAdd: (p: any) => void }) {
  return (
    <div className="card group overflow-hidden transition-all hover:-translate-y-0.5 hover:shadow-lift">
      <div className="relative">
        <Cover src={place.imageUrl} alt={place.name} kind={place.category} className="h-44" />
        {place.category && (
          <span className="absolute left-3 top-3 rounded-full bg-black/55 backdrop-blur ring-1 ring-white/10 px-3 py-1 text-xs font-semibold text-white shadow-sm">{label(place.category)}</span>
        )}
        {place.rating ? (
          <span className="absolute right-3 top-3 rounded-full bg-black/55 backdrop-blur ring-1 ring-white/10 px-2.5 py-1 text-xs font-bold text-white shadow-sm">★ {Number(place.rating).toFixed(1)}</span>
        ) : null}
      </div>
      <div className="space-y-3 p-4">
        <div>
          <h4 className="font-display text-lg font-semibold leading-snug">{place.name}</h4>
          <p className="mt-0.5 text-sm text-slate-400">
            {typeof place.distanceKm === "number" ? `${place.distanceKm.toFixed(1)} km away` : place.distance ? `${place.distance} km away` : place.city || "Popular nearby"}
          </p>
        </div>
        <div className="flex gap-2">
          <Link href={`/place/${place.id}`} className="btn btn-secondary btn-sm flex-1">Explore</Link>
          <button type="button" className="btn btn-primary btn-sm flex-1" onClick={() => onAdd(place)}>Add to tour</button>
        </div>
      </div>
    </div>
  );
}
