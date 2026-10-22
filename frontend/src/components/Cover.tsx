"use client";

import { useState } from "react";

const TONES: Record<string, { bg: string; icon: string }> = {
  FOOD: { bg: "from-orange-700 to-amber-900", icon: "🍜" },
  CULTURE: { bg: "from-fuchsia-700 to-rose-900", icon: "🏛️" },
  NATURE: { bg: "from-emerald-700 to-teal-900", icon: "🌿" },
  ADVENTURE: { bg: "from-red-700 to-orange-900", icon: "🧗" },
  SHOPPING: { bg: "from-pink-700 to-rose-900", icon: "🛍️" },
  NIGHTLIFE: { bg: "from-indigo-700 to-violet-900", icon: "🌙" },
  RELAXATION: { bg: "from-cyan-700 to-teal-900", icon: "🧘" },
  PACKAGE: { bg: "from-brand-700 to-slate-900", icon: "🎒" },
};

/** A photo when there is one (falling back to a tile if it fails to load); otherwise a layered coloured tile with the category icon. */
export default function Cover({ src, alt = "", kind = "PACKAGE", className = "h-44" }: { src?: string | null; alt?: string; kind?: string; className?: string }) {
  const [broken, setBroken] = useState(false);
  if (src && !broken) {
    return (
      <img
        src={src}
        alt={alt}
        loading="lazy"
        onError={() => setBroken(true)}
        className={`w-full bg-slate-800 object-cover transition-transform duration-500 group-hover:scale-105 ${className}`}
      />
    );
  }
  const tone = TONES[kind] || TONES.PACKAGE;
  return (
    <div aria-hidden="true" className={`relative grid w-full place-items-center overflow-hidden bg-gradient-to-br ${tone.bg} ${className}`}>
      <div className="absolute -left-8 -top-10 h-40 w-40 rounded-full bg-white/10 blur-2xl" />
      <div className="absolute -bottom-12 -right-8 h-44 w-44 rounded-full bg-white/5" />
      <span className="absolute -bottom-4 right-3 select-none text-[6.5rem] leading-none opacity-20">{tone.icon}</span>
      <span className="relative grid h-16 w-16 place-items-center rounded-2xl bg-white/10 text-4xl shadow-lg shadow-black/30 ring-1 ring-white/20 backdrop-blur-sm">{tone.icon}</span>
    </div>
  );
}
