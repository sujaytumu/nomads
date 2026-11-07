"use client";

/** Opens the place in the phone's maps app (or Google Maps in a browser). */
export default function DirectionsButton({ lat, lon, name }: { lat?: number | null; lon?: number | null; name: string }) {
  if (typeof lat !== "number" || typeof lon !== "number" || (lat === 0 && lon === 0)) return null;
  const href = `https://www.google.com/maps/dir/?api=1&destination=${lat},${lon}`;
  return (
    <a href={href} target="_blank" rel="noopener noreferrer" className="btn btn-secondary" aria-label={`Directions to ${name}`}>
      Directions
    </a>
  );
}
