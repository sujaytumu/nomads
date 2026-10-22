"use client";

import { useState } from "react";

/** Shares a read-only link to the itinerary (native share sheet on phones, copy on desktop). */
export default function ShareTripButton({ token, city, className = "btn btn-secondary btn-sm" }: { token?: string | null; city?: string | null; className?: string }) {
  const [msg, setMsg] = useState<string | null>(null);
  if (!token) return null;

  const share = async () => {
    const url = `${window.location.origin}/share/${token}`;
    try {
      if (navigator.share) {
        await navigator.share({ title: `My NOMAD trip${city ? ` to ${city}` : ""}`, url });
        return;
      }
      await navigator.clipboard.writeText(url);
      setMsg("Link copied");
    } catch (e: any) {
      if (e?.name === "AbortError") return;
      setMsg(url);
    }
    setTimeout(() => setMsg(null), 4000);
  };

  return (
    <span className="inline-flex flex-col items-start gap-1">
      <button type="button" className={className} onClick={share} data-done={msg === "Link copied" ? "true" : undefined}>{msg === "Link copied" ? "Copied ✓" : "Share"}</button>
      {msg && msg !== "Link copied" && <span role="status" className="max-w-[16rem] break-all text-xs text-green-300">{msg}</span>}
    </span>
  );
}
