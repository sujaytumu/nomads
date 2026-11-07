export type CsvTrip = {
  tripRequestId: number;
  city?: string | null;
  status?: string | null;
  tripDate?: string | null;
  createdAt?: string | null;
  weekendType?: string | null;
  days?: number | null;
  interest?: string | null;
  travelMode?: string | null;
  groupSize?: number | string | null;
  estimatedCost?: number | string | null;
};

const HEAD = ["Trip ID", "City", "Status", "Trip date", "Planned on", "Days", "Interest", "Mode", "People", "Estimated cost (INR)"];

/** One cell, quoted when needed. Cells that start with = + - @ get a leading apostrophe so spreadsheets never run them as formulas. */
export function cell(v: unknown): string {
  let s = v === null || v === undefined ? "" : String(v);
  if (/^[=+\-@\t\r]/.test(s) && !/^-?\d+(\.\d+)?$/.test(s)) s = "'" + s;
  return /[",\n\r]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
}

export function tripsToCsv(trips: CsvTrip[]): string {
  const rows = trips.map((t) => [
    t.tripRequestId,
    t.city,
    t.status,
    t.tripDate,
    t.createdAt ? String(t.createdAt).slice(0, 10) : "",
    t.days ?? (t.weekendType === "TWO_DAY" ? 2 : t.weekendType === "ONE_DAY" ? 1 : ""),
    t.interest,
    t.travelMode,
    t.groupSize,
    t.estimatedCost,
  ]);
  // Leading BOM so Excel reads the file as UTF-8.
  return "﻿" + [HEAD, ...rows].map((r) => r.map(cell).join(",")).join("\r\n") + "\r\n";
}
