"use client";

import React, { useState } from "react";
import { addToWishlist, removeFromWishlist } from "../lib/wishlistApi";
import { errorText, toast } from "@/lib/toast";

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
      toast(saved ? "Removed from wishlist" : "Saved to wishlist", "success");
      onChange?.(packageId, !saved);
    } catch (e: any) {
      toast(errorText(e, "Could not update your wishlist."), "error");
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
      className="icon-btn h-9 w-9 disabled:opacity-60"
    >
      <svg
        key={saved ? "on" : "off"}
        aria-hidden="true"
        viewBox="0 0 24 24"
        className={`h-5 w-5 transition-colors ${saved ? "animate-heart-pop fill-rose-500 stroke-rose-500" : "fill-transparent stroke-current"} ${busy ? "opacity-50" : ""}`}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <path d="M12 21s-8-5.4-8-11a4.5 4.5 0 0 1 8-2.8A4.5 4.5 0 0 1 20 10c0 5.6-8 11-8 11z" />
      </svg>
    </button>
  );
}
