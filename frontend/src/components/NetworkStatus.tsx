"use client";

import { useEffect, useRef, useState } from "react";
import { clearCache } from "@/lib/api";

/**
 * Two small things that make the app feel alive:
 *  - a banner that slides in when the connection drops (and says "Back online" when it returns)
 *  - pull down from the top of any page on a phone to refresh it
 */
export default function NetworkStatus() {
  const [online, setOnline] = useState(true);
  const [justBack, setJustBack] = useState(false);
  const [pull, setPull] = useState(0);
  const [refreshing, setRefreshing] = useState(false);
  const state = useRef({ startY: 0, active: false, pull: 0 });

  useEffect(() => {
    setOnline(navigator.onLine);
    let t: ReturnType<typeof setTimeout> | undefined;
    const up = () => {
      setOnline(true);
      setJustBack(true);
      t = setTimeout(() => setJustBack(false), 2500);
    };
    const down = () => {
      setOnline(false);
      setJustBack(false);
    };
    window.addEventListener("online", up);
    window.addEventListener("offline", down);
    return () => {
      window.removeEventListener("online", up);
      window.removeEventListener("offline", down);
      if (t) clearTimeout(t);
    };
  }, []);

  useEffect(() => {
    if (window.innerWidth >= 1024) return;
    const SKIP = "input, textarea, select, .leaflet-container, [data-no-swipe], [data-no-pull]";
    const THRESHOLD = 80;

    const onStart = (e: TouchEvent) => {
      const el = e.target as HTMLElement | null;
      const atTop = window.scrollY <= 0 && !document.body.style.overflow;
      state.current = { startY: e.touches[0].clientY, active: atTop && e.touches.length === 1 && !el?.closest?.(SKIP), pull: 0 };
    };
    const onMove = (e: TouchEvent) => {
      const s = state.current;
      if (!s.active) return;
      const dy = e.touches[0].clientY - s.startY;
      if (dy <= 0) {
        if (s.pull) setPull(0);
        s.pull = 0;
        return;
      }
      // Rubber-band: the further you pull, the heavier it feels.
      s.pull = Math.min(110, dy * 0.5);
      setPull(s.pull);
    };
    const onEnd = () => {
      const s = state.current;
      if (!s.active) return;
      s.active = false;
      if (s.pull >= THRESHOLD * 0.5) {
        setRefreshing(true);
        setPull(THRESHOLD * 0.6);
        try { navigator.vibrate?.(10); } catch { /* not supported */ }
        clearCache();
        setTimeout(() => window.location.reload(), 350);
      } else {
        setPull(0);
      }
    };

    document.addEventListener("touchstart", onStart, { passive: true });
    document.addEventListener("touchmove", onMove, { passive: true });
    document.addEventListener("touchend", onEnd, { passive: true });
    document.addEventListener("touchcancel", onEnd, { passive: true });
    return () => {
      document.removeEventListener("touchstart", onStart);
      document.removeEventListener("touchmove", onMove);
      document.removeEventListener("touchend", onEnd);
      document.removeEventListener("touchcancel", onEnd);
    };
  }, []);

  const ready = pull >= 40;

  return (
    <>
      {/* Pull-to-refresh indicator */}
      {(pull > 0 || refreshing) && (
        <div aria-hidden="true" className="pointer-events-none fixed inset-x-0 top-14 z-40 flex justify-center lg:hidden" style={{ transform: `translateY(${Math.max(0, pull - 28)}px)`, opacity: Math.min(1, pull / 40), transition: refreshing || pull === 0 ? "all .25s" : "none" }}>
          <span className="grid h-9 w-9 place-items-center rounded-full border border-brand-500/40 bg-slate-900 text-brand-300 shadow-lg">
            <svg viewBox="0 0 24 24" className={`h-5 w-5 ${refreshing ? "animate-spin" : ""}`} style={refreshing ? undefined : { transform: `rotate(${pull * 3}deg)` }} fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 12a9 9 0 1 1-3-6.7M21 4v5h-5" />
            </svg>
          </span>
          {ready && !refreshing && <span className="sr-only">Release to refresh</span>}
        </div>
      )}

      {/* Offline / back-online banner */}
      {(!online || justBack) && (
        <div role="status" aria-live="polite" className={`banner-in fixed inset-x-3 top-16 z-50 mx-auto flex max-w-md items-center justify-between gap-3 rounded-2xl border px-4 py-2.5 text-sm font-semibold shadow-xl backdrop-blur lg:top-4 ${online ? "border-emerald-500/40 bg-emerald-500/15 text-emerald-200" : "border-amber-500/40 bg-amber-500/15 text-amber-200"}`}>
          <span className="flex items-center gap-2">
            <span className={`h-2 w-2 rounded-full ${online ? "bg-emerald-400" : "bg-amber-400 animate-pulse-ring"}`} aria-hidden="true" />
            {online ? "Back online" : "You're offline. Booked trips still open."}
          </span>
          {!online && (
            <button type="button" className="btn btn-secondary btn-sm" onClick={() => setOnline(navigator.onLine)}>Retry</button>
          )}
        </div>
      )}
    </>
  );
}
