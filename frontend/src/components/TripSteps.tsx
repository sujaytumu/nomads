import Link from "next/link";

// Shows where the user is in: Plan -> Review -> Pay -> Booked, and lets them
// jump back to earlier steps for the same trip.

type Props = {
  /** 1 Plan, 2 Review, 3 Pay, 4 Booked */
  current: 1 | 2 | 3 | 4;
  tripId?: number | string | null;
};

export default function TripSteps({ current, tripId }: Props) {
  const hasTrip = tripId !== undefined && tripId !== null && tripId !== "";
  const steps = [
    { n: 1, label: "Plan", href: "/trip-planner" },
    { n: 2, label: "Review", href: hasTrip ? `/trip-summary?tripId=${tripId}` : undefined },
    { n: 3, label: "Pay", href: hasTrip ? `/payment?tripRequestId=${tripId}` : undefined },
    { n: 4, label: "Booked", href: "/bookings" },
  ];

  return (
    <ol className="flex items-center gap-2 text-sm" aria-label="Trip progress">
      {steps.map((step, i) => {
        const done = step.n < current;
        const active = step.n === current;
        const circle = done
          ? "bg-brand-600 text-white"
          : active
            ? "border-2 border-brand-500 text-slate-100"
            : "border border-slate-700 text-slate-500";
        const content = (
          <span className="flex items-center gap-2">
            <span className={`grid h-6 w-6 place-items-center rounded-full text-xs font-bold ${circle}`}>
              {done ? "✓" : step.n}
            </span>
            <span className={`${active ? "font-semibold text-slate-100" : done ? "text-slate-300" : "hidden text-slate-500 sm:inline"}`}>
              {step.label}
            </span>
          </span>
        );
        // Only earlier steps are links; you can't skip ahead.
        const canLink = step.href && step.n < current;
        return (
          <li key={step.n} className="flex items-center gap-1.5 sm:gap-2" aria-current={active ? "step" : undefined}>
            {canLink ? (
              <Link href={step.href as string} className="hover:text-slate-100">
                {content}
              </Link>
            ) : (
              content
            )}
            {i < steps.length - 1 && <span aria-hidden="true" className="h-px w-4 bg-slate-700 sm:w-10" />}
          </li>
        );
      })}
    </ol>
  );
}
