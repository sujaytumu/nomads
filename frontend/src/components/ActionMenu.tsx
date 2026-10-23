"use client";

import { useEffect, useRef, useState } from "react";

/** A "⋯ More" button that opens a small tidy list of secondary actions. Closes on outside tap, Escape or choosing an item. */
export default function ActionMenu({ label = "More", children }: { label?: string; children: React.ReactNode }) {
  const [open, setOpen] = useState(false);
  const box = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!open) return;
    const onDown = (e: Event) => {
      if (box.current && !box.current.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && setOpen(false);
    document.addEventListener("pointerdown", onDown);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("pointerdown", onDown);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  return (
    <div ref={box} className="relative">
      <button type="button" aria-haspopup="menu" aria-expanded={open} onClick={() => setOpen((v) => !v)} className="btn btn-secondary btn-sm">
        {label}
        <svg aria-hidden="true" viewBox="0 0 24 24" className={`h-3.5 w-3.5 transition-transform ${open ? "rotate-180" : ""}`} fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
          <path d="M6 9l6 6 6-6" />
        </svg>
      </button>
      {open && (
        <div
          role="menu"
          onClick={(e) => {
            // Close after choosing a link or button inside (not when tapping the menu's padding).
            if ((e.target as HTMLElement).closest("a, button")) setTimeout(() => setOpen(false), 150);
          }}
          className="animate-pop-in absolute right-0 z-20 mt-2 flex w-52 flex-col gap-0.5 rounded-2xl border border-slate-700 bg-slate-900 p-1.5 shadow-2xl shadow-black/60"
        >
          {children}
        </div>
      )}
    </div>
  );
}
