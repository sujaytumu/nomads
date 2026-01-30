"use client";

import React, { useEffect, useState } from "react";
import { api } from "@/lib/api";
import PlaceCard from "./PlaceCard";

export default function PlacesList() {
  const [places, setPlaces] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [debugInfo, setDebugInfo] = useState<any>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      try {
        // Determine user coords (geolocation -> localStorage -> fallback)
        let latitude: number | null = null;
        let longitude: number | null = null;
        let city: string | null = null;

        try {
          const raw = localStorage.getItem("nomad_location");
          if (raw) {
            const loc = JSON.parse(raw);
            if (loc.latitude) latitude = Number(loc.latitude);
            if (loc.longitude) longitude = Number(loc.longitude);
            if (loc.city) city = loc.city;
          }

          if ((!latitude || !longitude) && typeof navigator !== "undefined" && navigator.geolocation) {
            const pos = await new Promise<GeolocationPosition>((resolve, reject) => {
              navigator.geolocation.getCurrentPosition(resolve, reject, { timeout: 7000 });
            }).catch(() => null as any);
            if (pos && pos.coords) {
              latitude = pos.coords.latitude;
              longitude = pos.coords.longitude;
            }
          }
        } catch (e) {
          // ignore geolocation/localStorage errors
        }

        // fallback coordinates (Bengaluru) if nothing available
        if (!latitude || !longitude) {
          latitude = 12.9716;
          longitude = 77.5946;
        }

        // Try reverse-geocoding on the client (Mapbox) to get a consistent city for the coords.
        async function reverseGeocode(lat: number, lon: number): Promise<string | null> {
          try {
            const token = (
              process.env.NEXT_PUBLIC_MAPBOX_ACCESS_TOKEN ||
              process.env.NEXT_PUBLIC_MAPBOX_TOKEN ||
              (window as any).__NEXT_PUBLIC_MAPBOX_ACCESS_TOKEN ||
              (window as any).__NEXT_PUBLIC_MAPBOX_TOKEN
            ) as string | undefined;
            if (!token) return null;
            const url = `https://api.mapbox.com/geocoding/v5/mapbox.places/${lon},${lat}.json?types=place&limit=1&access_token=${token}`;
            const r = await fetch(url);
            if (!r.ok) return null;
            const j = await r.json();
            return j.features?.[0]?.text ?? null;
          } catch (err) {
            return null;
          }
        }

        const resolvedCity = await reverseGeocode(latitude, longitude);
        if (resolvedCity) city = resolvedCity;

        // persist the chosen location for future requests
        try {
          localStorage.setItem("nomad_location", JSON.stringify({ city, latitude, longitude }));
        } catch (e) { /* ignore storage errors */ }

        // Build params; omit city if we couldn't resolve it so backend can infer from seeded data
        const params: any = { latitude, longitude, radiusKm: 50, limit: 50 };
        if (city) params.city = city;

        const res = await api.get("/api/places/nearby", { params });
        if (!cancelled) {
          const count = (res.data || []).length;
          setDebugInfo({ params, status: res.status, count });
          setPlaces(res.data || []);
        }
      } catch (e: any) {
        const msg = e?.response?.data || e?.message || "Failed to load places";
        if (!cancelled) {
          setError(String(msg));
          setDebugInfo({ params: (e as any)?.config?.params, status: (e as any)?.response?.status, error: msg });
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, []);

  function handleAdd(place: any) {
    try {
      const raw = localStorage.getItem("nomad_tour");
      const arr = raw ? JSON.parse(raw) : [];
      if (!arr.find((p: any) => p.id === place.id)) {
        arr.push({ id: place.id, name: place.name });
        localStorage.setItem("nomad_tour", JSON.stringify(arr));
        alert(`${place.name} added to your tour`);
      } else {
        alert(`${place.name} already in your tour`);
      }
    } catch (err) {
      console.error(err);
      alert("Unable to add to tour");
    }
  }

  if (loading) return <div>Loading nearby places…</div>;
  if (error) return (
    <div>
      <div className="text-red-600">{error}</div>
      <pre className="text-xs text-slate-500 mt-2">{JSON.stringify(debugInfo, null, 2)}</pre>
      <div className="mt-2">
        <button className="btn-outline" onClick={() => {
          // populate demo places for UI testing
          const demo = [{ id: 1, name: "Demo Beach", shortDescription: "Demo place", imageUrl: null, distanceKm: 1, rating: 4.5 }];
          setPlaces(demo);
          setError(null);
        }}>Use demo places</button>
      </div>
    </div>
  );
  if (!places || places.length === 0) return (
    <div>
      <div>No nearby places found.</div>
      <pre className="text-xs text-slate-500 mt-2">{JSON.stringify(debugInfo, null, 2)}</pre>
      <div className="mt-2">
        <button className="btn-outline" onClick={() => {
          const demo = [{ id: 1, name: "Demo Beach", shortDescription: "Demo place", imageUrl: null, distanceKm: 1, rating: 4.5 }];
          setPlaces(demo);
        }}>Use demo places</button>
      </div>
    </div>
  );

  return (
    <div className="grid md:grid-cols-2 gap-4">
      {places.map((p) => (
        <PlaceCard key={p.id} place={p} onAdd={handleAdd} />
      ))}
    </div>
  );
}
