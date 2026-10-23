"use client";

import { useEffect, useState } from "react";

type Theme = "dark" | "light";

export function applyTheme(t: Theme) {
  if (t === "light") document.documentElement.setAttribute("data-theme", "light");
  else document.documentElement.removeAttribute("data-theme");
  try {
    localStorage.setItem("nomad_theme", t);
  } catch {
    /* private mode: the choice just lasts until you close the app */
  }
  const meta = document.querySelector('meta[name="theme-color"]');
  if (meta) meta.setAttribute("content", t === "light" ? "#f3f6fc" : "#020617");
}

/** Dark / Light switch. Dark is the default. */
export default function ThemeToggle() {
  const [theme, setTheme] = useState<Theme>("dark");

  useEffect(() => {
    setTheme(document.documentElement.getAttribute("data-theme") === "light" ? "light" : "dark");
  }, []);

  const pick = (t: Theme) => {
    setTheme(t);
    applyTheme(t);
  };

  return (
    <div role="radiogroup" aria-label="Appearance" className="inline-grid grid-cols-2 gap-1 rounded-full border border-slate-700 bg-slate-900 p-1">
      {([["dark", "🌙 Dark"], ["light", "☀️ Light"]] as const).map(([t, label]) => (
        <button
          key={t}
          type="button"
          role="radio"
          aria-checked={theme === t}
          onClick={() => pick(t)}
          className={`rounded-full px-4 py-1.5 text-xs font-semibold transition-all ${theme === t ? "bg-brand-600 text-white shadow-md shadow-brand-600/30" : "text-slate-300 hover:text-slate-100"}`}
        >
          {label}
        </button>
      ))}
    </div>
  );
}
