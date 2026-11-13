"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { formatSpan, nextLongWeekend, type LongWeekend } from "@/lib/longWeekend";
import { nextWeekend, weekendRange, type Weekend } from "@/lib/weekend";

/** "Weekend in 3 days · 14 Nov – 15 Nov", with a button to plan it. Drawn after load so server and browser agree. */
export default function WeekendCard() {
  const [w, setW] = useState<Weekend | null>(null);
  const [long, setLong] = useState<LongWeekend | null>(null);

  useEffect(() => {
    const now = new Date();
    setW(nextWeekend(now));
    setLong(nextLongWeekend(now, 30));
  }, []);

  if (!w) return <div className="h-[4.5rem]" aria-hidden="true" />;

  const when = w.inProgress ? "It's the weekend" : w.daysAway === 1 ? "Weekend starts tomorrow" : `Weekend in ${w.daysAway} days`;

  const longLine = long ? (
    <p className="mt-0.5 text-xs font-semibold text-amber-300">
      Long weekend: {formatSpan(long)} ({long.holiday}){long.daysAway === 0 ? "" : ` · in ${long.daysAway} day${long.daysAway === 1 ? "" : "s"}`}
      <span className="block font-normal text-slate-400">Trips over 2 days are for groups.</span>
    </p>
  ) : null;

  return (
    <div className="card flex items-center justify-between gap-3 p-4">
      <div className="min-w-0">
        <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">{weekendRange(w)}</p>
        <p className="truncate font-display text-lg font-semibold">{when}</p>
        {longLine}
      </div>
      <Link href={long ? "/trip-planner?days=3&mode=GROUP" : "/trip-planner"} className="btn btn-primary btn-sm shrink-0">{long ? "Plan 3 days" : w.inProgress ? "Plan now" : "Plan it"}</Link>
    </div>
  );
}
