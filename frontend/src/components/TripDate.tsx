"use client";

import { useState } from "react";

import { downloadTripCalendar } from "@/lib/calendar";
import { setTripDate } from "@/lib/tripApi";

const fmt = (d: string) => new Date(`${d}T00:00:00`).toLocaleDateString("en-IN", { weekday: "short", day: "numeric", month: "short", year: "numeric" });

/** When you're going: shows the date, lets you set/change it, and adds it to your calendar. */
export default function TripDate({ trip, onChange }: { trip: any; onChange: (updated: any) => void }) {
  const [editing, setEditing] = useState(false);
  const [value, setValue] = useState(trip.tripDate || "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  if (trip.status === "CANCELLED") return null;

  const save = async () => {
    setBusy(true);
    setError(null);
    try {
      onChange(await setTripDate(Number(trip.tripRequestId), value));
      setEditing(false);
    } catch (e: any) {
      setError(e?.response?.data?.message || "Couldn't save the date.");
    } finally {
      setBusy(false);
    }
  };
  const today = new Date().toISOString().slice(0, 10);

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap items-center gap-2 text-sm">
        <span className="text-slate-400">Trip date:</span>
        {trip.tripDate && !editing ? <b>{fmt(trip.tripDate)}</b> : !editing && <span className="text-slate-500">not set</span>}
        {editing ? (
          <>
            <input type="date" min={today} value={value} onChange={(e) => setValue(e.target.value)} className="rounded-lg border px-3 py-1.5 text-sm" />
            <button type="button" className="btn btn-primary btn-sm" disabled={busy || !value} onClick={save}>{busy ? "Saving…" : "Save"}</button>
            <button type="button" className="btn btn-ghost btn-sm" onClick={() => setEditing(false)}>Cancel</button>
          </>
        ) : (
          <>
            <button type="button" className="btn btn-secondary btn-sm" onClick={() => { setValue(trip.tripDate || ""); setEditing(true); }}>{trip.tripDate ? "Change" : "Set date"}</button>
            {trip.tripDate && <button type="button" className="btn btn-secondary btn-sm" onClick={() => downloadTripCalendar(trip)}>Add to calendar</button>}
          </>
        )}
      </div>
      {error && <p role="alert" className="text-xs text-red-400">{error}</p>}
    </div>
  );
}
