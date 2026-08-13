"use client";

import L from "leaflet";
import "@/lib/leafletIconFix";
import { useEffect, useRef, useState } from "react";

import type { PlaceNearby } from "@/lib/placeApi";

type MapViewProps = {
  places?: PlaceNearby[];
  center?: [number, number]; // [lng, lat] to match existing call sites
  routeGeoJson?: string | null;
};

// Uses OpenStreetMap tiles via Leaflet and OSRM's free public routing server -
// no API key, no signup, no payment method ever required. Replaces the
// previous Mapbox GL + Mapbox Directions implementation.
const OSRM_ROUTE_URL = "https://router.project-osrm.org/route/v1/driving/";

export default function MapView({ places = [], center, routeGeoJson }: MapViewProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapInstance = useRef<L.Map | null>(null);
  const markerRefs = useRef<L.Marker[]>([]);
  const routeLayerRef = useRef<L.GeoJSON | null>(null);
  const [routeError, setRouteError] = useState<string | null>(null);

  useEffect(() => {
    if (!containerRef.current || mapInstance.current) return;

    const [lng, lat] = center ?? [77.5946, 12.9716];
    const map = L.map(containerRef.current).setView([lat, lng], 10);
    mapInstance.current = map;

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 19,
    }).addTo(map);

    return () => {
      markerRefs.current.forEach((marker) => marker.remove());
      markerRefs.current = [];
      map.remove();
      mapInstance.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const map = mapInstance.current;
    if (!map) return;
    markerRefs.current.forEach((marker) => marker.remove());
    markerRefs.current = [];
    setRouteError(null);

    if (!places.length) return;

    const bounds = L.latLngBounds([]);
    places.forEach((place) => {
      const marker = L.marker([place.latitude, place.longitude])
        .bindPopup(`<strong>${place.name}</strong>`)
        .addTo(map);
      markerRefs.current.push(marker);
      bounds.extend([place.latitude, place.longitude]);
    });

    map.fitBounds(bounds, { padding: [60, 60], maxZoom: 13 });
  }, [places]);

  useEffect(() => {
    const map = mapInstance.current;
    if (!map) return;

    const drawRoute = (geometry: any) => {
      if (routeLayerRef.current) {
        routeLayerRef.current.remove();
        routeLayerRef.current = null;
      }
      routeLayerRef.current = L.geoJSON(geometry, {
        style: { color: "#4f6cff", weight: 4 },
      }).addTo(map);
    };

    const clearRoute = () => {
      if (routeLayerRef.current) {
        routeLayerRef.current.remove();
        routeLayerRef.current = null;
      }
    };

    if (routeGeoJson) {
      try {
        drawRoute(JSON.parse(routeGeoJson));
      } catch (err) {
        setRouteError("Invalid route data");
      }
      return;
    }

    if (places.length < 2) {
      clearRoute();
      return;
    }

    const coords = places.map((p) => `${p.longitude},${p.latitude}`).join(";");
    const url = `${OSRM_ROUTE_URL}${coords}?geometries=geojson&overview=full`;

    fetch(url)
      .then((res) => res.json())
      .then((data) => {
        if (!data.routes?.length) {
          setRouteError("No route found for selected places");
          return;
        }
        drawRoute(data.routes[0].geometry);
      })
      .catch(() => setRouteError("Failed to load route"));
  }, [places, routeGeoJson]);

  return (
    <div className="space-y-3">
      {routeError && (
        <p className="text-sm text-red-600">{routeError}</p>
      )}
      <div className="h-[420px] rounded-xl overflow-hidden" ref={containerRef} />
    </div>
  );
}
