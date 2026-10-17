const TONES: Record<string, { bg: string; icon: string }> = {
  FOOD: { bg: "from-orange-300 to-amber-200", icon: "🍜" },
  CULTURE: { bg: "from-fuchsia-300 to-rose-200", icon: "🏛️" },
  NATURE: { bg: "from-emerald-300 to-teal-200", icon: "🌿" },
  ADVENTURE: { bg: "from-red-300 to-orange-200", icon: "🧗" },
  SHOPPING: { bg: "from-pink-300 to-rose-200", icon: "🛍️" },
  NIGHTLIFE: { bg: "from-indigo-300 to-violet-200", icon: "🌙" },
  RELAXATION: { bg: "from-cyan-300 to-teal-200", icon: "🧘" },
  PACKAGE: { bg: "from-brand-200 to-ocean-100", icon: "🎒" },
};

/** A photo when there is one; otherwise a soft coloured tile with the category icon (never a grey "coming soon" box). */
export default function Cover({ src, alt = "", kind = "PACKAGE", className = "h-44" }: { src?: string | null; alt?: string; kind?: string; className?: string }) {
  if (src) return <img src={src} alt={alt} loading="lazy" className={`w-full object-cover ${className}`} />;
  const tone = TONES[kind] || TONES.PACKAGE;
  return (
    <div aria-hidden="true" className={`grid w-full place-items-center bg-gradient-to-br ${tone.bg} ${className}`}>
      <span className="text-5xl drop-shadow-sm">{tone.icon}</span>
    </div>
  );
}
