"use client";

import { useEffect, useState } from "react";

import { addToWishlist, fetchWishlist, removeFromWishlist } from "@/lib/wishlistApi";

// One shared request for all cards on a page instead of one per card.
let cached: Promise<Set<number>> | null = null;
function loadIds(): Promise<Set<number>> {
  if (!cached) {
    cached = fetchWishlist()
      .then((items) => new Set(items.map((i) => i.id)))
      .catch(() => {
        cached = null;
        return new Set<number>();
      });
  }
  return cached;
}

export default function WishlistButton({ packageId }: { packageId: number }) {
  const [saved, setSaved] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (typeof window === "undefined" || !localStorage.getItem("nomad_token")) return;
    loadIds().then((ids) => setSaved(ids.has(packageId)));
  }, [packageId]);

  const toggle = async () => {
    if (!localStorage.getItem("nomad_token")) {
      window.location.href = "/auth";
      return;
    }
    setBusy(true);
    try {
      if (saved) await removeFromWishlist(packageId);
      else await addToWishlist(packageId);
      const ids = await loadIds();
      if (saved) ids.delete(packageId);
      else ids.add(packageId);
      setSaved(!saved);
    } catch {
      // leave state unchanged; the user can simply retry
    } finally {
      setBusy(false);
    }
  };

  return (
    <button
      type="button"
      onClick={toggle}
      disabled={busy}
      aria-pressed={saved}
      aria-label={saved ? "Remove from wishlist" : "Save to wishlist"}
      className="btn-outline"
    >
      {saved ? "♥ Saved" : "♡ Save"}
    </button>
  );
}
