"use client";

import Link from "next/link";
import { useEffect, useMemo, useState, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import dynamic from "next/dynamic";

const MapView = dynamic(() => import("@/components/MapView"), { ssr: false });
import DownloadItineraryButton from "@/components/DownloadItineraryButton";
import EmergencyCard from "@/components/EmergencyCard";
import ItineraryEditor from "@/components/ItineraryEditor";
import PackingChecklist from "@/components/PackingChecklist";
import ShareTripButton from "@/components/ShareTripButton";
import TripJournal from "@/components/TripJournal";
import TripPhotos from "@/components/TripPhotos";
import TripEssentials from "@/components/TripEssentials";
import TripDate from "@/components/TripDate";
import ProtectedPage from "@/components/ProtectedPage";
import TripProgress from "@/components/TripProgress";
import TripSteps from "@/components/TripSteps";
import { api } from "@/lib/api";
import { fetchRoute } from "@/lib/routeApi";
import { fetchTrip } from "@/lib/tripApi";
import { WeatherDays } from "@/components/WeatherStrip";
import { fetchTripWeather, WeatherForecast } from "@/lib/weatherApi";
import {
  formatCost,
  formatTime,
  isPayable,
  statusMeta,
  tripKind,
  tripTitle,
  type TripLike,
} from "@/lib/tripStatus";

import { CardSkeleton, StopListSkeleton } from "@/components/Skeleton";
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
  const [editing, setEditing] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
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
    setEditing(false);
    setNotice(null);
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

  // Stops grouped by day, in order
  const days = useMemo(() => {
    const byDay = new Map<number, any[]>();
    (summary?.plans ?? []).forEach((plan: any) => {
      const list = byDay.get(plan.dayNumber) ?? [];
      list.push(plan);
      byDay.set(plan.dayNumber, list);
    });
    return Array.from(byDay.entries()).sort((a, b) => a[0] - b[0]);
  }, [summary]);

  // Picker options: the user's trips, plus the one being viewed if it isn't theirs (e.g. a group trip)
  const pickerTrips = useMemo(() => {
    if (summary && !myTrips.some((t) => String(t.tripRequestId) === String(summary.tripRequestId))) {
      return [summary as TripLike, ...myTrips];
    }
    return myTrips;
  }, [myTrips, summary]);

  const hasPlans = days.length > 0;
  const canEdit = summary?.status === "PLANNED" && hasPlans;
  const meta = statusMeta(summary?.status);

  const handleSaved = (updated: any) => {
    setSummary((prev: any) => ({ ...prev, ...updated }));
    setEditing(false);
    setRouteGeoJson(null);
    setRouteError(null);
    const firstDay = Math.min(...((updated?.plans ?? []).map((p: any) => p.dayNumber) as number[]));
    setDayNumber(String(Number.isFinite(firstDay) ? firstDay : 1));
    setNotice(`Itinerary updated. Your estimated cost is now ${formatCost(updated?.estimatedCost)}.`);
  };

  return (
    <ProtectedPage>
      <div className="section py-10 space-y-6">
        <TripSteps
          current={summary?.status === "CONFIRMED" ? 4 : 2}
          tripId={summary?.tripRequestId ?? (tripId || null)}
        />
        {summary && (
          <div className="card p-4">
            <TripProgress status={summary.status} tripDate={summary.tripDate} plans={summary.plans} tripId={summary.tripRequestId} />
          </div>
        )}

        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <h2 className="text-2xl font-bold">Trip Summary</h2>
            <p className="text-sm text-slate-300">Your itinerary, route and weather in one place.</p>
          </div>
          <label className="block w-full max-w-sm space-y-1">
            <span className="text-xs font-semibold uppercase tracking-wide text-slate-500">Trip</span>
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
        </div>

        {!tripId && myTrips.length === 0 && (
          <div className="card p-8 text-center space-y-3">
            <p className="text-slate-300">You don't have any trips to review yet.</p>
            <div className="flex justify-center gap-3">
              <Link href="/trip-planner" className="btn-primary">Plan a trip</Link>
              <Link href="/packages" className="btn-outline">Browse packages</Link>
            </div>
          </div>
        )}

        {loadingTrip && (
          <div className="space-y-3">
            <CardSkeleton rows={2} label="Loading trip" />
            <StopListSkeleton />
          </div>
        )}
        {loadError && (
          <p className="text-sm text-red-400 bg-red-500/10 border border-red-500/20 rounded-xl px-4 py-3" role="alert">
            {loadError}
          </p>
        )}

        {summary && (
          <>
            {/* Overview: what it is, what it costs, what to do next */}
            <div className="card p-6 flex flex-wrap items-start justify-between gap-6">
              <div className="space-y-1">
                <div className="flex flex-wrap items-center gap-2">
                  <h3 className="text-xl font-semibold">{tripTitle(summary)}</h3>
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${meta.badge}`}>{meta.label}</span>
                </div>
                <p className="text-sm text-slate-400">
                  #{summary.tripRequestId} · {tripKind(summary)}{summary.days ? ` · ${summary.days} ${summary.days === 1 ? "day" : "days"}` : ""}
                </p>
                <p className="text-sm text-slate-300">{meta.hint}</p>
                <TripDate trip={summary} onChange={(u: any) => setSummary((prev: any) => ({ ...prev, tripDate: u.tripDate }))} />
              </div>

              <div className="flex flex-col items-start gap-3 sm:items-end">
                <div className="sm:text-right">
                  <p className="text-xs uppercase tracking-wide text-slate-500">Estimated cost</p>
                  <p className="text-3xl font-bold">{formatCost(summary.estimatedCost)}</p>
                </div>
                <div className="flex flex-wrap items-start gap-3">
                  {summary.status !== "CANCELLED" && <DownloadItineraryButton tripId={summary.tripRequestId} label="Download PDF" />}
                  <ShareTripButton token={summary.shareToken} city={summary.city} className="btn btn-secondary" />
                  <Link href="/bookings" className="btn-outline">My trips</Link>
                </div>
              </div>
            </div>

            {isPayable(summary.status) && !editing && (
              <>
                <div className="h-20" aria-hidden="true" />
                <div className="fixed inset-x-0 bottom-14 z-30 border-t border-slate-800 bg-slate-900/95 px-4 py-3 backdrop-blur lg:bottom-0 lg:left-64">
                  <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3">
                    <div>
                      <p className="text-xs text-slate-400">Next: pay to book this trip</p>
                      <p className="text-lg font-bold">{formatCost(summary.estimatedCost)}</p>
                    </div>
                    <div className="flex gap-2">
                      {canEdit && (
                        <button type="button" className="btn btn-secondary" onClick={() => setEditing(true)}>Change places</button>
                      )}
                      <Link href={`/payment?tripRequestId=${summary.tripRequestId}`} className="btn btn-primary">
                        {summary.status === "PAYMENT_PENDING" ? "Continue payment →" : "Continue to payment →"}
                      </Link>
                    </div>
                  </div>
                </div>
              </>
            )}

            {notice && (
              <p
                className="text-sm text-green-300 bg-green-500/10 border border-green-500/20 rounded-xl px-4 py-3"
                role="status"
              >
                {notice}
              </p>
            )}

            {summary.status === "CONFIRMED" && (
              <div className="grid gap-6 lg:grid-cols-2 items-start">
                <PackingChecklist tripId={summary.tripRequestId} trip={{ interest: summary.interest, weekendType: summary.weekendType, days: summary.days, travelMode: summary.travelMode, groupSize: summary.groupSize, pickupRequired: summary.pickupRequired, tripDate: summary.tripDate }} />
                <EmergencyCard tripId={summary.tripRequestId} city={summary.city} />
              </div>
            )}

            {summary.status === "CONFIRMED" && (
              <TripJournal tripId={summary.tripRequestId} city={summary.city} days={Math.max(1, ...((summary.plans || []) as { dayNumber?: number }[]).map((p) => p.dayNumber || 1))} />
            )}

            {summary.status !== "CANCELLED" && <TripEssentials tripId={Number(summary.tripRequestId)} />}

            {summary.status !== "CANCELLED" && <TripPhotos tripId={Number(summary.tripRequestId)} />}

            <div className="grid gap-6 lg:grid-cols-[3fr_2fr] items-start">
              {/* Itinerary */}
              <section className="card p-6 space-y-5">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <h3 className="text-lg font-semibold">Itinerary</h3>
                  {canEdit && !editing && (
                    <button type="button" className="btn-outline" onClick={() => setEditing(true)}>
                      Choose places
                    </button>
                  )}
                </div>

                {editing ? (
                  <ItineraryEditor
                    tripId={Number(summary.tripRequestId)}
                    onSaved={handleSaved}
                    onCancel={() => setEditing(false)}
                  />
                ) : hasPlans ? (
                  <div className="space-y-6">
                    {days.map(([day, stops]) => (
                      <div key={day} className="space-y-3">
                        <p className="text-sm font-semibold uppercase tracking-wide text-brand-400">Day {day}</p>
                        <ol className="space-y-4 border-l border-slate-700 pl-5">
                          {stops.map((plan: any, index: number) => (
                            <li key={`${plan.placeId}-${index}`} className="relative">
                              <span
                                aria-hidden="true"
                                className="absolute -left-[26px] top-1.5 h-2.5 w-2.5 rounded-full bg-brand-500"
                              />
                              <p className="font-medium">{plan.placeName}</p>
                              <p className="text-sm text-slate-400">
                                {formatTime(plan.startTime)} – {formatTime(plan.endTime)}
                                {typeof plan.distanceFromPrevious === "number" &&
                                  ` · ${plan.distanceFromPrevious.toFixed(1)} km ${
                                    index === 0 ? "from your start" : "from previous stop"
                                  }`}
                              </p>
                            </li>
                          ))}
                        </ol>
                      </div>
                    ))}
                    {summary.status === "PLANNED" && (
                      <p className="text-xs text-slate-500">
                        You can change these places until you start paying — the price updates with your choices.
                      </p>
                    )}
                  </div>
                ) : (
                  <p className="text-sm text-slate-400">
                    This is a package booking, so there's no day-by-day itinerary to show here.
                  </p>
                )}
              </section>

              {/* Route, weather, sharing */}
              <div className="space-y-6">
                {hasPlans && (
                  <section className="card p-6 space-y-3">
                    <h3 className="text-lg font-semibold">Route</h3>
                    <div className="flex items-center gap-3">
                      <select
                        value={dayNumber}
                        onChange={(e) => setDayNumber(e.target.value)}
                        className="border rounded-xl px-4 py-2"
                        aria-label="Day"
                      >
                        {days.map(([day]) => (
                          <option key={day} value={day}>Day {day}</option>
                        ))}
                      </select>
                      <button className="btn-primary" onClick={loadRoute}>Load route</button>
                    </div>
                    {routeError && <p className="text-sm text-red-400">{routeError}</p>}
                    <MapView routeGeoJson={routeGeoJson} />
                  </section>
                )}

                {weather && Array.isArray(weather.days) && weather.days.length > 0 && (
                  <section className="card p-5">
                    <WeatherDays days={weather.days} city={weather.city} />
                  </section>
                )}

                {summary.shareToken && (
                  <section className="card p-6 space-y-1">
                    <h3 className="text-lg font-semibold">Share</h3>
                    <a className="text-sm text-brand-400 underline break-all" href={`/share/${summary.shareToken}`}>
                      /share/{summary.shareToken}
                    </a>
                  </section>
                )}
              </div>
            </div>

            {summary.groupId && (
              <div className="card flex flex-wrap items-center justify-between gap-3 p-5">
                <div>
                  <p className="font-semibold">You&apos;re in a travel group</p>
                  <p className="text-sm text-slate-400">See who&apos;s going, chat, and split costs.</p>
                </div>
                <Link href={`/group-status?tripId=${summary.tripRequestId}`} className="btn btn-primary">Open group</Link>
              </div>
            )}
          </>
        )}
      </div>
    </ProtectedPage>
  );
}
