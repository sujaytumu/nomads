"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

import { searchPlaces } from "@/lib/placeSearchApi";
import { toast } from "@/lib/toast";

/** Opens a random good place (rated 4 and up) when you can't decide. */
export default function SurpriseMeButton() {
  const router = useRouter();
  const [busy, setBusy] = useState(false);

  async function go() {
    setBusy(true);
    try {
      const first = await searchPlaces({ minRating: 4, sort: "rating", page: 0, size: 1 });
      if (first.totalItems === 0) {
        toast("No places to suggest yet.");
        return;
      }
      const pick = Math.floor(Math.random() * first.totalItems);
      const r = pick === 0 ? first : await searchPlaces({ minRating: 4, sort: "rating", page: pick, size: 1 });
      const p = r.items[0];
      if (p) router.push(`/place/${p.id}`);
    } catch {
      toast("Couldn't pick one right now. Try again.", "error");
    } finally {
      setBusy(false);
    }
  }

  return (
    <button type="button" className="btn btn-secondary btn-sm" onClick={go} disabled={busy} data-loading={busy}>
      🎲 Surprise me
    </button>
  );
}
