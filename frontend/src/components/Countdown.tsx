"use client";

import { useEffect, useState } from "react";

function Box({ value, label }: { value: number; label: string }) {
  const text = String(value).padStart(2, "0");
  return (
    <div className="count-box min-w-[3.4rem] rounded-2xl border border-brand-500/30 bg-slate-950/50 px-2 py-2 text-center backdrop-blur">
      {/* key re-mounts on change so the digits tick in */}
      <p key={text} className="tick font-display text-2xl font-semibold leading-none sm:text-3xl">{text}</p>
      <p className="mt-1 text-[10px] font-semibold uppercase tracking-wider text-slate-400">{label}</p>
    </div>
  );
}

/** Live countdown to a moment: days, hours, minutes, seconds, ticking every second. */
export default function Countdown({ to, onDone }: { to: Date; onDone?: () => void }) {
  // Starts empty so the server and browser agree, then fills in on the client.
  const [now, setNow] = useState<number | null>(null);
  const target = to.getTime();

  useEffect(() => {
    setNow(Date.now());
    const t = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(t);
  }, []);

  const left = now === null ? null : Math.max(0, target - now);
  useEffect(() => {
    if (left === 0) onDone?.();
  }, [left, onDone]);

  if (left === null) return <div className="h-[4.25rem]" aria-hidden="true" />;

  const s = Math.floor(left / 1000);
  const d = Math.floor(s / 86400);
  const h = Math.floor((s % 86400) / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = s % 60;

  return (
    <div className="flex items-center gap-2" role="timer" aria-label={`${d} days ${h} hours ${m} minutes to go`}>
      {d > 0 && <Box value={d} label={d === 1 ? "day" : "days"} />}
      <Box value={h} label="hrs" />
      <Box value={m} label="min" />
      <Box value={sec} label="sec" />
    </div>
  );
}
