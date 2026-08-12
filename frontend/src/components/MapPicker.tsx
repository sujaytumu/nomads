"use client";

import { useEffect, useRef, useMemo } from "react";
import mapboxgl from "mapbox-gl";

type Props = {
  initialCenter?: [number, number];
  onSet: (coords: { latitude: number; longitude: number }) => void;
};

export default function MapPicker({ initialCenter = [72.8777, 19.076], onSet }: Props) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<mapboxgl.Map | null>(null);
  const markerRef = useRef<mapboxgl.Marker | null>(null);
  const mapToken = useMemo(() => process.env.NEXT_PUBLIC_MAPBOX_TOKEN ?? "", []);

  useEffect(() => {
    if (!containerRef.current || !mapToken || mapRef.current) return;

    mapboxgl.accessToken = mapToken;

    const map = new mapboxgl.Map({
      container: containerRef.current,
      style: "mapbox://styles/mapbox/streets-v11",
      center: initialCenter,
      zoom: 12,
    });
    mapRef.current = map;

    map.on("click", (e: mapboxgl.MapMouseEvent) => {
      const { lng, lat } = e.lngLat;
      if (!markerRef.current) {
        markerRef.current = new mapboxgl.Marker().setLngLat([lng, lat]).addTo(map);
      } else {
        markerRef.current.setLngLat([lng, lat]);
      }
      onSet({ latitude: lat, longitude: lng });
    });

    return () => {
      mapRef.current?.remove();
      mapRef.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mapToken]);

  if (!mapToken) {
    return (
      <div className="border rounded-xl p-4 text-sm text-slate-500 bg-gray-50">
        Map picker needs NEXT_PUBLIC_MAPBOX_TOKEN to be set. Use the &quot;Use my location&quot; button above instead for now.
      </div>
    );
  }

  return <div ref={containerRef} style={{ width: "100%", height: 320 }} />;
}
