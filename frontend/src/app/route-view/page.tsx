"use client";

import MapView from "@/components/MapView";
import ProtectedPage from "@/components/ProtectedPage";
import { fetchRoute } from "@/lib/routeApi";
import { useState } from "react";

export default function RouteViewPage() {
  const [tripId, setTripId] = useState("");
  const [dayNumber, setDayNumber] = useState("1");
  const [route, setRoute] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleFetch = async () => {
    setError(null);
    try {
      const data = await fetchRoute(Number(tripId), Number(dayNumber));
      setRoute(data.geoJson);
    } catch (err) {
      setError("Failed to fetch route");
    }
  };

  return (
    <ProtectedPage>
      <div className="section py-12 space-y-6">
        <div className="card p-6 space-y-4">
        <h2 className="text-2xl font-bold">Trip Route</h2>
        <div className="flex gap-3">
          <input
            value={tripId}
            onChange={(e) => setTripId(e.target.value)}
            placeholder="Trip Request ID"
            className="border rounded-xl px-4 py-2"
          />
          <input
            value={dayNumber}
            onChange={(e) => setDayNumber(e.target.value)}
            placeholder="Day"
            className="border rounded-xl px-4 py-2"
          />
          <button className="btn-primary" onClick={handleFetch}>Fetch Route</button>
        </div>
        {error && <p className="text-sm text-red-600">{error}</p>}
      </div>

        <div className="card p-4">
          <MapView routeGeoJson={route} />
        </div>
      </div>
    </ProtectedPage>
  );
}
