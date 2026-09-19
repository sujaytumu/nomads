"use client";

import { useState } from "react";
import dynamic from "next/dynamic";

const MapView = dynamic(() => import("@/components/MapView"), { ssr: false });
import { fetchNearbyPlaces, PlaceNearby } from "@/lib/placeApi";

const CITIES: Record<string, [number, number]> = {
  Bengaluru: [12.9716, 77.5946],
  Mumbai: [19.076, 72.8777],
  Delhi: [28.6139, 77.209],
};

const INTERESTS: { value: string; label: string; icon: string }[] = [
  { value: "FOOD", label: "Food", icon: "🍽️" },
  { value: "CULTURE", label: "Culture", icon: "🏛️" },
  { value: "NATURE", label: "Nature", icon: "🌿" },
  { value: "ADVENTURE", label: "Adventure", icon: "🧗" },
  { value: "SHOPPING", label: "Shopping", icon: "🛍️" },
  { value: "NIGHTLIFE", label: "Nightlife", icon: "🌃" },
  { value: "RELAXATION", label: "Relax", icon: "🧘" },
];

export default function MapPage() {
  const [city, setCity] = useState("Bengaluru");
  const [interest, setInterest] = useState("CULTURE");
  const [places, setPlaces] = useState<PlaceNearby[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [optimizeRoute, setOptimizeRoute] = useState(true);

  const [lat, lng] = CITIES[city];

  const handleSearch = async () => {
    setError(null);
    setLoading(true);
    try {
      const data = await fetchNearbyPlaces({
        city,
        latitude: lat,
        longitude: lng,
        interest,
        radiusKm: 15,
        limit: 20,
      });
      setPlaces(data);
    } catch (err) {
      setError("Failed to load nearby places - the server may be waking up, try again in a moment.");
    } finally {
      setLoading(false);
    }
  };

  const orderedPlaces = optimizeRoute ? [...places].sort((a, b) => a.distanceKm - b.distanceKm) : places;

  return (
    <div className="section py-12 space-y-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">Explore the Map</h2>
        <p className="text-slate-400 mt-1">Find places near you, filtered by what you're into.</p>
      </div>

      <div className="card p-6 space-y-6">
        <div className="grid sm:grid-cols-3 gap-3">
          {Object.keys(CITIES).map((c) => (
            <button
              key={c}
              onClick={() => setCity(c)}
              className={`rounded-xl px-4 py-3 font-semibold border transition ${
                city === c
                  ? "bg-brand-600 border-brand-600 text-white shadow-md shadow-brand-900/40"
                  : "border-slate-700 text-slate-300 hover:border-slate-600 hover:bg-slate-800"
              }`}
            >
              📍 {c}
            </button>
          ))}
        </div>

        <div className="space-y-2">
          <span className="text-sm font-semibold text-slate-300">Interest</span>
          <div className="flex flex-wrap gap-2">
            {INTERESTS.map((i) => (
              <button
                key={i.value}
                onClick={() => setInterest(i.value)}
                className={`rounded-full px-4 py-2 text-sm font-medium border transition ${
                  interest === i.value
                    ? "bg-brand-500/20 border-brand-500 text-brand-300"
                    : "border-slate-700 text-slate-300 hover:border-slate-600"
                }`}
              >
                <span className="mr-1">{i.icon}</span>
                {i.label}
              </button>
            ))}
          </div>
        </div>

        <div className="flex items-center justify-between flex-wrap gap-4">
          <label className="flex items-center gap-2 text-sm text-slate-300">
            <input
              type="checkbox"
              checked={optimizeRoute}
              onChange={(e) => setOptimizeRoute(e.target.checked)}
              className="accent-brand-500"
            />
            Optimize order by distance
          </label>
          <button className="btn-primary" onClick={handleSearch} disabled={loading}>
            {loading ? "Loading…" : "Load Nearby Places"}
          </button>
        </div>

        {error && (
          <p className="text-sm text-red-400 bg-red-500/10 border border-red-500/20 rounded-xl px-4 py-3">{error}</p>
        )}
      </div>

      <div className="card p-4 overflow-hidden">
        {places.length === 0 && !loading ? (
          <div className="h-[380px] flex flex-col items-center justify-center text-center gap-2 text-slate-500">
            <span className="text-4xl">🗺️</span>
            <p className="font-medium text-slate-400">Pick a city and interest, then load places</p>
          </div>
        ) : loading ? (
          <div className="h-[380px] flex flex-col items-center justify-center gap-3 text-slate-400">
            <div className="w-8 h-8 border-2 border-brand-500 border-t-transparent rounded-full animate-spin" />
            <p className="text-sm">Loading map… (may take up to a minute if the server was asleep)</p>
          </div>
        ) : (
          <MapView places={orderedPlaces} center={[lng, lat]} />
        )}
      </div>

      {!!places.length && (
        <div className="card p-6 space-y-4">
          <h3 className="text-lg font-semibold">Nearby Places ({places.length})</h3>
          <div className="grid md:grid-cols-2 gap-4">
            {orderedPlaces.map((place) => (
              <div key={place.id} className="flex gap-4 border border-slate-700 rounded-xl p-3 hover:border-slate-600 transition">
                <img
                  src={place.imageUrl || "/images/place-placeholder.svg"}
                  alt={place.name}
                  className="w-20 h-20 rounded-lg object-cover flex-shrink-0"
                />
                <div className="min-w-0">
                  <p className="font-semibold truncate">{place.name}</p>
                  <p className="text-sm text-slate-400">{place.category} · ⭐ {place.rating.toFixed(1)}</p>
                  <p className="text-xs text-slate-500 mt-1">{place.distanceKm.toFixed(2)} km away</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
