"use client";

import dynamic from "next/dynamic";
import { Suspense, useEffect, useState } from "react";

import PageHeader from "@/components/PageHeader";
import ProtectedPage from "@/components/ProtectedPage";
import TripPicker from "@/components/TripPicker";
import { fetchRoute } from "@/lib/routeApi";
import { useTripSelection } from "@/lib/useTripSelection";

const MapView = dynamic(() => import("@/components/MapView"), { ssr: false });

const MODES: { value: "driving" | "walking"; label: string }[] = [
  { value: "driving", label: "Driving" },
  { value: "walking", label: "Walking" },
];

export default function RouteViewPage() {
  return (
    <Suspense fallback={null}>
      <RouteInner />
    </Suspense>
  );
}

function RouteInner() {
  // A route needs stops, so only trips with an itinerary are offered.
  const sel = useTripSelection((t) => t.status !== "CANCELLED" && !!t.plans && t.plans.length > 0);
  const [day, setDay] = useState(1);
  const [mode, setMode] = useState<"driving" | "walking">("driving");
  const [route, setRoute] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const days = sel.selected
    ? Array.from(new Set((sel.selected.plans || []).map((p: any) => Number(p.dayNumber)))).sort()
    : [];
  const id = sel.selected?.tripRequestId;

  useEffect(() => {
    setDay(1);
  }, [id]);

  // The route loads by itself whenever the trip, day or mode changes.
  useEffect(() => {
    setRoute(null);
    setError(null);
    if (!id) return;
    let live = true;
    setLoading(true);
    fetchRoute(Number(id), day, mode)
      .then((d) => live && setRoute(d.geoJson))
      .catch(() => live && setError("Couldn't load this route. It needs at least two stops on that day, or the server may be waking up."))
      .finally(() => live && setLoading(false));
    return () => {
      live = false;
    };
  }, [id, day, mode]);

  const pill = (on: boolean) =>
    `btn btn-sm ${on ? "btn-primary" : "btn-secondary"}`;

  return (
    <ProtectedPage>
      <div className="section space-y-6 py-8">
        <PageHeader title="Trip route" subtitle="The path between your planned stops, day by day." />
        <div className="card space-y-5 p-6">
          <TripPicker trips={sel.trips} selected={sel.selected} onSelect={sel.select} loading={sel.loading} error={sel.error} onRetry={sel.reload} emptyText="No trip with an itinerary yet. Plan a trip and its route will show up here." />
          {sel.selected && (
            <div className="flex flex-wrap gap-6">
              <div className="space-y-1.5">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Day</p>
                <div className="flex gap-2">
                  {(days.length ? days : [1]).map((d) => (
                    <button key={d} type="button" className={pill(day === d)} onClick={() => setDay(d)}>Day {d}</button>
                  ))}
                </div>
              </div>
              <div className="space-y-1.5">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Travel mode</p>
                <div className="flex gap-2">
                  {MODES.map((m) => (
                    <button key={m.value} type="button" className={pill(mode === m.value)} onClick={() => setMode(m.value)}>{m.label}</button>
                  ))}
                </div>
              </div>
            </div>
          )}
          {error && <p role="alert" className="rounded-xl border border-red-500/20 bg-red-500/10 px-4 py-3 text-sm text-red-400">{error}</p>}
        </div>

        {sel.selected && (
          <div className="card overflow-hidden p-4">
            {loading && <div className="flex h-[420px] items-center justify-center text-sm text-slate-400">Loading route…</div>}
            {route && !loading && <MapView routeGeoJson={route} />}
          </div>
        )}
      </div>
    </ProtectedPage>
  );
}
