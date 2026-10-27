"use client";

import { toast } from "@/lib/toast";

/** Share a place with the phone's share sheet, or copy its link where there isn't one. */
export default function SharePlaceButton({ placeId, name, city }: { placeId: number; name: string; city?: string }) {
  async function share() {
    const url = `${window.location.origin}/place/${placeId}`;
    const text = city ? `${name}, ${city} on Nomads` : `${name} on Nomads`;
    try {
      if (navigator.share) {
        await navigator.share({ title: name, text, url });
        return;
      }
      await navigator.clipboard.writeText(url);
      toast("Link copied", "success");
    } catch (e: any) {
      if (e?.name === "AbortError") return; // closed the share sheet
      toast("Couldn't share. Copy the address from the browser instead.", "error");
    }
  }

  return (
    <button type="button" onClick={share} className="btn btn-secondary" aria-label={`Share ${name}`}>
      Share
    </button>
  );
}
