"use client";

import { useEffect, useMemo, useState } from "react";
import { fetchAnalytics, type AnalyticsSnapshot, type DayPoint } from "@/lib/analyticsApi";
import { errorText } from "@/lib/toast";

const RANGES = [7, 30, 90];
type Metric = "revenue" | "bookings" | "tripsPlanned" | "signups";
const METRICS: { key: Metric; label: string }[] = [
  { key: "revenue", label: "Revenue" },
  { key: "bookings", label: "Bookings" },
  { key: "tripsPlanned", label: "Trips planned" },
  { key: "signups", label: "Sign-ups" },
];

const inr = (n: number) => `₹${Math.round(n).toLocaleString("en-IN")}`;
const shortDate = (iso: string) => new Date(iso + "T00:00:00").toLocaleDateString("en-IN", { day: "numeric", month: "short" });

function Tile({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <div className="card p-4">
      <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">{label}</p>
      <p className="mt-1 font-display text-2xl font-semibold">{value}</p>
      {hint && <p className="mt-0.5 text-xs text-slate-400">{hint}</p>}
    </div>
  );
}

/** One metric over time, one series, one axis. Hover or focus a bar for its value; a table view is one click away. */
function DailyChart({ daily, metric }: { daily: DayPoint[]; metric: Metric }) {
  const [hover, setHover] = useState<number | null>(null);
  const values = daily.map((d) => d[metric] as number);
  const max = Math.max(1, ...values);
  const W = 640, H = 180, PAD = 4;
  const step = (W - PAD * 2) / daily.length;
  const bw = Math.max(2, Math.min(18, step * 0.62));
  const fmt = (v: number) => (metric === "revenue" ? inr(v) : String(v));
  const active = hover === null ? null : daily[hover];

  return (
    <div>
      <div className="mb-2 h-5 text-sm text-slate-300" aria-live="polite">
        {active ? <><span className="font-semibold text-slate-100">{fmt(active[metric] as number)}</span> · {shortDate(active.date)}</> : <span className="text-slate-500">Hover a bar</span>}
      </div>
      <svg viewBox={`0 0 ${W} ${H + 18}`} className="w-full" role="img" aria-label={`${METRICS.find((m) => m.key === metric)?.label} per day`}>
        <line x1={0} x2={W} y1={H} y2={H} stroke="currentColor" strokeOpacity={0.25} />
        <line x1={0} x2={W} y1={H * 0.5} y2={H * 0.5} stroke="currentColor" strokeOpacity={0.08} />
        {values.map((v, i) => {
          const h = v === 0 ? 0 : Math.max(3, (v / max) * (H - 8));
          const x = PAD + i * step + (step - bw) / 2;
          return (
            <g key={daily[i].date} onMouseEnter={() => setHover(i)} onMouseLeave={() => setHover(null)} onFocus={() => setHover(i)} onBlur={() => setHover(null)} tabIndex={0}>
              <rect x={PAD + i * step} y={0} width={step} height={H} fill="transparent" />
              <rect x={x} y={H - h} width={bw} height={h} rx={Math.min(4, bw / 2)} className={hover === i ? "fill-brand-300" : "fill-brand-500"} />
            </g>
          );
        })}
        <text x={PAD} y={H + 14} fontSize="10" fill="currentColor" opacity={0.55}>{shortDate(daily[0].date)}</text>
        <text x={W - PAD} y={H + 14} fontSize="10" textAnchor="end" fill="currentColor" opacity={0.55}>{shortDate(daily[daily.length - 1].date)}</text>
        <text x={W - PAD} y={10} fontSize="10" textAnchor="end" fill="currentColor" opacity={0.55}>max {fmt(max)}</text>
      </svg>
    </div>
  );
}

function BarList({ items, total }: { items: { label: string; count: number }[]; total?: number }) {
  const max = Math.max(1, ...items.map((i) => i.count), total ?? 0);
  if (items.length === 0) return <p className="text-sm text-slate-400">Nothing yet.</p>;
  return (
    <ul className="space-y-2">
      {items.map((i) => (
        <li key={i.label}>
          <div className="flex justify-between text-sm"><span className="truncate">{i.label}</span><span className="tabular-nums text-slate-300">{i.count}</span></div>
          <div className="mt-1 h-2 rounded-full bg-slate-800"><div className="h-2 rounded-full bg-brand-500" style={{ width: `${(i.count / max) * 100}%` }} /></div>
        </li>
      ))}
    </ul>
  );
}

