"use client";

import { useEffect, useState } from "react";

/**
 * One calm line under a loading screen. Plain "Loading…" at first, then (if the free server is
 * waking up) a live counter so the wait never looks stuck.
 */
export default function LoadingHint({ label = "Loading" }: { label?: string }) {
  const [seconds, setSeconds] = useState(0);
  useEffect(() => {
    const started = Date.now();
    const t = setInterval(() => setSeconds(Math.floor((Date.now() - started) / 1000)), 1000);
    return () => clearInterval(t);
  }, []);

  return (
    <p role="status" aria-live="polite" className="flex items-center gap-2 text-xs text-slate-400">
      <svg aria-hidden="true" viewBox="0 0 24 24" className="h-3.5 w-3.5 shrink-0 animate-spin" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round">
        <path d="M12 3a9 9 0 1 0 9 9" />
      </svg>
      {seconds < 4
        ? `${label}…`
        : seconds < 60
        ? `Waking up the server… ${seconds}s. The first load after a quiet spell can take up to a minute.`
        : `Still waking up… ${seconds}s. Almost there.`}
    </p>
  );
}
