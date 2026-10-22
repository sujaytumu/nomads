// The real-life flow of a trip, from first idea to coming home:
//   Plan -> Pay -> Booked -> Pickup -> Trip day -> Review
// Derived from the trip's status plus its date, so it matches what is actually happening.

export type Stage = { key: string; label: string; icon: string; href?: (id: number | string) => string; hint: string };

export const STAGES: Stage[] = [
  { key: "plan", label: "Plan", icon: "🧭", hint: "Pick places and build the itinerary." },
  { key: "pay", label: "Pay", icon: "💳", hint: "Pay to lock in the trip." },
  { key: "booked", label: "Booked", icon: "🎟️", hint: "Itinerary PDF emailed. Set your trip date." },
  { key: "pickup", label: "Pickup", icon: "🚖", hint: "Arrange your ride and meeting point.", href: (id) => `/pickup?tripId=${id}` },
  { key: "trip", label: "Trip day", icon: "🌄", hint: "Follow today's stops and directions.", href: (id) => `/trip-summary?tripId=${id}` },
  { key: "review", label: "Review", icon: "⭐", hint: "Tell us how it went.", href: (id) => `/review?tripId=${id}` },
];

export type JourneyInput = {
  status: string;
  tripDate?: string | null;
  plans?: { dayNumber?: number | null }[] | null;
};

export type Journey = {
  /** Index in STAGES of what is happening now (STAGES.length = everything finished). */
  current: number;
  cancelled: boolean;
  /** Plain-words line about where the trip is. */
  message: string;
};

const DAY = 86_400_000;

export function journeyOf(t: JourneyInput): Journey {
  if (t.status === "CANCELLED") return { current: 0, cancelled: true, message: "Cancelled. Nothing more will happen on this trip." };
  if (t.status === "PLANNED") return { current: 1, cancelled: false, message: "Itinerary ready. Pay to book it; the PDF is emailed once paid." };
  if (t.status === "PAYMENT_PENDING") return { current: 1, cancelled: false, message: "Payment started but not finished. Finish it to book the trip." };

  // CONFIRMED: where we are depends on the date.
  if (!t.tripDate) return { current: 2, cancelled: false, message: "Booked. Pick your trip date so we can count down and remind you." };
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const start = new Date(`${t.tripDate}T00:00:00`);
  const daysTo = Math.round((start.getTime() - today.getTime()) / DAY);
  const lastDay = Math.max(1, ...(t.plans || []).map((p) => p.dayNumber || 1));
  if (daysTo > 0) return { current: 3, cancelled: false, message: `Booked. ${daysTo === 1 ? "Tomorrow" : `In ${daysTo} days`}: arrange your pickup before you go.` };
  if (daysTo + lastDay - 1 >= 0) return { current: 4, cancelled: false, message: "Your trip is on. Enjoy it, today's stops are on Home." };
  return { current: 5, cancelled: false, message: "Welcome back! Leave a review of the trip." };
}
