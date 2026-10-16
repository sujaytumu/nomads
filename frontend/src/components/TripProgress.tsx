const STEPS = ["Planned", "Payment", "Booked"];

/** Where this trip is, in plain words: what's done, what's happening, what's next. */
export default function TripProgress({ status }: { status: string }) {
  if (status === "CANCELLED")
    return <p className="text-xs text-red-300">Cancelled — nothing more will happen on this trip.</p>;
  const idx = status === "CONFIRMED" ? 3 : status === "PAYMENT_PENDING" ? 1 : 0;
  const next =
    status === "CONFIRMED"
      ? "Booked. Your itinerary PDF was emailed to you; arrange pickup any time."
      : status === "PAYMENT_PENDING"
        ? "Payment started but not finished. Finish it to book the trip."
        : "Itinerary ready. Pay to book it — the PDF is emailed once paid.";
  return (
    <div className="space-y-1.5">
      <ol className="flex items-center gap-1.5" aria-label="Trip progress">
        {STEPS.map((s, i) => (
          <li key={s} className="flex items-center gap-1.5 text-xs">
            <span className={`h-2 w-2 rounded-full ${i < idx ? "bg-green-400" : i === idx ? "bg-amber-400" : "bg-slate-700"}`} />
            <span className={i < idx ? "text-slate-300" : i === idx ? "font-semibold text-white" : "text-slate-500"}>{s}</span>
            {i < STEPS.length - 1 && <span className="h-px w-4 bg-slate-700" />}
          </li>
        ))}
      </ol>
      <p className="text-xs text-slate-400">{next}</p>
    </div>
  );
}
