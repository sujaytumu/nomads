"use client";

import { useEffect, useMemo, useState } from "react";

const COLOURS = ["#4f6cff", "#22d3ee", "#a78bfa", "#fbbf24", "#34d399", "#fb7185", "#ffffff"];

/** A one-off burst of confetti (pure CSS, no library). Removes itself when it finishes. */
export default function Confetti({ count = 70 }: { count?: number }) {
  const [alive, setAlive] = useState(true);

  const pieces = useMemo(
    () =>
      Array.from({ length: count }, (_, i) => ({
        id: i,
        left: 50 + (Math.random() - 0.5) * 20,
        dx: (Math.random() - 0.5) * 120,
        rot: Math.random() * 720 - 360,
        delay: Math.random() * 0.25,
        dur: 1.8 + Math.random() * 1.4,
        size: 6 + Math.random() * 7,
        round: Math.random() > 0.6,
        colour: COLOURS[i % COLOURS.length],
        rise: 25 + Math.random() * 35,
      })),
    [count]
  );

  useEffect(() => {
    if (window.matchMedia?.("(prefers-reduced-motion: reduce)").matches) {
      setAlive(false);
      return;
    }
    try { navigator.vibrate?.([30, 40, 30]); } catch { /* not supported */ }
    const t = setTimeout(() => setAlive(false), 3800);
    return () => clearTimeout(t);
  }, []);

  if (!alive) return null;
  return (
    <div aria-hidden="true" className="pointer-events-none fixed inset-0 z-[60] overflow-hidden">
      {pieces.map((p) => (
        <span
          key={p.id}
          className="confetti-piece"
          style={{
            left: `${p.left}%`,
            width: p.size,
            height: p.round ? p.size : p.size * 0.5,
            borderRadius: p.round ? "9999px" : "2px",
            background: p.colour,
            animationDelay: `${p.delay}s`,
            animationDuration: `${p.dur}s`,
            ["--dx" as any]: `${p.dx}vw`,
            ["--rot" as any]: `${p.rot}deg`,
            ["--rise" as any]: `-${p.rise}vh`,
          }}
        />
      ))}
    </div>
  );
}
