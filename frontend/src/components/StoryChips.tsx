"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

const INTERESTS = [
  { key: "FOOD", label: "Food", icon: "🍜" },
  { key: "CULTURE", label: "Culture", icon: "🏛️" },
  { key: "NATURE", label: "Nature", icon: "🌿" },
  { key: "ADVENTURE", label: "Adventure", icon: "🧗" },
  { key: "SHOPPING", label: "Shopping", icon: "🛍️" },
  { key: "NIGHTLIFE", label: "Nightlife", icon: "🌙" },
  { key: "RELAXATION", label: "Relax", icon: "🧘" },
];

const SEEN_KEY = "nomad_seen_interests";

/**
 * Instagram "stories" row: round pictures with a colourful ring, name underneath.
 * Open one and its ring turns grey, like a story you have already seen.
 */
export default function StoryChips() {
  const [seen, setSeen] = useState<string[]>([]);

  useEffect(() => {
    try {
      setSeen(JSON.parse(localStorage.getItem(SEEN_KEY) || "[]"));
    } catch {
      /* ignore */
    }
  }, []);

  const markSeen = (key: string) => {
    setSeen((prev) => {
      if (prev.includes(key)) return prev;
      const next = [...prev, key];
      try {
        localStorage.setItem(SEEN_KEY, JSON.stringify(next));
      } catch {
        /* ignore */
      }
      return next;
    });
  };

  return (
    <nav aria-label="Pick a mood" className="-mx-1 flex gap-4 overflow-x-auto px-1 pb-1 pt-1 sm:gap-5">
      {INTERESTS.map((i) => {
        const done = seen.includes(i.key);
        return (
          <Link key={i.key} href={`/trip-planner?interest=${i.key}`} onClick={() => markSeen(i.key)} className="story group flex w-[4.4rem] shrink-0 flex-col items-center gap-1.5 text-center sm:w-20">
            <span className={`story-ring grid h-[4.4rem] w-[4.4rem] place-items-center rounded-full p-[3px] sm:h-20 sm:w-20 ${done ? "story-ring-seen" : ""}`}>
              <span className="grid h-full w-full place-items-center rounded-full border-[3px] border-slate-950 bg-slate-800 text-3xl transition-transform group-active:scale-90">
                <span aria-hidden="true">{i.icon}</span>
              </span>
            </span>
            <span className={`w-full truncate text-xs ${done ? "text-slate-400" : "text-slate-100"}`}>{i.label}</span>
          </Link>
        );
      })}
    </nav>
  );
}
