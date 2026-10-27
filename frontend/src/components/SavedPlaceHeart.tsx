"use client";

import { useState } from "react";
import { savePlace, unsavePlace } from "@/lib/savedPlacesApi";
import { errorText, toast } from "@/lib/toast";

type Props = {
  placeId: number;
  saved: boolean;
  onChange?: (placeId: number, saved: boolean) => void;
  className?: string;
};

/** Heart for a place. Sits on top of cards, so it stops the click from opening the card's link. */
export default function SavedPlaceHeart({ placeId, saved, onChange, className = "" }: Props) {
  const [busy, setBusy] = useState(false);

  async function toggle(e: React.MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
    if (typeof window !== "undefined" && !localStorage.getItem("nomad_token")) {
      window.location.href = "/auth";
      return;
    }
    if (busy) return;
    setBusy(true);
    try {
      if (saved) await unsavePlace(placeId);
      else await savePlace(placeId);
      toast(saved ? "Removed from saved places" : "Saved place", "success");
      onChange?.(placeId, !saved);
    } catch (err: any) {
      toast(errorText(err, "Could not update saved places."), "error");
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
      aria-label={saved ? "Remove from saved places" : "Save place"}
      className={`grid h-9 w-9 place-items-center rounded-full bg-black/55 text-white ring-1 ring-white/10 backdrop-blur disabled:opacity-60 ${className}`}
    >
      <svg key={saved ? "on" : "off"} aria-hidden="true" viewBox="0 0 24 24" className={`h-5 w-5 ${saved ? "animate-heart-pop fill-rose-500 stroke-rose-500" : "fill-transparent stroke-current"}`} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M12 21s-8-5.4-8-11a4.5 4.5 0 0 1 8-2.8A4.5 4.5 0 0 1 20 10c0 5.6-8 11-8 11z" />
      </svg>
    </button>
  );
}
