"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";

import Cover from "@/components/Cover";
import ComparePlaces from "@/components/ComparePlaces";
import RecentlyViewed from "@/components/RecentlyViewed";
import SurpriseMeButton from "@/components/SurpriseMeButton";
import TrendingPlaces from "@/components/TrendingPlaces";
import PageHeader from "@/components/PageHeader";
import { Bone } from "@/components/Skeleton";
import { toast } from "@/lib/toast";
import { clearSearches, readSearches, rememberSearch } from "@/lib/recentSearches";
import { fetchMe } from "@/lib/authApi";
import SavedPlaceHeart from "@/components/SavedPlaceHeart";
import { fetchSavedPlaceIds } from "@/lib/savedPlacesApi";
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
  const [urlReady, setUrlReady] = useState(false);
  const searchRef = useRef<HTMLInputElement>(null);

  // "/" jumps to the search box, like on most sites (ignored while typing somewhere else).
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key !== "/" || e.ctrlKey || e.metaKey || e.altKey) return;
      const el = e.target as HTMLElement | null;
      if (el && (el.tagName === "INPUT" || el.tagName === "TEXTAREA" || el.tagName === "SELECT" || el.isContentEditable)) return;
      e.preventDefault();
      searchRef.current?.focus();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);
  const [recent, setRecent] = useState<string[]>([]);
  useEffect(() => setRecent(readSearches()), []);

  // Read filters from the address once (so a shared link opens the same search), then keep the address in step.
  useEffect(() => {
    const sp = new URLSearchParams(window.location.search);
    setQ(sp.get("q") || "");
    setCity(sp.get("city") || "");
    const c = sp.get("category") || "";
    setCategory(c in ICONS ? c : "");
    const r = sp.get("minRating") || "";
    setMinRating(r === "4" || r === "4.5" ? r : "");
    const so = sp.get("sort");
    if (so === "rating" || so === "name") setSort(so);
    setUrlReady(true);
  }, []);

  useEffect(() => {
    if (!urlReady) return;
    const sp = new URLSearchParams();
    if (q) sp.set("q", q);
    if (city) sp.set("city", city);
    if (category) sp.set("category", category);
    if (minRating) sp.set("minRating", minRating);
    if (sort === "rating" || sort === "name") sp.set("sort", sort);
    const qs = sp.toString();
    window.history.replaceState(null, "", qs ? `?${qs}` : window.location.pathname);
  }, [urlReady, q, city, category, minRating, sort]);

  const [locating, setLocating] = useState(false);
  const [canLocate, setCanLocate] = useState(false);
  useEffect(() => setCanLocate("geolocation" in navigator), []);
  const [home, setHome] = useState<{ lat: number; lon: number } | null>(null);

  const [items, setItems] = useState<SearchItem[]>([]);
  const [meta, setMeta] = useState<SearchResult | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const seq = useRef(0);
  const [saved, setSaved] = useState<number[]>([]);

  useEffect(() => {
    if (!localStorage.getItem("nomad_token")) return;
    fetchSavedPlaceIds().then(setSaved).catch(() => {});
  }, []);

  const [listView, setListView] = useState(false);
  useEffect(() => {
    try { setListView(localStorage.getItem("nomad_explore_view") === "list"); } catch { /* ignore */ }
  }, []);
  const setView = (list: boolean) => {
    setListView(list);
    try { localStorage.setItem("nomad_explore_view", list ? "list" : "grid"); } catch { /* ignore */ }
  };
  const [compare, setCompare] = useState<SearchItem[]>([]);
  const toggleCompare = (p: SearchItem) =>
    setCompare((cur) => (cur.some((x) => x.id === p.id) ? cur.filter((x) => x.id !== p.id) : cur.length >= 3 ? (toast("You can compare up to 3 places", "info"), cur) : [...cur, p]));

  const onSaved = (id: number, on: boolean) => setSaved((prev) => (on ? [...prev, id] : prev.filter((x) => x !== id)));

  // Your home location lets "Nearest" sort and show kilometres.
  useEffect(() => {
    fetchMe().then((m) => {
      const { latitude, longitude } = m;
      if (latitude && longitude) setHome((cur) => cur ?? { lat: latitude, lon: longitude });
    }).catch(() => undefined);
  }, []);

  // Any change starts again from the first page (after a short pause while typing).
  useEffect(() => {
    if (!urlReady) return;
    const t = setTimeout(() => run(0), q ? 280 : 0);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [urlReady, q, city, category, minRating, sort, home]);

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
        <input ref={searchRef} value={q} onChange={(e) => setQ(e.target.value)} onBlur={() => { if (q) { rememberSearch(q); setRecent(readSearches()); } }} onKeyDown={(e) => { if (e.key === "Enter") { rememberSearch(q); setRecent(readSearches()); (e.target as HTMLInputElement).blur(); } }} placeholder="Search places, cities…  ( / )" aria-label="Search places" className="w-full rounded-xl border-0 bg-slate-800 py-3 pl-11 pr-10 text-sm" />
        {q && <button type="button" aria-label="Clear search" onClick={() => setQ("")} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400">✕</button>}
      </div>

      {!q && recent.length > 0 && (
        <div className="-mx-1 flex items-center gap-2 overflow-x-auto px-1 pb-1" aria-label="Recent searches">
          <span className="shrink-0 text-xs text-slate-400">Recent</span>
          {recent.map((r) => (
            <button key={r} type="button" onClick={() => setQ(r)} className="chip-btn shrink-0 rounded-lg bg-slate-800 px-3 py-1 text-sm text-slate-100">{r}</button>
          ))}
          <button type="button" onClick={() => { clearSearches(); setRecent([]); }} className="shrink-0 text-xs text-slate-400 hover:text-slate-200">Clear</button>
        </div>
      )}

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

      <div className="flex flex-wrap gap-2"><SurpriseMeButton /></div>

      {!home && canLocate && (
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          disabled={locating}
          onClick={() => {
            setLocating(true);
            navigator.geolocation.getCurrentPosition(
              (pos) => {
                setHome({ lat: pos.coords.latitude, lon: pos.coords.longitude });
                setSort("distance");
                setLocating(false);
              },
              () => {
                setLocating(false);
                toast("Couldn't get your location. Check the browser's location permission.", "error");
              },
              { timeout: 10000, maximumAge: 300000 }
            );
          }}
        >
          {locating ? "Finding you…" : "📍 Use my location"}
        </button>
      )}

      {filtered && (
        <div className="flex flex-wrap items-center gap-2" aria-label="Active filters">
          {([
            q ? { k: "q", text: `“${q}”`, clear: () => setQ("") } : null,
            city ? { k: "city", text: city, clear: () => setCity("") } : null,
            category ? { k: "cat", text: label(category), clear: () => setCategory("") } : null,
            minRating ? { k: "rating", text: `${minRating}★ and up`, clear: () => setMinRating("") } : null,
          ].filter(Boolean) as { k: string; text: string; clear: () => void }[]).map((c) => (
            <button key={c.k} type="button" onClick={c.clear} aria-label={`Remove filter ${c.text}`} className="chip-btn inline-flex items-center gap-1.5 rounded-full bg-slate-800 px-3 py-1 text-xs font-semibold text-slate-100">
              {c.text} <span aria-hidden="true" className="text-slate-400">✕</span>
            </button>
          ))}
        </div>
      )}

      {meta && !error && (
        <p className="flex items-center text-xs text-slate-400">
          <span className="mr-auto">{meta.totalItems} {meta.totalItems === 1 ? "place" : "places"}</span>
          <span className="order-last ml-3 inline-flex overflow-hidden rounded-lg bg-slate-800" role="group" aria-label="View">
            <button type="button" aria-pressed={!listView} onClick={() => setView(false)} className={`px-2.5 py-1 text-xs font-semibold ${!listView ? "bg-slate-100 text-slate-950" : "text-slate-200"}`}>Grid</button>
            <button type="button" aria-pressed={listView} onClick={() => setView(true)} className={`px-2.5 py-1 text-xs font-semibold ${listView ? "bg-slate-100 text-slate-950" : "text-slate-200"}`}>List</button>
          </span>
          {filtered && <button type="button" onClick={reset} className="ml-2 font-semibold text-[#0095f6]">Clear filters</button>}
          {filtered && (
            <button
              type="button"
              className="ml-3 font-semibold text-[#0095f6]"
              onClick={async () => {
                const url = window.location.href;
                try {
                  if (navigator.share) await navigator.share({ title: "Nomads search", url });
                  else {
                    await navigator.clipboard.writeText(url);
                    toast("Search link copied", "success");
                  }
                } catch (e: any) {
                  if (e?.name !== "AbortError") toast("Couldn't share this search.", "error");
                }
              }}
            >
              Share search
            </button>
          )}
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

      {!q && !city && !category && !minRating && (
        <>
          <RecentlyViewed />
          <TrendingPlaces />
        </>
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
        <div className={`stagger grid gap-3 ${listView ? "" : "sm:grid-cols-2 lg:grid-cols-3"}`}>
          {items.map((p) => (
            <Link key={p.id} href={`/place/${p.id}`} className={`card group overflow-hidden transition-all hover:-translate-y-0.5 ${listView ? "flex" : "block"}`}>
              <div className={`relative ${listView ? "w-36 shrink-0 sm:w-48" : ""}`}>
                <Cover src={p.imageUrl} alt={p.name} kind={p.category} className={listView ? "h-full min-h-[9rem]" : "h-36"} />
                <span className="absolute left-3 top-3 rounded-full bg-black/55 px-2.5 py-1 text-xs font-bold text-white ring-1 ring-white/10 backdrop-blur">★ {p.rating.toFixed(1)}</span>
                <button
                  type="button"
                  aria-pressed={compare.some((x) => x.id === p.id)}
                  onClick={(e) => { e.preventDefault(); e.stopPropagation(); toggleCompare(p); }}
                  className={`absolute bottom-3 left-3 rounded-full px-2.5 py-1 text-xs font-bold ring-1 ring-white/10 backdrop-blur ${compare.some((x) => x.id === p.id) ? "bg-white text-slate-950" : "bg-black/55 text-white"}`}
                >
                  {compare.some((x) => x.id === p.id) ? "✓ Comparing" : "+ Compare"}
                </button>
                <SavedPlaceHeart className="absolute right-3 top-3" placeId={p.id} saved={saved.includes(p.id)} onChange={onSaved} />
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

      <ComparePlaces picked={compare} onRemove={(id) => setCompare((c) => c.filter((x) => x.id !== id))} onClear={() => setCompare([])} />

      {more && (
        <div className="text-center">
          <button type="button" className="btn btn-secondary" disabled={loading} data-loading={loading} onClick={() => run(page + 1)}>Show more</button>
        </div>
      )}
    </div>
  );
}
