"use client";

import React from "react";
import Link from "next/link";

export default function PlaceCard({ place, onAdd }: { place: any; onAdd: (p: any) => void }) {
  return (
    <div className="card p-4">
      <div className="grid grid-cols-3 gap-4 items-center">
        <img src={place.imageUrl || "https://via.placeholder.com/400x300?text=Place"} alt={place.name} className="w-full h-24 object-cover rounded-md col-span-1" />
        <div className="col-span-2">
          <div className="flex items-center justify-between">
            <h4 className="font-semibold">{place.name}</h4>
            <span className="text-sm text-slate-500">{place.distance ? `${place.distance} km` : ""}</span>
          </div>
          <p className="text-sm text-slate-600 mt-1">{place.shortDescription || place.description || "Popular nearby place"}</p>
          <div className="mt-3 flex gap-2">
            <Link href={`/place/${place.id}`} className="btn-outline">Explore</Link>
            <button className="btn-primary" onClick={() => onAdd(place)}>Add to tour</button>
          </div>
        </div>
      </div>
    </div>
  );
}
