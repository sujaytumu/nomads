"use client";

import { useEffect, useState } from "react";

type Phase = "morning" | "day" | "evening" | "night";

function phaseOf(h: number): Phase {
  if (h >= 5 && h < 11) return "morning";
  if (h >= 11 && h < 17) return "day";
  if (h >= 17 && h < 20) return "evening";
  return "night";
}

const GREETING: Record<Phase, string> = {
  morning: "Good morning",
  day: "Good afternoon",
  evening: "Good evening",
  night: "Late night planning",
};

/** Time-of-day greeting for the home hero ("Good evening, Sujay"). */
export function useGreeting(name: string) {
  const [phase, setPhase] = useState<Phase>("day");
  useEffect(() => {
    const tick = () => setPhase(phaseOf(new Date().getHours()));
    tick();
    const t = setInterval(tick, 60000);
    return () => clearInterval(t);
  }, []);
  return { phase, greeting: name ? `${GREETING[phase]}, ${name}` : GREETING[phase] };
}

/**
 * Decorative hero background: a sun or moon for the time of day, drifting clouds, twinkling stars at night,
 * and a plane that flies along a dotted route forever. Pure SVG + CSS, hidden from screen readers.
 */
export default function HeroScene({ phase }: { phase: Phase }) {
  const night = phase === "night";
  const orb = night ? "#e2e8f0" : phase === "evening" ? "#fb923c" : "#fde68a";
  const orbY = phase === "morning" ? 78 : phase === "day" ? 34 : phase === "evening" ? 84 : 40;

  return (
    <div aria-hidden="true" className="pointer-events-none absolute inset-0 overflow-hidden">
      {/* sky tint that changes with the hour */}
      <div className="absolute inset-0 transition-colors duration-1000" style={{ background: night ? "linear-gradient(180deg, rgba(2,6,23,.55), transparent 70%)" : phase === "evening" ? "linear-gradient(180deg, rgba(251,146,60,.22), transparent 70%)" : "linear-gradient(180deg, rgba(255,255,255,.10), transparent 70%)" }} />

      <svg viewBox="0 0 400 200" preserveAspectRatio="xMaxYMid slice" className="absolute inset-y-0 right-0 h-full w-full opacity-95 sm:w-3/4">
        {night && [[40, 30], [120, 18], [210, 44], [300, 22], [350, 60], [90, 70], [260, 80]].map(([x, y], i) => (
          <circle key={i} cx={x} cy={y} r={1.4} fill="#fff" className="star" style={{ animationDelay: `${i * 0.37}s` }} />
        ))}

        {/* sun / moon */}
        <circle cx="330" cy={orbY} r="20" fill={orb} opacity={night ? 0.95 : 0.9} className="orb" />
        {night && <circle cx="338" cy={orbY - 4} r="17" fill="#1B2559" opacity="0.9" />}

        {/* clouds */}
        <g className="cloud cloud-a" fill="#fff" opacity="0.22"><ellipse cx="70" cy="48" rx="26" ry="9" /><ellipse cx="90" cy="42" rx="18" ry="9" /></g>
        <g className="cloud cloud-b" fill="#fff" opacity="0.16"><ellipse cx="240" cy="96" rx="30" ry="10" /><ellipse cx="262" cy="90" rx="20" ry="10" /></g>

        {/* route + plane */}
        <path id="hero-route" d="M10 170 C 90 170, 110 90, 190 110 S 300 150, 390 70" fill="none" stroke="#fff" strokeOpacity="0.55" strokeWidth="2" strokeDasharray="2 8" strokeLinecap="round" className="route-dash" />
        <circle cx="10" cy="170" r="4" fill="none" stroke="#fff" strokeWidth="2" />
        <g>
          <g>
            <path d="M12 0 L-9 -8 L-4 0 L-9 8 Z" fill="#fff" />
            <path d="M-4 0 L-9 8 L2 2 Z" fill="#c4d6ff" />
          </g>
          <animateMotion dur="9s" repeatCount="indefinite" rotate="auto" keyPoints="0;1" keyTimes="0;1" calcMode="linear">
            <mpath href="#hero-route" />
          </animateMotion>
        </g>
      </svg>
    </div>
  );
}
