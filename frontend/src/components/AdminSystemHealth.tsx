"use client";

import { useCallback, useEffect, useState } from "react";

import { fetchSystemHealth, type SystemHealth } from "@/lib/systemApi";
import { errorText } from "@/lib/toast";

const dur = (s: number) => (s < 3600 ? `${Math.floor(s / 60)} min` : s < 86400 ? `${Math.floor(s / 3600)} h ${Math.floor((s % 3600) / 60)} min` : `${Math.floor(s / 86400)} d ${Math.floor((s % 86400) / 3600)} h`);

function Tile({ label, value, hint, bad }: { label: string; value: string; hint?: string; bad?: boolean }) {
  return (
    <div className="card p-4">
      <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">{label}</p>
      <p className={`mt-1 font-display text-2xl font-semibold ${bad ? "text-red-400" : ""}`}>{value}</p>
      {hint && <p className="mt-0.5 text-xs text-slate-400">{hint}</p>}
    </div>
  );
}

/** Server health for admins. Refreshes every 15 seconds while the page is open. */
export default function AdminSystemHealth() {
  const [h, setH] = useState<SystemHealth | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    fetchSystemHealth().then((d) => { setH(d); setError(null); }).catch((e) => setError(errorText(e, "Couldn't load system health.")));
  }, []);

  useEffect(() => {
    load();
    const t = window.setInterval(load, 15000);
    return () => window.clearInterval(t);
  }, [load]);

  if (error && !h) return <p role="alert" className="text-sm text-red-400">{error}</p>;
  if (!h) return <div className="card h-40 animate-pulse" aria-hidden="true" />;

  const errRate = h.requests.total ? (100 * h.requests.serverErrors) / h.requests.total : 0;
  return (
    <section className="space-y-4" aria-label="System health">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-xl font-semibold">System health</h2>
        <span className={`rounded-full px-3 py-1 text-xs font-semibold ${h.database.ok ? "bg-emerald-500/15 text-emerald-300" : "bg-red-500/15 text-red-300"}`}>
          Database {h.database.ok ? `OK · ${h.database.pingMs} ms` : "DOWN"}
        </span>
      </div>
      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <Tile label="Up for" value={dur(h.uptimeSeconds)} hint="Since the last restart" />
        <Tile label="Requests" value={h.requests.total.toLocaleString("en-IN")} hint={`${h.requests.clientErrors} client errors`} />
        <Tile label="Server errors" value={String(h.requests.serverErrors)} hint={`${errRate.toFixed(1)}% of requests`} bad={h.requests.serverErrors > 0 && errRate >= 1} />
        <Tile label="Speed" value={`${h.requests.p50Ms} ms`} hint={`p95 ${h.requests.p95Ms} ms (last ${h.requests.windowSize})`} bad={h.requests.p95Ms > 2000} />
        <Tile label="Cache hit rate" value={`${h.cache.hitRatePercent}%`} hint={`${h.cache.hits} hits, ${h.cache.misses} misses, ${h.cache.entries} stored`} />
        <Tile label="Push queue" value={h.push.enabled ? String(h.push.pending) : "Off"} hint={h.push.enabled ? `${h.push.failed} gave up` : "Set the VAPID keys to turn on"} bad={h.push.failed > 0} />
        <Tile label="Memory" value={`${h.memoryUsedMb} MB`} hint={`of ${h.memoryMaxMb} MB`} bad={h.memoryUsedMb > h.memoryMaxMb * 0.9} />
        <Tile label="Payment events" value={h.paymentEvents.toLocaleString("en-IN")} hint="Audit trail rows" />
      </div>
      {h.requests.slowest.length > 0 && (
        <div className="card p-4">
          <p className="mb-2 text-sm font-semibold">Slowest endpoints (p95)</p>
          <ul className="space-y-1.5 text-sm">
            {h.requests.slowest.map((r) => (
              <li key={r.route} className="flex items-center justify-between gap-3">
                <code className="truncate text-xs text-slate-300">{r.route}</code>
                <span className="shrink-0 text-slate-400">{r.p95Ms} ms · {r.count} calls{r.errors ? ` · ${r.errors} errors` : ""}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}
