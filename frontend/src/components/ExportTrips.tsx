"use client";

import { useState } from "react";

import { api } from "@/lib/api";
import { saveBlob } from "@/lib/download";
import { errorText, toast } from "@/lib/toast";
import { tripsToCsv, type CsvTrip } from "@/lib/tripsCsv";

/** Download every trip you have planned or booked as a spreadsheet-friendly CSV. */
export default function ExportTrips() {
  const [busy, setBusy] = useState(false);

  async function go() {
    setBusy(true);
    try {
      const { data } = await api.get("/api/trips/me");
      const trips = (Array.isArray(data) ? data : []) as CsvTrip[];
      if (trips.length === 0) {
        toast("You have no trips to export yet.", "info");
        return;
      }
      saveBlob(new Blob([tripsToCsv(trips)], { type: "text/csv;charset=utf-8" }), "my-nomad-trips.csv");
      toast(`Exported ${trips.length} ${trips.length === 1 ? "trip" : "trips"}`, "success");
    } catch (e) {
      toast(errorText(e, "Couldn't export your trips."), "error");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="card flex items-center justify-between gap-3 p-4">
      <div>
        <h3 className="font-semibold">Your trips as a spreadsheet</h3>
        <p className="text-sm text-slate-400">A CSV of every trip you have planned or booked.</p>
      </div>
      <button type="button" className="btn btn-secondary shrink-0" disabled={busy} data-loading={busy} onClick={go}>Download CSV</button>
    </section>
  );
}
