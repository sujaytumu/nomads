"use client";

import { useEffect, useRef } from "react";
import L from "leaflet";
import "@/lib/leafletIconFix";

type Props = {
  initialCenter?: [number, number]; // [lng, lat] to match the old Mapbox-style call sites
  onSet: (coords: { latitude: number; longitude: number }) => void;
};

// Uses OpenStreetMap tiles via Leaflet - completely free, no API key, no signup,
// no payment method ever required. Replaces the previous Mapbox GL implementation.
export default function MapPicker({ initialCenter = [72.8777, 19.076], onSet }: Props) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<L.Map | null>(null);
  const markerRef = useRef<L.Marker | null>(null);

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;

    const [lng, lat] = initialCenter;
    const map = L.map(containerRef.current).setView([lat, lng], 12);
    mapRef.current = map;

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 19,
    }).addTo(map);

    map.on("click", (e: L.LeafletMouseEvent) => {
      const { lat, lng } = e.latlng;
      if (!markerRef.current) {
        markerRef.current = L.marker([lat, lng]).addTo(map);
      } else {
        markerRef.current.setLatLng([lat, lng]);
      }
      onSet({ latitude: lat, longitude: lng });
    });

    return () => {
      mapRef.current?.remove();
      mapRef.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return <div ref={containerRef} style={{ width: "100%", height: 320 }} />;
}
