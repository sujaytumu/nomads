export type StepState = "done" | "todo" | "info";
export type Step = { key: string; phase: "Before" | "Getting there" | "Stay" | "On the trip" | "After"; title: string; detail: string; state: StepState; href?: string };

export type JourneyInput = {
  tripId: number | string;
  status?: string | null;
  /** yyyy-mm-dd */
  tripDate?: string | null;
  days: number;
  stops: number;
  pickupRequired?: boolean | null;
  packed: number;
  packTotal: number;
  /** Quickest one-way time to the first stop, if known. */
  quickestHours?: number | null;
  today: Date;
};

const DAY = 86400000;
const midnight = (d: Date) => Date.UTC(d.getFullYear(), d.getMonth(), d.getDate());

export function daysUntil(iso: string, today: Date): number | null {
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso);
  if (!m) return null;
  return Math.round((Date.UTC(+m[1], +m[2] - 1, +m[3]) - midnight(today)) / DAY);
}

const hours = (h: number) => (h < 1 ? `${Math.max(1, Math.round(h * 60))} min` : `${Math.round(h * 10) / 10} h`);

/** The whole trip in order, from booking to the review, with what is done and what still needs you. */
export function journeySteps(i: JourneyInput): Step[] {
  const confirmed = i.status === "CONFIRMED";
  const steps: Step[] = [];

  steps.push(confirmed
    ? { key: "book", phase: "Before", title: "Booked", detail: "Your itinerary is locked in.", state: "done" }
    : { key: "book", phase: "Before", title: "Pay to book", detail: "Lock in this itinerary.", state: "todo", href: `/payment?tripRequestId=${i.tripId}` });

  const until = i.tripDate ? daysUntil(i.tripDate, i.today) : null;
  steps.push(i.tripDate
    ? { key: "date", phase: "Before", title: "Date set", detail: until === null ? i.tripDate : until === 0 ? "Today!" : until === 1 ? "Tomorrow" : until > 1 ? `In ${until} days` : `${-until} days ago`, state: "done" }
    : { key: "date", phase: "Before", title: "Pick a date", detail: "We count down and remind you once you do.", state: "todo" });

  if (i.pickupRequired) {
    steps.push({ key: "pickup", phase: "Before", title: "Arrange your pickup", detail: "Choose the vehicle and pickup point.", state: "info", href: `/pickup?tripId=${i.tripId}` });
  }

  const allPacked = i.packTotal > 0 && i.packed >= i.packTotal;
  steps.push({ key: "pack", phase: "Before", title: "Pack", detail: i.packTotal > 0 ? `${i.packed} of ${i.packTotal} packed` : "Open your packing list.", state: allPacked ? "done" : "todo" });

  if (i.quickestHours && i.quickestHours > 0) {
    steps.push({ key: "travel", phase: "Getting there", title: "Get to your first stop", detail: `About ${hours(i.quickestHours)} each way. Compare options below.`, state: "info" });
  }

  if (i.days >= 2) {
    const nights = i.days - 1;
    steps.push({ key: "stay", phase: "Stay", title: `${nights} night${nights > 1 ? "s" : ""} to sort`, detail: "See stay areas, prices and hotel search below.", state: "info" });
  }

  steps.push({ key: "trip", phase: "On the trip", title: `${i.stops} stop${i.stops === 1 ? "" : "s"} over ${i.days} day${i.days === 1 ? "" : "s"}`, detail: "Plan B has backups if it rains or a place is closed.", state: "info" });

  steps.push({ key: "after", phase: "After", title: "Keep the memories", detail: "Write journal notes, then rate the trip.", state: "info" });
  return steps;
}
