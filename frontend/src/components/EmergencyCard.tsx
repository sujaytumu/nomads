"use client";

import { useEffect, useState } from "react";

import { fetchTravel } from "@/lib/travelApi";

const NUMBERS = [
  { label: "Emergency (all)", tel: "112" },
  { label: "Police", tel: "100" },
  { label: "Ambulance", tel: "108" },
  { label: "Fire", tel: "101" },
  { label: "Women helpline", tel: "1091" },
];

/** Everything you'd need in a bad moment, in one tap: numbers, your ride, and your live location. */
export default function EmergencyCard({ tripId, city }: { tripId: number | string; city?: string | null }) {
  const [ride, setRide] = useState<any>(null);
  const [msg, setMsg] = useState<string | null>(null);

  useEffect(() => {
    fetchTravel(Number(tripId)).then(setRide).catch(() => setRide(null));
  }, [tripId]);

  const shareLocation = () => {
    setMsg(null);
    if (!navigator.geolocation) return setMsg("Location isn't available on this device.");
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        const url = `https://maps.google.com/?q=${pos.coords.latitude},${pos.coords.longitude}`;
        const text = `I'm on a NOMAD trip${city ? ` in ${city}` : ""} (booking #${tripId}). My location: ${url}`;
        try {
          if (navigator.share) await navigator.share({ text });
          else {
            await navigator.clipboard.writeText(text);
            setMsg("Location message copied — paste it to someone you trust.");
          }
        } catch (e: any) {
          if (e?.name !== "AbortError") setMsg(text);
        }
      },
      () => setMsg("Couldn't get your location. Allow location access and try again."),
      { enableHighAccuracy: true, timeout: 15000 }
    );
  };

  return (
    <section className="card space-y-4 border-red-500/20 p-6" aria-label="Emergency card">
      <div>
        <h3 className="text-lg font-semibold">Emergency card</h3>
        <p className="text-sm text-slate-400">Save this page or screenshot it before you travel.</p>
      </div>

      <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
        {NUMBERS.map((n) => (
          <a key={n.tel} href={`tel:${n.tel}`} className="btn btn-danger flex-col gap-0 py-2">
            <span className="text-lg font-bold">{n.tel}</span>
            <span className="text-[11px] font-medium">{n.label}</span>
          </a>
        ))}
      </div>

      <div className="space-y-1 rounded-xl border border-slate-800 p-4 text-sm">
        <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Your ride</p>
        {ride ? (
          <>
            <p>{[ride.vehicleType, ride.vehicleNumber].filter(Boolean).join(" · ") || "Vehicle not assigned yet"}</p>
            <p className="text-slate-400">Driver: {ride.driverName || "—"}</p>
            <p className="text-slate-400">Pickup: {ride.pickupLocation || "—"}{ride.pickupTime ? ` at ${String(ride.pickupTime).replace("T", " ").slice(0, 16)}` : ""}</p>
          </>
        ) : (
          <p className="text-slate-400">No pickup arranged. You can request one on the Pickup page.</p>
        )}
        <p className="text-slate-400">Booking #{tripId}</p>
      </div>

      <button type="button" className="btn btn-secondary w-full sm:w-auto" onClick={shareLocation}>Share my live location</button>
      {msg && <p role="status" className="break-words text-xs text-slate-300">{msg}</p>}
    </section>
  );
}
