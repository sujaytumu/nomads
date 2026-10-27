"use client";

import { useEffect, useMemo, useState } from "react";
import { packingPlan, type PackInput } from "@/lib/packingPlan";

type Saved = { done: string[]; custom: string[] };

/**
 * Packing list that fits the trip (days, season, interest, group), with room for your own items.
 * Ticks and extra items are saved on this device only.
 */
export default function PackingChecklist({ tripId, trip }: { tripId: number | string; trip?: PackInput }) {
  const key = `nomad_pack2_${tripId}`;
  const [saved, setSaved] = useState<Saved>({ done: [], custom: [] });
  const [draft, setDraft] = useState("");

  const sections = useMemo(() => packingPlan(trip ?? {}), [trip?.interest, trip?.weekendType, trip?.travelMode, trip?.groupSize, trip?.pickupRequired, trip?.tripDate]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    try {
      const raw = JSON.parse(localStorage.getItem(key) || "null");
      if (raw && Array.isArray(raw.done) && Array.isArray(raw.custom)) setSaved(raw);
    } catch {
      /* start fresh */
    }
  }, [key]);

  const persist = (next: Saved) => {
    setSaved(next);
    try {
      localStorage.setItem(key, JSON.stringify(next));
    } catch {
      /* private mode: keep it in memory only */
    }
  };

  const toggle = (id: string) => persist({ ...saved, done: saved.done.includes(id) ? saved.done.filter((d) => d !== id) : [...saved.done, id] });

  const addCustom = (e: React.FormEvent) => {
    e.preventDefault();
    const text = draft.trim().slice(0, 60);
    if (!text) return;
    const id = "custom:" + text.toLowerCase();
    if (!saved.custom.some((c) => "custom:" + c.toLowerCase() === id)) persist({ ...saved, custom: [...saved.custom, text] });
    setDraft("");
  };

  const removeCustom = (text: string) =>
    persist({ done: saved.done.filter((d) => d !== "custom:" + text.toLowerCase()), custom: saved.custom.filter((c) => c !== text) });

  const all = [...sections.flatMap((s) => s.items.map((i) => i.id)), ...saved.custom.map((c) => "custom:" + c.toLowerCase())];
  const packed = all.filter((id) => saved.done.includes(id)).length;
  const pct = all.length === 0 ? 0 : Math.round((packed / all.length) * 100);

  const row = (id: string, label: string, onRemove?: () => void) => (
    <li key={id} className="flex items-center gap-1">
      <label className="flex flex-1 cursor-pointer items-center gap-3 rounded-lg px-2 py-2 text-sm hover:bg-slate-800/60">
        <input type="checkbox" checked={saved.done.includes(id)} onChange={() => toggle(id)} className="h-4 w-4 accent-brand-600" />
        <span className={saved.done.includes(id) ? "text-slate-500 line-through" : "text-slate-200"}>{label}</span>
      </label>
      {onRemove && (
        <button type="button" onClick={onRemove} aria-label={`Remove ${label}`} className="px-2 text-slate-500 hover:text-slate-200">×</button>
      )}
    </li>
  );

  return (
    <section className="card space-y-4 p-6">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold">Before you go</h3>
        <span className="text-xs text-slate-400">{packed}/{all.length} packed</span>
      </div>
      <div className="h-1.5 rounded-full bg-slate-800" role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100} aria-label="Packing progress">
        <div className="h-1.5 rounded-full bg-brand-500 transition-all" style={{ width: `${pct}%` }} />
      </div>
      {pct === 100 && all.length > 0 && <p className="text-sm text-emerald-300">All packed. Have a great trip!</p>}

      {sections.map((s) => (
        <div key={s.title}>
          <h4 className="px-2 text-xs font-semibold uppercase tracking-wider text-slate-400">{s.title}</h4>
          <ul className="mt-1 space-y-0.5">{s.items.map((i) => row(i.id, i.label))}</ul>
        </div>
      ))}

      <div>
        <h4 className="px-2 text-xs font-semibold uppercase tracking-wider text-slate-400">Your own</h4>
        <ul className="mt-1 space-y-0.5">{saved.custom.map((c) => row("custom:" + c.toLowerCase(), c, () => removeCustom(c)))}</ul>
        <form onSubmit={addCustom} className="mt-2 flex gap-2">
          <input value={draft} onChange={(e) => setDraft(e.target.value)} maxLength={60} placeholder="Add an item" aria-label="Add an item" className="min-w-0 flex-1 rounded-xl border px-3 py-2 text-sm" />
          <button type="submit" className="btn btn-secondary btn-sm" disabled={!draft.trim()}>Add</button>
        </form>
      </div>
    </section>
  );
}
