"use client";

import { useState } from "react";

import MapView from "@/components/MapView";
import { fetchNearbyPlaces, PlaceNearby } from "@/lib/placeApi";

export default function MapPage() {
  const [city, setCity] = useState("Bengaluru");
  const [latitude, setLatitude] = useState("12.9716");
  const [longitude, setLongitude] = useState("77.5946");
  const [interest, setInterest] = useState("CULTURE");
  const [places, setPlaces] = useState<PlaceNearby[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [optimizeRoute, setOptimizeRoute] = useState(true);

  const handleSearch = async () => {
    setError(null);
    try {
      const data = await fetchNearbyPlaces({
        city,
        latitude: Number(latitude),
        longitude: Number(longitude),
        interest,
        radiusKm: 15,
        limit: 20,
      });
      setPlaces(data);
    } catch (err) {
      setError("Failed to load nearby places");
    }
  };

  return (
    <div className="section py-12 space-y-6">
      <div>
        <h2 className="text-2xl font-bold">Map View</h2>
        <p className="text-slate-600">Visualize places and route suggestions (Mapbox Directions).</p>
      </div>
      <div className="card p-6 space-y-4">
        <div className="grid md:grid-cols-4 gap-4">
          <input value={city} onChange={(e) => setCity(e.target.value)} className="border rounded-xl px-4 py-2" placeholder="City" />
          <input value={latitude} onChange={(e) => setLatitude(e.target.value)} className="border rounded-xl px-4 py-2" placeholder="Latitude" />
          <input value={longitude} onChange={(e) => setLongitude(e.target.value)} className="border rounded-xl px-4 py-2" placeholder="Longitude" />
          <select value={interest} onChange={(e) => setInterest(e.target.value)} className="border rounded-xl px-4 py-2">
            {["FOOD", "CULTURE", "NATURE", "ADVENTURE", "SHOPPING", "NIGHTLIFE", "RELAXATION"].map((item) => (
              <option key={item} value={item}>{item}</option>
            ))}
          </select>
        </div>
        <label className="flex items-center gap-2 text-sm text-slate-600">
          <input
            type="checkbox"
            checked={optimizeRoute}
            onChange={(e) => setOptimizeRoute(e.target.checked)}
          />
          Optimize route order by distance
        </label>
        <button className="btn-primary" onClick={handleSearch}>Load Nearby Places</button>
        {error && <p className="text-sm text-red-600">{error}</p>}
      </div>

      <div className="card p-4">
        {(() => {
          const orderedPlaces = optimizeRoute
            ? [...places].sort((a, b) => a.distanceKm - b.distanceKm)
            : places;
          return (
            <MapView
              places={orderedPlaces}
              center={[Number(longitude), Number(latitude)]}
            />
          );
        })()}
      </div>

      {!!places.length && (
        <div className="card p-6 space-y-3">
          <h3 className="text-lg font-semibold">Nearby Places</h3>
          <div className="grid md:grid-cols-2 gap-3">
            {(optimizeRoute ? [...places].sort((a, b) => a.distanceKm - b.distanceKm) : places).map((place) => (
              <div key={place.id} className="border rounded-xl p-4">
                <p className="font-semibold">{place.name}</p>
                <p className="text-sm text-slate-500">{place.category} · {place.distanceKm.toFixed(2)} km</p>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
