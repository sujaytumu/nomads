"use client";

import React, { useState } from "react";
import { addToWishlist, removeFromWishlist } from "../lib/wishlistApi";

type Props = {
  packageId: number;
  saved: boolean;
  onChange?: (packageId: number, saved: boolean) => void;
};

export default function WishlistHeart({ packageId, saved, onChange }: Props) {
  const [busy, setBusy] = useState(false);

  async function toggle() {
    if (typeof window !== "undefined" && !localStorage.getItem("nomad_token")) {
      window.location.href = "/auth";
      return;
    }
    setBusy(true);
    try {
      if (saved) {
        await removeFromWishlist(packageId);
      } else {
        await addToWishlist(packageId);
      }
      onChange?.(packageId, !saved);
    } catch (e: any) {
      alert("Could not update wishlist: " + (e?.message || "unknown error"));
    } finally {
      setBusy(false);
    }
  }

  return (
    <button
      type="button"
      onClick={toggle}
      disabled={busy}
      aria-pressed={saved}
      aria-label={saved ? "Remove from wishlist" : "Save to wishlist"}
      title={saved ? "Remove from wishlist" : "Save to wishlist"}
      className="text-2xl leading-none"
    >
      {saved ? "♥" : "♡"}
    </button>
  );
}
