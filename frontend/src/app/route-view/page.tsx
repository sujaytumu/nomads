"use client";

import dynamic from "next/dynamic";
import { useState } from "react";

const MapView = dynamic(() => import("@/components/MapView"), { ssr: false });
import ProtectedPage from "@/components/ProtectedPage";
import { fetchRoute } from "@/lib/routeApi";

const DAY_OPTIONS = ["1", "2"];
const MODE_OPTIONS: { value: "driving" | "walking"; label: string; icon: string }[] = [
  { value: "driving", label: "Driving", icon: "🚗" },
  { value: "walking", label: "Walking", icon: "🚶" },
];

export default function RouteViewPage() {
  const [tripId, setTripId] = useState("");
  const [dayNumber, setDayNumber] = useState("1");
  const [mode, setMode] = useState<"driving" | "walking">("driving");
  const [route, setRoute] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleFetch = async () => {
    if (!tripId) {
      setError("Enter a Trip ID first");
      return;
    }
    setError(null);
    setLoading(true);
    setRoute(null);
    try {
      const data = await fetchRoute(Number(tripId), Number(dayNumber), mode);
      setRoute(data.geoJson);
    } catch (err) {
      setError("Failed to fetch route - the trip may not have enough places, or the server is waking up.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <ProtectedPage>
      <div className="section py-12 space-y-6">
        <div>
          <h2 className="text-3xl font-bold tracking-tight">Trip Route</h2>
          <p className="text-slate-400 mt-1">Visualize the day-by-day path between your planned stops.</p>
        </div>

        <div className="card p-6 space-y-6">
          <div className="grid md:grid-cols-[1fr_auto] gap-6 items-end">
            <label className="space-y-2 block">
              <span className="text-sm font-semibold text-slate-300">Trip ID</span>
              <input
                value={tripId}
                onChange={(e) => setTripId(e.target.value)}
                placeholder="e.g. 1"
                className="w-full border rounded-xl px-4 py-3 text-lg"
              />
            </label>

            <button
              className="btn-primary h-[50px] px-8 whitespace-nowrap"
              onClick={handleFetch}
              disabled={loading}
            >
              {loading ? "Loading route…" : "Fetch Route"}
            </button>
          </div>

          <div className="grid sm:grid-cols-2 gap-6">
            <div className="space-y-2">
              <span className="text-sm font-semibold text-slate-300">Day</span>
              <div className="flex gap-2">
                {DAY_OPTIONS.map((d) => (
                  <button
                    key={d}
                    onClick={() => setDayNumber(d)}
                    className={`flex-1 rounded-xl px-4 py-2.5 font-medium border transition ${
                      dayNumber === d
                        ? "bg-brand-600 border-brand-600 text-white shadow-md shadow-brand-900/40"
                        : "border-slate-700 text-slate-300 hover:border-slate-600 hover:bg-slate-800"
                    }`}
                  >
                    Day {d}
                  </button>
                ))}
              </div>
            </div>

            <div className="space-y-2">
              <span className="text-sm font-semibold text-slate-300">Travel Mode</span>
              <div className="flex gap-2">
                {MODE_OPTIONS.map((m) => (
                  <button
                    key={m.value}
                    onClick={() => setMode(m.value)}
                    className={`flex-1 rounded-xl px-4 py-2.5 font-medium border transition ${
                      mode === m.value
                        ? "bg-brand-600 border-brand-600 text-white shadow-md shadow-brand-900/40"
                        : "border-slate-700 text-slate-300 hover:border-slate-600 hover:bg-slate-800"
                    }`}
                  >
                    <span className="mr-1.5">{m.icon}</span>
                    {m.label}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {error && (
            <p className="text-sm text-red-400 bg-red-500/10 border border-red-500/20 rounded-xl px-4 py-3">{error}</p>
          )}
        </div>

        <div className="card p-4 overflow-hidden">
          {!route && !loading && (
            <div className="h-[420px] flex flex-col items-center justify-center text-center gap-2 text-slate-500">
              <span className="text-4xl">🗺️</span>
              <p className="font-medium text-slate-400">No route loaded yet</p>
              <p className="text-sm max-w-xs">Enter a Trip ID above and click Fetch Route to see the path between stops.</p>
            </div>
          )}
          {loading && (
            <div className="h-[420px] flex flex-col items-center justify-center gap-3 text-slate-400">
              <div className="w-8 h-8 border-2 border-brand-500 border-t-transparent rounded-full animate-spin" />
              <p className="text-sm">Fetching route… (may take up to a minute if the server was asleep)</p>
            </div>
          )}
          {route && !loading && <MapView routeGeoJson={route} />}
        </div>
      </div>
    </ProtectedPage>
  );
}
