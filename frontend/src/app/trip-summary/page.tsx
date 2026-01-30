"use client";

import { useMemo, useState } from "react";

import MapView from "@/components/MapView";
import ProtectedPage from "@/components/ProtectedPage";
import { fetchRoute } from "@/lib/routeApi";
import { fetchTrip } from "@/lib/tripApi";

export default function TripSummaryPage() {
  const [tripId, setTripId] = useState("");
  const [summary, setSummary] = useState<any>(null);
  const [dayNumber, setDayNumber] = useState("1");
  const [routeGeoJson, setRouteGeoJson] = useState<string | null>(null);
  const [routeError, setRouteError] = useState<string | null>(null);

  const loadTrip = async () => {
    try {
      const data = await fetchTrip(Number(tripId));
      setSummary(data);
      setRouteGeoJson(null);
    } catch (error) {
      setSummary(null);
    }
  };

  const loadRoute = async () => {
    setRouteError(null);
    try {
      const data = await fetchRoute(Number(tripId), Number(dayNumber));
      setRouteGeoJson(data.geoJson);
    } catch (err) {
      setRouteError("Failed to load route");
    }
  };

  const dayOptions = useMemo(() => {
    if (!summary?.plans?.length) return [1];
    const days = new Set<number>();
    summary.plans.forEach((plan: any) => days.add(plan.dayNumber));
    return Array.from(days).sort();
  }, [summary]);

  return (
    <ProtectedPage>
      <div className="section py-12 space-y-6">
        <div className="card p-6 space-y-4">
        <h2 className="text-2xl font-bold">Trip Summary</h2>
        <div className="flex gap-3">
          <input
            value={tripId}
            onChange={(e) => setTripId(e.target.value)}
            placeholder="Trip Request ID"
            className="border rounded-xl px-4 py-2"
          />
          <button className="btn-primary" onClick={loadTrip}>Fetch</button>
        </div>
        {summary && (
          <div className="space-y-4">
            <div className="card p-4 bg-slate-50">
              <p className="text-sm text-slate-600">Trip ID</p>
              <p className="font-semibold">{summary.tripRequestId}</p>
              <p className="text-sm text-slate-600">Status</p>
              <p className="font-semibold">{summary.status}</p>
              <p className="text-sm text-slate-600">Estimated Cost</p>
              <p className="font-semibold">₹ {summary.estimatedCost}</p>
              {summary.shareToken && (
                <div className="pt-2">
                  <p className="text-sm text-slate-600">Share Link</p>
                  <a
                    className="text-sm text-brand-700 underline"
                    href={`/share/${summary.shareToken}`}
                  >
                    /share/{summary.shareToken}
                  </a>
                </div>
              )}
            </div>
            <div className="card p-4 space-y-3">
              <div className="flex items-center gap-3">
                <select
                  value={dayNumber}
                  onChange={(e) => setDayNumber(e.target.value)}
                  className="border rounded-xl px-4 py-2"
                >
                  {dayOptions.map((day: number) => (
                    <option key={day} value={day}>Day {day}</option>
                  ))}
                </select>
                <button className="btn-primary" onClick={loadRoute}>Load Route</button>
              </div>
              {routeError && <p className="text-sm text-red-600">{routeError}</p>}
              <MapView routeGeoJson={routeGeoJson} />
            </div>
            <div className="grid gap-3">
              {summary.plans?.map((plan: any, index: number) => (
                <div key={`${plan.placeId}-${index}`} className="card p-4">
                  <p className="font-semibold">Day {plan.dayNumber} - {plan.placeName}</p>
                  <p className="text-sm text-slate-600">{plan.startTime} - {plan.endTime}</p>
                </div>
              ))}
            </div>
          </div>
        )}
        </div>
      </div>
    </ProtectedPage>
  );
}
