"use client";

import { Suspense, useEffect, useState } from "react";
import Link from "next/link";

import PageHeader from "@/components/PageHeader";
import ProtectedPage from "@/components/ProtectedPage";
import TripPicker from "@/components/TripPicker";
import { fetchMe } from "@/lib/authApi";
import { confirmPickup, updatePickup } from "@/lib/pickupApi";
import { assignTravel, fetchTravel } from "@/lib/travelApi";
import { useTripSelection } from "@/lib/useTripSelection";

const STAGES = ["ASSIGNED", "CONFIRMED", "EN_ROUTE", "COMPLETED"];
const STAGE_TEXT: Record<string, string> = {
  ASSIGNED: "A vehicle is assigned. Confirm your pickup time to lock it in.",
  CONFIRMED: "Pickup time confirmed. The driver will arrive then.",
  EN_ROUTE: "Your driver is on the way.",
  COMPLETED: "Pickup completed.",
  CANCELLED: "This pickup was cancelled.",
};

export default function PickupPage() {
  return (
    <Suspense fallback={null}>
      <PickupInner />
    </Suspense>
  );
}

function PickupInner() {
  const sel = useTripSelection((t) => t.status !== "CANCELLED");
  const [isAdmin, setIsAdmin] = useState(false);
  const [data, setData] = useState<any>(null);
  const [none, setNone] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pickupTime, setPickupTime] = useState("");
  const [adminStatus, setAdminStatus] = useState("ASSIGNED");
  const [routeMapUrl, setRouteMapUrl] = useState("");
  const [vehicleId, setVehicleId] = useState("");

  useEffect(() => {
    fetchMe().then((m) => setIsAdmin(m.role === "ADMIN")).catch(() => undefined);
  }, []);

  const id = sel.selected?.tripRequestId;
  useEffect(() => {
    setData(null);
    setNone(false);
    setError(null);
    if (!id) return;
    setBusy(true);
    fetchTravel(Number(id))
      .then(setData)
      .catch(() => setNone(true))
      .finally(() => setBusy(false));
  }, [id]);

  const run = async (fn: () => Promise<any>, fail: string) => {
    setError(null);
    setBusy(true);
    try {
      setData(await fn());
      setNone(false);
    } catch (e: any) {
      setError(e?.response?.data?.message || fail);
    } finally {
      setBusy(false);
    }
  };

  const stage = data ? STAGES.indexOf(data.status) : -1;

  return (
    <ProtectedPage>
      <div className="section space-y-6 py-8">
        <PageHeader
          title="Pickup & vehicle"
          subtitle="Pick a trip to see its vehicle, driver and pickup status — or request a pickup."
        />
        <div className="card space-y-5 p-6">
          <TripPicker trips={sel.trips} selected={sel.selected} onSelect={sel.select} loading={sel.loading} error={sel.error} onRetry={sel.reload} emptyText="You have no trips yet. Plan one first, then arrange its pickup here." />

          {sel.selected && busy && !data && <p className="text-sm text-slate-400">Checking pickup…</p>}

          {sel.selected && none && !busy && (
            <div className="space-y-3 rounded-xl border border-slate-800 p-5">
              <p className="font-semibold">No pickup arranged for trip #{sel.selected.tripRequestId}</p>
              <p className="text-sm text-slate-400">We&apos;ll assign a vehicle and driver. You then confirm the pickup time.</p>
              <button type="button" className="btn btn-primary" disabled={busy} onClick={() => run(() => assignTravel(Number(id)), "Couldn't request a pickup.")}>
                Request pickup
              </button>
            </div>
          )}

          {data && (
            <div className="space-y-4">
              {data.status !== "CANCELLED" && (
                <ol className="flex flex-wrap items-center gap-2 text-xs" aria-label="Pickup progress">
                  {STAGES.map((s, i) => (
                    <li key={s} className="flex items-center gap-2">
                      <span className={`rounded-full px-3 py-1 font-semibold ${i < stage ? "bg-green-500/15 text-green-300" : i === stage ? "bg-brand-600 text-white" : "bg-slate-800 text-slate-500"}`}>
                        {s.replace("_", " ")}
                      </span>
                    </li>
                  ))}
                </ol>
              )}
              <p className="text-sm text-slate-300">{STAGE_TEXT[data.status] ?? data.status}</p>
              <dl className="grid gap-3 rounded-xl border border-slate-800 p-4 text-sm sm:grid-cols-2">
                <div><dt className="text-xs text-slate-500">Pickup location</dt><dd>{data.pickupLocation || "—"}</dd></div>
                <div><dt className="text-xs text-slate-500">Pickup time</dt><dd>{data.pickupTime ? String(data.pickupTime).replace("T", " ") : "Not confirmed yet"}</dd></div>
                <div><dt className="text-xs text-slate-500">Vehicle</dt><dd>{[data.vehicleType, data.vehicleNumber].filter(Boolean).join(" · ") || "—"}</dd></div>
                <div><dt className="text-xs text-slate-500">Driver</dt><dd>{data.driverName || "—"}</dd></div>
              </dl>
              {data.routeMapUrl && (
                <a className="btn btn-secondary btn-sm" href={data.routeMapUrl} target="_blank" rel="noreferrer">Open route link</a>
              )}

              {data.status !== "CANCELLED" && data.status !== "COMPLETED" && (
                <div className="flex flex-wrap items-end gap-3">
                  <label className="space-y-1.5 text-sm">
                    <span className="block text-xs font-semibold uppercase tracking-wide text-slate-500">Your pickup time</span>
                    <input type="datetime-local" value={pickupTime} onChange={(e) => setPickupTime(e.target.value)} className="rounded-xl border px-4 py-2" />
                  </label>
                  <button type="button" className="btn btn-primary" disabled={busy || !pickupTime} onClick={() => run(() => confirmPickup(Number(id), { pickupTime }), "Couldn't confirm the pickup.")}>
                    Confirm pickup time
                  </button>
                </div>
              )}

              {isAdmin && (
                <div className="space-y-3 rounded-xl border border-amber-500/30 bg-amber-500/5 p-4">
                  <p className="text-sm font-semibold text-amber-300">Admin controls</p>
                  <div className="grid gap-3 md:grid-cols-3">
                    <select value={adminStatus} onChange={(e) => setAdminStatus(e.target.value)} className="rounded-xl border px-4 py-2 text-sm">
                      {[...STAGES, "CANCELLED"].map((s) => <option key={s}>{s}</option>)}
                    </select>
                    <input value={routeMapUrl} onChange={(e) => setRouteMapUrl(e.target.value)} placeholder="Route map URL" className="rounded-xl border px-4 py-2 text-sm" />
                    <input value={vehicleId} onChange={(e) => setVehicleId(e.target.value)} placeholder="Vehicle ID" className="rounded-xl border px-4 py-2 text-sm" />
                  </div>
                  <button type="button" className="btn btn-secondary btn-sm" disabled={busy} onClick={() => run(() => updatePickup(Number(id), { status: adminStatus, routeMapUrl: routeMapUrl || undefined, vehicleId: vehicleId ? Number(vehicleId) : undefined, pickupTime: pickupTime || undefined }), "Update failed.")}>
                    Save changes
                  </button>
                </div>
              )}
            </div>
          )}

          {error && <p role="alert" className="rounded-xl border border-red-500/20 bg-red-500/10 px-4 py-3 text-sm text-red-400">{error}</p>}
        </div>
        <p className="text-sm text-slate-500">Need to see the trip itself? <Link className="underline" href="/bookings">Go to My Trips</Link>.</p>
      </div>
    </ProtectedPage>
  );
}
