/**
 * Fixed-date national holidays in India. Festival dates that move with the lunar calendar (Diwali, Holi, Eid)
 * are left out on purpose: a wrong date here would send people planning around a day that is not a holiday.
 */
export const FIXED_HOLIDAYS: { month: number; day: number; name: string }[] = [
  { month: 1, day: 26, name: "Republic Day" },
  { month: 5, day: 1, name: "May Day" },
  { month: 8, day: 15, name: "Independence Day" },
  { month: 10, day: 2, name: "Gandhi Jayanti" },
  { month: 12, day: 25, name: "Christmas" },
];

export type LongWeekend = { start: Date; end: Date; days: number; holiday: string; daysAway: number };

const at = (y: number, m: number, d: number) => new Date(y, m - 1, d);
const addDays = (d: Date, n: number) => new Date(d.getFullYear(), d.getMonth(), d.getDate() + n);
const midnight = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate());

/**
 * The next long weekend within {@code withinDays}: a fixed holiday that falls on a Friday (Fri to Sun) or a
 * Monday (Sat to Mon) gives three days off in a row. Holidays on other days give no extra time, so they are skipped.
 * A long weekend already under way counts until it ends.
 */
export function nextLongWeekend(now: Date, withinDays = 45): LongWeekend | null {
  const today = midnight(now);
  let best: LongWeekend | null = null;
  for (const y of [today.getFullYear(), today.getFullYear() + 1]) {
    for (const h of FIXED_HOLIDAYS) {
      const date = at(y, h.month, h.day);
      const dow = date.getDay();
      if (dow !== 5 && dow !== 1) continue;
      const start = dow === 5 ? date : addDays(date, -2);
      const end = dow === 5 ? addDays(date, 2) : date;
      if (end < today) continue;
      const daysAway = Math.max(0, Math.round((start.getTime() - today.getTime()) / 86400000));
      if (daysAway > withinDays) continue;
      if (!best || start < best.start) best = { start, end, days: 3, holiday: h.name, daysAway };
    }
  }
  return best;
}

export function formatSpan(w: LongWeekend): string {
  const f = (d: Date) => d.toLocaleDateString("en-IN", { weekday: "short", day: "numeric", month: "short" });
  return `${f(w.start)} – ${f(w.end)}`;
}
