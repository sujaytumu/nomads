"use client";

import { useEffect, useState } from "react";

const DEFAULTS = ["Government photo ID", "Phone + charger / power bank", "Comfortable walking shoes", "Water bottle", "Cash / UPI ready", "Sunscreen or umbrella", "This itinerary (PDF saved offline)"];

/** A small tick-list for booked trips. Saved on this device only. */
export default function PackingChecklist({ tripId }: { tripId: number | string }) {
  const key = `nomad_pack_${tripId}`;
  const [done, setDone] = useState<string[]>([]);

  useEffect(() => {
    try {
      setDone(JSON.parse(localStorage.getItem(key) || "[]"));
    } catch {
      setDone([]);
    }
  }, [key]);

  const toggle = (item: string) => {
    const next = done.includes(item) ? done.filter((d) => d !== item) : [...done, item];
    setDone(next);
    try {
      localStorage.setItem(key, JSON.stringify(next));
    } catch {
      /* private mode: keep it in memory only */
    }
  };

  return (
    <section className="card space-y-3 p-6">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold">Before you go</h3>
        <span className="text-xs text-slate-400">{done.length}/{DEFAULTS.length} packed</span>
      </div>
      <ul className="space-y-1">
        {DEFAULTS.map((item) => (
          <li key={item}>
            <label className="flex cursor-pointer items-center gap-3 rounded-lg px-2 py-2 text-sm hover:bg-slate-800/60">
              <input type="checkbox" checked={done.includes(item)} onChange={() => toggle(item)} className="h-4 w-4 accent-brand-600" />
              <span className={done.includes(item) ? "text-slate-500 line-through" : "text-slate-200"}>{item}</span>
            </label>
          </li>
        ))}
      </ul>
    </section>
  );
}