export default function AdminAnalytics() {
  const [days, setDays] = useState(30);
  const [metric, setMetric] = useState<Metric>("revenue");
  const [table, setTable] = useState(false);
  const [data, setData] = useState<AnalyticsSnapshot | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let live = true;
    setError(null);
    fetchAnalytics(days).then((d) => live && setData(d)).catch((e) => live && setError(errorText(e) || "Could not load analytics"));
    return () => { live = false; };
  }, [days]);

  const funnel = useMemo(() => data?.funnel ?? [], [data]);

  return (
    <section className="space-y-4" aria-label="Analytics">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="text-lg font-semibold">Analytics</h3>
        <div className="flex gap-1" role="group" aria-label="Date range">
          {RANGES.map((r) => (
            <button key={r} type="button" onClick={() => setDays(r)} aria-pressed={days === r} className={`btn btn-sm ${days === r ? "btn-primary" : "btn-secondary"}`}>{r}d</button>
          ))}
        </div>
      </div>

      {error && <p className="text-sm text-red-400">{error}</p>}
      {!data && !error && <div className="h-40 animate-pulse rounded-2xl bg-slate-800/60" aria-label="Loading" />}

      {data && (
        <>
          <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
            <Tile label="Net revenue" value={inr(data.totals.netRevenue)} hint={data.totals.refunded > 0 ? `${inr(data.totals.refunded)} refunded` : "all time"} />
            <Tile label="Booked trips" value={String(data.totals.bookedTrips)} hint={`${Math.round(data.totals.bookingRate * 100)}% of ${data.totals.trips} trips`} />
            <Tile label="Avg booking" value={inr(data.totals.averageBookingValue)} />
            <Tile label="Users" value={String(data.totals.users)} hint={`${data.totals.cancelledTrips} cancelled trips`} />
          </div>

          <div className="card space-y-3 p-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="flex flex-wrap gap-1" role="group" aria-label="Metric">
                {METRICS.map((m) => (
                  <button key={m.key} type="button" onClick={() => setMetric(m.key)} aria-pressed={metric === m.key} className={`btn btn-sm ${metric === m.key ? "btn-primary" : "btn-secondary"}`}>{m.label}</button>
                ))}
              </div>
              <button type="button" className="btn btn-secondary btn-sm" onClick={() => setTable((t) => !t)}>{table ? "Chart" : "Table"}</button>
            </div>
            {table ? (
              <div className="max-h-64 overflow-auto">
                <table className="w-full text-left text-sm">
                  <thead className="sticky top-0 bg-slate-900 text-xs uppercase text-slate-400"><tr><th className="py-1 pr-3">Date</th><th className="pr-3">Sign-ups</th><th className="pr-3">Planned</th><th className="pr-3">Bookings</th><th>Revenue</th></tr></thead>
                  <tbody>
                    {[...data.daily].reverse().map((d) => (
                      <tr key={d.date} className="border-t border-slate-800 tabular-nums"><td className="py-1 pr-3">{shortDate(d.date)}</td><td>{d.signups}</td><td>{d.tripsPlanned}</td><td>{d.bookings}</td><td>{inr(d.revenue)}</td></tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <DailyChart daily={data.daily} metric={metric} />
            )}
          </div>

          <div className="grid gap-4 lg:grid-cols-3">
            <div className="card space-y-3 p-4">
              <h4 className="font-semibold">Funnel</h4>
              <BarList items={funnel.map((f) => ({ label: `${f.label} (${f.percentOfFirst}%)`, count: f.count }))} />
            </div>
            <div className="card space-y-3 p-4">
              <h4 className="font-semibold">Top cities</h4>
              <BarList items={data.topCities} />
            </div>
            <div className="card space-y-3 p-4">
              <h4 className="font-semibold">Interest mix</h4>
              <BarList items={data.interestMix} />
            </div>
          </div>
          <p className="text-xs text-slate-500">Booked = confirmed trips. Revenue = captured payments, so refunds are already out. Cities and interests count booked trips. Totals are all time; the chart covers the last {data.days} days.</p>
        </>
      )}
    </section>
  );
}
