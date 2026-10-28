"use client";

import { useEffect, useState } from "react";
import { fetchEssentials, hotelsIn, mapsSearch, TripEssentials as Data } from "@/lib/essentialsApi";
import { toast } from "@/lib/toast";

const TABS = [
  { k: "stay", l: "🛏️ Stay" },
  { k: "scenic", l: "🌅 Scenic" },
  { k: "medical", l: "🏥 Medical" },
] as const;

const TIER: Record<string, { label: string; cls: string }> = {
  BUDGET: { label: "Budget", cls: "bg-emerald-500/15 text-emerald-300" },
  COMFORT: { label: "Comfort", cls: "bg-sky-500/15 text-sky-300" },
  PREMIUM: { label: "Premium", cls: "bg-amber-500/15 text-amber-300" },
};

const rupees = (n: number) => `₹${n.toLocaleString("en-IN")}`;

/** Opens a map search around where the phone is right now (hospital, pharmacy and so on). */
function searchNearMe(what: string) {
  if (!navigator.geolocation) {
    window.open(mapsSearch(`${what} near me`), "_blank", "noopener");
    return;
  }
  navigator.geolocation.getCurrentPosition(
    (pos) => window.open(`https://www.google.com/maps/search/${encodeURIComponent(what)}/@${pos.coords.latitude},${pos.coords.longitude},14z`, "_blank", "noopener"),
    () => {
      toast("Couldn't get your location. Showing a general search instead.", "info");
      window.open(mapsSearch(`${what} near me`), "_blank", "noopener");
    },
    { enableHighAccuracy: true, timeout: 12000 }
  );
}

/** Where to stay, scenic spots and medical help, picked around this trip's stops. */
export default function TripEssentials({ tripId }: { tripId: number }) {
  const [data, setData] = useState<Data | null>(null);
  const [failed, setFailed] = useState(false);
  const [tab, setTab] = useState<(typeof TABS)[number]["k"]>("stay");

  useEffect(() => {
    let active = true;
    setData(null);
    setFailed(false);
    fetchEssentials(tripId)
      .then((d) => active && setData(d))
      .catch(() => active && setFailed(true));
    return () => {
      active = false;
    };
  }, [tripId]);

  const city = data?.city ?? "";

  return (
    <section className="card space-y-4 p-6">
      <div>
        <h3 className="text-lg font-semibold">Stay, views and medical help</h3>
        <p className="text-sm text-slate-400">Picked around your stops, so you spend less time and money getting around.</p>
      </div>

      <div role="tablist" className="grid grid-cols-3 gap-2">
        {TABS.map((t) => (
          <button
            key={t.k}
            role="tab"
            aria-selected={tab === t.k}
            type="button"
            onClick={() => setTab(t.k)}
            className={`rounded-full border px-3 py-2 text-sm font-semibold transition-colors ${tab === t.k ? "border-brand-600 bg-brand-600 text-white" : "border-slate-700 bg-slate-900 text-slate-300 hover:bg-slate-800"}`}
          >
            {t.l}
          </button>
        ))}
      </div>

      {!data && !failed && <div className="h-28 animate-pulse rounded-xl bg-slate-800/60" />}
      {failed && <p className="text-sm text-slate-400">Couldn&apos;t load suggestions right now. Try again in a moment.</p>}

      {data && tab === "stay" && (
        <div className="space-y-3">
          {data.stay.length === 0 && <p className="text-sm text-slate-400">No stay areas saved for this city yet.</p>}
          {data.stay.map((s) => (
            <div key={s.area} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-800 p-4">
              <div className="min-w-0 space-y-1">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="font-semibold">{s.area}</p>
                  <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${TIER[s.tier]?.cls ?? ""}`}>{TIER[s.tier]?.label ?? s.tier}</span>
                </div>
                <p className="text-sm text-slate-400">{s.why}</p>
                <p className="text-xs text-slate-500">About {s.avgKmToStops} km from your stops on average</p>
              </div>
              <div className="text-right">
                <p className="text-xs text-slate-500">Typical per night</p>
                <p className="text-lg font-bold">{rupees(s.pricePerNight)}</p>
                <a href={hotelsIn(s.area, city)} target="_blank" rel="noopener noreferrer" className="btn btn-secondary mt-1 text-xs">See hotels</a>
              </div>
            </div>
          ))}
          <p className="text-xs text-slate-500">Prices are typical ranges, not live rates. NOMAD doesn&apos;t book hotels. Check the final price and book with the hotel.</p>
        </div>
      )}

      {data && tab === "scenic" && (
        <div className="space-y-3">
          {data.scenic.length === 0 && <p className="text-sm text-slate-400">No scenic spots saved for this city yet.</p>}
          {data.scenic.map((s) => (
            <div key={s.name} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-800 p-4">
              <div className="min-w-0 space-y-1">
                <p className="font-semibold">{s.name}</p>
                <p className="text-sm text-slate-400">{s.why}</p>
                <p className="text-xs text-slate-500">Best at <span className="font-semibold text-brand-300">{s.bestTime.toLowerCase()}</span> · {s.kmFromTrip} km from your stops</p>
              </div>
              <a href={mapsSearch(`${s.name}, ${city}`)} target="_blank" rel="noopener noreferrer" className="btn btn-secondary text-xs">Directions</a>
            </div>
          ))}
        </div>
      )}

      {data && tab === "medical" && (
        <div className="space-y-3">
          <div className="grid grid-cols-2 gap-2">
            <button type="button" className="btn btn-danger" onClick={() => searchNearMe("hospital")}>🏥 Hospital near me</button>
            <button type="button" className="btn btn-secondary" onClick={() => searchNearMe("24 hour pharmacy")}>💊 Pharmacy near me</button>
          </div>
          <p className="text-xs text-slate-400">These use your phone&apos;s location right now. In an emergency, call <a className="font-semibold underline" href="tel:112">112</a> or an ambulance on <a className="font-semibold underline" href="tel:108">108</a>.</p>
          {data.medical.map((m) => (
            <div key={m.name} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-800 p-4">
              <div className="min-w-0 space-y-1">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="font-semibold">{m.name}</p>
                  <span className="rounded-full bg-slate-700/60 px-2 py-0.5 text-xs">{m.type}</span>
                </div>
                <p className="text-sm text-slate-400">{m.note}</p>
                <p className="text-xs text-slate-500">{m.kmFromTrip} km from your stops</p>
              </div>
              <a href={mapsSearch(`${m.name}, ${city}`)} target="_blank" rel="noopener noreferrer" className="btn btn-secondary text-xs">Directions</a>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
