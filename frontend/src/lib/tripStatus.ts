// One place that defines what a trip's backend status means to the user, so
// the planner, summary, payment and trips pages all say the same thing.
//
// Lifecycle:  PLANNED -> PAYMENT_PENDING -> CONFIRMED   (or CANCELLED)
//   PLANNED          itinerary created in Trip Planner, nothing paid yet
//   PAYMENT_PENDING  payment started (also the state right after enrolling in a package)
//   CONFIRMED        paid - this is the only state that counts as "booked"
//   CANCELLED        cancelled (refunded automatically if it had been paid)

export type TripStatus = "PLANNED" | "PAYMENT_PENDING" | "CONFIRMED" | "CANCELLED";

type StatusMeta = {
  label: string;
  hint: string;
  badge: string;
};

export const STATUS_META: Record<TripStatus, StatusMeta> = {
  PLANNED: {
    label: "Planned",
    hint: "Not booked yet. Pay to confirm it.",
    badge: "bg-sky-500/15 text-sky-300",
  },
  PAYMENT_PENDING: {
    label: "Awaiting payment",
    hint: "Payment was started but not completed.",
    badge: "bg-yellow-500/15 text-yellow-300",
  },
  CONFIRMED: {
    label: "Booked",
    hint: "Payment received. You're all set.",
    badge: "bg-green-500/15 text-green-300",
  },
  CANCELLED: {
    label: "Cancelled",
    hint: "This trip was cancelled.",
    badge: "bg-red-500/15 text-red-300",
  },
};

const FALLBACK: StatusMeta = {
  label: "Unknown",
  hint: "",
  badge: "bg-slate-800 text-slate-200",
};

export function statusMeta(status?: string | null): StatusMeta {
  return (status && STATUS_META[status as TripStatus]) || { ...FALLBACK, label: status || FALLBACK.label };
}

/** A trip can be paid for while it is planned or its payment is unfinished. */
export function isPayable(status?: string | null): boolean {
  return status === "PLANNED" || status === "PAYMENT_PENDING";
}

/** Matches what the backend refunds/cancels sensibly: started or completed bookings. */
export function isCancellable(status?: string | null): boolean {
  return status === "PAYMENT_PENDING" || status === "CONFIRMED";
}

export type TripLike = {
  tripRequestId: number | string;
  city?: string | null;
  status: string;
  estimatedCost?: number | string | null;
  createdAt?: string | null;
  plans?: unknown[] | null;
};

/** Package enrollments are created without a day-by-day itinerary. */
export function tripKind(trip: Pick<TripLike, "plans">): "Custom itinerary" | "Package" {
  return trip.plans && trip.plans.length > 0 ? "Custom itinerary" : "Package";
}

export function tripTitle(trip: TripLike): string {
  const city = trip.city?.trim();
  return city ? `${city} trip` : tripKind(trip) === "Package" ? "Package trip" : "Weekend trip";
}

export function formatCost(cost?: number | string | null): string {
  if (cost === null || cost === undefined || cost === "") return "—";
  const n = Number(cost);
  return Number.isFinite(n) ? `₹${n.toLocaleString("en-IN")}` : `₹${cost}`;
}

/** Times arrive as "09:00:00" (or [9, 0] depending on server config); show "09:00". */
export function formatTime(value: unknown): string {
  if (Array.isArray(value)) {
    return value
      .slice(0, 2)
      .map((part) => String(part).padStart(2, "0"))
      .join(":");
  }
  return typeof value === "string" ? value.slice(0, 5) : "";
}
