"use client";

import { useEffect, useRef } from "react";
import mapboxgl from "mapbox-gl";

mapboxgl.accessToken = process.env.NEXT_PUBLIC_MAPBOX_TOKEN || "";

type Props = {
  initialCenter?: [number, number];
  onSet: (coords: { latitude: number; longitude: number }) => void;
};

export default function MapPicker({ initialCenter = [72.8777, 19.076], onSet }: Props) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<mapboxgl.Map | null>(null);
  const markerRef = useRef<mapboxgl.Marker | null>(null);

  useEffect(() => {
    if (!containerRef.current) return;
    mapRef.current = new mapboxgl.Map({
      container: containerRef.current,
      style: "mapbox://styles/mapbox/streets-v11",
      center: initialCenter,
      zoom: 12,
    });

    mapRef.current.on("click", (e) => {
      const { lng, lat } = e.lngLat;
      if (!markerRef.current) {
        markerRef.current = new mapboxgl.Marker().setLngLat([lng, lat]).addTo(mapRef.current!);
      } else {
        markerRef.current.setLngLat([lng, lat]);
      }
      onSet({ latitude: lat, longitude: lng });
    });

    return () => mapRef.current?.remove();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [containerRef.current]);

  return <div ref={containerRef} style={{ width: "100%", height: 320 }} />;
}
