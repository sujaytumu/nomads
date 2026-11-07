"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import type { SearchItem } from "@/lib/placeSearchApi";

const label = (s: string) => s.charAt(0) + s.slice(1).toLowerCase();

/** A bar that appears once you pick places to compare; opens a side-by-side table. */
export default function ComparePlaces({ picked, onRemove, onClear }: { picked: SearchItem[]; onRemove: (id: number) => void; onClear: () => void }) {
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (picked.length < 2) setOpen(false);
  }, [picked.length]);

  if (picked.length === 0) return null;

  const best = Math.max(...picked.map((p) => p.rating));
  const nearest = picked.some((p) => typeof p.distanceKm === "number") ? Math.min(...picked.map((p) => (typeof p.distanceKm === "number" ? p.distanceKm : Infinity))) : null;

  return (
    <>
      <div className="fixed inset-x-3 bottom-20 z-40 mx-auto flex max-w-md items-center justify-between gap-2 rounded-2xl border border-slate-700 bg-slate-900/95 px-3 py-2 shadow-xl backdrop-blur lg:bottom-6">
        <span className="min-w-0 truncate text-sm">{picked.length} to compare{picked.length === 1 ? " · pick one more" : ""}</span>
        <span className="flex shrink-0 gap-2">
          <button type="button" className="btn btn-secondary btn-sm" onClick={onClear}>Clear</button>
          <button type="button" className="btn btn-primary btn-sm" disabled={picked.length < 2} onClick={() => setOpen(true)}>Compare</button>
        </span>
      </div>

      {open && (
        <div role="dialog" aria-modal="true" aria-label="Compare places" className="fixed inset-0 z-50 flex items-end justify-center bg-black/70 p-3 sm:items-center" onClick={() => setOpen(false)}>
          <div className="max-h-[85vh] w-full max-w-2xl overflow-auto rounded-2xl bg-slate-900 p-4" onClick={(e) => e.stopPropagation()}>
            <div className="mb-3 flex items-center justify-between">
              <h3 className="text-lg font-semibold">Compare places</h3>
              <button type="button" onClick={() => setOpen(false)} aria-label="Close" className="px-2 text-slate-400">✕</button>
            </div>
            <table className="w-full table-fixed text-left text-sm">
              <thead>
                <tr>
                  <th className="w-20" />
                  {picked.map((p) => (
                    <th key={p.id} className="px-2 pb-2 align-top">
                      <Link href={`/place/${p.id}`} className="block truncate font-semibold hover:underline">{p.name}</Link>
                      <button type="button" onClick={() => onRemove(p.id)} className="text-xs font-normal text-slate-400">Remove</button>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                <tr className="border-t border-slate-800">
                  <td className="py-2 text-xs uppercase text-slate-400">City</td>
                  {picked.map((p) => <td key={p.id} className="px-2">{p.city}</td>)}
                </tr>
                <tr className="border-t border-slate-800">
                  <td className="py-2 text-xs uppercase text-slate-400">Kind</td>
                  {picked.map((p) => <td key={p.id} className="px-2">{label(p.category)}</td>)}
                </tr>
                <tr className="border-t border-slate-800">
                  <td className="py-2 text-xs uppercase text-slate-400">Rating</td>
                  {picked.map((p) => (
                    <td key={p.id} className={`px-2 tabular-nums ${p.rating === best ? "font-semibold text-emerald-300" : ""}`}>★ {p.rating.toFixed(1)}{p.rating === best && picked.length > 1 ? " · best" : ""}</td>
                  ))}
                </tr>
                {nearest !== null && (
                  <tr className="border-t border-slate-800">
                    <td className="py-2 text-xs uppercase text-slate-400">Distance</td>
                    {picked.map((p) => (
                      <td key={p.id} className={`px-2 tabular-nums ${p.distanceKm === nearest ? "font-semibold text-emerald-300" : ""}`}>{typeof p.distanceKm === "number" ? `${p.distanceKm.toFixed(1)} km` : "–"}</td>
                    ))}
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </>
  );
}
