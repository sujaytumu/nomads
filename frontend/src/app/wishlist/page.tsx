"use client";

import React, { useEffect, useState } from "react";
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
    <div className="section py-12">
      <h2 className="text-2xl font-bold mb-6">My Wishlist</h2>
      {error && <p className="text-red-400">{error}</p>}
      {!error && pkgs === null && <p className="text-slate-400">Loading…</p>}
      {pkgs && pkgs.length === 0 && (
        <p className="text-slate-300">
          Nothing saved yet. <a href="/packages" className="underline">Browse packages</a> and tap ♡ to save one.
        </p>
      )}
      {pkgs && pkgs.length > 0 && (
        <div className="grid md:grid-cols-3 gap-4">
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
