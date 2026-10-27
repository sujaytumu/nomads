"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";

import Cover from "@/components/Cover";
import PageHeader from "@/components/PageHeader";
import { Bone } from "@/components/Skeleton";
import { fetchMe } from "@/lib/authApi";
import { searchPlaces, type SearchItem, type SearchResult } from "@/lib/placeSearchApi";

const ICONS: Record<string, string> = { FOOD: "🍜", CULTURE: "🏛️", NATURE: "🌿", ADVENTURE: "🧗", SHOPPING: "🛍️", NIGHTLIFE: "🌙", RELAXATION: "🧘" };
const label = (s: string) => s.charAt(0) + s.slice(1).toLowerCase();
const PAGE = 12;

export default function ExplorePage() {
  const [q, setQ] = useState("");
  const [city, setCity] = useState("");
  const [category, setCategory] = useState("");
  const [minRating, setMinRating] = useState("");
  const [sort, setSort] = useState<"relevance" | "rating" | "name" | "distance">("relevance");
  const [home, setHome] = useState<{ lat: number; lon: number } | null>(null);

  const [items, setItems] = useState<SearchItem[]>([]);
  const [meta, setMeta] = useState<SearchResult | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const seq = useRef(0);

  // Your home location lets "Nearest" sort and show kilometres.
  useEffect(() => {
    fetchMe().then((m) => m.latitude && m.longitude && setHome({ lat: m.latitude, lon: m.longitude })).catch(() => undefined);
  }, []);

  // Any change starts again from the first page (after a short pause while typing).
  useEffect(() => {
    const t = setTimeout(() => run(0), q ? 280 : 0);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [q, city, category, minRating, sort, home]);

  function run(p: number) {
    const mine = ++seq.current;
    setLoading(true);
    setError(false);
    searchPlaces({ q, city, category, minRating: minRating ? Number(minRating) : undefined, sort: sort === "distance" && !home ? "rating" : sort, lat: home?.lat, lon: home?.lon, page: p, size: PAGE })
      .then((r) => {
        if (mine !== seq.current) return;
        setMeta(r);
        setPage(p);
        setItems((cur) => (p === 0 ? r.items : [...cur, ...r.items]));
      })
      .catch(() => mine === seq.current && setError(true))
      .finally(() => mine === seq.current && setLoading(false));
  }

  const categories = meta ? Object.entries(meta.facets.categories) : [];
  const cities = meta ? Object.entries(meta.facets.cities) : [];
  const filtered = !!(q || city || category || minRating);
  const more = meta ? page + 1 < meta.totalPages : false;

  function reset() {
    setQ("");
    setCity("");
    setCategory("");
    setMinRating("");
    setSort("relevance");
  }

  return (
    <div className="section max-w-5xl space-y-4 py-6">
      <PageHeader title="Explore" subtitle="Find places by name, city or kind of trip." />

      <div className="relative">
        <svg aria-hidden="true" viewBox="0 0 24 24" className="pointer-events-none absolute left-3.5 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><path d="M21 21l-4.3-4.3M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14z" /></svg>
        <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search places, cities…" aria-label="Search places" className="w-full rounded-xl border-0 bg-slate-800 py-3 pl-11 pr-10 text-sm" />
        {q && <button type="button" aria-label="Clear search" onClick={() => setQ("")} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400">✕</button>}
      </div>

      {/* Category chips with how many places each has (counts follow the other filters) */}
      <div className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1">
        <button type="button" onClick={() => setCategory("")} className={`chip-btn shrink-0 rounded-lg px-3.5 py-1.5 text-sm font-semibold ${category === "" ? "bg-slate-100 text-slate-950" : "bg-slate-800 text-slate-100"}`}>All</button>
        {categories.map(([c, n]) => (
          <button key={c} type="button" onClick={() => setCategory(category === c ? "" : c)} className={`chip-btn shrink-0 rounded-lg px-3.5 py-1.5 text-sm font-semibold ${category === c ? "bg-slate-100 text-slate-950" : "bg-slate-800 text-slate-100"}`}>
            <span aria-hidden="true">{ICONS[c]} </span>{label(c)} <span className="opacity-60">{n}</span>
          </button>
        ))}
      </div>

      <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
        <select value={city} onChange={(e) => setCity(e.target.value)} aria-label="City" className="rounded-xl border-0 bg-slate-800 px-3 py-2.5 text-sm">
          <option value="">All cities</option>
          {cities.map(([c, n]) => <option key={c} value={c}>{c} ({n})</option>)}
        </select>
        <select value={minRating} onChange={(e) => setMinRating(e.target.value)} aria-label="Minimum rating" className="rounded-xl border-0 bg-slate-800 px-3 py-2.5 text-sm">
          <option value="">Any rating</option>
          <option value="4">4★ and up</option>
          <option value="4.5">4.5★ and up</option>
        </select>
        <select value={sort} onChange={(e) => setSort(e.target.value as typeof sort)} aria-label="Sort" className="col-span-2 rounded-xl border-0 bg-slate-800 px-3 py-2.5 text-sm sm:col-span-1">
          <option value="relevance">{q ? "Best match" : "Top rated"}</option>
          <option value="rating">Highest rated</option>
          {home && <option value="distance">Nearest to me</option>}
          <option value="name">Name A–Z</option>
        </select>
      </div>

      {meta && !error && (
        <p className="text-xs text-slate-400">
          {meta.totalItems} {meta.totalItems === 1 ? "place" : "places"}
          {filtered && <button type="button" onClick={reset} className="ml-2 font-semibold text-[#0095f6]">Clear filters</button>}
        </p>
      )}

      {error && (
        <div className="card space-y-2 p-5">
          <p className="text-sm text-red-300">Couldn&apos;t search right now. The server may be waking up.</p>
          <button type="button" className="btn btn-secondary btn-sm" onClick={() => run(0)}>Retry</button>
        </div>
      )}

      {loading && items.length === 0 && !error && (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3" aria-busy="true">
          {[0, 1, 2, 3, 4, 5].map((i) => <Bone key={i} className="h-56 rounded-3xl" />)}
        </div>
      )}

      {!loading && !error && meta && items.length === 0 && (
        <div className="card p-8 text-center">
          <p className="text-4xl" aria-hidden="true">🧭</p>
          <p className="mt-2 font-semibold">No places match</p>
          <p className="text-sm text-slate-400">Try a different word or clear a filter.</p>
          <button type="button" className="btn btn-secondary btn-sm mt-3" onClick={reset}>Clear filters</button>
        </div>
      )}

      {items.length > 0 && (
        <div className="stagger grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {items.map((p) => (
            <Link key={p.id} href={`/place/${p.id}`} className="card group block overflow-hidden transition-all hover:-translate-y-0.5">
              <div className="relative">
                <Cover src={p.imageUrl} alt={p.name} kind={p.category} className="h-36" />
                <span className="absolute right-3 top-3 rounded-full bg-black/55 px-2.5 py-1 text-xs font-bold text-white ring-1 ring-white/10 backdrop-blur">★ {p.rating.toFixed(1)}</span>
              </div>
              <div className="p-3">
                <p className="truncate font-display text-base font-semibold">{p.name}</p>
                <p className="text-xs text-slate-400">
                  {ICONS[p.category]} {label(p.category)} · {p.city}
                  {typeof p.distanceKm === "number" ? ` · ${p.distanceKm.toFixed(1)} km` : ""}
                </p>
              </div>
            </Link>
          ))}
        </div>
      )}

      {more && (
        <div className="text-center">
          <button type="button" className="btn btn-secondary" disabled={loading} data-loading={loading} onClick={() => run(page + 1)}>Show more</button>
        </div>
      )}
    </div>
  );
}
