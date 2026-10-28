"use client";

import { useEffect, useRef } from "react";
import { getCached } from "@/lib/api";
import { notifyOnce, pruneSeen, remindersOn } from "@/lib/reminders";
import { pushActive, pushSupported, subscribePush } from "@/lib/push";
import { formatCost, isPayable, tripTitle } from "@/lib/tripStatus";

const DAY = 86_400_000;
const mins = (t?: string | null) => {
  const [h, m] = (t || "").slice(0, 5).split(":").map(Number);
  return Number.isFinite(h) ? h * 60 + (m || 0) : null;
};

/** Watches your trips and nudges you at the right moments. Renders nothing. */
export default function Reminders({ updateReady, build }: { updateReady: boolean; build: string }) {
  const ready = useRef(updateReady);
  ready.current = updateReady;

  // People who turned reminders on before push existed get signed up quietly.
  useEffect(() => {
    if (!remindersOn() || !pushSupported()) return;
    pushActive().then((on) => (on ? undefined : subscribePush()));
  }, []);

  useEffect(() => {
    let stop = false;

    const run = async () => {
      if (stop || !remindersOn()) return;
      const keep = new Set<string>();
      const day = new Date().toISOString().slice(0, 10);

      if (ready.current) {
        const tag = `update-${build}`;
        keep.add(tag);
        await notifyOnce(tag, "A new version of NOMAD is ready", "Open the app and tap Update.", "/profile");
      }

      try {
        const trips = (await getCached<any[]>("/api/trips/me", { ttl: 60000 })) || [];
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const nowMin = new Date().getHours() * 60 + new Date().getMinutes();

        for (const t of trips) {
          const id = t.tripRequestId;
          if (isPayable(t.status)) {
            const tag = `pay-${id}-${day}`;
            keep.add(tag);
            await notifyOnce(tag, `Finish booking your ${tripTitle(t)}`, `${formatCost(t.estimatedCost)} is waiting to be paid.`, `/payment?tripRequestId=${id}`);
          }
          if (t.status !== "CONFIRMED" || !t.tripDate) continue;
          const start = new Date(`${t.tripDate}T00:00:00`);
          const daysTo = Math.round((start.getTime() - today.getTime()) / DAY);
          if (daysTo === 1) {
            const tag = `tomorrow-${id}`;
            keep.add(tag);
            await notifyOnce(tag, `${tripTitle(t)} is tomorrow`, "Check the weather, pack, and arrange your pickup.", `/pickup?tripId=${id}`);
          }
          if (daysTo === 0) {
            const tag = `today-${id}`;
            keep.add(tag);
            await notifyOnce(tag, "Your trip starts today", "Open NOMAD for today's plan and directions.", "/");
          }
          // During the trip: a heads-up before each stop.
          const dayNumber = Math.max(1, 1 - daysTo);
          if (daysTo <= 0) {
            for (const p of t.plans || []) {
              if ((p.dayNumber || 1) !== dayNumber) continue;
              const a = mins(p.startTime);
              if (a === null || a - nowMin > 15 || a - nowMin < 0) continue;
              const tag = `stop-${id}-${day}-${p.placeName}`;
              keep.add(tag);
              await notifyOnce(tag, `Next: ${p.placeName}`, `Starts at ${String(p.startTime).slice(0, 5)}. Tap for directions.`, "/");
            }
          }
        }
      } catch {
        /* offline or server waking up: try again next time */
      }
      // Keep today's markers only.
      pruneSeen(keep);
    };

    run();
    const timer = setInterval(run, 5 * 60 * 1000);
    const onVisible = () => document.visibilityState === "visible" && run();
    document.addEventListener("visibilitychange", onVisible);
    return () => {
      stop = true;
      clearInterval(timer);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [build]);

  return null;
}
