"use client";

import { useEffect, useMemo, useState } from "react";
import { api } from "@/lib/api";
import { toast } from "@/lib/toast";

export type LiveStop = { placeId?: number | null; placeName: string; startTime?: string | null; endTime?: string | null };

const hm = (t?: string | null) => (t ? t.slice(0, 5) : "");
const mins = (t?: string | null) => {
  const [h, m] = hm(t).split(":").map(Number);
  return Number.isFinite(h) ? h * 60 + (m || 0) : null;
};
const clock = (m: number) => {
  const x = ((m % 1440) + 1440) % 1440;
  return `${String(Math.floor(x / 60)).padStart(2, "0")}:${String(x % 60).padStart(2, "0")}`;
};
const span = (m: number) => (m < 60 ? `${Math.max(1, Math.round(m))} min` : `${Math.floor(m / 60)} h ${Math.round(m % 60)} min`);

function km(a: { lat: number; lon: number }, b: { lat: number; lon: number }) {
  const R = 6371, rad = Math.PI / 180;
  const dLat = (b.lat - a.lat) * rad, dLon = (b.lon - a.lon) * rad;
  const x = Math.sin(dLat / 2) ** 2 + Math.cos(a.lat * rad) * Math.cos(b.lat * rad) * Math.sin(dLon / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(x));
}

