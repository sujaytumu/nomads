"use client";

import Link from "next/link";
import { useEffect, useMemo, useState, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import dynamic from "next/dynamic";

const MapView = dynamic(() => import("@/components/MapView"), { ssr: false });
const GroupChat = dynamic(() => import("@/components/GroupChat"), { ssr: false });
import ProtectedPage from "@/components/ProtectedPage";
import TripSteps from "@/components/TripSteps";
import { api } from "@/lib/api";
import { fetchRoute } from "@/lib/routeApi";
import { fetchTrip } from "@/lib/tripApi";
import { fetchTripWeather, WeatherForecast } from "@/lib/weatherApi";
import { formatCost, isPayable, statusMeta, tripKind, tripTitle, type TripLike } from "@/lib/tripStatus";

export default function TripSummaryPage() {
  return (
    <Suspense fallback={null}>
      <TripSummaryPageInner />
    </Suspense>
  );
}

function TripSummaryPageInner() {
  const router = useRouter();
  const searchParams = useSearchParams();
  // The trip being viewed lives in the URL (?tripId=X) so links from the
  // planner, payment page and My Trips all land on the right trip.
  const tripId = searchParams?.get("tripId") ?? "";

  const [myTrips, setMyTrips] = useState<TripLike[]>([]);
  const [summary, setSummary] = useState<any>(null);
  const [loadingTrip, setLoadingTrip] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [dayNumber, setDayNumber] = useState("1");
  const [routeGeoJson, setRouteGeoJson] = useState<string | null>(null);
  const [routeError, setRouteError] = useState<string | null>(null);
  const [weather, setWeather] = useState<WeatherForecast | null>(null);

  // Trips for the picker
  useEffect(() => {
    api
      .get("/api/trips/me")
      .then((res) => setMyTrips(res.data || []))
      .catch(() => setMyTrips([]));
  }, []);

  // Load whichever trip the URL points at
  useEffect(() => {
    if (!tripId) {
      setSummary(null);
      setLoadError(null);
      setLoadingTrip(false);
      return;
    }
    let active = true;
    setLoadingTrip(true);
    setLoadError(null);
    setRouteGeoJson(null);
    setRouteError(null);
    setWeather(null);
    fetchTrip(Number(tripId))
      .then((data) => {
        if (!active) return;
        setSummary(data);
        const days: number[] = (data?.plans ?? []).map((p: any) => p.dayNumber);
        setDayNumber(String(days.length ? Math.min(...days) : 1));
        // Weather is a nice-to-have - if it fails, the trip summary itself
        // still loaded fine, so fail quietly rather than showing an error
        // for something that isn't core to the booking.
        fetchTripWeather(Number(tripId))
          .then((w) => active && setWeather(w))
          .catch(() => active && setWeather(null));
      })
      .catch(() => {
        if (!active) return;
        setSummary(null);
        setLoadError(`Couldn't load trip #${tripId}. It may not exist, or it belongs to another account.`);
      })
      .finally(() => active && setLoadingTrip(false));
    return () => {
      active = false;
    };
  }, [tripId]);

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
    return Array.from(days).sort((a, b) => a - b);
  }, [summary]);

  // Picker options: the user's trips, plus the one being viewed if it isn't theirs (e.g. a group trip)
  const pickerTrips = useMemo(() => {
    if (summary && !myTrips.some((t) => String(t.tripRequestId) === String(summary.tripRequestId))) {
      return [summary as TripLike, ...myTrips];
    }
    return myTrips;
  }, [myTrips, summary]);

  const hasPlans = !!summary?.plans?.length;
  const meta = statusMeta(summary?.status);

  return (
    <ProtectedPage>
      <div className="section py-12 space-y-6">
        <div className="card p-6 space-y-5">
          <TripSteps
            current={summary?.status === "CONFIRMED" ? 4 : 2}
            tripId={summary?.tripRequestId ?? (tripId || null)}
          />
          <div>
            <h2 className="text-2xl font-bold">Trip Summary</h2>
            <p className="text-sm text-slate-300">Review a trip's itinerary, route and weather.</p>
          </div>

          <label className="block space-y-2 max-w-xl">
            <span className="text-sm font-semibold">Trip</span>
            <select
              value={tripId}
              onChange={(e) => {
                const v = e.target.value;
                router.replace(v ? `/trip-summary?tripId=${v}` : "/trip-summary");
              }}
              className="w-full border rounded-xl px-4 py-2"
            >
              <option value="">-- Choose a trip --</option>
              {pickerTrips.map((t) => (
                <option key={t.tripRequestId} value={String(t.tripRequestId)}>
                  {`#${t.tripRequestId} — ${tripTitle(t)} · ${statusMeta(t.status).label}`}
                </option>
              ))}
            </select>
          </label>

          {!tripId && myTrips.length === 0 && (
            <div className="rounded-xl border border-slate-800 p-6 text-center space-y-3">
              <p className="text-slate-300">You don't have any trips to review yet.</p>
              <div className="flex justify-center gap-3">
                <Link href="/trip-planner" className="btn-primary">Plan a trip</Link>
                <Link href="/packages" className="btn-outline">Browse packages</Link>
              </div>
            </div>
          )}

          {loadingTrip && <p className="text-sm text-slate-400">Loading trip…</p>}
          {loadError && (
            <p className="text-sm text-red-400 bg-red-500/10 border border-red-500/20 rounded-xl px-4 py-3" role="alert">
              {loadError}
            </p>
          )}

          {summary && (
            <div className="space-y-4">
              <div className="card p-4 space-y-4">
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div className="space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="text-lg font-semibold">{tripTitle(summary)}</p>
                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${meta.badge}`}>{meta.label}</span>
                    </div>
                    <p className="text-sm text-slate-400">
                      #{summary.tripRequestId} · {tripKind(summary)}
                    </p>
                    <p className="text-sm text-slate-300">{meta.hint}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-xs uppercase tracking-wide text-slate-500">Estimated cost</p>
                    <p className="text-2xl font-bold">{formatCost(summary.estimatedCost)}</p>
                  </div>
                </div>

                <div className="flex flex-wrap items-center gap-3">
                  {isPayable(summary.status) && (
                    <Link href={`/payment?tripRequestId=${summary.tripRequestId}`} className="btn-primary">
                      {summary.status === "PAYMENT_PENDING" ? "Continue payment" : "Pay & book"}
                    </Link>
                  )}
                  <Link href="/bookings" className="btn-outline">My trips</Link>
                </div>

                {summary.shareToken && (
                  <div>
                    <p className="text-sm text-slate-300">Share Link</p>
                    <a className="text-sm text-brand-400 underline" href={`/share/${summary.shareToken}`}>
                      /share/{summary.shareToken}
                    </a>
                  </div>
                )}
              </div>

              {hasPlans && (
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
                  {routeError && <p className="text-sm text-red-400">{routeError}</p>}
                  <MapView routeGeoJson={routeGeoJson} />
                </div>
              )}

              {weather && weather.days.length > 0 && (
                <div className="card p-4">
                  <p className="font-semibold mb-2">Weather in {weather.city}</p>
                  <div className="grid grid-cols-3 gap-3">
                    {weather.days.map((day) => (
                      <div key={day.date} className="border rounded-xl p-3 text-center">
                        <p className="text-xs text-slate-400">{day.date}</p>
                        <p className="text-sm font-semibold mt-1">{day.description}</p>
                        <p className="text-sm text-slate-300">{Math.round(day.minTempC)}°–{Math.round(day.maxTempC)}°C</p>
                        <p className="text-xs text-slate-400">{day.precipitationChance}% rain</p>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {summary.groupId && <GroupChat groupId={summary.groupId} />}

              {hasPlans ? (
                <div className="grid gap-3">
                  {summary.plans.map((plan: any, index: number) => (
                    <div key={`${plan.placeId}-${index}`} className="card p-4">
                      <p className="font-semibold">Day {plan.dayNumber} - {plan.placeName}</p>
                      <p className="text-sm text-slate-300">{plan.startTime} - {plan.endTime}</p>
                      {typeof plan.distanceFromPrevious === "number" && (
                        <p className="text-xs text-slate-400">{plan.distanceFromPrevious.toFixed(1)} km from previous stop</p>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-slate-400">
                  This is a package booking, so there's no day-by-day itinerary to show here.
                </p>
              )}
            </div>
          )}
        </div>
      </div>
    </ProtectedPage>
  );
}
