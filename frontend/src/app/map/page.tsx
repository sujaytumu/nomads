"use client";

import Link from "next/link";
import dynamic from "next/dynamic";
import { useEffect, useMemo, useState } from "react";

import Cover from "@/components/Cover";
import PageHeader from "@/components/PageHeader";
import { fetchMe } from "@/lib/authApi";
import { fetchNearbyPlaces, PlaceNearby } from "@/lib/placeApi";

const MapView = dynamic(() => import("@/components/MapView"), { ssr: false });

const CITIES: Record<string, [number, number]> = {
  Bengaluru: [12.9716, 77.5946],
  Mumbai: [19.076, 72.8777],
  Delhi: [28.6139, 77.209],
  Hyderabad: [17.385, 78.4867],
  Vishakapatnam: [17.6868, 83.2185],
};

const INTERESTS: { value: string; label: string; icon: string }[] = [
  { value: "", label: "All", icon: "✨" },
  { value: "FOOD", label: "Food", icon: "🍜" },
  { value: "CULTURE", label: "Culture", icon: "🏛️" },
  { value: "NATURE", label: "Nature", icon: "🌿" },
  { value: "ADVENTURE", label: "Adventure", icon: "🧗" },
  { value: "SHOPPING", label: "Shopping", icon: "🛍️" },
  { value: "NIGHTLIFE", label: "Nightlife", icon: "🌙" },
  { value: "RELAXATION", label: "Relax", icon: "🧘" },
];

const chip = (on: boolean) =>
  `shrink-0 rounded-full border px-4 py-2 text-sm font-semibold transition-colors ${
    on ? "border-brand-600 bg-brand-600 text-white shadow-md shadow-brand-600/25" : "border-slate-700 bg-slate-900 text-slate-300 hover:bg-slate-800"
  }`;

export default function MapPage() {
  const [city, setCity] = useState("Bengaluru");
  const [interest, setInterest] = useState("");
  const [places, setPlaces] = useState<PlaceNearby[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [sort, setSort] = useState<"near" | "top">("near");

  // Start in the city from your profile.
  useEffect(() => {
    if (!localStorage.getItem("nomad_token")) return;
    fetchMe()
      .then((m) => m.city && CITIES[m.city] && setCity(m.city))
      .catch(() => undefined);
  }, []);

  // Places load by themselves whenever the city or interest changes.
  useEffect(() => {
    let live = true;
    const [lat, lng] = CITIES[city];
    setLoading(true);
    setError(null);
    fetchNearbyPlaces({ city, latitude: lat, longitude: lng, interest: interest || undefined, radiusKm: 15, limit: 30 } as any)
      .then((d) => live && setPlaces(d))
      .catch(() => live && setError("Couldn't load places. The server may be waking up — give it a moment and try again."))
      .finally(() => live && setLoading(false));
    return () => {
      live = false;
    };
  }, [city, interest]);

  const ordered = useMemo(
    () => [...places].sort((a, b) => (sort === "near" ? a.distanceKm - b.distanceKm : (b.rating || 0) - (a.rating || 0))),
    [places, sort]
  );
  const [lat, lng] = CITIES[city];

  return (
    <div className="section space-y-5 py-8">
      <PageHeader title="Explore" subtitle="See what's around, then build a weekend from it." />

      <div className="space-y-3">
        <div className="-mx-5 flex gap-2 overflow-x-auto px-5 pb-1 sm:mx-0 sm:px-0">
          {Object.keys(CITIES).map((c) => (
            <button key={c} type="button" onClick={() => setCity(c)} className={chip(city === c)}>📍 {c}</button>
          ))}
        </div>
        <div className="-mx-5 flex gap-2 overflow-x-auto px-5 pb-1 sm:mx-0 sm:px-0">
          {INTERESTS.map((i) => (
            <button key={i.value} type="button" onClick={() => setInterest(i.value)} className={chip(interest === i.value)}>
              <span aria-hidden="true" className="mr-1">{i.icon}</span>{i.label}
            </button>
          ))}
        </div>
      </div>

      {error && <p role="alert" className="rounded-2xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm text-red-300">{error}</p>}

      <div className="card overflow-hidden p-2">
        {loading ? (
          <div className="flex h-[360px] flex-col items-center justify-center gap-3 text-slate-400 sm:h-[440px]">
            <div className="h-8 w-8 animate-spin rounded-full border-2 border-brand-500 border-t-transparent" />
            <p className="text-sm">Finding places…</p>
          </div>
        ) : places.length === 0 ? (
          <div className="flex h-[260px] flex-col items-center justify-center gap-2 text-center text-slate-400">
            <span className="text-4xl">🗺️</span>
            <p className="font-medium">Nothing found here yet</p>
            <p className="text-sm">Try another interest or city.</p>
          </div>
        ) : (
          <div className="h-[360px] overflow-hidden rounded-2xl sm:h-[440px]">
            <MapView places={ordered} center={[lng, lat]} />
          </div>
        )}
      </div>

      {!loading && places.length > 0 && (
        <section className="space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h2 className="font-display text-xl font-semibold">{places.length} places in {city}</h2>
            <div className="flex gap-2">
              <button type="button" className={`btn btn-sm ${sort === "near" ? "btn-primary" : "btn-secondary"}`} onClick={() => setSort("near")}>Nearest</button>
              <button type="button" className={`btn btn-sm ${sort === "top" ? "btn-primary" : "btn-secondary"}`} onClick={() => setSort("top")}>Top rated</button>
            </div>
          </div>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {ordered.map((p) => (
              <Link key={p.id} href={`/place/${p.id}`} className="card group overflow-hidden transition-all hover:-translate-y-0.5 hover:shadow-lift">
                <div className="relative">
                  <Cover src={p.imageUrl} alt={p.name} kind={p.category as string} className="h-36" />
                  <span className="absolute right-3 top-3 rounded-full bg-black/55 backdrop-blur ring-1 ring-white/10 px-2.5 py-1 text-xs font-bold text-white">★ {p.rating.toFixed(1)}</span>
                </div>
                <div className="p-3">
                  <p className="truncate font-display text-base font-semibold">{p.name}</p>
                  <p className="text-xs text-slate-400">{String(p.category).charAt(0) + String(p.category).slice(1).toLowerCase()} · {p.distanceKm.toFixed(1)} km away</p>
                </div>
              </Link>
            ))}
          </div>
          <div className="pt-2 text-center">
            <Link href={`/trip-planner${interest ? `?interest=${interest}` : ""}`} className="btn btn-primary">Plan a weekend around {interest ? INTERESTS.find((i) => i.value === interest)?.label.toLowerCase() : "these"} →</Link>
          </div>
        </section>
      )}
    </div>
  );
}
