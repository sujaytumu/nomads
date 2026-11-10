export type Weekend = { saturday: Date; sunday: Date; daysAway: number; inProgress: boolean };

const startOfDay = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate());

/** The weekend to plan for. On Saturday or Sunday it is the current weekend (in progress); otherwise the coming Saturday and Sunday. */
export function nextWeekend(now: Date): Weekend {
  const today = startOfDay(now);
  const dow = today.getDay(); // 0 = Sunday
  if (dow === 6) {
    return { saturday: today, sunday: new Date(today.getFullYear(), today.getMonth(), today.getDate() + 1), daysAway: 0, inProgress: true };
  }
  if (dow === 0) {
    return { saturday: new Date(today.getFullYear(), today.getMonth(), today.getDate() - 1), sunday: today, daysAway: 0, inProgress: true };
  }
  const away = 6 - dow;
  return {
    saturday: new Date(today.getFullYear(), today.getMonth(), today.getDate() + away),
    sunday: new Date(today.getFullYear(), today.getMonth(), today.getDate() + away + 1),
    daysAway: away,
    inProgress: false,
  };
}

export function weekendRange(w: Weekend): string {
  const f = (d: Date) => d.toLocaleDateString("en-IN", { day: "numeric", month: "short" });
  return `${f(w.saturday)} – ${f(w.sunday)}`;
}
