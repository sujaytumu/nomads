"use client";

import Link from "next/link";
import { Suspense, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";

import DownloadItineraryButton from "@/components/DownloadItineraryButton";
import ProtectedPage from "@/components/ProtectedPage";
import { api } from "@/lib/api";
import { cancelBooking } from "@/lib/paymentApi";
import {
  formatCost,
  isCancellable,
  isPayable,
  statusMeta,
  tripKind,
  tripTitle,
  type TripLike,
} from "@/lib/tripStatus";

type Trip = TripLike & {
  tripRequestId: number;
  createdAt: string;
  shareToken?: string;
};

// Grouped by what the user needs to do next, not by raw backend status.
const SECTIONS: { key: string; title: string; subtitle: string; match: (s: string) => boolean }[] = [
  {
    key: "pay",
    title: "Needs payment",
    subtitle: "Planned or started, but not booked until you pay.",
    match: isPayable,
  },
  {
    key: "booked",
    title: "Booked",
    subtitle: "Paid and confirmed.",
    match: (s) => s === "CONFIRMED",
  },
  {
    key: "cancelled",
    title: "Cancelled",
    subtitle: "No longer active.",
    match: (s) => s === "CANCELLED",
  },
];

export default function MyTripsPage() {
  return (
    <Suspense fallback={null}>
      <MyTripsPageInner />
    </Suspense>
  );
}

function MyTripsPageInner() {
  const searchParams = useSearchParams();
  // Set by the payment page right after a successful payment: ?paid=<tripId>
  const [paidId, setPaidId] = useState<number | null>(null);
  const [trips, setTrips] = useState<Trip[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [cancellingId, setCancellingId] = useState<number | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    setLoadError(false);
    api
      .get("/api/trips/me")
      .then((res) => setTrips(res.data || []))
      .catch(() => setLoadError(true))
      .finally(() => setLoading(false));
  };

  const handleCancel = async (trip: Trip) => {
    const message =
      trip.status === "CONFIRMED"
        ? "Cancel this booking? Your payment will be refunded automatically."
        : "Cancel this trip? The pending payment will be dropped.";
    if (!confirm(message)) return;
    setActionError(null);
    setCancellingId(trip.tripRequestId);
    try {
      await cancelBooking(trip.tripRequestId);
      load();
    } catch (err: any) {
      setActionError(err?.response?.data?.message || "Failed to cancel the trip");
    } finally {
      setCancellingId(null);
    }
  };

  useEffect(() => {
    load();
  }, []);

  // Remember the just-paid trip for the banner, then drop the parameter from
  // the address so a refresh doesn't show the banner again.
  useEffect(() => {
    const paid = Number(searchParams?.get("paid"));
    if (paid > 0) {
      setPaidId(paid);
      window.history.replaceState(null, "", "/bookings");
    }
  }, [searchParams]);

  const paidTrip = paidId ? trips.find((t) => t.tripRequestId === paidId) : undefined;

  const grouped = useMemo(() => {
    const newestFirst = [...trips].sort(
      (a, b) => new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
    );
    return SECTIONS.map((section) => ({
      ...section,
      items: newestFirst.filter((t) => section.match(t.status)),
    })).filter((section) => section.items.length > 0);
  }, [trips]);

  return (
    <ProtectedPage>
      <div className="section py-12 space-y-6">
        <div className="card p-6 space-y-4">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h2 className="text-2xl font-bold">My Trips</h2>
              <p className="text-sm text-slate-300">
                Every trip you've planned or booked. A trip is only booked once it's paid.
              </p>
            </div>
            <div className="flex flex-wrap gap-3">
              <Link href="/trip-planner" className="btn-primary">Plan a trip</Link>
              <Link href="/packages" className="btn-outline">Browse packages</Link>
            </div>
          </div>

          {loading && (
            <p className="text-sm text-slate-400">
              Loading your trips… (may take up to a minute if the server was asleep)
            </p>
          )}

          {!loading && loadError && (
            <div className="space-y-2">
              <p className="text-sm text-red-400">Couldn't load your trips.</p>
              <button className="btn-outline" onClick={load}>Retry</button>
            </div>
          )}

          {!loading && !loadError && trips.length === 0 && (
            <div className="rounded-xl border border-slate-800 p-6 text-center space-y-3">
              <p className="text-slate-300">You haven't planned or booked anything yet.</p>
              <div className="flex justify-center gap-3">
                <Link href="/trip-planner" className="btn-primary">Plan a trip</Link>
                <Link href="/packages" className="btn-outline">Browse packages</Link>
              </div>
            </div>
          )}

          {actionError && (
            <p className="text-sm text-red-400 bg-red-500/10 border border-red-500/20 rounded-xl px-4 py-3">
              {actionError}
            </p>
          )}
        </div>

        {paidId && (
          <div
            className="rounded-2xl border border-green-500/30 bg-green-500/10 p-5 flex flex-wrap items-center justify-between gap-4"
            role="status"
          >
            <div>
              <p className="text-lg font-semibold text-green-300">Payment received — your trip is booked 🎉</p>
              <p className="text-sm text-slate-300">
                {paidTrip
                  ? `${tripTitle(paidTrip)} (#${paidTrip.tripRequestId}) · ${formatCost(paidTrip.estimatedCost)}. A confirmation is on its way to you.`
                  : `Trip #${paidId} is now confirmed. A confirmation is on its way to you.`}
              </p>
            </div>
            <div className="flex flex-wrap items-start gap-3">
              {paidTrip && tripKind(paidTrip) === "Custom itinerary" && (
                <DownloadItineraryButton tripId={paidId} className="btn-primary" />
              )}
              <Link href={`/trip-summary?tripId=${paidId}`} className="btn-outline">View trip</Link>
              <button type="button" className="btn-outline" onClick={() => setPaidId(null)}>Dismiss</button>
            </div>
          </div>
        )}

        {!loading &&
          !loadError &&
          grouped.map((section) => (
            <section key={section.key} className="space-y-3">
              <div>
                <h3 className="text-lg font-semibold">
                  {section.title} <span className="text-slate-500 font-normal">({section.items.length})</span>
                </h3>
                <p className="text-sm text-slate-400">{section.subtitle}</p>
              </div>

              <div className="grid gap-3">
                {section.items.map((trip) => {
                  const meta = statusMeta(trip.status);
                  return (
                    <div
                      key={trip.tripRequestId}
                      className={`card p-4 flex flex-wrap items-center justify-between gap-4 ${
                        trip.tripRequestId === paidId ? "ring-2 ring-green-500/50" : ""
                      }`}
                    >
                      <div className="space-y-1 min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <p className="font-semibold">{tripTitle(trip)}</p>
                          <span className={`px-2 py-1 rounded-full text-xs font-medium ${meta.badge}`}>{meta.label}</span>
                        </div>
                        <p className="text-sm text-slate-400">
                          #{trip.tripRequestId} · {tripKind(trip)}
                          {trip.createdAt ? ` · ${new Date(trip.createdAt).toLocaleDateString()}` : ""}
                          {` · ${formatCost(trip.estimatedCost)}`}
                        </p>
                        <p className="text-xs text-slate-500">{meta.hint}</p>
                      </div>

                      <div className="flex flex-wrap items-center gap-3">
                        {isPayable(trip.status) && (
                          <Link href={`/payment?tripRequestId=${trip.tripRequestId}`} className="btn-primary">
                            {trip.status === "PAYMENT_PENDING" ? "Continue payment" : "Pay & book"}
                          </Link>
                        )}
                        <Link href={`/trip-summary?tripId=${trip.tripRequestId}`} className="btn-outline">
                          View trip
                        </Link>
                        {trip.status !== "CANCELLED" && tripKind(trip) === "Custom itinerary" && (
                          <DownloadItineraryButton tripId={trip.tripRequestId} label="PDF" />
                        )}
                        {isCancellable(trip.status) && (
                          <button
                            onClick={() => handleCancel(trip)}
                            disabled={cancellingId === trip.tripRequestId}
                            className="text-sm text-red-400 hover:underline disabled:opacity-50"
                          >
                            {cancellingId === trip.tripRequestId ? "Cancelling…" : "Cancel"}
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </section>
          ))}
      </div>
    </ProtectedPage>
  );
}
