"use client";

import React, { useEffect, useState } from "react";
import PackageCard from "./PackageCard";
import { searchPackages, PackageSort } from "../lib/packageApi";
import { fetchWishlistIds } from "../lib/wishlistApi";

export default function PackageBrowser({ initial }: { initial: any[] }) {
  const [pkgs, setPkgs] = useState<any[]>(initial);
  const [q, setQ] = useState("");
  const [minPrice, setMinPrice] = useState("");
  const [maxPrice, setMaxPrice] = useState("");
  const [sort, setSort] = useState<PackageSort>("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState<Set<number>>(new Set());

  // Load which packages the signed-in user has hearted (silently skipped when logged out).
  useEffect(() => {
    if (typeof window === "undefined" || !localStorage.getItem("nomad_token")) return;
    fetchWishlistIds()
      .then((ids) => setSaved(new Set(ids)))
      .catch(() => {});
  }, []);

  // Debounced server-side search whenever a filter changes.
  useEffect(() => {
    const handle = setTimeout(async () => {
      setLoading(true);
      setError(null);
      try {
        setPkgs(await searchPackages({ q, minPrice, maxPrice, sort }));
      } catch (e: any) {
        setError(e?.message || "Could not load packages");
      } finally {
        setLoading(false);
      }
    }, 300);
    return () => clearTimeout(handle);
  }, [q, minPrice, maxPrice, sort]);

  function onSavedChange(id: number, isSaved: boolean) {
    setSaved((prev) => {
      const next = new Set(prev);
      if (isSaved) next.add(id);
      else next.delete(id);
      return next;
    });
  }

  function reset() {
    setQ("");
    setMinPrice("");
    setMaxPrice("");
    setSort("");
  }

  return (
    <>
      <div className="card p-4 mb-6 grid gap-3 md:grid-cols-5">
        <input
          className="md:col-span-2 rounded-md px-3 py-2 text-slate-900"
          placeholder="Search destinations…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          aria-label="Search packages"
        />
        <input
          className="rounded-md px-3 py-2 text-slate-900"
          type="number"
          min={0}
          placeholder="Min ₹"
          value={minPrice}
          onChange={(e) => setMinPrice(e.target.value)}
          aria-label="Minimum price"
        />
        <input
          className="rounded-md px-3 py-2 text-slate-900"
          type="number"
          min={0}
          placeholder="Max ₹"
          value={maxPrice}
          onChange={(e) => setMaxPrice(e.target.value)}
          aria-label="Maximum price"
        />
        <select
          className="rounded-md px-3 py-2 text-slate-900"
          value={sort}
          onChange={(e) => setSort(e.target.value as PackageSort)}
          aria-label="Sort packages"
        >
          <option value="">Featured</option>
          <option value="price_asc">Price: low to high</option>
          <option value="price_desc">Price: high to low</option>
          <option value="name">Name A–Z</option>
        </select>
      </div>

      {error && <p className="text-red-400 mb-4">{error}</p>}
      {loading && <p className="text-slate-400 mb-4">Searching…</p>}

      {!loading && pkgs.length === 0 ? (
        <div className="text-center py-10">
          <p className="text-slate-300 mb-3">No packages match your filters.</p>
          <button className="btn-outline" onClick={reset}>Clear filters</button>
        </div>
      ) : (
        <div className="grid md:grid-cols-3 gap-4">
          {pkgs.map((p: any) => (
            <PackageCard key={p.id} pkg={p} saved={saved.has(p.id)} onSavedChange={onSavedChange} />
          ))}
        </div>
      )}
    </>
  );
}
