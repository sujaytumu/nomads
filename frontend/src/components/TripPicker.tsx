import Link from "next/link";

import { formatCost, statusMeta, tripTitle } from "@/lib/tripStatus";
import type { MyTrip } from "@/lib/useTripSelection";

/** Dropdown of the user's trips, or a clear empty state with where to go next. */
export default function TripPicker({
  trips,
  selected,
  onSelect,
  loading,
  error,
  emptyText,
  onRetry,
}: {
  trips: MyTrip[];
  selected: MyTrip | null;
  onSelect: (id: string) => void;
  loading: boolean;
  error: boolean;
  emptyText: string;
  onRetry: () => void;
}) {
  if (loading) return <p className="text-sm text-slate-400">Loading your trips…</p>;
  if (error)
    return (
      <div className="flex items-center gap-3">
        <p className="text-sm text-red-400">Couldn&apos;t load your trips.</p>
        <button type="button" className="btn btn-secondary btn-sm" onClick={onRetry}>Retry</button>
      </div>
    );
  if (trips.length === 0)
    return (
      <div className="space-y-3 rounded-xl border border-slate-800 p-5">
        <p className="text-sm text-slate-300">{emptyText}</p>
        <div className="flex flex-wrap gap-2">
          <Link href="/trip-planner" className="btn btn-primary btn-sm">Plan a trip</Link>
          <Link href="/bookings" className="btn btn-secondary btn-sm">My Trips</Link>
        </div>
      </div>
    );
  return (
    <label className="block space-y-1.5">
      <span className="text-xs font-semibold uppercase tracking-wide text-slate-500">Trip</span>
      <select
        value={selected ? String(selected.tripRequestId) : ""}
        onChange={(e) => onSelect(e.target.value)}
        className="w-full rounded-xl border px-4 py-2.5 text-sm"
      >
        <option value="">Choose a trip…</option>
        {trips.map((t) => (
          <option key={t.tripRequestId} value={String(t.tripRequestId)}>
            {`#${t.tripRequestId} · ${tripTitle(t)} · ${formatCost(t.estimatedCost)} · ${statusMeta(t.status).label}`}
          </option>
        ))}
      </select>
    </label>
  );
}
