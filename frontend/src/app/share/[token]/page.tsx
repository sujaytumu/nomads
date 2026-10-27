"use client";

import { fetchSharedTrip } from "@/lib/shareApi";
import { fetchSharedPhotos, TripPhoto } from "@/lib/photoApi";
import { useEffect, useState } from "react";

export default function ShareTripPage({ params }: { params: { token: string } }) {
  const [trip, setTrip] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);
  const [photos, setPhotos] = useState<TripPhoto[]>([]);

  useEffect(() => {
    fetchSharedTrip(params.token)
      .then((data) => setTrip(data))
      .catch(() => setError("Shared trip not found"));
    fetchSharedPhotos(params.token).then(setPhotos).catch(() => setPhotos([]));
  }, [params.token]);

  const days: Record<number, any[]> = {};
  (trip?.plans || []).forEach((p: any) => (days[p.dayNumber] = [...(days[p.dayNumber] || []), p]));
  const t = (v: any) => (typeof v === "string" ? v.slice(0, 5) : "");

  return (
    <div className="mx-auto max-w-2xl space-y-6 px-5 py-10">
      <div className="text-center">
        <p className="text-2xl font-bold tracking-wide text-brand-300">NOMAD</p>
        <p className="text-sm text-slate-400">A shared weekend itinerary</p>
      </div>
      {error && <p role="alert" className="card p-5 text-center text-sm text-red-400">{error}</p>}
      {!trip && !error && <div className="card h-40 animate-pulse" />}
      {trip && (
        <>
          <h1 className="text-center text-3xl font-bold">{trip.city ? `${trip.city} weekend` : "Weekend trip"}</h1>
          {Object.entries(days).map(([day, stops]) => (
            <section key={day} className="card space-y-3 p-5">
              <p className="text-sm font-semibold uppercase tracking-wide text-brand-400">Day {day}</p>
              <ol className="space-y-3 border-l border-slate-700 pl-4">
                {stops.map((p: any, i: number) => (
                  <li key={`${p.placeId}-${i}`}>
                    <p className="font-semibold">{p.placeName}</p>
                    <p className="text-sm text-slate-400">{t(p.startTime)} – {t(p.endTime)}{typeof p.distanceFromPrevious === "number" ? ` · ${p.distanceFromPrevious.toFixed(1)} km from previous` : ""}</p>
                  </li>
                ))}
              </ol>
            </section>
          ))}
          {photos.length > 0 && (
            <section className="card space-y-3 p-5">
              <p className="text-sm font-semibold uppercase tracking-wide text-brand-400">Photos</p>
              <ul className="grid grid-cols-2 gap-2 sm:grid-cols-3">
                {photos.map((ph) => (
                  <li key={ph.id} className="aspect-square overflow-hidden rounded-xl border border-slate-700">
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img src={ph.dataUrl} alt={ph.caption || "Trip photo"} className="h-full w-full object-cover" loading="lazy" />
                  </li>
                ))}
              </ul>
            </section>
          )}
          <div className="text-center">
            <a href="/auth?mode=register" className="btn btn-primary">Plan your own weekend</a>
          </div>
        </>
      )}
    </div>
  );
}
