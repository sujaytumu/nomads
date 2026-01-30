"use client";

import mapboxgl from "mapbox-gl";
import { useEffect, useMemo, useRef, useState } from "react";

import type { PlaceNearby } from "@/lib/placeApi";

type MapViewProps = {
  places?: PlaceNearby[];
  center?: [number, number];
  routeGeoJson?: string | null;
};

export default function MapView({ places = [], center, routeGeoJson }: MapViewProps) {
  const mapRef = useRef<HTMLDivElement | null>(null);
  const mapInstance = useRef<any>(null);
  const markerRefs = useRef<any[]>([]);
  const mapToken = useMemo(() => process.env.NEXT_PUBLIC_MAPBOX_TOKEN ?? "", []);
  const [routeError, setRouteError] = useState<string | null>(null);

  useEffect(() => {
    if (!mapRef.current || !mapToken || mapInstance.current) return;

    mapboxgl.accessToken = mapToken;

    const map = new mapboxgl.Map({
      container: mapRef.current,
      style: "mapbox://styles/mapbox/streets-v12",
      center: center ?? [77.5946, 12.9716],
      zoom: 10,
    });

    mapInstance.current = map;

    return () => {
      markerRefs.current.forEach((marker) => marker.remove());
      markerRefs.current = [];
      map.remove();
      mapInstance.current = null;
    };
  }, [mapToken, center]);

  useEffect(() => {
    if (!mapInstance.current) return;
    markerRefs.current.forEach((marker) => marker.remove());
    markerRefs.current = [];
    setRouteError(null);

    if (!places.length) return;

    const bounds = new mapboxgl.LngLatBounds();
    places.forEach((place) => {
      const marker = new mapboxgl.Marker({ color: "#4f6cff" })
        .setLngLat([place.longitude, place.latitude])
        .setPopup(new mapboxgl.Popup().setHTML(`<strong>${place.name}</strong>`))
        .addTo(mapInstance.current as any);
      markerRefs.current.push(marker);
      bounds.extend([place.longitude, place.latitude]);
    });

    mapInstance.current.fitBounds(bounds, { padding: 60, maxZoom: 13 });
  }, [places]);

  useEffect(() => {
    const map = mapInstance.current;
    if (!map || !mapToken) return;
    if (routeGeoJson) {
      try {
        const geometry = JSON.parse(routeGeoJson);
        if (map.getLayer("route-line")) {
          map.removeLayer("route-line");
        }
        if (map.getSource("route")) {
          map.removeSource("route");
        }
        map.addSource("route", {
          type: "geojson",
          data: {
            type: "Feature",
            geometry,
          },
        });
        map.addLayer({
          id: "route-line",
          type: "line",
          source: "route",
          layout: {
            "line-join": "round",
            "line-cap": "round",
          },
          paint: {
            "line-color": "#4f6cff",
            "line-width": 4,
          },
        });
        return;
      } catch (err) {
        setRouteError("Invalid route data");
      }
    }
    if (places.length < 2) {
      if (map.getLayer("route-line")) {
        map.removeLayer("route-line");
      }
      if (map.getSource("route")) {
        map.removeSource("route");
      }
      return;
    }

    const coords = places.map((p) => `${p.longitude},${p.latitude}`).join(";");
    const url = `https://api.mapbox.com/directions/v5/mapbox/driving/${coords}?geometries=geojson&access_token=${mapToken}`;

    fetch(url)
      .then((res) => res.json())
      .then((data) => {
        if (!data.routes?.length) {
          setRouteError("No route found for selected places");
          return;
        }
        const route = data.routes[0].geometry;

        if (map.getLayer("route-line")) {
          map.removeLayer("route-line");
        }
        if (map.getSource("route")) {
          map.removeSource("route");
        }

        map.addSource("route", {
          type: "geojson",
          data: {
            type: "Feature",
            geometry: route,
          },
        });

        map.addLayer({
          id: "route-line",
          type: "line",
          source: "route",
          layout: {
            "line-join": "round",
            "line-cap": "round",
          },
          paint: {
            "line-color": "#4f6cff",
            "line-width": 4,
          },
        });
      })
      .catch(() => setRouteError("Failed to load route"));
  }, [places, mapToken, routeGeoJson]);

  return (
    <div className="space-y-3">
      {!mapToken && (
        <p className="text-sm text-slate-500">
          Set NEXT_PUBLIC_MAPBOX_TOKEN in frontend/.env.local to enable the map.
        </p>
      )}
      {routeError && (
        <p className="text-sm text-red-600">{routeError}</p>
      )}
      <div className="h-[420px] rounded-xl overflow-hidden" ref={mapRef} />
    </div>
  );
}
