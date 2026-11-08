"use client";

import { useEffect, useState } from "react";
import { fetchTripHealth, type TripHealth } from "@/lib/healthApi";

const tone = (n: number) => (n >= 85 ? "text-emerald-300" : n >= 70 ? "text-sky-300" : n >= 50 ? "text-amber-300" : "text-red-400");
const bar = (n: number) => (n >= 85 ? "bg-emerald-400" : n >= 70 ? "bg-sky-400" : n >= 50 ? "bg-amber-300" : "bg-red-400");

/** How good the plan is, in one number, with the reasons and what to change. Refreshes when the stops change. */
export default function TripHealthCard({ tripId, version }: { tripId: number; version?: string | number }) {
  const [h, setH] = useState<TripHealth | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let live = true;
    setFailed(false);
    fetchTripHealth(tripId).then((d) => live && setH(d)).catch(() => live && setFailed(true));
    return () => {
      live = false;
    };
  }, [tripId, version]);

  if (failed) return null;
  if (!h) return <div className="card h-32 animate-pulse p-6" aria-hidden="true" />;
  if (h.factors.length === 0) return null;

  const R = 34;
  const C = 2 * Math.PI * R;
  return (
    <section className="card space-y-4 p-6" aria-label="Plan health">
      <div className="flex items-center gap-5">
        <div className="relative h-20 w-20 shrink-0">
          <svg viewBox="0 0 80 80" className="h-20 w-20 -rotate-90" aria-hidden="true">
            <circle cx="40" cy="40" r={R} fill="none" strokeWidth="8" className="stroke-slate-800" />
            <circle cx="40" cy="40" r={R} fill="none" strokeWidth="8" strokeLinecap="round" strokeDasharray={C} strokeDashoffset={C * (1 - h.score / 100)} className={`${tone(h.score).replace("text-", "stroke-")} transition-all duration-700`} />
          </svg>
          <span className={`absolute inset-0 grid place-items-center text-2xl font-bold ${tone(h.score)}`}>{h.score}</span>
        </div>
        <div>
          <h3 className="text-lg font-semibold">Plan health: <span className={tone(h.score)}>{h.label}</span></h3>
          <p className="text-sm text-slate-400">How relaxed, varied and easy this trip is.</p>
        </div>
      </div>

      <ul className="space-y-2.5">
        {h.factors.map((f) => (
          <li key={f.key}>
            <div className="flex items-baseline justify-between gap-3 text-sm">
              <span className="font-semibold">{f.label}</span>
              <span className="text-xs text-slate-400">{f.detail}</span>
            </div>
            <div className="mt-1 h-2 overflow-hidden rounded-full bg-slate-800" role="progressbar" aria-valuenow={f.score} aria-valuemin={0} aria-valuemax={100} aria-label={f.label}>
              <div className={`h-full rounded-full ${bar(f.score)} transition-all duration-700`} style={{ width: `${f.score}%` }} />
            </div>
          </li>
        ))}
      </ul>

      {h.tips.length > 0 && (
        <ul className="space-y-1.5 rounded-2xl bg-amber-400/10 p-4 text-sm text-amber-200">
          {h.tips.map((t) => (
            <li key={t}>💡 {t}</li>
          ))}
        </ul>
      )}
    </section>
  );
}
