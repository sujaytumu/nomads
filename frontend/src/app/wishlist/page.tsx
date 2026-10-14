"use client";

import { useEffect, useState } from "react";

import PackageCard from "@/components/PackageCard";
import ProtectedPage from "@/components/ProtectedPage";
import { fetchWishlist, type WishlistPackage } from "@/lib/wishlistApi";

export default function WishlistPage() {
  const [items, setItems] = useState<WishlistPackage[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchWishlist()
      .then(setItems)
      .catch(() => setError("Couldn't load your wishlist"));
  }, []);

  return (
    <ProtectedPage>
      <div className="section py-12">
        <h2 className="text-2xl font-bold mb-6">Your Wishlist</h2>
        {error && <p className="text-sm text-red-400">{error}</p>}
        {items && items.length === 0 && (
          <p className="text-sm text-slate-400">Nothing saved yet. Tap “Save” on any package to keep it here.</p>
        )}
        <div className="grid md:grid-cols-3 gap-4">
          {items?.map((p) => (
            <PackageCard key={p.id} pkg={p} />
          ))}
        </div>
      </div>
    </ProtectedPage>
  );
}
