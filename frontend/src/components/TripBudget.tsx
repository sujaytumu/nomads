"use client";

import { useEffect, useMemo, useState } from "react";
import { fetchEssentials, Stay } from "@/lib/essentialsApi";
import { formatCost } from "@/lib/tripStatus";

type Plan = { dayNumber: number; distanceFromPrevious?: number | null };
type Tier = "NONE" | "BUDGET" | "COMFORT" | "PREMIUM";

const FOOD_PER_DAY: Record<Tier, number> = { NONE: 350, BUDGET: 350, COMFORT: 650, PREMIUM: 1200 };
const TIER_LABEL: Record<Tier, string> = { NONE: "No stay", BUDGET: "Budget", COMFORT: "Comfort", PREMIUM: "Premium" };
const CAB_PER_KM = 14; // a typical city cab fare per km
const ROAD_FACTOR = 1.3; // roads are longer than the straight line between two places
const rupees = (n: number) => `₹${Math.round(n).toLocaleString("en-IN")}`;

function read(key: string): any {
  try {
    const v = localStorage.getItem(key);
    return v ? JSON.parse(v) : null;
  } catch {
    return null;
  }
}

/** A whole-trip money estimate: NOMAD price, stay, food and getting around, split per person. */
export default function TripBudget({ tripId, days, plans, nomadCost }: { tripId: number; days: number; plans: Plan[]; nomadCost: number | string | null | undefined }) {
  const key = `nomad_budget_${tripId}`;
  const [people, setPeople] = useState(1);
  const [tier, setTier] = useState<Tier>("COMFORT");
  const [stays, setStays] = useState<Stay[]>([]);

  useEffect(() => {
    const saved = read(key);
    if (saved?.people) setPeople(Math.max(1, Math.min(8, Number(saved.people) || 1)));
    if (saved?.tier && saved.tier in TIER_LABEL) setTier(saved.tier);
  }, [key]);

  useEffect(() => {
    try {
      localStorage.setItem(key, JSON.stringify({ people, tier }));
    } catch {
      /* ignore */
    }
  }, [key, people, tier]);

  useEffect(() => {
    let active = true;
    fetchEssentials(tripId).then((d) => active && setStays(d.stay || [])).catch(() => active && setStays([]));
    return () => {
      active = false;
    };
  }, [tripId]);

  const calc = useMemo(() => {
    const nights = Math.max(0, days - 1);
    const rooms = Math.ceil(people / 2);
    const rate = tier === "NONE" ? 0 : stays.find((s) => s.tier === tier)?.pricePerNight ?? { BUDGET: 1500, COMFORT: 3200, PREMIUM: 6500 }[tier];
    const stay = nights * rooms * rate;
    const food = days * people * FOOD_PER_DAY[tier];
    // Route length, plus the trip back from the last stop of each day
    const byDay = new Map<number, number[]>();
    plans.forEach((p) => byDay.set(p.dayNumber, [...(byDay.get(p.dayNumber) ?? []), Number(p.distanceFromPrevious) || 0]));
    let km = 0;
    byDay.forEach((legs) => {
      km += legs.reduce((a, b) => a + b, 0) + (legs[0] || 0);
    });
    const cabs = Math.ceil(people / 4);
    const travel = km * ROAD_FACTOR * CAB_PER_KM * cabs;
    const nomad = Number(nomadCost) || 0;
    const total = nomad + stay + food + travel;
    return { nights, rooms, stay, food, travel, km: km * ROAD_FACTOR, cabs, nomad, total };
  }, [days, people, tier, stays, plans, nomadCost]);

  const rows: [string, string, number][] = [
    ["NOMAD itinerary", "Your stops, as priced above", calc.nomad],
    ["Stay", tier === "NONE" || calc.nights === 0 ? "Not needed" : `${calc.nights} night${calc.nights > 1 ? "s" : ""} × ${calc.rooms} room${calc.rooms > 1 ? "s" : ""}`, calc.stay],
    ["Food", `${days} day${days > 1 ? "s" : ""} × ${people} ${people > 1 ? "people" : "person"}`, calc.food],
    ["Getting around", `about ${Math.round(calc.km)} km by cab${calc.cabs > 1 ? `, ${calc.cabs} cabs` : ""}`, calc.travel],
  ];

  return (
    <section className="card space-y-4 p-6">
      <div>
        <h3 className="text-lg font-semibold">Trip budget</h3>
        <p className="text-sm text-slate-400">What the whole trip may cost, split between everyone.</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-2">
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">People</p>
          <div className="flex items-center gap-3">
            <button type="button" className="btn btn-secondary h-10 w-10 p-0" aria-label="Fewer people" disabled={people <= 1} onClick={() => setPeople((p) => Math.max(1, p - 1))}>−</button>
            <span className="min-w-[2ch] text-center text-xl font-bold" aria-live="polite">{people}</span>
            <button type="button" className="btn btn-secondary h-10 w-10 p-0" aria-label="More people" disabled={people >= 8} onClick={() => setPeople((p) => Math.min(8, p + 1))}>+</button>
          </div>
        </div>
        <div className="space-y-2">
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Comfort level</p>
          <div className="flex flex-wrap gap-2">
            {(Object.keys(TIER_LABEL) as Tier[]).map((t) => (
              <button
                key={t}
                type="button"
                onClick={() => setTier(t)}
                aria-pressed={tier === t}
                className={`rounded-full border px-3 py-1.5 text-sm font-semibold transition-colors ${tier === t ? "border-brand-600 bg-brand-600 text-white" : "border-slate-700 bg-slate-900 text-slate-300 hover:bg-slate-800"}`}
              >
                {TIER_LABEL[t]}
              </button>
            ))}
          </div>
        </div>
      </div>

      <ul className="divide-y divide-slate-800 rounded-2xl border border-slate-800">
        {rows.map(([label, note, amount]) => (
          <li key={label} className="flex items-center justify-between gap-3 px-4 py-3">
            <div className="min-w-0">
              <p className="text-sm font-semibold">{label}</p>
              <p className="text-xs text-slate-400">{note}</p>
            </div>
            <p className="font-semibold">{amount > 0 ? rupees(amount) : "—"}</p>
          </li>
        ))}
        <li className="flex items-center justify-between gap-3 bg-slate-800/40 px-4 py-3">
          <p className="font-bold">Total</p>
          <p className="text-xl font-bold">{rupees(calc.total)}</p>
        </li>
      </ul>

      <div className="flex flex-wrap items-center justify-between gap-2 rounded-2xl bg-brand-600/10 px-4 py-3">
        <p className="text-sm text-slate-300">{people > 1 ? "Each person pays about" : "You'll spend about"}</p>
        <p className="text-2xl font-bold text-brand-300">{rupees(calc.total / people)}</p>
      </div>
      <p className="text-xs text-slate-500">Only the NOMAD itinerary ({formatCost(calc.nomad)}) is charged here. Stay, food and cab costs are rough estimates to help you plan, and you pay them directly.</p>
    </section>
  );
}
