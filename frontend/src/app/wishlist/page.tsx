"use client";

import Link from "next/link";
import React, { useEffect, useState } from "react";
import PageHeader from "@/components/PageHeader";
import PackageCard from "@/components/PackageCard";
import ProtectedPage from "@/components/ProtectedPage";
import { fetchWishlist } from "@/lib/wishlistApi";

function WishlistContent() {
  const [pkgs, setPkgs] = useState<any[] | null>(null);
  const [error, setError] = useState<string | null>(null);

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
      {pkgs && pkgs.length === 0 && (
        <div className="card flex flex-col items-center gap-3 px-6 py-14 text-center">
          <span className="text-5xl" aria-hidden="true">💛</span>
          <h2 className="font-display text-xl font-semibold">Nothing saved yet</h2>
          <p className="max-w-sm text-sm text-slate-400">Tap the heart on any package to keep it here for later.</p>
          <Link href="/packages" className="btn btn-primary mt-2">Browse packages</Link>
        </div>
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