/** Trip-day view: what you are doing now, what is next and when, how far it is, and a "running late" shift. */
export default function LiveTrip({ tripId, day, city, stops }: { tripId: number | string; day: number; city?: string | null; stops: LiveStop[] }) {
  const lateKey = `nomad_late_${tripId}_${day}`;
  const [now, setNow] = useState(() => new Date());
  const [late, setLate] = useState(0);
  const [me, setMe] = useState<{ lat: number; lon: number } | null>(null);
  const [dest, setDest] = useState<{ lat: number; lon: number } | null>(null);

  useEffect(() => {
    try {
      setLate(Number(localStorage.getItem(lateKey)) || 0);
    } catch {
      /* ignore */
    }
  }, [lateKey]);

  useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 10000);
    return () => clearInterval(t);
  }, []);

  // Your position, only if you allow it. Used just for the distance to the next stop.
  useEffect(() => {
    if (!("geolocation" in navigator)) return;
    const id = navigator.geolocation.watchPosition(
      (p) => setMe({ lat: p.coords.latitude, lon: p.coords.longitude }),
      () => undefined,
      { enableHighAccuracy: false, maximumAge: 60000, timeout: 15000 }
    );
    return () => navigator.geolocation.clearWatch(id);
  }, []);

  const nowMin = now.getHours() * 60 + now.getMinutes();
  const nowExact = nowMin + now.getSeconds() / 60;
  const rows = useMemo(
    () =>
      stops.map((s) => {
        const a = mins(s.startTime);
        const b = mins(s.endTime) ?? (a !== null ? a + 60 : null);
        return { ...s, a: a === null ? null : a + late, b: b === null ? null : b + late };
      }),
    [stops, late]
  );

  const doneCount = rows.filter((r) => r.b !== null && r.b < nowMin).length;
  const nowIdx = rows.findIndex((r) => r.a !== null && r.b !== null && r.a <= nowMin && nowMin <= r.b);
  const nextIdx = rows.findIndex((r) => r.a !== null && r.a > nowMin);
  const focusIdx = nowIdx >= 0 ? nowIdx : nextIdx;
  const focus = focusIdx >= 0 ? rows[focusIdx] : null;
  const upNext = nowIdx >= 0 && nextIdx >= 0 ? rows[nextIdx] : null;

  // Coordinates of the stop in focus (loaded once per stop).
  useEffect(() => {
    setDest(null);
    if (!focus?.placeId) return;
    let live = true;
    api
      .get(`/api/places/${focus.placeId}`)
      .then((r) => {
        const lat = Number(r.data?.latitude), lon = Number(r.data?.longitude);
        if (live && Number.isFinite(lat) && Number.isFinite(lon)) setDest({ lat, lon });
      })
      .catch(() => undefined);
    return () => {
      live = false;
    };
  }, [focus?.placeId]);

  const shift = (by: number) => {
    const next = by === 0 ? 0 : late + by;
    setLate(next);
    try {
      localStorage.setItem(lateKey, String(next));
    } catch {
      /* ignore */
    }
    toast(by === 0 ? "Back to the original times" : `Times shifted by ${next} min`, "info");
  };

  const maps = (s: LiveStop) => `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${s.placeName} ${city || ""}`.trim())}`;
  const away = me && dest ? km(me, dest) : null;
  // Overall day progress counts the stop you are at as partly done, so the bar moves smoothly.
  const stopPct = nowIdx >= 0 && focus && focus.a !== null && focus.b !== null && focus.b > focus.a ? Math.min(100, Math.max(0, ((nowExact - focus.a) / (focus.b - focus.a)) * 100)) : 0;
  const pct = rows.length ? Math.min(100, Math.round(((doneCount + (nowIdx >= 0 ? stopPct / 100 : 0)) / rows.length) * 100)) : 0;
  const gap = upNext && upNext.a !== null && focus && focus.b !== null ? Math.max(0, upNext.a - focus.b) : null;

  if (rows.length === 0) return null;

  return (
    <div className="mt-4 space-y-3">
      {/* Now / Next */}
      <div key={focusIdx} className="pop-in rounded-2xl border border-brand-500/40 bg-brand-600/10 p-4 shadow-glow">
        {focus ? (
          <>
            <p className="eyebrow flex items-center gap-2">
              {nowIdx >= 0 && <span className="live-dot" aria-hidden="true" />}
              {nowIdx >= 0 ? "Now" : "Next up"}
            </p>
            <p className="mt-0.5 font-display text-2xl font-semibold">{focus.placeName}</p>
            <p className="mt-1 text-sm text-slate-300">
              {nowIdx >= 0 && focus.b !== null
                ? `Ends at ${clock(focus.b)} · ${span(focus.b - nowMin)} left`
                : focus.a !== null
                  ? `Starts at ${clock(focus.a)} · in ${span(focus.a - nowMin)}`
                  : ""}
              {away !== null && ` · ${away < 1 ? `${Math.round(away * 1000)} m` : `${away.toFixed(1)} km`} from you`}
            </p>
            {nowIdx >= 0 && (
              <div className="mt-3" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(stopPct)} aria-label="Time at this stop">
                <div className="h-2 overflow-hidden rounded-full bg-slate-800">
                  <div className="stop-fill h-full rounded-full bg-gradient-to-r from-emerald-400 to-ocean-300" style={{ width: `${stopPct}%` }} />
                </div>
              </div>
            )}
            {upNext && upNext.a !== null && (
              <p className="mt-2 text-xs text-slate-400">
                Then {upNext.placeName} at {clock(upNext.a)}
                {gap !== null && gap > 0 ? ` · ${span(gap)} to get there` : ""}
              </p>
            )}
            <div className="mt-3 flex flex-wrap gap-2">
              <a href={maps(focus)} target="_blank" rel="noopener noreferrer" className="btn btn-primary btn-sm">Directions</a>
            </div>
          </>
        ) : (
          <>
            <p className="eyebrow">Day complete</p>
            <p className="mt-0.5 font-display text-xl font-semibold">That&apos;s everything planned for today.</p>
          </>
        )}
      </div>

      {/* Day progress */}
      <div>
        <div className="mb-1 flex items-center justify-between text-xs text-slate-400">
          <span>{doneCount} of {rows.length} stops done</span>
          <span>{pct}%</span>
        </div>
        <div className="h-1.5 overflow-hidden rounded-full bg-slate-800">
          <div className="h-full rounded-full bg-gradient-to-r from-brand-500 to-ocean-400 transition-all duration-1000" style={{ width: `${pct}%` }} />
        </div>
      </div>

      {/* Stops */}
      <ol className="space-y-2">
        {rows.map((s, i) => {
          const done = i < focusIdx || (focusIdx < 0 && s.b !== null && s.b < nowMin);
          const isFocus = i === focusIdx;
          return (
            <li key={`${s.placeName}-${i}`} className={`flex items-center gap-3 rounded-2xl border px-3 py-2.5 ${isFocus ? "border-brand-500/60 bg-brand-600/15" : "border-slate-800 bg-slate-900/60"} ${done ? "opacity-55" : ""}`}>
              <span aria-hidden="true" className={`grid h-6 w-6 shrink-0 place-items-center rounded-full text-[11px] font-bold ${done ? "bg-emerald-500/20 text-emerald-300" : isFocus ? "bg-brand-600 text-white" : "bg-slate-800 text-slate-400"}`}>
                {done ? "✓" : i + 1}
              </span>
              <span className="w-24 shrink-0 text-xs font-semibold text-slate-400">
                {s.a !== null ? clock(s.a) : hm(s.startTime)}{s.b !== null && s.endTime ? `–${clock(s.b)}` : ""}
              </span>
              <span className="min-w-0 flex-1 truncate text-sm font-semibold">{s.placeName}</span>
              <a href={maps(s)} target="_blank" rel="noopener noreferrer" aria-label={`Directions to ${s.placeName}`} className="btn btn-secondary btn-sm shrink-0">Go</a>
            </li>
          );
        })}
      </ol>

      {/* Running late */}
      <div className="flex flex-wrap items-center gap-2 text-xs">
        <span className="font-semibold text-slate-400">Running late?</span>
        <button type="button" className="btn btn-secondary btn-sm" onClick={() => shift(15)}>+15 min</button>
        <button type="button" className="btn btn-secondary btn-sm" onClick={() => shift(30)}>+30 min</button>
        {late !== 0 && <button type="button" className="btn btn-ghost btn-sm" onClick={() => shift(0)}>Reset ({late > 0 ? "+" : ""}{late})</button>}
      </div>
    </div>
  );
}
