"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { fetchAlternates, type Alternates } from "@/lib/alternatesApi";

const label = (s: string) => s.charAt(0) + s.slice(1).toLowerCase();

/** Plan B for each stop. Stops that need a rain backup come first and open by themselves; the rest are one tap away. */
export default function TripAlternates({ tripId, version }: { tripId: number; version?: string | number }) {
  const [data, setData] = useState<Alternates | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let live = true;
    setFailed(false);
    fetchAlternates(tripId).then((d) => live && setData(d)).catch(() => live && setFailed(true));
    return () => { live = false; };
  }, [tripId, version]);

  if (failed) return null;
  if (!data) return <div className="card h-24 animate-pulse p-6" aria-hidden="true" />;

  const withOptions = data.stops.filter((s) => s.options.length > 0);
  if (withOptions.length === 0) return null;
  const urgent = withOptions.filter((s) => s.needsBackup);
  const rest = withOptions.filter((s) => !s.needsBackup);
  const ordered = [...urgent, ...rest];

  return (
    <section className="card space-y-3 p-6" aria-label="Backup plans">
      <div>
        <h3 className="text-lg font-semibold">Plan B</h3>
        <p className="text-sm text-slate-400">
          {urgent.length > 0
            ? `Rain is likely on day ${data.rainyDays.join(", ")}. Here are indoor backups for your outdoor stops.`
            : data.forecastKnown === false
              ? "We can only see the forecast a few days ahead, so it isn't known for your dates yet. If a place is closed, crowded or it rains, here are similar ones nearby."
              : "No rain expected. If a place is closed or crowded, here are similar ones nearby."}
        </p>
      </div>
      <ul className="space-y-2">
        {ordered.map((s) => (
          <li key={`${s.day}-${s.placeId}`}>
            <details open={s.needsBackup} className="rounded-xl bg-slate-800/60 px-3 py-2">
              <summary className="cursor-pointer list-none">
                <span className="flex items-center justify-between gap-2">
                  <span className="min-w-0">
                    <span className="block truncate text-sm font-semibold">Day {s.day} · {s.name}</span>
                    <span className={`block text-xs ${s.needsBackup ? "text-amber-300" : "text-slate-400"}`}>{s.headline}</span>
                  </span>
                  <span aria-hidden="true" className="text-slate-500">▾</span>
                </span>
              </summary>
              <ul className="mt-2 space-y-1">
                {s.options.map((o) => (
                  <li key={o.placeId}>
                    <Link href={`/place/${o.placeId}`} className="flex items-center justify-between gap-2 rounded-lg px-2 py-2 hover:bg-slate-700/60">
                      <span className="min-w-0">
                        <span className="block truncate text-sm font-medium">{o.name}</span>
                        <span className="block truncate text-xs text-slate-400">{label(o.category)} · {o.why}</span>
                      </span>
                      <span className="shrink-0 text-xs tabular-nums text-slate-300">★ {o.rating.toFixed(1)}</span>
                    </Link>
                  </li>
                ))}
              </ul>
            </details>
          </li>
        ))}
      </ul>
    </section>
  );
}
