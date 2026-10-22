import Link from "next/link";
import { STAGES, journeyOf, type JourneyInput } from "@/lib/journey";

type Props = JourneyInput & {
  tripId?: number | string | null;
  /** Hide the sentence under the steps. */
  compact?: boolean;
  /** Show a fixed stage instead of reading the trip (used for the "how it works" preview). */
  stage?: number;
};

const COLOURS = [
  "from-sky-400 to-brand-500",
  "from-brand-500 to-violet-500",
  "from-violet-500 to-fuchsia-500",
  "from-fuchsia-500 to-rose-400",
  "from-rose-400 to-amber-400",
  "from-amber-400 to-emerald-400",
];

/** The trip's real-life journey: Plan > Pay > Booked > Pickup > Trip day > Review, with where you are now. */
export default function TripProgress({ status, tripDate, plans, tripId, compact, stage }: Props) {
  const j = journeyOf({ status, tripDate, plans });
  if (j.cancelled && stage === undefined) return <p className="text-xs text-red-300">{j.message}</p>;
  const current = stage ?? j.current;
  const here = STAGES[current];
  const link = here?.href && tripId != null ? here.href(tripId) : null;

  return (
    <div className="space-y-2">
      <ol className="flex items-start" aria-label="Trip journey">
        {STAGES.map((s, i) => {
          const done = i < current;
          const now = i === current;
          return (
            <li key={s.key} className="relative flex min-w-0 flex-1 flex-col items-center gap-1 text-center" aria-current={now ? "step" : undefined}>
              {i > 0 && (
                <span aria-hidden="true" className={`absolute right-1/2 top-3.5 h-0.5 w-full -translate-y-1/2 ${i <= current ? "bg-gradient-to-r from-brand-500 to-ocean-400" : "bg-slate-800"}`} />
              )}
              <span
                className={`relative z-10 grid h-7 w-7 place-items-center rounded-full text-[13px] transition-all ${
                  done
                    ? `bg-gradient-to-br ${COLOURS[i]} text-white shadow-md`
                    : now
                      ? `bg-gradient-to-br ${COLOURS[i]} text-white shadow-glow ring-4 ring-brand-500/20 ${stage === undefined ? "animate-pulse-ring" : ""}`
                      : "border border-slate-700 bg-slate-900 text-slate-500 grayscale"
                }`}
              >
                {done ? "✓" : s.icon}
              </span>
              <span className={`w-full truncate text-[10px] leading-tight sm:text-[11px] ${now ? "font-bold text-white" : done ? "text-slate-300" : "text-slate-500"}`}>{s.label}</span>
            </li>
          );
        })}
      </ol>
      {!compact && (
        <p className="flex flex-wrap items-center gap-x-2 text-xs text-slate-400">
          <span>{stage === undefined ? j.message : here?.hint}</span>
          {link && stage === undefined && <Link href={link} className="font-semibold text-brand-300 hover:text-brand-200">{here.label} →</Link>}
        </p>
      )}
    </div>
  );
}
