"use client";

import { useEffect, useState } from "react";

import { Bone } from "@/components/Skeleton";
import { fetchTravelStats, type TravelStats as Stats } from "@/lib/statsApi";

const money = (n: number) => `₹${Math.round(n).toLocaleString("en-IN")}`;

/** Level, XP bar, a few numbers, and every badge with how close you are. Reads /api/me/stats. */
export default function TravelStats() {
  const [s, setS] = useState<Stats | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let live = true;
    fetchTravelStats().then((d) => live && setS(d)).catch(() => live && setFailed(true));
    return () => {
      live = false;
    };
  }, []);

  if (failed) return null;
  if (!s) {
    return (
      <div className="card space-y-3 p-5" aria-busy="true" aria-label="Loading your travel stats">
        <Bone className="h-6 w-1/3" />
        <Bone className="h-3 w-full rounded-full" />
        <div className="grid grid-cols-4 gap-2">{[0, 1, 2, 3].map((i) => <Bone key={i} className="h-14" />)}</div>
      </div>
    );
  }

  const earned = s.badges.filter((b) => b.earned);
  const locked = s.badges.filter((b) => !b.earned);

  return (
    <section className="card pop-in space-y-4 p-5" aria-label="Your travel stats">
      <div>
        <div className="flex items-end justify-between gap-3">
          <div>
            <p className="eyebrow">Level {s.level.level}</p>
            <p className="font-display text-2xl font-semibold">{s.level.title}</p>
          </div>
          <p className="text-xs text-slate-400">{s.level.xp.toLocaleString("en-IN")} XP</p>
        </div>
        <div className="mt-2 h-2 overflow-hidden rounded-full bg-slate-800" role="progressbar" aria-valuenow={s.level.percentToNext} aria-valuemin={0} aria-valuemax={100} aria-label="Progress to next level">
          <div className="h-full rounded-full bg-gradient-to-r from-[#feda75] via-[#d62976] to-[#962fbf] transition-all duration-1000" style={{ width: `${s.level.percentToNext}%` }} />
        </div>
        <p className="mt-1 text-xs text-slate-500">{s.level.xpForNextLevel - s.level.xpIntoLevel} XP to level {s.level.level + 1}</p>
      </div>

      <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
        {[
          ["Trips", String(s.bookedTrips)],
          ["Places", String(s.placesVisited)],
          ["Cities", String(s.citiesVisited)],
          ["Distance", `${s.kmTravelled.toFixed(1)} km`],
        ].map(([label, value]) => (
          <div key={label} className="rounded-2xl bg-slate-800/70 p-3 text-center">
            <p className="font-display text-xl font-semibold">{value}</p>
            <p className="text-[11px] text-slate-400">{label}</p>
          </div>
        ))}
      </div>
      {s.totalSpent > 0 && <p className="text-xs text-slate-400">Spent on trips: {money(s.totalSpent)}{s.favouriteInterest ? ` · Favourite: ${s.favouriteInterest.charAt(0) + s.favouriteInterest.slice(1).toLowerCase()}` : ""}</p>}

      <div>
        <p className="mb-2 text-sm font-semibold">Badges <span className="font-normal text-slate-400">{s.badgesEarned}/{s.badgesTotal}</span></p>
        <ul className="grid grid-cols-3 gap-2 sm:grid-cols-5">
          {earned.map((b) => (
            <li key={b.code} title={b.description} className="flex flex-col items-center gap-1 rounded-2xl border border-brand-500/40 bg-brand-600/10 p-2 text-center">
              <span aria-hidden="true" className="text-2xl">{b.icon}</span>
              <span className="text-[11px] font-semibold leading-tight">{b.title}</span>
            </li>
          ))}
          {locked.map((b) => (
            <li key={b.code} title={b.description} className="flex flex-col items-center gap-1 rounded-2xl border border-slate-800 p-2 text-center opacity-60">
              <span aria-hidden="true" className="text-2xl grayscale">{b.icon}</span>
              <span className="text-[11px] leading-tight">{b.title}</span>
              <span className="text-[10px] text-slate-400">{b.current}/{b.target}</span>
            </li>
          ))}
        </ul>
        {s.nextBadge && <p className="mt-2 text-xs text-slate-400">Next: <span className="font-semibold text-slate-200">{s.nextBadge.title}</span>, {s.nextBadge.description.toLowerCase()} ({s.nextBadge.current}/{s.nextBadge.target})</p>}
      </div>
    </section>
  );
}
