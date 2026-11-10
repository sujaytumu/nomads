"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { nextWeekend, weekendRange, type Weekend } from "@/lib/weekend";

/** "Weekend in 3 days · 14 Nov – 15 Nov", with a button to plan it. Drawn after load so server and browser agree. */
export default function WeekendCard() {
  const [w, setW] = useState<Weekend | null>(null);

  useEffect(() => {
    setW(nextWeekend(new Date()));
  }, []);

  if (!w) return <div className="h-[4.5rem]" aria-hidden="true" />;

  const when = w.inProgress ? "It's the weekend" : w.daysAway === 1 ? "Weekend starts tomorrow" : `Weekend in ${w.daysAway} days`;

  return (
    <div className="card flex items-center justify-between gap-3 p-4">
      <div className="min-w-0">
        <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">{weekendRange(w)}</p>
        <p className="truncate font-display text-lg font-semibold">{when}</p>
      </div>
      <Link href="/trip-planner" className="btn btn-primary btn-sm shrink-0">{w.inProgress ? "Plan now" : "Plan it"}</Link>
    </div>
  );
}
