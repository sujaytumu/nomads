"use client";

import { useEffect, useMemo, useState } from "react";

import { fetchItineraryOptions, saveItinerary, type ItineraryOptions } from "@/lib/tripApi";
import { formatCost } from "@/lib/tripStatus";

type Props = {
  tripId: number;
  /** Called with the updated trip once the new itinerary is saved. */
  onSaved: (trip: any) => void;
  onCancel: () => void;
};

export default function ItineraryEditor({ tripId, onSaved, onCancel }: Props) {
  const [options, setOptions] = useState<ItineraryOptions | null>(null);
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [original, setOriginal] = useState<Set<number>>(new Set());
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    fetchItineraryOptions(tripId)
      .then((data) => {
        if (!active) return;
        const current = new Set(data.options.filter((o) => o.selected).map((o) => o.placeId));
        setOptions(data);
        setSelected(current);
        setOriginal(current);
      })
      .catch(() => active && setError("Couldn't load the places. Please try again."))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [tripId]);

  const count = selected.size;
  const price = useMemo(() => {
    if (!options) return 0;
    return count * Number(options.costPerPlace) + (options.pickupRequired ? Number(options.pickupCost) : 0);
  }, [options, count]);

  const changed = useMemo(() => {
    if (selected.size !== original.size) return true;
    for (const id of Array.from(selected)) if (!original.has(id)) return true;
    return false;
  }, [selected, original]);

  const atMax = !!options && count >= options.maxPlaces;
  const belowMin = !!options && count < options.minPlaces;

  const toggle = (placeId: number) => {
    setError(null);
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(placeId)) next.delete(placeId);
      else if (!options || next.size < options.maxPlaces) next.add(placeId);
      return next;
    });
  };

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    try {
      const updated = await saveItinerary(tripId, Array.from(selected));
      onSaved(updated);
    } catch (err: any) {
      setError(err?.response?.data?.message || "Couldn't save your itinerary. Please try again.");
      setSaving(false);
    }
  };

  if (loading) return <p className="text-sm text-slate-400">Loading places…</p>;

  if (!options || !options.editable) {
    return (
      <div className="space-y-3">
        <p className="text-sm text-slate-300">
          {error ?? "This itinerary can't be changed any more — it's locked once payment starts."}
        </p>
        <button type="button" className="btn-outline" onClick={onCancel}>Close</button>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-2">
        <div>
          <p className="font-semibold">Choose your places</p>
          <p className="text-sm text-slate-400">
            Pick {options.minPlaces}–{options.maxPlaces}. We'll arrange them into days and the shortest route.
          </p>
        </div>
        <p className="text-sm text-slate-300" aria-live="polite">
          <span className="font-semibold text-white">{count}</span> of {options.maxPlaces} selected
        </p>
      </div>

      <ul className="grid gap-2 sm:grid-cols-2">
        {options.options.map((option) => {
          const checked = selected.has(option.placeId);
          const blocked = !checked && atMax;
          return (
            <li key={option.placeId}>
              <label
                className={`flex h-full cursor-pointer items-start gap-3 rounded-xl border p-3 transition-colors ${
                  checked
                    ? "border-brand-500 bg-brand-500/10"
                    : "border-slate-800 hover:border-slate-700"
                } ${blocked ? "cursor-not-allowed opacity-50" : ""}`}
              >
                <input
                  type="checkbox"
                  className="mt-1 h-4 w-4 shrink-0 accent-brand-500"
                  checked={checked}
                  disabled={blocked || saving}
                  onChange={() => toggle(option.placeId)}
                />
                <span className="min-w-0 space-y-1">
                  <span className="block font-medium leading-snug">{option.name}</span>
                  <span className="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-slate-400">
                    <span className="badge">{option.category.charAt(0) + option.category.slice(1).toLowerCase()}</span>
                    {option.rating != null && <span>★ {option.rating.toFixed(1)}</span>}
                    {option.distanceFromHomeKm != null && <span>{option.distanceFromHomeKm.toFixed(1)} km away</span>}
                  </span>
                </span>
              </label>
            </li>
          );
        })}
      </ul>

      <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-800 pt-4">
        <div>
          <p className="text-xs uppercase tracking-wide text-slate-500">New estimated cost</p>
          <p className="text-xl font-bold">{formatCost(price)}</p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <button type="button" className="btn-outline" onClick={onCancel} disabled={saving}>Cancel</button>
          <button
            type="button"
            className="btn-primary"
            onClick={handleSave}
            disabled={saving || !changed || belowMin}
          >
            {saving ? "Saving…" : "Save itinerary"}
          </button>
        </div>
      </div>

      {belowMin && <p className="text-sm text-yellow-300">Choose at least {options.minPlaces} place.</p>}
      {error && (
        <p className="text-sm text-red-400 bg-red-500/10 border border-red-500/20 rounded-xl px-4 py-3" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
