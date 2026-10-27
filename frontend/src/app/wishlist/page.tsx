"use client";

import Link from "next/link";
import React, { useEffect, useState } from "react";
import PageHeader from "@/components/PageHeader";
import PackageCard from "@/components/PackageCard";
import ProtectedPage from "@/components/ProtectedPage";
import { fetchWishlist } from "@/lib/wishlistApi";
import Cover from "@/components/Cover";
import SavedPlaceHeart from "@/components/SavedPlaceHeart";
import { fetchSavedPlaces } from "@/lib/savedPlacesApi";
import type { SearchItem } from "@/lib/placeSearchApi";

function WishlistContent() {
  const [pkgs, setPkgs] = useState<any[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [places, setPlaces] = useState<SearchItem[] | null>(null);

  useEffect(() => {
    fetchSavedPlaces().then(setPlaces).catch(() => setPlaces([]));
  }, []);

  useEffect(() => {
    fetchWishlist()
      .then(setPkgs)
      .catch((e) => setError(e?.message || "Could not load wishlist"));
  }, []);

  function onSavedChange(id: number, isSaved: boolean) {
    // On this page a toggle can only mean "unsave" - drop the card immediately.
    if (!isSaved) setPkgs((prev) => (prev ? prev.filter((p) => p.id !== id) : prev));
  }

  return (
    <div className="section space-y-6 py-8">
      <PageHeader title="Wishlist" subtitle="Weekends you've saved. Tap ♥ on a card to remove it." />
      {error && <p role="alert" className="rounded-2xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm text-red-300">{error}</p>}
      {!error && pkgs === null && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {[0, 1, 2].map((i) => <div key={i} className="card h-72 animate-pulse" />)}
        </div>
      )}
      {pkgs && pkgs.length === 0 && places && places.length === 0 && (
        <div className="card flex flex-col items-center gap-3 px-6 py-14 text-center">
          <span className="text-5xl" aria-hidden="true">💛</span>
          <h2 className="font-display text-xl font-semibold">Nothing saved yet</h2>
          <p className="max-w-sm text-sm text-slate-400">Tap the heart on any package to keep it here for later.</p>
          <Link href="/packages" className="btn btn-primary mt-2">Browse packages</Link>
        </div>
      )}
      {places && places.length > 0 && (
        <section className="space-y-3" aria-label="Saved places">
          <h2 className="font-display text-lg font-semibold">Saved places</h2>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {places.map((p) => (
              <Link key={p.id} href={`/place/${p.id}`} className="card group block overflow-hidden transition-all hover:-translate-y-0.5">
                <div className="relative">
                  <Cover src={p.imageUrl} alt={p.name} kind={p.category} className="h-32" />
                  <SavedPlaceHeart className="absolute right-3 top-3" placeId={p.id} saved onChange={(id) => setPlaces((prev) => (prev ? prev.filter((x) => x.id !== id) : prev))} />
                </div>
                <div className="p-3">
                  <p className="truncate font-display text-base font-semibold">{p.name}</p>
                  <p className="text-xs text-slate-400">{p.city} · ★ {p.rating.toFixed(1)}</p>
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}
      {pkgs && pkgs.length > 0 && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {pkgs.map((p) => (
            <PackageCard key={p.id} pkg={p} saved onSavedChange={onSavedChange} />
          ))}
        </div>
      )}
    </div>
  );
}

export default function WishlistPage() {
  return (
    <ProtectedPage>
      <WishlistContent />
    </ProtectedPage>
  );
}
