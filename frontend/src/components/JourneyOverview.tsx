"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";

import { gettingThere } from "@/lib/gettingThere";
import { journeySteps, type Step } from "@/lib/journeyChecklist";
import { packingPlan, type PackInput } from "@/lib/packingPlan";

type Plan = { dayNumber: number; distanceFromPrevious?: number | null };

const ICON: Record<Step["state"], string> = { done: "✓", todo: "!", info: "•" };
const TONE: Record<Step["state"], string> = {
  done: "bg-emerald-500/20 text-emerald-300",
  todo: "bg-amber-500/20 text-amber-300",
  info: "bg-slate-700/60 text-slate-300",
};

/** The whole trip in order, from booking to the review, with what is done and what still needs you. */
export default function JourneyOverview({ trip, plans }: { trip: PackInput & { tripRequestId: number | string; status?: string; days?: number | string; tripDate?: string | null }; plans: Plan[] }) {
  const [packed, setPacked] = useState(0);
  const days = Math.max(1, Number(trip.days) || 1);

  const packTotal = useMemo(
    () => packingPlan({ ...trip, weekendType: trip.weekendType ?? undefined }).reduce((n, s) => n + s.items.length, 0),
    [trip.interest, trip.weekendType, trip.travelMode, trip.groupSize, trip.pickupRequired, trip.tripDate] // eslint-disable-line react-hooks/exhaustive-deps
  );

  useEffect(() => {
    try {
      const raw = JSON.parse(localStorage.getItem(`nomad_pack2_${trip.tripRequestId}`) || "null");
      const ids = new Set(packingPlan(trip).flatMap((s) => s.items.map((i) => i.id)));
      const done: string[] = Array.isArray(raw?.done) ? raw.done : [];
      const custom: string[] = Array.isArray(raw?.custom) ? raw.custom : [];
      // Your own items count too, so the total here matches the checklist.
      setPacked(done.filter((d) => ids.has(d) || d.startsWith("custom:")).length);
      setCustomCount(custom.length);
    } catch {
      /* no saved ticks yet */
    }
  }, [trip.tripRequestId]); // eslint-disable-line react-hooks/exhaustive-deps
  const [customCount, setCustomCount] = useState(0);

  const first = plans.find((p) => p.dayNumber === 1) ?? plans[0];
  const journey = useMemo(() => gettingThere(Number(first?.distanceFromPrevious), Number(trip.groupSize) || 1, days >= 2), [first, trip.groupSize, days]);

  const steps = journeySteps({
    tripId: trip.tripRequestId,
    status: trip.status,
    tripDate: trip.tripDate,
    days,
    stops: plans.length,
    pickupRequired: trip.pickupRequired,
    packed,
    packTotal: packTotal + customCount,
    quickestHours: journey?.quickestHours ?? null,
    today: new Date(),
  });

  const todo = steps.filter((s) => s.state === "todo").length;

  return (
    <section className="card space-y-3 p-6" aria-label="Your journey">
      <div className="flex items-center justify-between gap-2">
        <h3 className="text-lg font-semibold">Your journey</h3>
        <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${todo === 0 ? "bg-emerald-500/15 text-emerald-300" : "bg-amber-500/15 text-amber-300"}`}>
          {todo === 0 ? "All set" : `${todo} to do`}
        </span>
      </div>
      <ol className="space-y-1">
        {steps.map((s) => {
          const body = (
            <span className="flex items-start gap-3">
              <span aria-hidden="true" className={`mt-0.5 grid h-6 w-6 shrink-0 place-items-center rounded-full text-xs font-bold ${TONE[s.state]}`}>{ICON[s.state]}</span>
              <span className="min-w-0">
                <span className="block text-[10px] font-semibold uppercase tracking-wider text-slate-500">{s.phase}</span>
                <span className="block text-sm font-semibold">{s.title}</span>
                <span className="block text-xs text-slate-400">{s.detail}</span>
              </span>
            </span>
          );
          return (
            <li key={s.key}>
              {s.href ? <Link href={s.href} className="block rounded-xl px-2 py-2 hover:bg-slate-800/60">{body}</Link> : <div className="px-2 py-2">{body}</div>}
            </li>
          );
        })}
      </ol>
    </section>
  );
}
